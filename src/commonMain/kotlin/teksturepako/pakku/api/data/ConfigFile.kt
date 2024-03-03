@file:Suppress("unused")

package teksturepako.pakku.api.data

import kotlinx.serialization.Serializable
import teksturepako.pakku.api.overrides.Overrides
import teksturepako.pakku.io.decodeOrNew
import teksturepako.pakku.io.decodeToResult
import teksturepako.pakku.io.readFileOrNull
import teksturepako.pakku.io.writeToFile

@Serializable
data class ConfigFile(
    var name: String? = null,
    var version: String = "",
    val description: String? = null,
    val author: String? = null,
    val loaders: MutableMap<String, String> = mutableMapOf(),
    val overrides: MutableList<String> = mutableListOf(),
)
{
    // -- OVERRIDES --

    fun addOverride(override: String)
    {
        this.overrides.add(override)
    }

    fun addAllOverride(overrides: Collection<String>)
    {
        this.overrides.addAll(overrides)
    }

    fun removeOverride(override: String)
    {
        this.overrides.remove(override)
    }

    fun removeAllOverrides()
    {
        this.overrides.clear()
    }

    fun getAllOverrides(): List<String> = Overrides.filter(this.overrides)

    // -- FILE I/O --

    companion object
    {
        private const val FILE_NAME = "pakku.json"

        suspend fun exists(): Boolean = readFileOrNull(FILE_NAME) != null

        suspend fun readOrNew(): ConfigFile = decodeOrNew(ConfigFile(), FILE_NAME)

        /**
         * Reads [LockFile] and parses it, or returns an exception.
         * Use [Result.fold] to map it's [success][Result.success] or [failure][Result.failure] values.
         */
        suspend fun readToResult(): Result<ConfigFile> = decodeToResult(ConfigFile(), FILE_NAME)

        suspend fun readToResultFrom(path: String): Result<ConfigFile> = decodeToResult(ConfigFile(), path)
    }

    suspend fun write() = writeToFile(this, FILE_NAME, overrideText = true)
}