# WorldForge — Hoja de ruta y lista de aceptación

Estados: PENDING · IN_PROGRESS · PASS · FAIL · BLOCKED. «PASS» exige evidencia
(prueba automática o registro de una prueba en servidor real). Los números de
pasos son los de la tanda final del 2026-10-21 ([`TEST-REPORT.md`](TEST-REPORT.md)).

## Fases

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Investigación y matriz de viabilidad | PASS (FEASIBILITY.md, 15 pruebas en Paper y Folia) |
| 1 | Diseño: arquitectura, seguridad, pruebas, esta hoja | PASS |
| 2 | Núcleo: plugin, config, SQLite, perfiles, validación, comandos, permisos, diagnóstico | PASS (91 pruebas unitarias en total) |
| 3 | Generación básica: vacío, vanilla, híbrido, plano, semillas, presets, import/export | PASS (`escenario.sh`, 84 pasos) |
| 4 | Menas por populator, plantillas `.nbt`, `dimension_type` | PASS (`escenario-tpl.sh` y `escenario-principal.sh`) |
| 5 | Dimensiones por datapack con reinicio | PASS (`escenario-dim.sh`, 48 pasos) |
| 6 | GUI | PASS (`escenario-gui.sh`, 37 pasos; menú en Folia) |
| 7 | Regeneración, respaldos y recuperación | PASS (`escenario-regen.sh`, 72 pasos) |
| — | Mundos como Multiverse (encargo posterior) | PASS (`escenario-mv.sh`, 65 pasos) |
| — | Terrenos estilo Tectonic/Terralith y motor propio (encargo posterior) | PASS (`escenario-terreno.sh`, `escenario-terralith.sh`, `escenario-motor.sh`, 13 unitarias) |
| 8 | Folia: revisión y pruebas | PASS (`escenario-folia.sh` 45, `escenario-principal.sh` 18 y `escenario-motor.sh` 12 en Folia 26.1.2-8, sin fallos); `folia-supported: true` declarado |
| 9 | Endurecimiento y entrega | PASS (jar sin `-Prpgroll.dev`, documentación); sin publicar ni commitear: lo decide Sack |

## Lista de aceptación (encargo §18)

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| A1 | JAR compilado contra Paper 26.1.2 verificado | PASS | `paper-api:26.1.2.build.74-stable`; corre en Paper 26.1.2-74 y Folia 26.1.2-8 |
| A2 | Arranca sin errores en el servidor de prueba | PASS | cada escenario termina con «sin excepciones de WorldForge en el registro» |
| A3 | Núcleo sin dependencias obligatorias de terceros | PASS | sin `depend` en plugin.yml; SQLite y YAML vienen con Paper; LuckPerms y el plugin `otro-generador` solo en las pruebas |
| A4 | Configuración validada y persistida | PASS | ProfileCodecTest, ProfileServiceTest; archivos creados y recargados tras reinicio |
| A5 | SQLite conserva historial y trabajos | PASS | StorageTest (migración 1 → 2 con filas); historial en el diagnóstico tras reiniciar (fase 3); trabajos que sobreviven al reinicio y se detectan en RUNNING (escenario-regen) |
| A6 | Mundo vacío | PASS | t_empty y mundo principal void-platform comprobados con `execute if block` |
| A7 | Mundo con proveedor comprobado | PASS | empty, flat, vanilla, amplified, hybrid, terrain y plugin en Paper real |
| A8 | Perfiles: crear, editar, validar, exportar, importar | PASS | unitarias + escenario (duplicar, semilla, publicar, exportar con SHA-256, importar) |
| A9 | Cada opción indica qué proveedor la soporta | PASS | capacidades por proveedor en diagnóstico; el validador rechaza lo no soportado (plantillas en vanilla, menas en un generador ajeno) |
| A10 | Menas → lógica funcional donde el proveedor lo admite | PASS | +49 diamante y +52 oro frente a vanilla con la misma semilla; menas en el mundo principal con preset |
| A11 | Estructuras/mazmorras: flujo de creación y validación | PASS | `structure save` con 8 rechazos; presets que piden una plantilla que no hay no se publican; colocación comprobada bloque a bloque en los cuatro giros (escenario-tpl) |
| A12 | Dimensiones con mecanismo comprobado y aviso de reinicio | PASS | escenario-dim (cinco tipos READY, quitar, JSON roto); escenario-terreno (siete con terreno); Folia |
| A13 | Gravedad simulada no se presenta como real | PASS | solo el atributo `Gravity` de los jugadores, medido con `/attribute` (escenario-mv); el README dice que no hay física para bloques ni ítems |
| A14 | GUI gestiona lo implementado | PASS | presets, mundos, regeneración, zonas, trabajos y diagnóstico (escenario-gui); plantillas, dimensiones, terrenos, `import` y `tp` solo por comandos (lo dice el README) |
| A15 | Comandos y permisos bloquean lo no autorizado | PASS | bot sin op con LuckPerms: viewer/operator, autocompletado filtrado; en el menú cada clic pide su permiso |
| A16 | Regeneración exige plan y confirmación | PASS | código por plan; equivocado, cancelado o caducado no confirma; doble clic rechazado |
| A17 | Zonas protegidas excluidas | PASS | el chunk protegido conserva su marca; `protections.yml` roto bloquea la regeneración |
| A18 | Respaldo y recuperación probados | PASS | deshacer desde el respaldo (secundario y principal); respaldo manipulado rechazado (BackupsAndJobsTest) |
| A19 | Trabajos interrumpidos detectados, no reanudados a ciegas | PASS | RUNNING al apagar → NEEDS_RECOVERY, mundo bloqueado, restaurar → RESTORED |
| A20 | Folia probado o funciones bloqueadas con mensaje | PASS | tres escenarios en Folia con un jugador (menú, plantillas, `world tp`, mundo principal con el motor); crear y descargar explican por qué no |
| A21 | Pruebas negativas de entradas y persistencia | PASS | entradas y BD (fases 2-3), plantillas, dimensiones, terrenos rotos, regeneración |
| A22 | Documentación de instalación, uso, límites y recuperación | PASS | README: instalación, cada función, Folia, Límites, «Si el servidor se apaga a mitad», Problemas frecuentes |
| A23 | Distribución = código compilado y probado | PASS | jar final compilado sin `-Prpgroll.dev` con las 91 pruebas en verde. En servidor se probó el jar de desarrollo: mismo código, con la comprobación de licencia omitida |
| A24 | Nada declarado completo sin verificar | PASS | cada afirmación del README tiene su paso en TEST-REPORT; lo no probado está en «Sin probar» |
