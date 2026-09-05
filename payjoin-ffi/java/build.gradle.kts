plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("com.google.code.gson:gson:2.13.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    val libDir = layout.projectDirectory.dir("lib").asFile
    val native = listOf("libpayjoin_ffi.so", "libpayjoin_ffi.dylib", "payjoin_ffi.dll")
        .map { libDir.resolve(it) }
        .firstOrNull { it.exists() }
    if (native != null) {
        // FFM loader: absolute path uses System.load(), not java.library.path / JNA.
        systemProperty("uniffi.component.payjoin.libraryOverride", native.absolutePath)
    }
    systemProperty("java.library.path", libDir.absolutePath)
}
