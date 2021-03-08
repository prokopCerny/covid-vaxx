package blue.mild.covid.vaxx.setup

enum class EnvVariables {
    RELEASE_FILE_PATH,

    POSTGRES_DB,
    POSTGRES_HOST,
    POSTGRES_USER,
    POSTGRES_PASSWORD,

    MAIL_JET_API_SECRET,
    MAIL_JET_API_KEY,
    MAIL_ADDRESS_FROM,
    NAME_FROM,

    FRONTEND_PATH,

    JWT_SIGNING_SECRET,
    JWT_EXPIRATION_REGISTERED_USER_MINUTES,
    JWT_EXPIRATION_PATIENT_MINUTES,

    RATE_LIMIT,
    RATE_LIMIT_DURATION_MINUTES,

    ENABLE_SWAGGER,

    CORS_ALLOWED_HOSTS,
    ENABLE_CORS,

    PORT,

    JSON_LOGGING,
    GLOBAL_LOG_LEVEL,
    LOG_LEVEL,
    LOG_PATH
}
