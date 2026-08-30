plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Chat")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core para RPGRollAPI (nivel/clase/raza/job del jugador para los formatos
    // dinámicos) y el framework de GUIs (InventoryGUI/ItemBuilder).
    compileOnly(project(":core"))

    // Integración blanda (softdepend en plugin.yml): canales/menciones de
    // Guild y Team, y los placeholders %rpgrollguilds_...% si está instalado.
    compileOnly(project(":guilds"))

    compileOnly("me.clip:placeholderapi:2.11.5")

    // :common trae ContentParser/RPGContent usados por los parsers/definiciones testeadas;
    // :guilds trae GuildsAPI, referenciada por MentionResolver.
    testImplementation(project(":guilds"))

    // rpgroll.plugin-conventions fija mockito-core/mockito-junit-jupiter en 5.15.2, cuya
    // versión de ByteBuddy no soporta instrumentar interfaces de Bukkit en el JDK 25 del
    // toolchain (falla "Could not modify all classes ..."); Gradle resuelve por versión más
    // alta entre coordenadas iguales, así que forzamos una versión más nueva solo acá.
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
}
