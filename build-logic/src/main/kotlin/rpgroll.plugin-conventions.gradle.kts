plugins {
    java
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/") // ProtocolLib
    maven("https://jitpack.io/")                            // Vault, DecentHolograms
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
    maven("https://maven.enginehub.org/repo/") // WorldEdit (import de schematics en Dungeons)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.1.build.29-alpha")

    testImplementation("io.papermc.paper:paper-api:26.1.1.build.29-alpha")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.15.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.15.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks.test {
    useJUnitPlatform()

    // Byte Buddy (usado por Mockito) todavía no reconoce el bytecode de Java 25 como estable;
    // sin esto, cualquier mock() falla con "Java 25 (69) is not supported".
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}