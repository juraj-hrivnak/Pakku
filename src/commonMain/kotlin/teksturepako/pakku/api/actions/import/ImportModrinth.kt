package teksturepako.pakku.api.actions.import

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.ModpackModel
import teksturepako.pakku.api.models.mr.MrModpackModel
import teksturepako.pakku.io.readPathTextFromZip
import teksturepako.pakku.io.readPathTextOrNull
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.pathString

private fun String?.toMrModpackModel(): ModpackModel? =
    this?.let { json.decodeFromString<MrModpackModel>(it) }

fun Path.isMrModpack(): Boolean = this.extension == MrModpackModel.EXTENSION || this.name == MrModpackModel.MANIFEST

suspend fun importModrinth(path: Path): Result<ModpackModel, ActionError>
{
    if (!path.exists()) return Err(FileNotFound(path.pathString))

    return if (path.extension == MrModpackModel.EXTENSION)
    {
        val cfModpackModel = readPathTextFromZip(path, MrModpackModel.MANIFEST).toMrModpackModel()
            ?: return Err(FileNotFound(path.pathString))

        Ok(cfModpackModel)
    }
    else
    {
        val cfModpackModel = readPathTextOrNull(path).toMrModpackModel()
            ?: return Err(FileNotFound(path.pathString))

        Ok(cfModpackModel)
    }
}