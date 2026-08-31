plugins {
    id("rpgroll.plugin-conventions")
}

dependencies {
    // La verificación de licencia parsea JSON. Gson ya viene embebido en el
    // server de Paper en runtime — compileOnly alcanza, igual que en :core.
    compileOnly("com.google.code.gson:gson:2.11.0")
    testImplementation("com.google.code.gson:gson:2.11.0")

    // SackResourcePack es standalone (no depende de :common) y expone su
    // AssetsAPI pública — se usa acá para ModuleAssetSync, que los módulos
    // consumidores usan sin necesitar su propia dependencia directa (la
    // firma pública de ModuleAssetSync no expone tipos de sackresourcepack).
    // softdepend en runtime, se chequea AssetsAPI.isReady() antes de usarla.
    compileOnly(project(":sackresourcepack"))
}
