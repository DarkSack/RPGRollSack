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

    // RPGRoll-Quests es opcional (softdepend): solo se toca su NpcTalkEvent
    // desde QuestsIntegration, y solo si el plugin está habilitado.
    compileOnly(project(":quests"))

    // Sin ProtocolLib: los NPCs son entidades Mannequin del propio servidor
    // (ver NpcSpawnManager), no jugadores falsos hechos a base de packets.

    // Gson: Paper/Bukkit ya lo trae en su classpath — compileOnly, no se bundlea.
    compileOnly("com.google.code.gson:gson:2.11.0")

    // OkHttp: NO viene con el servidor, se empaqueta y reubica en el shadow jar.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // :common trae ContentParser/RPGContent usados por los parsers/definiciones testeadas.

    // rpgroll.plugin-conventions fija mockito-core/mockito-junit-jupiter en 5.15.2, cuya
    // versión de ByteBuddy no soporta instrumentar interfaces de Bukkit en el JDK 25 del
    // toolchain (falla "Could not modify all classes ..."); Gradle resuelve por versión más
    // alta entre coordenadas iguales, así que forzamos una versión más nueva solo acá.
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
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
