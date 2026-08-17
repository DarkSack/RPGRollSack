plugins {
    id("rpgroll.plugin-conventions")
}

dependencies {
    // SackResourcePack es standalone (no depende de :common) y expone su
    // AssetsAPI pública — se usa acá para ModuleAssetSync, que los módulos
    // consumidores usan sin necesitar su propia dependencia directa (la
    // firma pública de ModuleAssetSync no expone tipos de sackresourcepack).
    // softdepend en runtime, se chequea AssetsAPI.isReady() antes de usarla.
    compileOnly(project(":sackresourcepack"))
}