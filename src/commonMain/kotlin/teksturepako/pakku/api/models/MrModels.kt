package teksturepako.pakku.api.models

import kotlinx.serialization.Serializable

@Serializable
data class GetVersionsFromHashesRequest(val hashes: List<String>, val algorithm: String)