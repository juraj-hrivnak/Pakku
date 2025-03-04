package teksturepako.pakku.api.actions.remote

import teksturepako.pakku.api.actions.errors.ActionError

data class CanNotInstallRemote(val url: String? = null): ActionError()
{
    override val rawMessage = message(
        "Can not install or update remote", optionalArg(url),
        "A remote modpack can only be installed in directory with non-initialized Pakku dev environment.",
        newline = true,
    )
}
