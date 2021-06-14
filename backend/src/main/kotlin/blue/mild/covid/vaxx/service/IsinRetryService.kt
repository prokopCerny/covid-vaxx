package blue.mild.covid.vaxx.service

import blue.mild.covid.vaxx.dao.repository.PatientRepository
import blue.mild.covid.vaxx.dto.internal.PatientValidationResult
import blue.mild.covid.vaxx.dto.request.IsinJobDtoIn
import blue.mild.covid.vaxx.dto.response.IsinJobDtoOut
import blue.mild.covid.vaxx.dto.response.PatientDtoOut
import blue.mild.covid.vaxx.dto.response.toPatientVaccinationDetailDto
import mu.KLogging


class IsinRetryService(
    private val patientService: PatientService,
    private val patientValidation: PatientValidationService,
    private val isinService: IsinServiceInterface,
    private val dataCorrectnessService: DataCorrectnessService,
    private val patientRepository: PatientRepository,
    private val vaccinationService: VaccinationService,
) {

    private companion object : KLogging()

    suspend fun runIsinRetry(isinJobDto: IsinJobDtoIn): IsinJobDtoOut {
        val stats = IsinJobDtoOut()

        val patients = patientService.getPatientsByConjunctionOf(
            n = isinJobDto.patientsCount,
            offset = isinJobDto.patientsOffset.toLong()
        )

        for (patient in patients) {
            logger.debug("Checking ISIN id of patient ${patient.id}")

            // 1. If patient has personal number but ISIN id is not set -> try ISIN validation
            val isinId = if (!patient.isinId.isNullOrBlank()) {
                patient.isinId
            } else if (isinJobDto.validatePatients && !patient.personalNumber.isNullOrBlank() ) {
                logger.info("Patient ${patient.id} has personal number but no ISIN id. Validating in ISIN...")

                val newIsinPatientId = retryPatientValidation(patient)

                if (newIsinPatientId != null) {
                    stats.validatedPatientsSuccess++;
                } else {
                    stats.validatedPatientsErrors++;
                }
                newIsinPatientId
            } else {
                null
            }

            if (isinId.isNullOrBlank()) continue

            // 2. If data are correct but not exported to ISIN -> try export to isin
            logger.debug("Checking correctness exported to ISIN of patient ${patient.id}")
            if (
                isinJobDto.exportPatientsInfo &&
                patient.dataCorrect != null &&
                patient.dataCorrect.dataAreCorrect &&
                patient.dataCorrect.exportedToIsinOn == null
            ) {
                val wasExported = retryPatientContactInfoExport(patient)

                if (wasExported) {
                    stats.exportedPatientsInfoSuccess++
                } else {
                    stats.exportedPatientsInfoErrors++
                }
            }

            // 3. If vaccinated but not vaccination is not exported to ISIN -> try export vaccination to isin
            if (isinJobDto.exportVaccinations && patient.vaccinated != null && patient.vaccinated.exportedToIsinOn == null) {
                val wasExported = retryPatientVaccinationCreation(patient)

                if (wasExported) {
                    stats.exportedVaccinationsSuccess++
                } else {
                    stats.exportedVaccinationsErrors++
                }
            }
        }

        return stats
    }

    private suspend fun retryPatientValidation(patient: PatientDtoOut): String? {
        val patientValidationResult = patientValidation.validatePatient(
            firstName = patient.firstName,
            lastName = patient.lastName,
            personalNumber = patient.personalNumber ?: throw AssertionError { "Personal number cannot be null."},
        )

        logger.info {
            "Validation of patient ${patient.firstName}/${patient.lastName}/${patient.personalNumber} " +
                    "completed: status=${patientValidationResult.status}, isinPatientId=${patientValidationResult.patientId}."
        }

        val newIsinPatientId = when (patientValidationResult.status) {
            PatientValidationResult.PATIENT_FOUND ->
                patientValidationResult.patientId
            else -> {
                null
            }
        }

        if (newIsinPatientId != null) {
            logger.debug { "Updating ISIN id of patient ${patient.id} to $newIsinPatientId"}
            patientRepository.updatePatientChangeSet(id = patient.id, isinId = newIsinPatientId.trim())
        } else {
            logger.debug { "NOT updating ISIN id of patient ${patient.id}"}
        }
        return newIsinPatientId
    }

    private suspend fun retryPatientContactInfoExport(patient: PatientDtoOut): Boolean {
        if (patient.dataCorrect == null) {
            throw AssertionError { "Data correctness cannot be null." }
        }

        val wasExported = isinService.tryExportPatientContactInfo(patient, notes= patient.dataCorrect.notes)

        if (wasExported) {
            logger.debug { "Updating exported to ISIN of correctness ${patient.dataCorrect.id} (patient ${patient.id})"}
            dataCorrectnessService.exportedToIsin(patient.dataCorrect.id)
        } else {
            logger.debug { "NOT updating exported to ISIN of correctness ${patient.dataCorrect.id} (patient ${patient.id})"}
        }
        return wasExported
    }

    private suspend fun retryPatientVaccinationCreation(patient: PatientDtoOut): Boolean {
        if (patient.vaccinated == null) {
            throw AssertionError { "Vaccination cannot be null." }
        }

        val vaccination = vaccinationService.get(patient.vaccinated.id)

        // Try to export vaccination to ISIN
        val wasExported = isinService.tryCreateVaccinationAndDose(
            vaccination.toPatientVaccinationDetailDto(),
            patient = patient
        )

        if (wasExported) {
            logger.debug { "Updating exported to ISIN of vaccination ${patient.vaccinated.id} (patient ${patient.id})"}
            vaccinationService.exportedToIsin(patient.vaccinated.id)
        } else {
            logger.debug { "NOT updating exported to ISIN of vaccination ${patient.vaccinated.id} (patient ${patient.id})"}
        }
        return wasExported
    }
}