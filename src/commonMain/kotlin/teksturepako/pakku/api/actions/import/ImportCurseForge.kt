package teksturepako.pakku.api.actions.import

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.FileNotFound
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.cf.CfModpackModel
import teksturepako.pakku.api.models.ModpackModel
import teksturepako.pakku.io.readFileOrNull
import teksturepako.pakku.io.unzip
import java.io.File

private const val CF_EXTENSION = "zip"
private const val CF_MANIFEST = "manifest.json"

private fun String?.toCfModpackModel(): ModpackModel? =
    this?.let { json.decodeFromString<CfModpackModel>(it) }

fun String.isCfModpack(): Boolean = this.endsWith(CF_EXTENSION) || this == CF_MANIFEST

suspend fun importCurseForge(path: String): Result<ModpackModel, ActionError>
{
    val file = File(path)

    if (!file.exists()) return Err(FileNotFound(path))

    return if (file.extension == CF_EXTENSION)
    {
        val cfModpackModel = runCatching {
            unzip(path)[CF_MANIFEST].readString().toCfModpackModel()
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