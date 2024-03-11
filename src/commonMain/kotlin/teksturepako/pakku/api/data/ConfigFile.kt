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
    private var name: String = "",
    private var version: String = "",
    private var description: String = "",
    private var author: String = "",
    private val overrides: MutableList<String> = mutableListOf(),
)
{
    // -- PACK --

    fun setName(name: String)
    {
        this.name = name
    }

    fun setVersion(version: String)
    {
        this.version = version
    }

    fun setDescription(description: String)
    {
        this.description = description
    }

    fun setAuthor(author: String)
    {
        this.author = author
    }

    fun getName() = this.name
    fun getVersion() = this.version
    fun getDescription() = this.description
    fun getAuthor() = this.author

    // -- OVERRIDES --

    fun addOverride(override: String)
    {
        this.overrides.add(override)
    }

    fun addAllOverrides(overrides: Collection<String>)
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
        suspend fun readToResult(): Result<ConfigFile> = decodeToResult(FILE_NAME)

        suspend fun readToResultFrom(path: String): Result<ConfigFile> = decodeToResult(path)
    }

    suspend fun write() = writeToFile(this, FILE_NAME, overrideText = true, format = jsonEncodeDefaults)
}