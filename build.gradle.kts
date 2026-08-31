// ============================================================================
// Empaquetado/distribución final: un solo release versionado del ecosistema.
// No hace nada en un `./gradlew build` normal — es opt-in vía `./gradlew release`.
// Ver la sección "Distribución" del README para el flujo completo.
// ============================================================================

val addonModuleNames = listOf(
    "core", "npcs", "crates", "enchantments", "quests", "items", "ascension", "mobs",
    "dungeons", "guilds", "chat", "sackeffects", "effects", "magic", "seasons", "fishing",
    "sackresourcepack", "ranching", "workers", "economy", "crafting", "tab", "extras", "traps",
)

val addonProjects = addonModuleNames.map { project(it) }
val releaseDir = layout.buildDirectory.dir("release")

val checkReleaseVersions by tasks.registering {
    group = "distribution"
    description = "Falla si algún módulo tiene una versión distinta al resto — un release es UN número para todo el ecosistema."

    doLast {
        val versions = addonProjects.associate { it.name to it.version.toString() }
        val distinct = versions.values.toSet()

        if (distinct.size > 1) {
            val breakdown = versions.entries.joinToString("\n") { (name, v) -> "  - $name: $v" }
            throw GradleException(
                "Versiones desincronizadas entre módulos — un release necesita el mismo número en todos:\n$breakdown",
            )
        }

        logger.lifecycle("✔ Los ${versions.size} módulos comparten la versión ${distinct.first()}.")
    }
}

// core y npcs aplican el plugin shadow (relocan/empaquetan dependencias reales: :api/:common en
// core, OkHttp en npcs) — para esos dos el jar que se distribuye es shadowJar, no el jar plano
// (que queda con archiveClassifier "plain" y le faltarían esas clases). El resto de los addons no
// tiene nada que shadear, así que su jar normal ya es el final.
val shadowedModuleNames = setOf("core", "npcs")

val collectRelease by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Junta el jar final de cada módulo (shadowJar para core/npcs, jar para el resto) en build/release/."

    dependsOn(checkReleaseVersions)

    // Copy no borra lo que ya esté en el destino: sin esto, el jar de un módulo
    // renombrado (o eliminado) sobrevive en build/release y se cuela en el zip.
    // El comprador terminaría instalando el plugin viejo y el nuevo a la vez.
    doFirst {
        delete(releaseDir)
    }

    into(releaseDir)

    addonProjects.forEach { addon ->
        if (addon.name in shadowedModuleNames) {
            dependsOn("${addon.path}:shadowJar")
            from(addon.tasks.named("shadowJar"))
        } else {
            dependsOn("${addon.path}:jar")
            from(addon.tasks.named("jar"))
        }
    }

    // El jar "-plain" de core/npcs (tasks.jar, sin las clases shadeadas) no es el que se distribuye.
    exclude("*-plain.jar")

    doLast {
        val version = project(":core").version.toString()
        val manifest = destinationDir.resolve("MANIFEST.txt")
        val jars = destinationDir.listFiles { f -> f.extension == "jar" }?.sortedBy { it.name } ?: emptyList()

        manifest.writeText(
            buildString {
                appendLine("RPGRoll Framework — release $version")
                appendLine("Generado: ${java.time.Instant.now()}")
                appendLine()
                appendLine("${jars.size} jar(s):")
                jars.forEach { appendLine("  - ${it.name}") }
                appendLine()
                appendLine("Instalación: copiá RPGRoll-<version>.jar (el core) y los addons que necesites")
                appendLine("a plugins/ del servidor Paper. Ninguno es obligatorio salvo el core.")
            },
        )
    }
}

tasks.register<Zip>("release") {
    group = "distribution"
    description = "Arma un único .zip de distribución con todos los jars del ecosistema en la misma versión."

    dependsOn(collectRelease)
    from(releaseDir)
    archiveFileName.set("RPGRoll-Ecosystem-${project(":core").version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    doLast {
        logger.lifecycle("✔ Release listo: ${archiveFile.get().asFile}")
    }
}
