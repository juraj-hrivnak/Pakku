package teksturepako.pakku.api.actions

import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.CfModpack
import teksturepako.pakku.io.readFileOrNull
import java.io.File

suspend fun importCfManifest(path: String): CfModpack? =
    readFileOrNull(File(path))?.let { json.decodeFromString<CfModpack>(it) }