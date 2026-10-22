plugins {
    id("rpgroll.plugin-conventions")
}

group = "com.sack"
version = "0.1.0"

base.archivesName.set("RPGRoll-WorldForge")

// Igual que SackResourcePack: NO usa rpgroll.addon-conventions porque no depende
// de RPGRoll-Lib. La librería no carga en Folia (no declara folia-supported y usa
// BukkitScheduler), y WorldForge tiene que poder hacerlo. Ver docs/FEASIBILITY.md.
val embeddedLicensing: Configuration = configurations.create("embeddedLicensing") {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    // La 26.1.2 estable, no la alpha 26.1.1 de las convenciones: Gradle se queda
    // con la versión más alta de las dos. Comprobado en docs/FEASIBILITY.md §1.
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.74-stable")

    compileOnly(project(":licensing"))
    embeddedLicensing(project(":licensing"))

    // En el servidor viene de Paper/Folia (META-INF/libraries.list); en las pruebas, de aquí.
    testImplementation("org.xerial:sqlite-jdbc:3.49.1.0")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(embeddedLicensing.elements.map { elements -> elements.map { zipTree(it.asFile) } }) {
        exclude("META-INF/**")
    }
}
