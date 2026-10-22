// WorldForge se compila APARTE del resto de RPGRoll hasta pasar su aceptación (docs/ARCHITECTURE.md,
// D10 y D15): el settings.gradle.kts de la raíz no lo incluye, así que ningún `./gradlew build` ni
// `release` del ecosistema lo compila ni lo empaqueta. Se compila desde esta carpeta:
//
//   cd E:/Dev/RPGRoll/worldforge && ../gradlew build
rootProject.name = "worldforge"

pluginManagement {
    includeBuild("../build-logic")
}

include("licensing")
project(":licensing").projectDir = file("../licensing")

// :licensing se comparte con el build de la raíz: que compile a una carpeta propia de WorldForge
// para que un jar de desarrollo (-Prpgroll.dev=true) nunca deje clases en licensing/build, que es
// de donde sale la release del ecosistema.
gradle.beforeProject {
    if (path == ":licensing") layout.buildDirectory.set(rootDir.resolve("build/licensing"))
}
