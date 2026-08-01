plugins {
    id("rpgroll.plugin-conventions")
    id("com.gradleup.shadow") version "9.0.0"
}

group = "com.sack"
version = "0.1.0"

dependencies {
    implementation(project(":api"))
    implementation(project(":common"))

    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("org.sqlite", "com.sack.rpgroll.libs.sqlite")
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
