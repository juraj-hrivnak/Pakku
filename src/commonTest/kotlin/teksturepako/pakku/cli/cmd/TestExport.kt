package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.testing.test
import kotlin.test.Test
import kotlin.test.assertContains

class TestExport
{
    @Test
    fun testCmd()
    {
        val cmd = Export()
        val output = cmd.test().output

        assertContains(output, "Could not read './pakku-lock.json")
    }
}