package teksturepako.pakku.api.actions

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.CfModpackModel
import teksturepako.pakku.api.models.MrModpackModel
import teksturepako.pakku.io.readFileOrNull
import java.io.File

suspend fun importCfManifest(path: String): CfModpackModel? =
    readFileOrNull(File(path))?.let { json.decodeFromString<CfModpackModel>(it) }

suspend fun importMrManifest(path: String): MrModpackModel? =
    readFileOrNull(File(path))?.let { json.decodeFromString<MrModpackModel>(it) }

fun importMrPack(path: Path): MrModpackModel? =
    runCatching { FileSystem.SYSTEM.openZip(path).read("modrinth.index.json".toPath()) { readUtf8() } }
        .getOrNull()?.let { json.decodeFromString<MrModpackModel>(it) }