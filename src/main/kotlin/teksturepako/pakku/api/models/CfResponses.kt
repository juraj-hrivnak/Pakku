package teksturepako.pakku.api.models

import kotlinx.serialization.Serializable

/**
 * API route: `mods/search`
 *
 * API protocol: GET
 *
 * Accepts: Slug | Project's name | etc.
 */
@Serializable
data class SearchProjectResponse(val `data`: List<CfModModel> = listOf())

/**
 * API route: `mods/{projectId}`
 *
 * API protocol: GET
 *
 * Accepts: Project ID (CurseForge)
 */
@Serializable
data class GetProjectResponse(val `data`: CfModModel)

/**
 * API route: `mods/{projectId}/files/{fileId}`
 *
 * API protocol: GET
 *
 * Accepts: Project ID & File ID (CurseForge)
 */
@Serializable
data class GetFileResponse(val `data`: CfModModel.File)

/**
 * API route: `mods`
 *
 * API protocol: POST
 *
 * Accepts: List of Project IDs (CurseForge)
 *
 * Body parameter:
 * ```
 * {
 *   "modIds": [
 *     0
 *   ],
 *   "filterPcOnly": true
 * }
 */
@Serializable
data class GetMultipleProjectsResponse(val `data`: List<CfModModel> = listOf())

@Serializable
data class MultipleProjectsRequest(val modIds: List<Int>, val filterPcOnly: Boolean = true)

/**
 * API route: `mods/files`
 *
 * API protocol: POST
 *
 * Accepts: List of File IDs (CurseForge)
 *
 * Body parameter:
 * ```
 * {
 *   "fileIds": [
 *     0
 *   ]
 * }
 */
@Serializable
data class GetMultipleFilesResponse(val `data`: List<CfModModel.File> = listOf())

@Serializable
data class MultipleFilesRequest(val fileIds: List<Int>)