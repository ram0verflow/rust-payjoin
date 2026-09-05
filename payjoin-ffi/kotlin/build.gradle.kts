plugins {
    kotlin("jvm") version "2.1.20"
}

repositories {
    mavenCentral()
}

dependencies {
    api("net.java.dev.jna:jna:5.17.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
    val libDir = layout.projectDirectory.dir("lib").asFile
    val native = listOf("libpayjoin_ffi.so", "libpayjoin_ffi.dylib", "payjoin_ffi.dll")
        .map { libDir.resolve(it) }
        .firstOrNull { it.exists() }
    if (native != null) {
        systemProperty("uniffi.component.payjoin.libraryOverride", native.absolutePath)
    }
    systemProperty("jna.library.path", libDir.absolutePath)
}
