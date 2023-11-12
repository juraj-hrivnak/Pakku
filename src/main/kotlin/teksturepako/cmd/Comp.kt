package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import teksturepako.json.json
import teksturepako.platforms.CurseForge
import teksturepako.platforms.Modrinth

class Comp : CliktCommand() {
    private val mcVersion: String by argument()
    private val mods: List<String> by argument().multiple()

    override fun run() = runBlocking {
        mods.forEach { arg ->
            try
            {
                val response: JsonObject = json.decodeFromString(CurseForge.requestProjectString(arg)!!)
                val response2: JsonArray = json.decodeFromString(Modrinth.requestProjectString(arg)!!)

                echo("CurseForge: " +
                        json.encodeToString(
                            response["data"]!!
                                .jsonArray
                                .first().jsonObject["latestFilesIndexes"]!!
                                .jsonArray
                                .find { file ->
                                    mcVersion in file.jsonObject["gameVersion"].toString()
                                }!!
                                .jsonObject["filename"]
                        )
                )
                echo("Modrinth: " +
                        json.encodeToString(
                            response2.find { file ->
                                mcVersion in file.jsonObject["game_versions"].toString()
                            }!!
                                .jsonObject["files"]!!
                                .jsonArray
                                .first().jsonObject["filename"]
                        )
                )
            } catch (e: Exception) {
                echo("$arg not found")
            }
            echo()
        }
    }
}