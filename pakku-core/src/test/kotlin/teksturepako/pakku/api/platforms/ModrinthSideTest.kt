package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.projects.ProjectSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModrinthSideTest
{
    @Test fun `client_only maps to CLIENT`()
    {
        assertEquals(ProjectSide.CLIENT, Modrinth.getProjectSide(listOf("client_only"), "required", "unsupported"))
    }

    @Test fun `server_only maps to SERVER`()
    {
        assertEquals(ProjectSide.SERVER, Modrinth.getProjectSide(listOf("server_only"), "optional", "required"))
    }

    @Test fun `dedicated_server_only maps to SERVER`()
    {
        assertEquals(
            ProjectSide.SERVER,
            Modrinth.getProjectSide(listOf("dedicated_server_only"), "optional", "required")
        )
    }

    @Test fun `client_and_server maps to BOTH`()
    {
        assertEquals(ProjectSide.BOTH, Modrinth.getProjectSide(listOf("client_and_server"), "required", "required"))
    }

    @Test fun `server_only_client_optional maps to BOTH`()
    {
        assertEquals(
            ProjectSide.BOTH,
            Modrinth.getProjectSide(listOf("server_only_client_optional"), "optional", "required")
        )
    }

    @Test fun `client_only_server_optional maps to BOTH`()
    {
        assertEquals(
            ProjectSide.BOTH,
            Modrinth.getProjectSide(listOf("client_only_server_optional"), "required", "optional")
        )
    }

    @Test fun `client_or_server_prefers_both maps to BOTH`()
    {
        assertEquals(
            ProjectSide.BOTH,
            Modrinth.getProjectSide(listOf("client_or_server_prefers_both"), "optional", "optional")
        )
    }

    @Test fun `client_or_server maps to BOTH`()
    {
        assertEquals(ProjectSide.BOTH, Modrinth.getProjectSide(listOf("client_or_server"), "optional", "optional"))
    }

    @Test fun `singleplayer_only maps to CLIENT`()
    {
        assertEquals(
            ProjectSide.CLIENT,
            Modrinth.getProjectSide(listOf("singleplayer_only"), "required", "unsupported")
        )
    }

    @Test fun `union of client_only and client_and_server maps to BOTH`()
    {
        assertEquals(
            ProjectSide.BOTH,
            Modrinth.getProjectSide(listOf("client_only", "client_and_server"), "required", "optional")
        )
    }

    @Test fun `union of server_only and client_and_server maps to BOTH`()
    {
        assertEquals(
            ProjectSide.BOTH,
            Modrinth.getProjectSide(listOf("server_only", "client_and_server"), "optional", "required")
        )
    }

    @Test fun `unknown-only environment falls back to legacy fields - both required`()
    {
        assertEquals(
            ProjectSide.BOTH, Modrinth.getProjectSide(listOf("unknown"), "required", "required")
        )
    }

    @Test fun `unknown-only environment falls back to legacy fields - client required`()
    {
        assertEquals(
            ProjectSide.CLIENT, Modrinth.getProjectSide(listOf("unknown"), "required", "unsupported")
        )
    }

    @Test fun `unknown-only environment with both unknown falls back to null`()
    {
        assertNull(
            Modrinth.getProjectSide(listOf("unknown"), "unknown", "unknown")
        )
    }

    @Test fun `empty environment falls back to legacy fields`()
    {
        assertEquals(ProjectSide.BOTH, Modrinth.getProjectSide(listOf(), "required", "required"))
    }

    @Test fun `legacy required_required maps to BOTH`()
    {
        assertEquals(ProjectSide.BOTH, Modrinth.getProjectSide(listOf(), "required", "required"))
    }

    @Test fun `legacy required_optional maps to BOTH`()
    {
        assertEquals(ProjectSide.BOTH, Modrinth.getProjectSide(listOf(), "required", "optional"))
    }

    @Test fun `legacy required_unsupported maps to CLIENT`()
    {
        assertEquals(ProjectSide.CLIENT, Modrinth.getProjectSide(listOf(), "required", "unsupported"))
    }

    @Test fun `legacy optional_required maps to BOTH`()
    {
        assertEquals(ProjectSide.BOTH, Modrinth.getProjectSide(listOf(), "optional", "required"))
    }

    @Test fun `legacy optional_optional maps to BOTH`()
    {
        assertEquals(ProjectSide.BOTH, Modrinth.getProjectSide(listOf(), "optional", "optional"))
    }

    @Test fun `legacy optional_unsupported maps to CLIENT`()
    {
        assertEquals(ProjectSide.CLIENT, Modrinth.getProjectSide(listOf(), "optional", "unsupported"))
    }

    @Test fun `legacy unsupported_required maps to SERVER`()
    {
        assertEquals(ProjectSide.SERVER, Modrinth.getProjectSide(listOf(), "unsupported", "required"))
    }

    @Test fun `legacy unsupported_optional maps to SERVER`()
    {
        assertEquals(ProjectSide.SERVER, Modrinth.getProjectSide(listOf(), "unsupported", "optional"))
    }

    @Test fun `legacy unsupported_unsupported maps to null`()
    {
        assertNull(Modrinth.getProjectSide(listOf(), "unsupported", "unsupported"))
    }

    @Test fun `legacy unknown_unknown maps to null`()
    {
        assertNull(Modrinth.getProjectSide(listOf(), "unknown", "unknown"))
    }

    @Test fun `legacy unknown_required maps to SERVER`()
    {
        assertEquals(ProjectSide.SERVER, Modrinth.getProjectSide(listOf(), "unknown", "required"))
    }

    @Test fun `legacy required_unknown maps to CLIENT`()
    {
        assertEquals(ProjectSide.CLIENT, Modrinth.getProjectSide(listOf(), "required", "unknown"))
    }

    @Test fun `GeckoLib - client required, server optional maps to BOTH`()
    {
        // GeckoLib: client_side="required", server_side="optional"
        // Previously wrongly returned CLIENT
        assertEquals(
            ProjectSide.BOTH, Modrinth.getProjectSide(listOf("client_only_server_optional"), "required", "optional")
        )
        // Also test legacy fallback path
        assertEquals(
            ProjectSide.BOTH, Modrinth.getProjectSide(listOf(), "required", "optional")
        )
    }

    @Test fun `Create - client optional, server required maps to BOTH`()
    {
        // Create: client_side="optional", server_side="required"
        // Previously wrongly returned SERVER
        assertEquals(
            ProjectSide.BOTH, Modrinth.getProjectSide(listOf("server_only_client_optional"), "optional", "required")
        )
        // Also test legacy fallback path
        assertEquals(
            ProjectSide.BOTH, Modrinth.getProjectSide(listOf(), "optional", "required")
        )
    }

    @Test fun `ModMenu - client required, server unsupported maps to CLIENT`()
    {
        // ModMenu: client_side="required", server_side="unsupported"
        assertEquals(
            ProjectSide.CLIENT, Modrinth.getProjectSide(listOf("client_only"), "required", "unsupported")
        )
        // Also test legacy fallback path
        assertEquals(
            ProjectSide.CLIENT, Modrinth.getProjectSide(listOf(), "required", "unsupported")
        )
    }

    @Test fun `Dynmap - unknown_unknown with server_only environment maps to SERVER`()
    {
        // Dynmap: client_side="unknown", server_side="unknown"
        // environment: ["server_only", "dedicated_server_only", "unknown"]
        assertEquals(
            ProjectSide.SERVER,
            Modrinth.getProjectSide(listOf("server_only", "dedicated_server_only", "unknown"), "unknown", "unknown")
        )
    }

    @Test fun `AppleSkin - optional_optional maps to BOTH`()
    {
        // AppleSkin: client_side="optional", server_side="optional"
        assertEquals(
            ProjectSide.BOTH, Modrinth.getProjectSide(listOf("client_or_server_prefers_both"), "optional", "optional")
        )
        // Also test legacy fallback path
        assertEquals(
            ProjectSide.BOTH, Modrinth.getProjectSide(listOf(), "optional", "optional")
        )
    }
}
