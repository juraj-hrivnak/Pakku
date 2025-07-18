package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pSuccess

class CredentialsSet : CliktCommand("set")
{
    override fun help(context: Context) = "Set a credential"

    private val curseForgeApiKeyOpt: String? by option("--cf-api-key", "--curseforge-api-key")
        .help("the CurseForge API key credential")

    private val gitHubAccessTokenOpt: String? by option("--gh-access-token", "--github-access-token")
        .help("the GitHub API access token credential")

    override val printHelpOnEmptyArgs = true

    override fun run(): Unit = runBlocking {
        curseForgeApiKeyOpt?.let { curseForgeApiKey ->
            CredentialsFile.update(updatedCurseForgeApiKey = curseForgeApiKey)?.onError {
                terminal.pError(it)
                echo()

                return@let
            }

            terminal.pSuccess("CurseForge API key successfully configured.")
            echo()
        }

        gitHubAccessTokenOpt?.let { gitHubAccessToken ->
            CredentialsFile.update(updatedGitHubAccessToken = gitHubAccessToken)?.onError {
                terminal.pError(it)
                echo()

                return@let
            }

            terminal.pSuccess("GitHub API access token successfully configured.")
            echo()
        }
    }
}