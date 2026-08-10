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
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.1.build.29-alpha")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
}