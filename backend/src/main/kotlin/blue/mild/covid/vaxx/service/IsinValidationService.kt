package blue.mild.covid.vaxx.service

import blue.mild.covid.vaxx.dto.config.IsinConfigurationDto
import blue.mild.covid.vaxx.dto.internal.IsinValidationResultDto
import blue.mild.covid.vaxx.dto.internal.IsinValidationResultStatus
import blue.mild.covid.vaxx.dto.request.PatientRegistrationDtoIn
import blue.mild.covid.vaxx.utils.normalizePersonalNumber
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.receive
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import mu.KLogging
import org.apache.http.ssl.SSLContextBuilder
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.util.Base64
import java.util.Locale

private const val URL_NAJDI_PACIENTA = "pacienti/VyhledatDleJmenoPrijmeniRc"

private const val REQUEST_TIMEOUT_MILLIS: Long = 15000

class IsinValidationService(
    private val configuration: IsinConfigurationDto
) {
    private val isinClient = client(configuration)

    private val userIdentification =
        "?pcz=${configuration.pracovnik.pcz}&pracovnikNrzpCislo=${configuration.pracovnik.nrzpCislo}"

    private companion object : KLogging()

    private enum class VyhledaniPacientaResult {
        PacientNalezen,
        NalezenoVicePacientu,
        PacientNebylNalezen,
        CizinecZaloz,
        ChybaVstupnichDat,
        Chyba
    }

    suspend fun validatePatientIsin(registrationDto: PatientRegistrationDtoIn): IsinValidationResultDto {
        val firstName = registrationDto.firstName.trim().uppercase(Locale.getDefault())
        val lastName = registrationDto.lastName.trim().uppercase(Locale.getDefault())
        val personalNumber = registrationDto.personalNumber.normalizePersonalNumber()

        val response = runCatching {
            getPatientResponse(
                jmeno = firstName,
                prijmeni = lastName,
                rodneCislo = personalNumber
            )
        }.getOrElse {
            logger.error(it) {
                "Getting data from isin server failed for patient ${firstName}/${lastName}/${personalNumber}"
            }
            return IsinValidationResultDto( status = IsinValidationResultStatus.WAS_NOT_VERIFIED )
        }

        val json = response.receive<JsonNode>()
        val result = json.get("vysledek")?.textValue()
        val resultMessage = json.get("vysledekZprava")?.textValue()
        val patientId = json.get("pacient")?.get("id")?.textValue()

        logger.debug {
            "Data from ISIN for patient ${firstName}/${lastName}/${personalNumber}: " +
            "result=${result}, resultMessage=${resultMessage}, patientId=${patientId}"
        }

        return when (result) {
            VyhledaniPacientaResult.PacientNalezen.name,
            VyhledaniPacientaResult.NalezenoVicePacientu.name ->
                IsinValidationResultDto(
                    status = IsinValidationResultStatus.PATIENT_FOUND,
                    patientId = patientId
                )
            VyhledaniPacientaResult.PacientNebylNalezen.name,
            VyhledaniPacientaResult.ChybaVstupnichDat.name ->
                IsinValidationResultDto(
                    status = IsinValidationResultStatus.PATIENT_NOT_FOUND
                )
            else ->
                IsinValidationResultDto(
                    status = IsinValidationResultStatus.WAS_NOT_VERIFIED
                )
        }
    }

    private suspend fun getPatientResponse(jmeno: String, prijmeni: String, rodneCislo: String): HttpResponse {
        val url = createIsinURL(URL_NAJDI_PACIENTA, parameters = listOf(jmeno, prijmeni, rodneCislo))
        return isinClient.get<HttpResponse>(url)
    }

    private fun createIsinURL(
        requestUrl: String,
        baseUrl: String = configuration.rootUrl,
        parameters: List<Any> = listOf(),
        includeIdentification: Boolean = true
    ): String {
        val parametersUrl = parameters.map { it.toString() }.joinToString(separator = "/")
        return "$baseUrl/$requestUrl/$parametersUrl${if (includeIdentification) userIdentification else ""}"
    }

    private fun client(
        config: IsinConfigurationDto
    ) =
        HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer()
            }

            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            }

            configureCertificates(config)
        }

    private fun HttpClientConfig<ApacheEngineConfig>.configureCertificates(config: IsinConfigurationDto) {
        engine {
            customizeClient {
                setSSLContext(
                    SSLContextBuilder
                        .create()
                        .loadKeyMaterial(readStore(config), config.keyPass.toCharArray())
                        .build()
                )
            }
        }
    }

    private fun readStore(config: IsinConfigurationDto): KeyStore? =
        runCatching {
            ByteArrayInputStream(Base64.getDecoder().decode(config.certBase64)).use {
                KeyStore.getInstance(config.storeType).apply {
                    load(it, config.storePass.toCharArray())
                }
            }
        }.onFailure {
            logger.error(it) { "It was not possible to load key store!" }
        }.onSuccess {
            logger.debug { "KeyStore loaded." }
        }.getOrNull()
}