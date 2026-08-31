plugins {
    // A propósito NO usa "rpgroll.addon-conventions" — ese convention agrega
    // compileOnly(:api)/compileOnly(:common), y SackResourcePack es un
    // framework genuinamente independiente (Paper/Spigot/Folia, no solo
    // RPGRoll): no debe depender de ningún módulo del ecosistema RPGRoll.
    id("rpgroll.plugin-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("SackResourcePack")

// :licensing no depende de :api ni de :common, así que SackResourcePack puede
// verificar su compra sin dejar de ser independiente del resto del ecosistema.
val embeddedLicensing: Configuration by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    compileOnly(project(":licensing"))
    embeddedLicensing(project(":licensing"))

    // Gson ya viene embebido en el server de Paper/Spigot en tiempo de
    // ejecución (mismo criterio que :core) — compileOnly alcanza, no hace
    // falta empaquetarlo ni relocarlo.
    compileOnly("com.google.code.gson:gson:2.11.0")
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(embeddedLicensing.elements.map { elements -> elements.map { zipTree(it.asFile) } }) {
        exclude("META-INF/**")
    }
}
