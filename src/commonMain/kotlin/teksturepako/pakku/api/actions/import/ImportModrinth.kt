package teksturepako.pakku.api.actions.import

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.Error
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.ModpackModel
import teksturepako.pakku.io.readFileOrNull
import teksturepako.pakku.io.unzip
import java.io.File

private const val MR_EXTENSION = "mrpack"
private const val MR_MANIFEST = "modrinth.index.json"

private fun String?.toMrModpackModel(): ModpackModel?
{
    return this?.let { json.decodeFromString<ModpackModel.MrModpackModel>(it) }
}

fun String.isMrModpack(): Boolean = this.endsWith(MR_EXTENSION) || this == MR_MANIFEST

suspend fun importModrinth(path: String): Result<ModpackModel, Error>
{
    val file = File(path)

    if (!file.exists()) return Err(Error.FileNotFound(path))

    return when
    {
        file.extension == MR_EXTENSION  ->
        {
            val cfModpackModel = runCatching {
                unzip(path)[MR_MANIFEST].readString().toMrModpackModel()
            }.getOrNull() ?: return Err(Error.FileNotFound(path))

            Ok(cfModpackModel)
        }
        file.name == MR_MANIFEST ->
        {
            val cfModpackModel = readFileOrNull(path).toMrModpackModel()
                ?: return Err(Error.FileNotFound(path))

            Ok(cfModpackModel)
        }
        else -> Err(Error.CouldNotImport(path))
    }
}