plugins {
    application
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "8.1.1"   // ← nuevo
}

group = "com.senapp"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    val ktor = "2.3.11"

    // Ktor Server
    implementation("io.ktor:ktor-server-core-jvm:$ktor")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Base de datos
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.4")

    // Tests (opcionales)
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor")
}

application {
    // <-- Debe coincidir con tu clase main del servidor
    mainClass.set("com.senapp.AppKt")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("senapp-interpret")
    archiveClassifier.set("")
    archiveVersion.set("")
}

kotlin {
    jvmToolchain(17)
}
