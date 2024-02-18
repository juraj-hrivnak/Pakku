package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import teksturepako.pakku.VERSION

class Version : CliktCommand("Get pakku version")
{
    override fun run()
    {
        echo("Pakku version $VERSION")
        echo()
    }
}