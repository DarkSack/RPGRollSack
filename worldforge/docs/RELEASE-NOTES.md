# WorldForge — Notas de la versión

## 0.1.0 (sin publicar)

Primera versión. Para Paper 26.1.2 y Folia 26.1.2 (Java 25). Lo probado y cómo está en
[`TEST-REPORT.md`](TEST-REPORT.md); lo que no hace, en el README («Límites»).

### Qué trae

- **Presets** versionados (YAML): vanilla, amplificado, vacío, vacío con
  plataforma, plano, rico en recursos, Nether, End, montañas y una base para el
  tuyo. Borradores, validación por campo, exportar con SHA-256, importar siempre
  como borrador.
- **Mundos**: crear, cargar, descargar, inspeccionar; vuelven solos al reiniciar.
  Como Multiverse: Nether y End aparte, importar una carpeta, `world tp`,
  gravedad de los jugadores por mundo, y el generador de otro plugin (Terra,
  Iris…) con `provider: plugin`.
- **Terrenos** al estilo Tectonic o Terralith (`terrains/*.yml`): altura,
  picos, tamaño de continentes, erosión, crestas y clima, nivel del mar. En una
  dimensión nueva, en el mundo normal (`/wf terrain main`) o, para mundos creados
  en caliente, con el motor propio (`provider: terrain`). Cinco de serie.
- **Menas** extra y **plantillas** `.nbt` (mazmorras, ruinas, islas) en los
  chunks nuevos.
- **Dimensiones** por datapack (tipos hasta y=575), con su estado tras cada
  reinicio.
- **Regeneración** de chunks con plan, código de confirmación, respaldo
  verificado, deshacer y recuperación de trabajos interrumpidos; zonas protegidas.
- **Menú** en el juego, permisos por nodo (viewer, operator, admin) y
  diagnóstico.

### Antes de instalar

- Haz copia del mundo antes de usar `/wf terrain main` o la regeneración.
- Folia: no crea, carga ni descarga mundos en caliente; lo demás pasa por el
  arranque (dimensiones, mundo principal, regeneración). Ver la sección del README.
