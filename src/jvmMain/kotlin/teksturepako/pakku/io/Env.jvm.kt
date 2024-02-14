package teksturepako.pakku.io

actual fun getEnv(env: String): String? = runCatching { System.getenv(env) }.getOrNull()
