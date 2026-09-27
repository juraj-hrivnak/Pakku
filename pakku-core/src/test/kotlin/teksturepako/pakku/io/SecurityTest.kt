package teksturepako.pakku.io

import org.junit.Assume.assumeNoException
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.data.workingPath
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityTest : PakkuTest()
{
    private val invalidPaths = listOf(
        "/", "\\", "C:/", "C:\\", "..", "test_path/../", "test_path\\..\\", "/coconut/", "\\coconut\\"
    )
    private val validPaths = listOf(
        "./coconut/", "test_path/coconut/", "coconut", "1.20.x"
    )

    @Test
    fun `test path filter`()
    {
        for (path in invalidPaths)
        {
            assertTrue(filterPath(path).isErr)
        }

        for (path in validPaths)
        {
            assertTrue(filterPath(path).isOk)
        }
    }

    @Test
    fun `absolute filesystem paths are not unsafe solely for being absolute`()
    {
        val absoluteUnderWorkingPath = testPath("mods", "example.jar").toAbsolutePath()

        assertFalse(absoluteUnderWorkingPath.hasUnsafePathComponents())
    }

    @Test
    fun `absolute paths with parent segments are unsafe`()
    {
        val withParent = testPath("mods").resolve("..").resolve("outside.txt")

        assertTrue(withParent.hasUnsafePathComponents())
    }

    @Test
    fun `relative entry paths still reject absolute roots via filterPath`()
    {
        assertTrue(filterPath("/etc/passwd").isErr)
        assertTrue(filterPath("mods/example.jar").isOk)
        assertFalse(Path("mods/example.jar").hasUnsafePathComponents())
        assertTrue(Path("../escape").hasUnsafePathComponents())
    }

    @Test
    fun `isSafeFileName rejects traversal and separators`()
    {
        assertFalse("../../etc/passwd".isSafeFileName())
        assertFalse("mods/evil.jar".isSafeFileName())
        assertTrue("evil.jar".isSafeFileName())
        assertTrue("Greenery-1.12.2-7.0.jar".isSafeFileName())
    }

    @Test
    fun `streamed hash matches byte array hash`()
    {
        val bytes = ByteArray(32_000) { it.toByte() }

        assert(createHash("sha1", bytes.inputStream()) == createHash("sha1", bytes))
    }

    @Test
    fun `symlinked descendant outside base is out of bounds`()
    {
        val base = Path(workingPath).toAbsolutePath().normalize()
        val outside = base.parent
        val link = base.resolve("outside-link")

        createSymbolicLinkOrSkip(link, outside)

        assertFalse(link.resolve("escaped.txt").isWithinBounds(base))
    }

    @Test
    fun `symlinked descendant inside base remains in bounds`()
    {
        val base = Path(workingPath).toAbsolutePath().normalize()
        val target = base.resolve("target").createDirectories()
        val link = base.resolve("inside-link")

        createSymbolicLinkOrSkip(link, target)

        assertTrue(link.resolve("file.txt").isWithinBounds(base))
    }

    private fun createSymbolicLinkOrSkip(link: java.nio.file.Path, target: java.nio.file.Path)
    {
        try
        {
            Files.createSymbolicLink(link, target)
        }
        catch (e: Exception)
        {
            assumeNoException("Symbolic links are not available on this platform", e)
        }
    }
}
