plugins {
    id("rpgroll.plugin-conventions")
}

// :common se distribuye como plugin propio, RPGRoll-Lib, y cada addon lo
// declara en `depend` de su plugin.yml. Paper deja que un plugin vea las clases
// de los que declara en depend/softdepend, así que en runtime todos usan la
// MISMA copia (una sola GUIListener, un solo registro de menús abiertos).
//
// Antes cada addon llevaba :common embebido porque los addons no veían las
// clases que core empaquetaba; con RPGRoll-Lib declarado eso ya no pasa, y
// además los módulos dejan de necesitar el core solo para tener menús.
//
// :api NO se empaqueta: trae Events con HandlerList estático
// (CharacterCreatedEvent, PlayerJobLevelUpEvent) y tipos de servicio
// (RaceManager, PlayerClass, StatType) que deben ser UNA sola clase para todo
// el ecosistema. Sigue viniendo de core en runtime.

// :licensing SÍ va embebido: cada módulo verifica su propia compra con el
// código que lleva dentro. Si viviera en RPGRoll-Lib (gratis y separado),
// cambiar ese único jar por uno modificado desbloquearía todos los módulos.
val embeddedLicensing: Configuration by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":common"))

    compileOnly(project(":licensing"))
    embeddedLicensing(project(":licensing"))

    // compileOnly no se propaga al source set de test, y los tests unitarios sí
    // referencian estos tipos (RPGContent, ContentParser, LangManager...).
    testImplementation(project(":api"))
    testImplementation(project(":common"))
}

// withType y no named("jar"): el módulo que aplique el plugin shadow distribuye
// su shadowJar, y esa es OTRA tarea Jar que no hereda nada de la configuración
// de `jar`. Cuando esto configuraba solo `jar`, el jar publicado de :npcs salía
// sin una sola clase de :licensing —incluido LicenseGate, que su onEnable
// llama— y nada lo delataba: compila, empaqueta y pesa lo que debe.
// Es el fallo que un `./gradlew build` verde no ve.
tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(embeddedLicensing.elements.map { elements -> elements.map { zipTree(it.asFile) } }) {
        exclude("META-INF/**")
    }
}
