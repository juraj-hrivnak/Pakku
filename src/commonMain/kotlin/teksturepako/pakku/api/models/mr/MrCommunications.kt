package teksturepako.pakku.api.models.mr

import kotlinx.serialization.Serializable

@Serializable
data class GetVersionsFromHashesRequest(val hashes: List<String>, val algorithm: String)