import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("jvm") version "2.0.21"
    id("io.papermc.paperweight.userdev") version "2.0.0-SNAPSHOT"
    kotlin("plugin.serialization") version "2.1.0"
    id("maven-publish")
}

group = "cc.modlabs"

version = System.getenv("VERSION_OVERRIDE") ?: Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
    "${get(Calendar.YEAR)}.${get(Calendar.MONTH) + 1}.${get(Calendar.DAY_OF_MONTH)}.${String.format("%02d%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))}"
}

repositories {
    mavenCentral()
    maven("https://nexus.modlabs.cc/repository/maven-mirrors/")
}

val minecraftVersion: String by project
val koTestVersion = "6.0.0.M1"
val mockkVersion = "1.13.16"

dependencies {
    paperweight.paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")

    api("dev.fruxz:stacked:2024.1.1") // TODO: Own implementation
    api("dev.fruxz:ascend:2024.2.2") // TODO: Own implementation

    api("cc.modlabs:KlassicX:2025.2.27.1916")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("io.kotest:kotest-runner-junit5:$koTestVersion")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev
        .ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

tasks.register<Jar>("sourcesJar") {
    description = "Generates the sources jar for this project."
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "ModLabs"
            url = uri("https://nexus.modlabs.cc/repository/maven-public/")
            credentials {
                username = System.getenv("NEXUS_USER")
                password = System.getenv("NEXUS_PASS")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {

            artifact(tasks.named("jar").get()) {
                classifier = null
            }

            artifact(tasks.named("sourcesJar"))

            pom {
                name.set("KPaper")
                description.set("A utility library designed to simplify plugin development with Paper and Kotlin.")
            }
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }
}

kotlin {
    jvmToolchain(21)
}