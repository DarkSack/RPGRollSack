plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Structures")

dependencies {
    // El pack se registra en SackResourcePack si está instalado (softdepend).
    compileOnly(project(":sackresourcepack"))

    // Cobro de las mejoras por Vault (VaultEconomy vive en la Lib); sin Vault se omite el cobro.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    // En el servidor viene de Paper/Folia; en las pruebas, de aquí.
    // El servicio de mesas referencia Economy (Vault); en las pruebas hay que tener la clase.
    testImplementation("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    testImplementation("org.xerial:sqlite-jdbc:3.49.1.0")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:3.5.11")
}
