import java.util.Properties

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

// ---------------------------------------------------------------------------
// Id de producto por módulo, inyectado como constante de compilación.
//
// Los 24 ids viven juntos en licensing.properties (raíz del repo) para poder
// completarlos de una sola pasada, pero cada módulo compila el suyo: siendo un
// `static final String` queda inlineado en el bytecode, no en un archivo que el
// comprador pueda editar para validar un producto que no compró.
// ---------------------------------------------------------------------------
val licenseIdsFile = rootProject.layout.projectDirectory.file("licensing.properties")
val generatedLicenseDir = layout.buildDirectory.dir("generated/license")

// :api y :common son librerías internas que se embeben en los productos, no
// se venden por separado y por lo tanto no llevan id.
val internalModules = setOf("api", "common", "licensing")

val generateLicenseIdentity by tasks.registering {
    description = "Genera LicenseIdentity con el id de producto de este módulo."

    val moduleName = project.name
    val isInternal = moduleName in internalModules
    val input = licenseIdsFile
    val outputDir = generatedLicenseDir

    inputs.file(input)
    inputs.property("module", moduleName)
    outputs.dir(outputDir)

    doLast {
        if (isInternal) {
            return@doLast
        }

        val properties = Properties()
        input.asFile.inputStream().use { properties.load(it) }

        val resourceId = properties.getProperty(moduleName)
            ?: throw GradleException(
                "Falta '$moduleName' en licensing.properties — todo módulo vendible necesita su id de producto.",
            )

        val target = outputDir.get().asFile.resolve("com/sack/rpgroll/license/identity/LicenseIdentity.java")
        target.parentFile.mkdirs()
        target.writeText(
            """
            package com.sack.rpgroll.license.identity;

            /** Generado por Gradle desde licensing.properties — no editar a mano. */
            public final class LicenseIdentity {

                private LicenseIdentity() {
                }

                /** Id de "$moduleName" en el marketplace. */
                public static final String RESOURCE_ID = "$resourceId";

            }
            """.trimIndent() + "\n",
        )
    }
}

sourceSets.named("main") {
    java.srcDir(generatedLicenseDir)
}

tasks.named("compileJava") {
    dependsOn(generateLicenseIdentity)
}

tasks.test {
    useJUnitPlatform()

    // Byte Buddy (usado por Mockito) todavía no reconoce el bytecode de Java 25 como estable;
    // sin esto, cualquier mock() falla con "Java 25 (69) is not supported".
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}