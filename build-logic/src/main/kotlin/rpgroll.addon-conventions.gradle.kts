plugins {
    id("rpgroll.plugin-conventions")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":common"))

    // compileOnly no se propaga al source set de test, y los tests unitarios sí
    // referencian estos tipos (RPGContent, ContentParser, LangManager...).
    testImplementation(project(":api"))
    testImplementation(project(":common"))
}