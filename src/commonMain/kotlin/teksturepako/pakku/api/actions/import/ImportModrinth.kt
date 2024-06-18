package teksturepako.pakku.api.actions.import

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.FileNotFound
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.ModpackModel
import teksturepako.pakku.api.models.mr.MrModpackModel
import teksturepako.pakku.io.readFileOrNull
import teksturepako.pakku.io.unzip
import java.io.File

private fun String?.toMrModpackModel(): ModpackModel? =
    this?.let { json.decodeFromString<MrModpackModel>(it) }

fun String.isMrModpack(): Boolean = this.endsWith(MrModpackModel.EXTENSION) || this == MrModpackModel.MANIFEST

suspend fun importModrinth(path: String): Result<ModpackModel, ActionError>
{
    val file = File(path)

    if (!file.exists()) return Err(FileNotFound(path))

    return if (file.extension == MrModpackModel.EXTENSION)
    {
        val cfModpackModel = runCatching {
            unzip(path)[MrModpackModel.MANIFEST].readString().toMrModpackModel()
        }.getOrNull() ?: return Err(FileNotFound(path))

        Ok(cfModpackModel)
    }
    else
    {
        val cfModpackModel = readFileOrNull(path).toMrModpackModel()
            ?: return Err(FileNotFound(path))

        Ok(cfModpackModel)
    }
}