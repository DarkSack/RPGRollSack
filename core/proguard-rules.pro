# Reglas de ProGuard para el jar final de RPGRoll (:core).
#
# Alcance deliberadamente conservador: TODO el árbol de paquetes
# com.sack.rpgroll.** se mantiene con sus nombres intactos, EXCEPTO
# com.sack.rpgroll.licensing.** — porque los demás addons (npcs, crates,
# enchantments, quests, items, ascension) referencian directamente en
# tiempo de compilación decenas de clases internas de :core (RPGPlayer,
# InventoryGUI, ItemBuilder, RPGRollAPI, etc., no solo :api). Ofuscar esas
# clases rompería toda la ecosistema en runtime con NoSuchMethodError.
#
# El sistema de licencias (com.sack.rpgroll.licensing.*) es autocontenido:
# los plugins solo lo tocan por LicenseGate.verify(...), así que es el
# único lugar donde ofuscar aporta valor real (dificulta parchear la
# validación) sin riesgo de romper otro módulo.
#
# OJO: este patrón tiene que seguir al paquete. Cuando el código pasó de
# com.sack.rpgroll.license a com.sack.rpgroll.licensing, la regla vieja
# dejó de excluir nada y la licencia quedó SIN ofuscar sin que fallara
# ninguna build — un cambio de nombre acá se rompe en silencio.
# Las dos exclusiones van en UNA sola regla: los -keep se suman entre sí, así
# que una segunda regla que no excluyera licensing.** volvería a preservarlo y
# anularía la primera. LicenseIdentity entra acá porque su id ya queda
# inlineado en los call sites — la clase en sí no hace falta legible.
-keep class !com.sack.rpgroll.licensing.**,!com.sack.rpgroll.license.identity.**,com.sack.rpgroll.** {
    *;
}

# El driver JDBC de SQLite (relocado a com.sack.rpgroll.libs.sqlite) se
# carga por reflection/ServiceLoader (META-INF/services/java.sql.Driver)
# — ofuscarlo rompería esa carga sin ningún beneficio, ya que es una
# librería externa igual.
-keep class com.sack.rpgroll.libs.sqlite.** { *; }

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Las dependencias compileOnly (Paper API, Adventure, VaultAPI, Gson) no
# están en el classpath de esta pasada de ProGuard a propósito — las
# provee el server en runtime. No hace falta que ProGuard las resuelva.
-dontwarn **

# Prioriza correctitud sobre tamaño/velocidad: no hay forma de probar en
# un servidor real desde este entorno, así que se evita cualquier
# optimización de bytecode que pueda alterar comportamiento sutilmente.
-dontoptimize
