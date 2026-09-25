// ============================================================================
// Empaquetado/distribución final: un solo release versionado del ecosistema.
// No hace nada en un `./gradlew build` normal — es opt-in vía `./gradlew release`.
// Ver la sección "Distribución" del README para el flujo completo.
// ============================================================================

val addonModuleNames = listOf(
    // :common se distribuye como RPGRoll-Lib, la librería que todos declaran en depend.
    "common", "core", "npcs", "crates", "enchantments", "quests", "items", "ascension", "mobs",
    "dungeons", "guilds", "chat", "fx", "effects", "magic", "seasons", "fishing",
    "sackresourcepack", "ranching", "workers", "economy", "crafting", "tab", "extras", "traps", "pass",
)

val addonProjects = addonModuleNames.map { project(it) }
val releaseDir = layout.buildDirectory.dir("release")

val checkReleaseVersions by tasks.registering {
    group = "distribution"
    description = "Falla si algún módulo tiene una versión distinta al resto — un release es UN número para todo el ecosistema."

    doLast {
        // Un jar compilado con -Prpgroll.dev=true acepta -Drpgroll.devmode, que
        // salta la licencia. Eso no puede llegar nunca a un comprador.
        if (providers.gradleProperty("rpgroll.dev").map { it.toBoolean() }.getOrElse(false)) {
            throw GradleException(
                "Este build es de desarrollo (-Prpgroll.dev=true): sus jars permiten saltarse la licencia. " +
                    "Vuelve a lanzar el release sin esa propiedad.",
            )
        }

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

/**
 * El jar que se distribuye de cada módulo: shadowJar para core/npcs, jar para
 * el resto. Va dentro de un `provider` porque al configurarse la raíz los
 * subproyectos todavía no se han evaluado y sus tareas no existen.
 */
val distributedJars = addonProjects.map { addon ->
    provider {
        addon.tasks.named(if (addon.name in shadowedModuleNames) "shadowJar" else "jar").get()
            .outputs.files.filter { f -> f.name.endsWith(".jar") && !f.name.endsWith("-plain.jar") }.singleFile
    }
}

fun sha256(bytes: ByteArray): String =
    java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

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
                appendLine("${jars.size} jar(s), con su SHA-256 para poder comprobar que el jar")
                appendLine("instalado en un servidor es exactamente el que salió de este release:")
                jars.forEach { appendLine("  - ${it.name}  ${sha256(it.readBytes())}") }
                appendLine()
                appendLine("Instalación: copiá RPGRoll-Lib-<version>.jar (la librería, gratuita y")
                appendLine("obligatoria) y los módulos que necesites a plugins/ del servidor Paper.")
            },
        )
    }
}

val release = tasks.register<Zip>("release") {
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

/**
 * Comprueba que el zip contiene EXACTAMENTE los jars que hay compilados ahora.
 *
 * Existe porque un `./gradlew release` que no tiene nada que hacer no dice
 * nada: `collectRelease` y `release` salen UP-TO-DATE, sus `doLast` no se
 * ejecutan y el zip conserva la fecha del día que se armó. Eso es correcto
 * —los archivos de Gradle son reproducibles, así que recompilar sin tocar
 * nada da jars byte a byte idénticos y no hay que rehacer el zip—, pero visto
 * desde fuera es indistinguible de "el zip se quedó viejo", y con una
 * publicación de por medio esa duda no se puede resolver a ojo.
 *
 * Esta tarea no declara salidas, así que se ejecuta SIEMPRE, y compara el
 * SHA-256 del jar compilado de cada módulo con el de su entrada en el zip.
 * Falla si falta alguno, si sobra alguno o si alguno no coincide.
 */
val verifyRelease by tasks.registering {
    group = "distribution"
    description = "Compara el SHA-256 de cada jar del zip con el jar compilado de su módulo; falla si algo no cuadra."

    val zipFile = release.flatMap { it.archiveFile }
    val jars = distributedJars

    doLast {
        val zip = zipFile.get().asFile

        if (!zip.isFile) {
            throw GradleException("No hay zip que comprobar en ${zip.parentFile}: lanzá `./gradlew release`.")
        }

        val compilados = jars.map { it.get() }.associate { it.name to sha256(it.readBytes()) }

        val enElZip = java.util.zip.ZipFile(zip).use { archivo ->
            archivo.entries().asSequence()
                .filter { it.name.endsWith(".jar") }
                .associate { entrada -> entrada.name to sha256(archivo.getInputStream(entrada).readBytes()) }
        }

        val faltan = compilados.keys - enElZip.keys
        val sobran = enElZip.keys - compilados.keys
        val distintos = compilados.filter { (nombre, hash) -> enElZip[nombre]?.let { it != hash } == true }.keys

        if (faltan.isNotEmpty() || sobran.isNotEmpty() || distintos.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("El zip de distribución no coincide con los jars compilados:")
                    faltan.forEach { appendLine("  - falta en el zip: $it") }
                    sobran.forEach { appendLine("  - sobra en el zip (¿módulo renombrado o eliminado?): $it") }
                    distintos.forEach { appendLine("  - distinto al jar compilado: $it") }
                    appendLine()
                    append("Borrá build/release y build/distributions y volvé a lanzar `./gradlew release`.")
                },
            )
        }

        logger.lifecycle("✔ ${compilados.size} jars del zip coinciden con los compilados — ${zip.name}")
    }
}

release.configure { finalizedBy(verifyRelease) }
