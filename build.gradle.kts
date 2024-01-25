
private val kotlinVersion = "1.9.22"
private val ktorVersion = "2.3.7"

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
    application
}

group = "teksturepako.pakku"
version = "0.0.7"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    implementation("com.soywiz.korge:korge-core:5.3.0")

    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    implementation("io.github.reactivecircus.cache4k:cache4k:0.12.0")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.slf4j:slf4j-simple:2.0.7")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("teksturepako.pakku.MainKt")
}

tasks.withType<Jar> {
    // Otherwise you'll get a "No main manifest attribute" error
    manifest {
        attributes["Main-Class"] = "teksturepako.pakku.MainKt"
    }

    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all the dependencies
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })
}