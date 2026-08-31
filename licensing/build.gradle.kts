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
