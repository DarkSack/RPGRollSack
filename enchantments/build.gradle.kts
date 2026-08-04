plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Enchantments")

// rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
// :core para el framework de GUIs compartido (InventoryGUI/ItemBuilder) que usa
// el editor visual de encantamientos — el resto del addon sigue siendo
// autocontenido (usa PersistentDataContainer directamente para taggear ítems).
dependencies {
    compileOnly(project(":core"))
    compileOnly("me.clip:placeholderapi:2.11.5")
}
