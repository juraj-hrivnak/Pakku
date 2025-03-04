package teksturepako.pakku.io

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue
import teksturepako.pakku.PakkuTest

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
            expectThat(filterPath(path))
                .get { isErr }
                .isTrue()
        }

        for (path in validPaths)
        {
            expectThat(filterPath(path))
                .get { isOk }
                .isTrue()
        }
    }
}