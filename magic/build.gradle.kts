plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Magic")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core es para RPGRollAPI (mana/nivel/raza/clase) y el framework de GUIs compartido.
    compileOnly(project(":core"))
    // Ambos son softdepend en tiempo de ejecución (plugin.yml) — acá son compileOnly
    // porque Magic solo llama a sus API públicas si el plugin está presente (isReady()).
    compileOnly(project(":fx"))
    compileOnly(project(":effects"))

    // compileOnly no se propaga al source set de test (:api/:common ya los aporta
    // rpgroll.addon-conventions; :core hace falta declararlo acá).
    testImplementation(project(":core"))
}
