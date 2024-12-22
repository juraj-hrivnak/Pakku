package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.testing.test
import com.github.michaelbull.result.runCatching
import kotlinx.serialization.json.*
import teksturepako.pakku.api.data.workingPath
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteRecursively
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class DiffTest
{
    init
    {
        workingPath = "build/test/diff"
        runCatching { Path("build/test/diff").deleteRecursively() }
        runCatching { Path("build/test/diff").createDirectory() }
    }

    private fun generateDiffTestCases()
    {
        val oldLockFileContent = """
        {"mc_versions":["1.14","1.21","1.16"],"loaders":{"neoforge":"21.1.64","forge":"14.23.5.2859","quilt":"0.26.3"},"projects":[{"pakku_id":"mVI8OcdAvlmoplT1","type":"MOD","slug":{"curseforge":"mekanism"},"name":{"curseforge":"Mekanism"},"id":{"curseforge":"268560"},"files":[{"type":"curseforge","file_name":"Mekanism-1.12.2-9.7.2.373.jar","mc_versions":["1.12.2"],"release_type":"release","url":"https://edge.forgecdn.net/files/2701/541/Mekanism-1.12.2-9.7.2.373.jar","id":"2701541","parent_id":"268560","hashes":{"sha1":"39795a17ed3e36c3719c668224df2935eb00e8de","md5":"0f7dea5a0278dc1c048b184ce86c73b1"},"required_dependencies":[],"size":7912094}]},{"pakku_id":"McxeYHDSbBIxuT1h","type":"MOD","slug":{"curseforge":"chisel"},"name":{"curseforge":"Chisel"},"id":{"curseforge":"235279"},"files":[{"type":"curseforge","file_name":"Chisel-MC1.12.2-0.2.1.35.jar","mc_versions":["1.12.1","1.12.2"],"release_type":"beta","url":"https://edge.forgecdn.net/files/2619/468/Chisel-MC1.12.2-0.2.1.35.jar","id":"2619468","parent_id":"235279","hashes":{"sha1":"1dda45074e17128451b3c8f66172bfaddf84f443","md5":"dbda5475859bcffc9f572c5a1506a1f4"},"required_dependencies":["267602"],"size":4310011}]},{"pakku_id":"p1FQx9ZuMcq81r0B","type":"MOD","slug":{"curseforge":"contenttweaker"},"name":{"curseforge":"ContentTweaker"},"id":{"curseforge":"237065"},"files":[{"type":"curseforge","file_name":"ContentTweaker-1.12.2-4.9.1.jar","mc_versions":["1.12.2"],"release_type":"release","url":"https://edge.forgecdn.net/files/2652/853/ContentTweaker-1.12.2-4.9.1.jar","id":"2652853","parent_id":"237065","hashes":{"sha1":"a6da81d9730274d468ee14a09ef33c7fdab6c160","md5":"a36b092d9ed0900b7143f4a14a12fba8"},"required_dependencies":["246996","239197"],"size":274895}]},{"pakku_id":"9w5zQeCOycVOIVbD","type":"MOD","slug":{"curseforge":"ender-io"},"name":{"curseforge":"Ender IO"},"id":{"curseforge":"64578"},"files":[{"type":"curseforge","file_name":"EnderIO-1.12.2-5.0.43.jar","mc_versions":["1.12.2"],"release_type":"release","url":"https://edge.forgecdn.net/files/2692/118/EnderIO-1.12.2-5.0.43.jar","id":"2692118","parent_id":"64578","hashes":{"sha1":"091a898644292b1eec56d1b6f697c87c2f669588","md5":"a08e6e9c81135fa77ac1df1f2f98c594"},"required_dependencies":["231868"],"size":7288125}]}],"lockfile_version":1}
        """.trimIndent()
        val oldLockFile = File("$workingPath/oldLockFile.json")
        oldLockFile.writeText(oldLockFileContent)

        val combinationsFile = File("src/commonTest/resources/diffTest/combinations.txt")

        combinationsFile.readLines().forEachIndexed { it, line ->
            val newLockFileContent = Json.parseToJsonElement(oldLockFileContent).jsonObject.toMutableMap()
            val newLockFile = File("$workingPath/${it + 1}.json")
            line.split(", ").forEach { singleCase ->
                // Add 1.7.10
                if (singleCase == "Game Added")
                {
                    val addedMCVersion = newLockFileContent["mc_versions"]!!.jsonArray + JsonPrimitive("1.7.10")
                    newLockFileContent["mc_versions"] = JsonArray(addedMCVersion)
                }
                // Remove 1.14
                if (singleCase == "Game Removed")
                {
                    val removedMCVersion =
                        newLockFileContent["mc_versions"]!!.jsonArray.filterNot { it.jsonPrimitive.content == "1.14" }
                    newLockFileContent["mc_versions"] = JsonArray(removedMCVersion)
                }
                // Add Fabric
                if (singleCase == "Loader Added")
                {
                    val loaders = newLockFileContent["loaders"]!!.jsonObject.toMutableMap()
                    loaders["fabric"] = JsonPrimitive("0.16.4")
                    newLockFileContent["loaders"] = JsonObject(loaders)
                }
                // Remove Forge
                if (singleCase == "Loader Removed")
                {
                    val loaders = newLockFileContent["loaders"]!!.jsonObject.toMutableMap()
                    loaders.remove("forge")
                    newLockFileContent["loaders"] = JsonObject(loaders)
                }
                // Update Neoforge
                if (singleCase == "Loader Updated")
                {
                    val loaders = newLockFileContent["loaders"]!!.jsonObject.toMutableMap()
                    loaders["neoforge"] = JsonPrimitive("21.1.65")
                    newLockFileContent["loaders"] = JsonObject(loaders)
                }
                // Add AE2
                if (singleCase == "Project Added")
                {
                    val newProject = Json.parseToJsonElement(
                        """
                        {"pakku_id":"404Wa3Gx9kPP5KFO","type":"MOD","slug":{"curseforge":"applied-energistics-2"},"name":{"curseforge":"Applied Energistics 2"},"id":{"curseforge":"223794"},"files":[{"type":"curseforge","file_name":"appliedenergistics2-rv6-stable-6.jar","mc_versions":["1.12.2"],"release_type":"release","url":"https://edge.forgecdn.net/files/2652/453/appliedenergistics2-rv6-stable-6.jar","id":"2652453","parent_id":"223794","hashes":{"sha1":"e5b66725610e84b35faac0514e49f9ee4b5beeb9","md5":"9c7b1b142d9676cec4f92b7e07ac3cd9"},"required_dependencies":[],"size":4062132}]}
                    """.trimIndent()
                    ).jsonObject
                    val projects = newLockFileContent["projects"]!!.jsonArray.toMutableList()
                    projects.add(newProject)
                    newLockFileContent["projects"] = JsonArray(projects)
                }
                // Remove Contenttweaker
                if (singleCase == "Project Removed")
                {
                    val projects = newLockFileContent["projects"]!!.jsonArray
                    val removedProjects = projects.filterNot { project ->
                        project.jsonObject["slug"]?.jsonObject?.get("curseforge")?.jsonPrimitive?.content == "contenttweaker"
                    }
                    newLockFileContent["projects"] = JsonArray(removedProjects)
                }
                // Update Mekanism
                if (singleCase == "Project Updated")
                {
                    val updatedProject = Json.parseToJsonElement(
                        """
                        {"pakku_id":"mVI8OcdAvlmoplT1","type":"MOD","slug":{"curseforge":"mekanism"},"name":{"curseforge":"Mekanism"},"id":{"curseforge":"268560"},"files":[{"type":"curseforge","file_name":"Mekanism-1.12.2-9.7.4.375.jar","mc_versions":["1.12.2"],"release_type":"release","url":"https://edge.forgecdn.net/files/2707/235/Mekanism-1.12.2-9.7.4.375.jar","id":"2707235","parent_id":"268560","hashes":{"sha1":"48fa3295b050d3f88934f9fbf8ac9972430b0aa7","md5":"8ba28800a7b27124125c5f62d8a1ba7d"},"required_dependencies":[],"size":7921690}]}
                    """.trimIndent()
                    ).jsonObject
                    val projects = newLockFileContent["projects"]!!.jsonArray.toMutableList()
                    val updatedProjects = projects.map {
                        if (it.jsonObject["slug"]?.jsonObject?.get("curseforge")?.jsonPrimitive?.content == "mekanism") updatedProject else it
                    }
                    newLockFileContent["projects"] = JsonArray(updatedProjects)
                }
            }
            newLockFile.writeText(JsonObject(newLockFileContent).toString())
        }
    }

    @Test
    fun `should success if newlines around verbose markdown headings are correct`()
    {
        val cmdArgs = "--markdown"
        val outputFileName = "markdown"
        testForNewlineHeadingsOnMarkdownOutput(cmdArgs, outputFileName)
    }

    @Test
    fun `should success if newlines around markdown headings are correct`()
    {
        val cmdArgs = "--verbose --markdown"
        val outputFileName = "markdown-verbose"
        testForNewlineHeadingsOnMarkdownOutput(cmdArgs, outputFileName)
    }

    private fun testForNewlineHeadingsOnMarkdownOutput(cmdArgs: String, outputFileName: String)
    {
        generateDiffTestCases()

        val cmd = Diff()

        val oldLockFile = File("$workingPath/oldLockFile.json")

        val numberOfTestCases = 255
        for (testNumber in 1..numberOfTestCases)
        {
            val newLockFile = "$workingPath/$testNumber.json"

            val outputFile = "$workingPath/$testNumber-$outputFileName.md"

            cmd.test("$oldLockFile $newLockFile $cmdArgs $outputFile")

            val contentText = File(outputFile).readText()

            /*
            * We need to check for 3 newlines since the entire content is one string
            * and there is only one beginning of the string.
            * Otherwise, ^\n\n could have been used.
            */
            // @formatter:off
            assertTrue(!Regex("\n\n\n").containsMatchIn(contentText),
                "Found two consecutive newlines in $outputFile\n" +
                        "File content:\n\"\"\"\n$contentText\n\"\"\"")

            // Checks for two empty lines at the end of the output file
            assertTrue(!Regex("\n\n$").containsMatchIn(contentText),
                "Found two empty lines at the end of $outputFile\n" +
                        "File content:\n\"\"\"\n$contentText\n\"\"\"")
            // @formatter:on

            val contentLines = contentText.lines()

            contentLines.forEachIndexed { it, line ->
                // Identify the line with the heading.
                // The headerSize = 0 option does not have to be checked since the logic for the header size does not modify newlines.
                // TODO: Invert this "if" once "continue" in lambdas is no longer unstable
                if (line.startsWith("#"))
                {
                    // Check the previous line for a newline
                    // it > 0 ignores the first line
                    if (it > 0)
                    {
                        val previousLine = contentLines[it - 1].trim()
                        // @formatter:off
                            assertTrue(previousLine.isEmpty(),
                                "Expected a newline before heading at line ${it + 1} in $outputFile\n" +
                                        "File content:\n\"\"\"\n$contentText\n\"\"\"")
                            // @formatter:on
                    }
                    // Check the next one for a newline
                    // index < content.size - 1 ignores last line
                    if (it < contentLines.size - 1)
                    {
                        val nextLine = contentLines[it + 1].trim()
                        // @formatter:off
                            assertTrue(nextLine.isEmpty(),
                                "Expected a newline after heading at line ${it + 1} in $outputFile\n" +
                                        "File content:\n\"\"\"\n$contentText\n\"\"\"")
                            // @formatter:on
                    }
                }
            }
        }
    }

    @Test
    fun `should success if newlines around markdown-diff is correct`()
    {
        val cmdArgs = "--markdown-diff"
        val outputFileName = "markdown-diff"
        testForNewlineHeadingsOnMarkdownDiffOutput(cmdArgs, outputFileName)
    }

    @Test
    fun `should success if newlines around verbose markdown-diff is correct`()
    {
        val cmdArgs = "--verbose --markdown-diff"
        val outputFileName = "markdown-diff-verbose"
        testForNewlineHeadingsOnMarkdownDiffOutput(cmdArgs, outputFileName)
    }

    private fun testForNewlineHeadingsOnMarkdownDiffOutput(cmdArgs: String, outputFileName: String)
    {
        generateDiffTestCases()

        val cmd = Diff()

        val oldLockFile = File("$workingPath/oldLockFile.json")

        val numberOfTestCases = 255
        for (testNumber in 1..numberOfTestCases)
        {
            val newLockFile = "$workingPath/$testNumber.json"

            val outputFile = "$workingPath/$testNumber-$outputFileName.md"

            cmd.test("$oldLockFile $newLockFile $cmdArgs $outputFile")

            val contentText = File(outputFile).readText()

            // Check that there is no empty line after ```diff
            // @formatter:off
                assertTrue(!Regex("```diff\n\n").containsMatchIn(contentText),
                    "Found empty line after header in $outputFile\n" +
                            "File content:\n\"\"\"\n$contentText\n\"\"\"")

                /*
                 * We need to check for 3 newlines since the entire content is one string
                 * and there is only one beginning of the string.
                 * Otherwise, ^\n\n could have been used.
                 */
                assertTrue(!Regex("\n\n\n").containsMatchIn(contentText),
                    "Found two consecutive newlines in $outputFile\n" +
                            "File content:\n\"\"\"\n$contentText\n\"\"\"")

                // Checks for two empty lines at the end of the output file
                assertTrue(!Regex("\n\n$").containsMatchIn(contentText),
                    "Found two empty lines at the end of $outputFile\n" +
                            "File content:\n\"\"\"\n$contentText\n\"\"\"")
                // @formatter:on
        }
    }

    @Test
    fun `should success if markdown diff matches`()
    {
        val cmdArgs = "--markdown"
        val outputFileName = "markdown"
        testIfDiffMatches(cmdArgs, outputFileName)
    }

    @Test
    fun `should success if verbose markdown diff matches`()
    {
        val cmdArgs = "--verbose --markdown"
        val outputFileName = "markdown-verbose"
        testIfDiffMatches(cmdArgs, outputFileName)
    }

    @Test
    fun `should success if markdown-diff diff matches`()
    {
        val cmdArgs = "--markdown-diff"
        val outputFileName = "markdown-diff"
        testIfDiffMatches(cmdArgs, outputFileName)
    }

    @Test
    fun `should success if verbose markdown-diff diff matches`()
    {
        val cmdArgs = "--verbose --markdown-diff"
        val outputFileName = "markdown-diff-verbose"
        testIfDiffMatches(cmdArgs, outputFileName)
    }

    // The primary purpose of this test is to make sure that Pakku correctly identifies which project/versions changed.
    // Formatting of all combinations is checked in the above tests.
    // The test can easily be expanded by increasing the test counter below and adding more expected result files to the diffMatch folder.
    private fun testIfDiffMatches(cmdArgs: String, outputFileName: String)
    {
        val cmd = Diff()

        val numberOfTestCases = 1
        for (testNumber in 1..numberOfTestCases)
        {
            val oldLockFile = File("src/commonTest/resources/diffTest/diffMatch/$testNumber-old.json")
            val newLockFile = File("src/commonTest/resources/diffTest/diffMatch/$testNumber-new.json")

            val outputFile = "$workingPath/$testNumber-$outputFileName-generated.json"
            val expectedOutputFile = "src/commonTest/resources/diffTest/diffMatch/$testNumber-$outputFileName-expected.md"

            cmd.test("$oldLockFile $newLockFile $cmdArgs $outputFile")

            assertEquals(
                actual = File(outputFile).readText(),
                expected = File(expectedOutputFile).readText(),
                message = "Test file ${File(outputFile)} does not match ${File(expectedOutputFile)}"
            )
        }
    }
}
