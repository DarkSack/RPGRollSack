plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-FX")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core es solo para el framework de GUIs compartido (InventoryGUI/ItemBuilder)
    // — esta librería es intencionalmente independiente de RPGRollAPI: solo
    // trabaja con conceptos vanilla (Player/Entity/Location), para que algún día
    // pueda usarla cualquier plugin, no solo los de RPGRoll.
    compileOnly(project(":core"))
}
