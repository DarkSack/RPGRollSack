plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Items")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core para RPGRollAPI (jugador, jobs, skills, traits, economía) y el
    // framework de GUIs (InventoryGUI/ItemBuilder) que reutiliza el editor.
    compileOnly(project(":core"))

    // Integración nativa con RPGRoll-Enchantments (aplicar encantamientos
    // custom al crear un ítem) — softdepend en plugin.yml, se chequea en
    // runtime si el plugin está presente antes de usar estas clases.
    compileOnly(project(":enchantments"))

    // VaultAPI es compileOnly en :core (no se propaga transitivamente) — hace
    // falta repetirlo acá porque el componente "Economy" referencia el tipo
    // Economy directamente.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.5")

    // SackResourcePack es standalone (no depende de RPGRoll) pero expone su
    // AssetsAPI pública para que otros plugins registren texturas — acá se
    // usa para sincronizar packs/<nombre>/resourcepack/ automáticamente.
    // softdepend en plugin.yml, se chequea AssetsAPI.isReady() en runtime.
    compileOnly(project(":sackresourcepack"))
}
