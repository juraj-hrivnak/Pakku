package teksturepako.pakku.api.actions.import

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.FileNotFound
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.ModpackModel
import teksturepako.pakku.api.models.cf.CfModpackModel
import teksturepako.pakku.io.readFileOrNull
import teksturepako.pakku.io.unzip
import java.io.File

private fun String?.toCfModpackModel(): ModpackModel? =
    this?.let { json.decodeFromString<CfModpackModel>(it) }

fun String.isCfModpack(): Boolean = this.endsWith(CfModpackModel.EXTENSION) || this == CfModpackModel.MANIFEST

suspend fun importCurseForge(path: String): Result<ModpackModel, ActionError>
{
    val file = File(path)

    if (!file.exists()) return Err(FileNotFound(path))

    return if (file.extension == CfModpackModel.EXTENSION)
    {
        val cfModpackModel = runCatching {
            unzip(path)[CfModpackModel.MANIFEST].readString().toCfModpackModel()
        }.getOrNull() ?: return Err(FileNotFound(path))

        Ok(cfModpackModel)
    }
    else
    {
        val cfModpackModel = readFileOrNull(path).toCfModpackModel()
            ?: return Err(FileNotFound(path))

        Ok(cfModpackModel)
    }
}