package teksturepako.pakku.api.models

import kotlinx.serialization.Serializable

object CurseForgeResponse
{
    @Serializable
    data class SearchProject(val `data`: List<CfMod> = listOf())

    @Serializable
    data class GetProject(val `data`: CfMod)

    @Serializable
    data class GetMultipleProjects(val `data`: List<CfMod> = listOf())

    @Serializable
    data class GetFile(val `data`: CfMod.File)

    @Serializable
    data class GetMultipleFiles(val `data`: List<CfMod.File> = listOf())
}