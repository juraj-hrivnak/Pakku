package teksturepako.pakku.io

fun getEnvOrNull(env: String): String? = runCatching { System.getenv(env) }.getOrNull()
