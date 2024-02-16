package teksturepako.pakku.io

import kotlin.system.exitProcess

actual fun exitPakku(statusCode: Int)
{
    exitProcess(statusCode)
}
