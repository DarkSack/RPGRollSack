plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common),
    // pero este addon también usa InventoryGUI/ItemBuilder (framework de GUIs) que
    // vive físicamente en :core.
    compileOnly(project(":core"))

    // DecentHolograms: tiene que ser la misma instancia que corre en el servidor
    // (el holograma real, no una copia embebida) — por eso compileOnly, nunca se
    // empaqueta. El repositorio de JitPack ya está declarado en rpgroll.plugin-conventions.
    compileOnly("com.github.decentsoftware-eu.DecentHolograms:decentholograms:2.10.1")
}
