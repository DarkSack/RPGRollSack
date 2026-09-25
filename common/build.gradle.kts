plugins {
    id("rpgroll.plugin-conventions")
}

group = "com.sack"
version = "1.0.0"

// :common se distribuye como un plugin propio, RPGRoll-Lib: todos los módulos
// lo declaran en depend y usan sus clases en runtime en vez de llevar cada uno
// su copia. Ver RPGRollLib.
base.archivesName.set("RPGRoll-Lib")

dependencies {
    compileOnly("com.google.code.gson:gson:2.11.0")
    testImplementation("com.google.code.gson:gson:2.11.0")

    compileOnly(project(":sackresourcepack"))
}
