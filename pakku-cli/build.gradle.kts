import org.gradle.jvm.application.scripts.TemplateBasedScriptGenerator
import org.gradle.jvm.application.tasks.CreateStartScripts
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "teksturepako.pakku"
version = rootProject.version

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

val pakkuMainClass = "teksturepako.pakku.MainKt"
val pakkuJarName = "pakku.jar"

application {
    mainClass.set(pakkuMainClass)
    applicationName = "Pakku"
}

dependencies {
    implementation(project(":pakku-core"))
}

tasks.jar {
    archiveFileName.set(pakkuJarName)
    manifest {
        attributes(
            "Main-Class" to pakkuMainClass,
            "Enable-Native-Access" to "ALL-UNNAMED", // Fix warning for `java -jar` runs
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Fix "Invalid signature file"
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}

// Launch the executable fat JAR so Enable-Native-Access from the manifest applies.
tasks.named<CreateStartScripts>("startScripts") {
    classpath = files(tasks.jar)
    (unixStartScriptGenerator as TemplateBasedScriptGenerator).template =
        resources.text.fromFile("gradle/unixStartScript.txt")
    (windowsStartScriptGenerator as TemplateBasedScriptGenerator).template =
        resources.text.fromFile("gradle/windowsStartScript.txt")
}

tasks.named<JavaExec>("run") {
    if (JavaVersion.current().majorVersion.toInt() >= 22) {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}

distributions {
    main {
        contents {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            into("lib") {
                from(tasks.jar)
            }
        }
    }
}

tasks.withType<Tar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
