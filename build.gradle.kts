
private val kotlinVersion = "2.0.0-Beta3"
private val ktorVersion = "2.3.7"

plugins {
    kotlin("multiplatform") version "2.0.0-Beta3"
    kotlin("plugin.serialization") version "2.0.0-Beta3"
    id("io.ktor.plugin") version "2.3.7"
    application
}

group = "teksturepako.pakku"
version = "0.0.7"

repositories {
    mavenCentral()
}

kotlin {
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
            }
        }
    }

    val korlibsVersion = "4.0.10"

    sourceSets {
        val nativeMain by getting
        val nativeTest by getting
        val commonMain by getting {
            dependencies {
                // Kotlin
                implementation("org.jetbrains.kotlin:kotlin-stdlib")

                // Ktor
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-curl:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

                // File I/O
                api("com.soywiz.korlibs.korio:korio:$korlibsVersion")

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
                api(kotlin("test"))
            }
        }
    }
}

application {
    mainClass.set("teksturepako.pakku.MainKt")
}

//tasks.withType<Jar> {
//    // Otherwise you'll get a "No main manifest attribute" error
//    manifest {
//        attributes["Main-Class"] = "teksturepako.pakku.MainKt"
//    }
//
//    // To avoid the duplicate handling strategy error
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//
//    // To add all the dependencies
//    from(sourceSets.main.get().output)
//
//    dependsOn(configurations.runtimeClasspath)
//    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })
//}