@file:Suppress("UNUSED_VARIABLE", "KotlinRedundantDiagnosticSuppress")

import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.FileInputStream
import java.io.InputStream
import java.util.*

plugins {
    kotlin("multiplatform") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
    id("org.jetbrains.dokka") version libs.versions.dokka
    id("io.ktor.plugin") version libs.versions.ktor
    id("maven-publish")
}

group = "teksturepako.pakku"
version = "0.24.0"

val nativeEnabled = false

repositories {
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    mavenCentral()
}

private val ktorVersion: String = libs.versions.ktor.get()
private val kotestVersion: String = libs.versions.kotest.get()

kotlin {
    withSourcesJar(publish = false)

    if (nativeEnabled) {
        val hostOs = System.getProperty("os.name")
        val isArm64 = System.getProperty("os.arch") == "aarch64"
        val isMingwX64 = hostOs.startsWith("Windows")
        val nativeTarget = when {
            hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
            hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
            hostOs == "Linux" && isArm64 -> linuxArm64("native")
            hostOs == "Linux" && !isArm64 -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }

        nativeTarget.apply {
            binaries {
                executable {
                    entryPoint = "teksturepako.pakku.main"
                    baseName = "pakku"
                }
            }
        }
    }

    jvm()

    sourceSets {
        // -- COMMON --

        val commonMain by getting {
            dependencies {
                // Kotlin | Apache-2.0
                implementation("org.jetbrains.kotlin:kotlin-stdlib")

                // Ktor | Apache-2.0
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

                // Coroutines | Apache-2.0
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

                // Serialization | Apache-2.0
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

                // AtomicFU | Apache-2.0
                implementation("org.jetbrains.kotlinx:atomicfu:0.26.0")

                // Datetime | Apache-2.0
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

                // URL Encoding | Apache-2.0
                implementation("net.thauvin.erik.urlencoder:urlencoder-lib:1.6.0")

                // Kotlin Result | ISC
                implementation("com.michael-bull.kotlin-result:kotlin-result:2.0.0")

                // Clikt | Apache-2.0
                implementation("com.github.ajalt.clikt:clikt:5.0.1")
                implementation("com.github.ajalt.clikt:clikt-markdown:5.0.1")
                implementation("com.github.ajalt.mordant:mordant-coroutines:3.0.0")

                // SLF4J Logging | MIT
                implementation("org.slf4j:slf4j-api:2.0.7")
                implementation("org.slf4j:slf4j-simple:2.0.7")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.mockk:mockk:1.13.12")
            }
        }

        // -- NATIVE --

        if (nativeEnabled)
        {
            val nativeMain by getting {
                dependencies {
                    // Ktor
                    implementation("io.ktor:ktor-client-curl:$ktorVersion")
                }
            }

            val nativeTest by getting
        }

        // -- JVM --

        val jvmMain by getting {
            dependencies {
                // Ktor
                implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
            }
        }

        val jvmTest by getting
    }
}

// -- JVM --

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks.withType<Jar> {
    doFirst {
        val classifier = archiveClassifier.get().let { if (it.isNotBlank()) "-$it" else "" }

        archiveFileName.set("pakku$classifier.jar")

        manifest {
            attributes(
                "Main-Class" to "teksturepako.pakku.MainKt",
            )
        }

        if ("sources" !in classifier)
        {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            val main by kotlin.jvm().compilations.getting
            from(
                main.runtimeDependencyFiles.files
                    .filter { it.name.endsWith("jar") }
                    .map { zipTree(it) }
            )
        }
        else
        {
            // Don't bundle to sources jar
            exclude("**/CurseForgeApiKey.kt")
        }
    }
}

// -- VERSION --

private val versionSourceFile = File("$rootDir/resources/teksturepako/pakku/TemplateVersion.kt")
private val versionDestFile = File("$rootDir/src/commonMain/kotlin/teksturepako/pakku/Version.kt")

tasks.register("generateVersion") {
    group = "build"

    inputs.file(versionSourceFile)
    outputs.file(versionDestFile)

    doFirst {
        generateVersion()
    }
}

if (nativeEnabled)
{
    tasks.named("compileKotlinNative") {
        dependsOn("generateVersion")
    }
}

tasks.named("compileKotlinJvm") {
    dependsOn("generateVersion")
}

tasks.named("jvmSourcesJar") {
    dependsOn("generateVersion")
}

fun generateVersion() {
    val inputStream: InputStream = versionSourceFile.inputStream()

    versionDestFile.printWriter().use { out ->
        inputStream.bufferedReader().forEachLine { inputLine ->
            val newLine = inputLine.replace("__VERSION", version.toString())
            out.println(newLine)
        }
    }

    inputStream.close()
}

// -- API KEY --

private val apiKeySourceFile = File("$rootDir/resources/teksturepako/pakku/api/platforms/TemplateApiKey.kt")
private val apiKeyDestFile = File("$rootDir/src/commonMain/kotlin/teksturepako/pakku/api/platforms/CurseForgeApiKey.kt")

tasks.register("embedApiKey") {
    group = "build"

    inputs.file(apiKeySourceFile)
    outputs.file(apiKeyDestFile)

    doLast {
        embedApiKey()
    }
}

if (nativeEnabled)
{
    tasks.named("compileKotlinNative") {
        dependsOn("embedApiKey")
    }
}

tasks.named("compileKotlinJvm") {
    dependsOn("embedApiKey")
}

tasks.named("jvmSourcesJar") {
    dependsOn("embedApiKey")
}

fun embedApiKey() {
    val apiKey = System.getenv("CURSEFORGE_API_KEY") ?: ""

    val inputStream: InputStream = apiKeySourceFile.inputStream()

    apiKeyDestFile.printWriter().use { out ->
        inputStream.bufferedReader().forEachLine { inputLine ->
            val newLine = inputLine.replace("__CURSEFORGE_API_KEY", apiKey.replace("$", "\\$"))
            out.println(newLine)
        }
    }

    inputStream.close()
}

// -- PUBLISHING --

fun getPublishVersion(): String {
    // Call gradle with -PsnapshotVersion to set the version as a snapshot.
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

tasks.getByName("sourcesJar") {
    enabled = false
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
        withType<MavenPublication> {
            artifactId = artifactId.lowercase()
            version = getPublishVersion()
            description = """A multiplatform modpack manager for Minecraft: Java Edition.
            | Create modpacks for CurseForge, Modrinth or both simultaneously.""".trimMargin()
            artifact(tasks["jvmSourcesJar"])
        }
    }
}

// -- DIST --

application {
    mainClass.set("teksturepako.pakku.MainKt")
    applicationName = "Pakku"
}

distributions {
    main {
        contents {
            into("lib") {
                from(tasks["jvmJar"])
            }
        }
    }
}

tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
