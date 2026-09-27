import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.power.assert)
    alias(libs.plugins.dokka)
    `java-library`
    `maven-publish`
}

group = "teksturepako.pakku"
version = rootProject.version

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    functions = listOf(
        "kotlin.assert",
        "kotlin.test.assertTrue",
        "kotlin.test.assertFalse",
        "kotlin.test.assertNotNull",
        "kotlin.test.assertIs",
    )
}

dependencies {
    api(libs.kotlin.stdlib)

    api(libs.bundles.ktor.client)

    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.atomicfu)
    api(libs.kotlinx.datetime)

    api(libs.urlencoder)
    api(libs.kotlin.result)

    api(libs.bundles.clikt)
    api(libs.bundles.mordant)

    api(libs.bundles.slf4j)

    api(libs.jgit)
    api(libs.bundles.jna)

    api(libs.flexver)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

// -- VERSION --

private val versionSourceFile = layout.projectDirectory.file("resources/teksturepako/pakku/TemplateVersion.kt").asFile
private val versionDestFile = layout.projectDirectory.file("src/main/kotlin/teksturepako/pakku/Version.kt").asFile

tasks.register("generateVersion") {
    group = "build"

    inputs.file(versionSourceFile)
    inputs.property("projectVersion", version)
    outputs.file(versionDestFile)

    doLast {
        versionDestFile.parentFile.mkdirs()
        versionDestFile.writeText(
            versionSourceFile.readText().replace("__VERSION", version.toString())
        )
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateVersion")
}

tasks.named("sourcesJar") {
    dependsOn("generateVersion")
}

// -- PUBLISHING --

fun getPublishVersion(): String {
    if (!project.hasProperty("snapshotVersion")) return version.toString()
    val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: "0"
    return "$version.$buildNumber-SNAPSHOT"
}

/**
 * Create `github.properties` in root project folder file with
 * `gpr.usr=GITHUB_USER_ID` & `gpr.key=PERSONAL_ACCESS_TOKEN`
 **/
val githubProperties: Properties = Properties().apply {
    val properties = runCatching { FileInputStream(rootProject.file("github.properties")) }
    properties.onSuccess { load(it) }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/juraj-hrivnak/Pakku")
            credentials {
                username = githubProperties["gpr.usr"] as String? ?: System.getenv("GITHUB_ACTOR")
                password = githubProperties["gpr.key"] as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "pakku"
            version = getPublishVersion()
            description = """A multiplatform modpack manager for Minecraft: Java Edition.
            | Create modpacks for CurseForge, Modrinth or both simultaneously.""".trimMargin()
        }
    }
}
