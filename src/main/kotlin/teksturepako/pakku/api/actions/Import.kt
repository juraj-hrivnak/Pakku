package teksturepako.pakku.api.actions

import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.CfModpackModel
import teksturepako.pakku.io.readFileOrNull
import java.io.File

suspend fun importCfManifest(path: String): CfModpackModel? =
    readFileOrNull(File(path))?.let { json.decodeFromString<CfModpackModel>(it) }