plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.1.11"

base.archivesName.set("RPGRoll-Enchantments")

// rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
// :core para el framework de GUIs compartido (InventoryGUI/ItemBuilder) que usa
// el editor visual de encantamientos — el resto del addon sigue siendo
// autocontenido (usa PersistentDataContainer directamente para taggear ítems).
dependencies {
    compileOnly("me.clip:placeholderapi:2.11.5")
    // El Desencantador cobra en dinero por Vault.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    // rpgroll.addon-conventions declara :api y :common como compileOnly, lo
    // que no se propaga al classpath de test — hace falta repetirlos acá
    // para que los tests unitarios puedan referenciar sus tipos (ej.
}
