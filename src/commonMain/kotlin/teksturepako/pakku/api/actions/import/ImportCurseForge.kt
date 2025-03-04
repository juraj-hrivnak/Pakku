package teksturepako.pakku.api.actions.import

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.ModpackModel
import teksturepako.pakku.api.models.cf.CfModpackModel
import teksturepako.pakku.io.readPathTextFromZip
import teksturepako.pakku.io.readPathTextOrNull
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.pathString

fun String?.toCfModpackModel(): CfModpackModel? =
    this?.let { json.decodeFromString<CfModpackModel>(it) }

fun Path.isCfModpack(): Boolean = this.extension == CfModpackModel.EXTENSION || this.name == CfModpackModel.MANIFEST

suspend fun importCurseForge(path: Path): Result<ModpackModel, ActionError>
{
    if (!path.exists()) return Err(FileNotFound(path.pathString))

    return if (path.extension == CfModpackModel.EXTENSION)
    {
        val cfModpackModel = readPathTextFromZip(path, CfModpackModel.MANIFEST).toCfModpackModel()
            ?: return Err(FileNotFound(path.pathString))

        Ok(cfModpackModel)
    }
    else
    {
        val cfModpackModel = readPathTextOrNull(path).toCfModpackModel()
            ?: return Err(FileNotFound(path.pathString))

        Ok(cfModpackModel)
    }
}