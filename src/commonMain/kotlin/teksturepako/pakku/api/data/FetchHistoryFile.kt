package teksturepako.pakku.api.data

import kotlinx.serialization.Serializable
import teksturepako.pakku.io.decodeOrNew
import teksturepako.pakku.io.writeToFile

@Serializable
data class FetchHistoryFile(
    var paths: MutableMap<String, String> = mutableMapOf(),
)
{
    companion object
    {
        const val FILE_NAME = "fetch-history.json"

        fun readOrNew(): FetchHistoryFile = decodeOrNew(FetchHistoryFile(), "$workingPath/${Dirs.PAKKU_DIR}/$FILE_NAME")
    }

    suspend fun write() = writeToFile(this, "$workingPath/${Dirs.PAKKU_DIR}/$FILE_NAME", overrideText = true, format = json)
}
