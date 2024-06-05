package teksturepako.pakku.api.actions.import

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.CouldNotImport
import teksturepako.pakku.api.models.ModpackModel

suspend fun importModpackModel(
    path: String,
): Result<ModpackModel, ActionError>
{
    return when
    {
        path.isCfModpack() -> importCurseForge(path)
        path.isMrModpack() -> importModrinth(path)
        else               -> Err(CouldNotImport(path))
    }
}
