plugins {
    kotlin("jvm") version "2.1.20"
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":kotlin"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
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
    // Only this project's tests. `:kotlin:test` is the official Kotlin suite.
    val libDir = layout.projectDirectory.dir("../kotlin/lib").asFile
    val native = listOf("libpayjoin_ffi.so", "libpayjoin_ffi.dylib", "payjoin_ffi.dll")
        .map { libDir.resolve(it) }
        .firstOrNull { it.exists() }
    if (native != null) {
        systemProperty("uniffi.component.payjoin.libraryOverride", native.absolutePath)
    }
    systemProperty("jna.library.path", libDir.absolutePath)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}
