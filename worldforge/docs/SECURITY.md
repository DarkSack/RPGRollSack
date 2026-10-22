# WorldForge — Seguridad

WorldForge borra datos de mundos por diseño. Estas son las reglas que lo hacen seguro.

## Archivos que puede tocar

| Ruta | Para qué | Cómo |
|---|---|---|
| `plugins/RPGRoll-WorldForge/**` | configuración, presets, BD, exportaciones | lectura/escritura |
| `plugins/RPGRoll-WorldForge/structures/*.nbt` | plantillas | escritura solo de nombres nuevos (no se sobrescribe ninguna) |
| `plugins/RPGRoll-WorldForge/backups/**` | respaldos | escritura; WorldForge nunca los borra (los borra el admin) |
| `<mundo>/dimensions/<ns>/<id>/{region,entities,poi}/*.mca` y `*.mcc` | regeneración y restauración | solo con el mundo descargado o antes de cargarlo, solo los archivos del plan, solo tras respaldo verificado |
| `<mundo principal>/datapacks/worldforge/**` | dimensiones y terrenos | escritura dentro de esa carpeta; un terreno ya escrito no se reescribe con otros valores |
| `<mundo principal>/datapacks/worldforge/data/minecraft/worldgen/noise_settings/overworld.json` | `terrain main` (admin) | sustituye el terreno del mundo normal desde el próximo arranque; `terrain main vanilla` lo borra |
| `plugins/RPGRoll-WorldForge/terrains/*.yml` | terrenos | se copian los de serie si faltan; nunca se reescribe uno existente |

Toda ruta se resuelve con `Path.normalize()` y se comprueba que siga dentro de
su raíz permitida (`SafeFiles.child`/`within` para datos del plugin; los
respaldos guardan rutas relativas a la carpeta del servidor y se rechazan si
salen de ella, también al leer un manifiesto). Las importaciones rechazan rutas
absolutas, `..`, letras de unidad y nombres con separadores.

## Entradas

- Identificadores (`[a-z0-9_-]{1,32}` para perfiles y regiones; nombres de mundo
  `[a-z0-9_]{1,40}`), validados en el borde (comando, GUI, YAML importado).
- Bloques con `Material.matchMaterial` y comprobados como bloque; biomas contra
  `Registry.BIOME`. Un identificador desconocido es un error, no se ignora.
- YAML importado: tamaño máximo configurable (por defecto 256 KB), se valida
  antes de guardarse y entra siempre como **borrador**.
- Plantillas `.nbt`: el nombre pasa por el mismo patrón que los identificadores
  (no hay rutas), el archivo se rechaza si pasa de 2 MB antes de leerlo, lo lee
  el lector de estructuras de Minecraft (`StructureManager.loadStructure`) y una
  plantilla de más de 32×64×32 no se usa. Se colocan sin entidades.
- Terrenos: claves conocidas y rangos cerrados (altura ×0,25–3, tamaños ×0,25–8,
  desplazamiento ±96, nivel del mar −64–256); id `[a-z0-9_]{1,24}`, el mismo que
  va en las rutas del datapack. El JSON generado sale del jar del servidor, no del
  archivo del usuario.
- `terrain main` puede impedir que arranque el servidor si Minecraft rechazara el
  JSON: es solo de admin (`worldforge.terrain.main`), y el README dice qué archivo
  borrar.
- Ningún perfil ejecuta código, comandos ni scripts.

## Operaciones destructivas

- Siempre plan → vista previa → confirmación con el **código del plan** (caduca
  a los `regeneration.confirm-minutes`) → respaldo verificado justo antes de
  editar → ejecución. Restaurar sigue el mismo camino como un trabajo propio.
- Confirmación atómica en SQLite (`UPDATE … WHERE state='PLANNED'`): un doble
  clic no lanza dos trabajos.
- Un mundo con un trabajo `NEEDS_RECOVERY` no acepta otro destructivo.
- Las protecciones se aplican al planificar **y** al ejecutar.
- Si `protections.yml` no se puede leer, la regeneración queda bloqueada y el
  archivo no se reescribe.
- Un trabajo interrumpido nunca se reanuda solo: queda en `NEEDS_RECOVERY`.
- El respaldo se comprueba (SHA-256 de cada copia y del manifiesto) al hacerlo y
  otra vez antes de restaurar; uno manipulado se rechaza.
- La consola también pasa por los permisos (`worldforge.*`).

## GUI

- Menús identificados por su `InventoryHolder` propio, nunca por título, nombre o lore.
- Cada menú tiene sesión (jugador + id aleatorio) y la versión (hash) del perfil
  que muestra; guardar compara la versión actual y rechaza si otro admin lo cambió.
- Permiso comprobado en cada clic, no solo al abrir.

## Registros

Quién hizo qué y cuándo (`history`). Sin rutas absolutas del sistema del
usuario más allá del servidor, sin tokens de licencia.
