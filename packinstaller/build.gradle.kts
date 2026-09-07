// ============================================================================
// Instalador de packs de contenido.
//
// No es un plugin: es una aplicación de escritorio que el comprador ejecuta en
// su máquina, así que NO aplica `rpgroll.plugin-conventions` — no necesita
// paper-api, ni id de licencia, ni ir dentro del zip de plugins.
//
// Cero dependencias a propósito. Swing viene en el JDK, así que el jar es un
// solo archivo que se abre con doble clic: nada que instalar, ni Python, ni
// Node, ni un instalador que pida permisos de administrador.
// ============================================================================

plugins {
    java
}

// La misma version que el ecosistema: el instalador acompana a los packs, y
// que diga "unspecified" en las propiedades del archivo no ayuda a nadie a
// saber cual tiene.
version = "1.0.0"

repositories {
    // mavenLocal primero, igual que el resto del repo: las dependencias de test
    // ya estan ahi y asi no hace falta salir a la red.
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// Se compila para Java 17 aunque el resto del repo apunte a 25.
//
// El servidor exige 25, pero esto no corre en el servidor: corre en la máquina
// del comprador, que puede tener cualquier cosa. Bajar el objetivo no cuesta
// nada acá —no se usa ninguna API posterior— y evita el peor final posible para
// un instalador: un doble clic que no hace nada y un mensaje sobre versiones de
// clase que nadie sabe interpretar.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("RPGRoll-PackInstaller")

    manifest {
        attributes(
            "Main-Class" to "com.sack.rpgroll.packinstaller.Main",
            "Implementation-Title" to "RPGRoll Pack Installer",
            "Implementation-Version" to project.version.toString(),
        )
    }
}
