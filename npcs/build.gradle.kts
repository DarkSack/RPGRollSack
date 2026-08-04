plugins {
    id("rpgroll.addon-conventions")
    id("com.gradleup.shadow") version "9.0.0"
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-NPCs")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common),
    // pero este addon también usa InventoryGUI/ItemBuilder (framework de GUIs) y
    // RPGPlayer/RPGRollAPI, que viven físicamente en :core.
    compileOnly(project(":core"))

    // ProtocolLib: requerido en runtime como plugin real del servidor (ver
    // depend: [RPGRoll, ProtocolLib] en plugin.yml) — nunca se empaqueta.
    // (El proyecto migró a Maven Central bajo el groupId "net.dmulloy2",
    // pero repo.dmulloy2.net todavía sirve versiones bajo el groupId
    // original "com.comphenix.protocol" — usamos esta última porque es la
    // que ya está resuelta/cacheada en este entorno.)
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")

    // Gson: Paper/Bukkit ya lo trae en su classpath — compileOnly, no se bundlea.
    compileOnly("com.google.code.gson:gson:2.11.0")

    // OkHttp: NO viene con el servidor, se empaqueta y reubica en el shadow jar.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("okhttp3", "com.sack.rpgroll.npcs.libs.okhttp3")
    relocate("okio", "com.sack.rpgroll.npcs.libs.okio")
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
