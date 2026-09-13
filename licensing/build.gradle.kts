plugins {
    // A propósito NO usa "rpgroll.addon-conventions": este módulo no depende de
    // :api ni :common. Es lo que permite que SackResourcePack —independiente de
    // RPGRoll— verifique su licencia sin arrastrar el resto del ecosistema, y
    // evita el ciclo :common -> :sackresourcepack -> :licensing.
    id("rpgroll.plugin-conventions")
}

group = "com.sack"
version = "1.0.0"

dependencies {
    // Gson ya viene embebido en el server de Paper en runtime.
    compileOnly("com.google.code.gson:gson:2.11.0")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

// ----------------------------------------------------------------------------
// Compilación de desarrollo.
//
// `-Drpgroll.devmode=true` salta la verificación de licencia para probar en un
// servidor local. Antes bastaba con esa propiedad en el comando de arranque —
// es decir, cualquier comprador (o cualquier copia pirata) podía apagar la
// licencia de los 24 plugins añadiendo un flag a su start.bat.
//
// Ahora la propiedad solo tiene efecto si el jar se compiló con
// `-Prpgroll.dev=true`. En un jar normal la constante es `false`, el compilador
// elimina la rama entera y el flag no hace nada. `./gradlew release` se niega a
// empaquetar una compilación de desarrollo (ver build.gradle.kts de la raíz).
// ----------------------------------------------------------------------------

val devBuild = providers.gradleProperty("rpgroll.dev").map { it.toBoolean() }.orElse(false)
val generatedBuildDir = layout.buildDirectory.dir("generated/licensebuild")

val generateLicenseBuild by tasks.registering {
    description = "Genera LicenseBuild con DEV_BUILD según -Prpgroll.dev."

    // Entrada declarada: sin esto Gradle reutilizaría el archivo de una
    // compilación anterior aunque cambie la propiedad.
    inputs.property("devBuild", devBuild)
    outputs.dir(generatedBuildDir)

    doLast {
        val target = generatedBuildDir.get().asFile.resolve("com/sack/rpgroll/licensing/LicenseBuild.java")
        target.parentFile.mkdirs()
        target.writeText(
            """
            package com.sack.rpgroll.licensing;

            /** Generado por Gradle. No editar: se sobrescribe en cada compilación. */
            final class LicenseBuild {

                private LicenseBuild() {
                }

                /**
                 * true solo en jars compilados con -Prpgroll.dev=true. Es lo único que
                 * habilita -Drpgroll.devmode; en un jar de venta vale false.
                 */
                static final boolean DEV_BUILD = ${devBuild.get()};
            }
            """.trimIndent() + "\n",
        )
    }
}

sourceSets.main {
    java.srcDir(generatedBuildDir)
}

tasks.named("compileJava") {
    dependsOn(generateLicenseBuild)
}
