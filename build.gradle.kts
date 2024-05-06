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
    id("org.jetbrains.dokka") version libs.versions.kotlin
    id("io.ktor.plugin") version libs.versions.ktor
//    id("io.kotest.multiplatform") version libs.versions.kotest
    id("maven-publish")
}

group = "teksturepako.pakku"
version = "0.8.7"

val nativeEnabled = false

repositories {
    mavenCentral()
}

private val ktorVersion: String = libs.versions.ktor.get()
private val kotestVersion: String = libs.versions.kotest.get()

kotlin {
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
                // Kotlin
                implementation("org.jetbrains.kotlin:kotlin-stdlib")

                // Ktor
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

                // URL Encoding
                implementation("net.thauvin.erik.urlencoder:urlencoder-lib:1.4.0")

                // Kotlin Result
                implementation("com.michael-bull.kotlin-result:kotlin-result:2.0.0")

                // File I/O
                implementation("com.soywiz.korlibs.korio:korio:4.0.10")

                // CLI
                implementation("com.github.ajalt.clikt:clikt:4.2.1")

                // Cache
                implementation("io.github.reactivecircus.cache4k:cache4k:0.12.0")

                // Logging
                implementation("org.slf4j:slf4j-api:2.0.7")
                implementation("org.slf4j:slf4j-simple:2.0.7")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
//                implementation("io.kotest:kotest-framework-engine:$kotestVersion")
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

                // ZIP
                implementation("net.lingala.zip4j:zip4j:2.11.5")
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
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        val main by kotlin.jvm().compilations.getting
        manifest {
            attributes(
                "Main-Class" to "teksturepako.pakku.MainKt",
            )
        }
        from(main.runtimeDependencyFiles.files.filter { it.name.endsWith("jar") }.map { zipTree(it) })
    }
}

// -- VERSION --

private val sourceFile = File("$rootDir/resources/teksturepako/pakku/TemplateVersion.kt")
private val destFile = File("$rootDir/src/commonMain/kotlin/teksturepako/pakku/Version.kt")

tasks.register("generateVersion") {
    group = "build"

    inputs.file(sourceFile)
    outputs.file(destFile)

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
    val inputStream: InputStream = sourceFile.inputStream()

    destFile.printWriter().use { out ->
        inputStream.bufferedReader().forEachLine { inputLine ->
            val newLine = inputLine.replace("__VERSION", version.toString())
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
    publications.withType<MavenPublication> {
        artifactId = artifactId.lowercase()
        version = getPublishVersion()
        description = """A multiplatform modpack manager for Minecraft: Java Edition.
            | Create modpacks for CurseForge, Modrinth or both simultaneously.""".trimMargin()
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
