package teksturepako.pakku.io

expect suspend fun zipFile(
    outputFileName: String,
    extension: String,
    overrides: List<String>,
    vararg create: Pair<String, Any>
): Result<String>