import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("jvm") version "2.4.10"
    `java-library`
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-SNAPSHOT"
    kotlin("plugin.serialization") version "2.4.10"
    id("maven-publish")
    id("org.sonarqube") version "7.3.1.8318"
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
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo-api.modlabs.cc/repo/maven/maven-mirror/")
}

// BCV pins ASM older than Java 25 class files (major 69); Paper 26.2 requires JVM 25.
configurations.matching { it.name.startsWith("bcv-rt-jvm") }.configureEach {
    resolutionStrategy {
        force(
            "org.ow2.asm:asm:9.10.1",
            "org.ow2.asm:asm-tree:9.10.1",
        )
    }
}

val minecraftVersion = project.property("minecraftVersion").toString()
val mockkVersion = "1.14.11"
val byteBuddyVersion = "1.18.11"

dependencies {
    paperweight.paperDevBundle(minecraftVersion)
    api("cc.modlabs:KlassicX:2026.3.30.1421")

    api("com.squareup.okhttp3:okhttp:5.4.0")

    // PacketEvents for TextDisplayFactory (display entities)
    api("com.github.retrooper:packetevents-spigot:2.13.0")

    // Redis client for Redis-backed PartyAPI implementation (Jedis)
    implementation("redis.clients:jedis:7.5.3")

    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testRuntimeOnly("net.bytebuddy:byte-buddy:$byteBuddyVersion")
    testRuntimeOnly("net.bytebuddy:byte-buddy-agent:$byteBuddyVersion")
    implementation("com.google.code.gson:gson:2.14.0")
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
            url = uri("https://repo-api.modlabs.cc/repo/maven/maven-public/")
            credentials {
                username = System.getenv("NEXUS_USER") ?: "modlabs"
                password = System.getenv("REPO_TOKEN")
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
        options.release.set(25)
    }

    withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
    }
}

kotlin {
    jvmToolchain(25)
}
