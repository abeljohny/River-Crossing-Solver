plugins {
    kotlin("jvm") version "2.4.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

val mordantVersion = "3.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.mordant:mordant:${mordantVersion}")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(26)
}

tasks.test {
    useJUnitPlatform()
}
