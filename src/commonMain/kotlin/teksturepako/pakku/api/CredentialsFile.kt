package teksturepako.pakku.api

import com.github.michaelbull.result.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.CouldNotRead
import teksturepako.pakku.api.actions.errors.CouldNotSave
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.io.decodeToResult
import teksturepako.pakku.io.tryOrNull
import teksturepako.pakku.io.tryToResult
import teksturepako.pakku.io.writeToFile
import kotlin.io.path.*

@Serializable
data class CredentialsFile(
    @SerialName("curseforge_api_key") val curseForgeApiKey: String? = null,
    @SerialName("github_access_token") val gitHubAccessToken: String? = null
)
{
    companion object
    {
        private const val FILE_NAME = "credentials"
        private const val CONFIG_DIR = ".pakku"
        private val home = System.getenv("HOME") ?: System.getProperty("user.home")

        suspend fun readToResult(): Result<CredentialsFile, ActionError>
        {
            val path = home?.let { Path(it, CONFIG_DIR, FILE_NAME) }
                ?: return Err(CouldNotRead(Path(CONFIG_DIR, FILE_NAME).pathString))

            return decodeToResult<CredentialsFile>(path).onFailure { return Ok(CredentialsFile()) }
        }

        suspend fun update(
            updatedCurseForgeApiKey: String? = null,
            updatedGitHubAccessToken: String? = null,
        ): ActionError?
        {
            return readToResult().fold(
                success = { credentialsFile ->
                    val updatedCredentialsFile = credentialsFile.copy(
                        curseForgeApiKey = updatedCurseForgeApiKey ?: credentialsFile.curseForgeApiKey,
                        gitHubAccessToken = updatedGitHubAccessToken ?: credentialsFile.gitHubAccessToken,
                    )

                    updatedCredentialsFile.curseForgeApiKey?.let { apiKey ->
                        CurseForge.testApiKey(apiKey)?.onError { return it }
                    }

                    pakku {
                        curseForge(apiKey = updatedCredentialsFile.curseForgeApiKey)
                        gitHub(accessToken = updatedCredentialsFile.gitHubAccessToken)
                    }

                    updatedCredentialsFile.write()?.onError { return it }

                    null
                },
                failure = { it }
            )
        }

        suspend fun delete(): Boolean
        {
            val path = home?.let { Path(it, CONFIG_DIR, FILE_NAME) } ?: return false

            return path.tryToResult { deleteIfExists() }.get() ?: false
        }
    }

    suspend fun write(): ActionError?
    {
        val path = home?.let {
            val dir = Path(it, CONFIG_DIR)

            dir.tryOrNull { createDirectories() }

            Path(it, CONFIG_DIR, FILE_NAME)
        } ?: return CouldNotSave(Path(CONFIG_DIR, FILE_NAME))

        return writeToFile<CredentialsFile>(this, path.pathString, overrideText = true, format = json)
    }
}
