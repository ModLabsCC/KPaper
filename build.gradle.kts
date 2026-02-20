import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("jvm") version "2.1.20"
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    kotlin("plugin.serialization") version "2.1.20"
    id("maven-publish")
    id("org.sonarqube") version "7.0.1.6134"
}

group = "cc.modlabs"

version = System.getenv("VERSION_OVERRIDE") ?: Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
    "${get(Calendar.YEAR)}.${get(Calendar.MONTH) + 1}.${get(Calendar.DAY_OF_MONTH)}.${String.format("%02d%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))}"
}

sonar {
  properties {
    property("sonar.projectKey", "ModLabsCC_KPaper_b16df947-ed31-4251-96c3-810b8516f8cc")
    property("sonar.projectName", "KPaper")
  }
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

    api("dev.fruxz:stacked:2024.1.1")

    api("cc.modlabs:KlassicX:2025.12.4.1928")

    api("com.squareup.okhttp3:okhttp:4.12.0")

    // PacketEvents for TextDisplayFactory (display entities)
    api("com.github.retrooper:packetevents-spigot:2.11.3")

    // Redis client for Redis-backed PartyAPI implementation (Jedis)
    implementation("redis.clients:jedis:7.1.0")

    testImplementation("io.kotest:kotest-runner-junit5:$koTestVersion")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
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
        mavenLocal()
    }
    publications {
        create<MavenPublication>("maven") {

            from(components["java"])

            artifact(tasks.named("sourcesJar"))

            pom {
                name.set("KPaper")
                description.set("A utility library designed to simplify plugin development with Paper and Kotlin.")
                url.set("https://github.com/ModLabsCC/KPaper")
                licenses {
                    license {
                        name.set("GPL-3.0")
                        url.set("https://github.com/ModLabsCC/KPaper/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("ModLabsCC")
                        name.set("ModLabsCC")
                        email.set("contact@modlabs.cc")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/ModLabsCC/KPaper.git")
                    developerConnection.set("scm:git:git@github.com:ModLabsCC/KPaper.git")
                    url.set("https://github.com/ModLabsCC/KPaper")
                }
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
