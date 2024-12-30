import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21"
    id("io.papermc.paperweight.userdev") version "2.0.0-SNAPSHOT"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "de.joker.kpaper"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://nexus.modlabs.cc/repository/maven-mirrors/")
}

val minecraftVersion = "1.21.4"

dependencies {
    paperweight.paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")

    api("dev.fruxz:stacked:2024.1.1") // TODO: Own implementation
    api("dev.fruxz:ascend:2024.2.2") // TODO: Own implementation
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev
        .ReobfArtifactConfiguration.MOJANG_PRODUCTION
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