plugins {
    id("rpgroll.plugin-conventions")
    id("com.gradleup.shadow") version "9.0.0"
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll")

dependencies {
    implementation(project(":api"))
    implementation(project(":common"))

    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.5")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")

    // Gson ya viene embebido en el server de Paper/Spigot en tiempo de
    // ejecución — compileOnly alcanza, no hace falta empaquetarlo.
    compileOnly("com.google.code.gson:gson:2.11.0")
}

tasks.shadowJar {
    archiveClassifier.set("")

    // NO relocar org.sqlite: sqlite-jdbc trae una librería nativa (JNI) cuyos
    // símbolos están atados al nombre de paquete original
    // (Java_org_sqlite_core_NativeDB_...). Reubicar la clase Java rompe ese
    // binding y el driver falla con UnsatisfiedLinkError al abrir la conexión.
    // Cada plugin de Bukkit/Paper ya tiene su propio classloader aislado, así
    // que dejar org.sqlite sin relocar no genera colisiones con otros plugins.
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// ============ Ofuscación (opt-in, NO forma parte de `build`) ============
//
// Deliberadamente aislada en su propia Configuration "detached": declarar
// esto NO dispara ninguna descarga de red — ProGuard solo se resuelve
// perezosamente si alguien corre `./gradlew :core:obfuscate` de forma
// explícita. Cualquier otro comando (compileJava, build, etc.) es
// exactamente tan rápido/confiable como antes de agregar esto.
//
// IMPORTANTE: esta tarea no pudo verificarse de punta a punta en el
// entorno donde se escribió (sin acceso de red a Maven Central para
// artefactos nuevos) — antes de distribuir el jar resultante, corré
// `./gradlew :core:obfuscate` vos mismo y probá el .jar en un servidor
// de prueba real.
val proguard = configurations.detachedConfiguration(
        project.dependencies.create("com.guardsquare:proguard-base:7.4.2"))

tasks.register<JavaExec>("obfuscate") {

    group = "distribution"
    description = "Ofusca com.sack.rpgroll.license.* con ProGuard (protege la validación de licencia). Opt-in, no corre con `build`."

    dependsOn(tasks.shadowJar)

    val inputJar = tasks.shadowJar.flatMap { it.archiveFile }
    val outputFile = layout.buildDirectory.file("libs/${project.name}-${project.version}-obfuscated.jar")

    inputs.file(inputJar)
    outputs.file(outputFile)

    doFirst {
        classpath = proguard
        mainClass.set("proguard.ProGuard")

        val javaHome = System.getProperty("java.home")

        args = listOf(
                "-injars", inputJar.get().asFile.absolutePath,
                "-outjars", outputFile.get().asFile.absolutePath,
                "-libraryjars", "$javaHome/jmods(!**.jar;!module-info.class)",
                "@${file("proguard-rules.pro").absolutePath}",
        )
    }
}
