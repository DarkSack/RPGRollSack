plugins {
    id("rpgroll.plugin-conventions")
}

// Cada plugin de Paper corre con su propio classloader aislado: las clases de
// :common que core empaqueta (shadowJar) NO son visibles para los addons en
// runtime — el servidor tira ClassNotFoundException (LangManager,
// DirectoryCreator...) al habilitar cada addon. Por eso :common se empaqueta
// DENTRO del jar de cada addon.
//
// Es seguro duplicarlo: :common no tiene estado estático mutable compartido —
// sus utilidades son puras o reciben explícitamente el Plugin dueño
// (EntityReskinService keyea por NamespacedKey de ese plugin).
//
// :api NO se empaqueta a propósito: trae Events con HandlerList estático
// (CharacterCreatedEvent, PlayerJobLevelUpEvent) y tipos de servicio
// (RaceManager, PlayerClass, StatType) que deben ser UNA sola clase para todo
// el ecosistema — duplicarlos haría que un evento disparado por core nunca
// llegue a los listeners de los addons. Sigue viniendo de core en runtime.
val embeddedCommon: Configuration by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":common"))

    embeddedCommon(project(":common"))

    // compileOnly no se propaga al source set de test, y los tests unitarios sí
    // referencian estos tipos (RPGContent, ContentParser, LangManager...).
    testImplementation(project(":api"))
    testImplementation(project(":common"))
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(embeddedCommon.elements.map { elements -> elements.map { zipTree(it.asFile) } }) {
        exclude("META-INF/**")
    }
}
