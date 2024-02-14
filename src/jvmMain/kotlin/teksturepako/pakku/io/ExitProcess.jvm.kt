package teksturepako.pakku.io

actual fun exitProcess(statusCode: Int): Nothing = kotlin.system.exitProcess(statusCode)