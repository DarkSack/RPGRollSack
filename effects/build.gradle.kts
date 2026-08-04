plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Effects")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core es el framework de GUIs compartido (InventoryGUI/ItemBuilder).
    // :sackeffects es la capa de renderizado (partículas/sonidos/títulos/bossbar) que
    // este motor de estados usa internamente para sus componentes visuales/sonoros.
    compileOnly(project(":core"))
    compileOnly(project(":sackeffects"))
    // Solo para las condiciones GUILD/TEAM — softdepend real (isReady() se
    // chequea siempre antes de tocar GuildsAPI, ver EffectConditionEvaluator).
    compileOnly(project(":guilds"))
}
