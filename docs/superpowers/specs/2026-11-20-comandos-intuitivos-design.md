# Comandos intuitivos (2026-11-20)

Pedido de Sack: comandos en el idioma del jugador, todos con autocompletado, y una forma fácil de darse
cosas, invocar cosas y abrir menús. Los comandos de admin siguen en inglés.

## Idiomas (en la Lib, un solo sitio)

- `common/.../command/CommandWords.java` lee `command-words.yml` del jar de cada módulo:
  `commands: <cmd>: names: {es, en, pt}` y `subcommands: <palabra de siempre>: {es, en, pt}`.
- `BrigadierCommands` registra los nombres como alias (los de otros plugins ganan si chocan), traduce el
  primer argumento a la palabra de siempre antes de llamar al comando (ningún comando cambió) y, si el
  comando tiene subcomandos en la tabla, sugiere el primer nivel en el idioma del cliente
  (es/pt; el resto, inglés; la consola, español). Lo escrito en otra lengua sigue sugiriéndose.
- Solo se traduce el primer argumento. Las tablas las genera y valida
  `scratchpad/palabras.py` (cada clave existe como literal en el código del módulo).
- Sin argumentos que completar: `socialspy`, `death`, `bestiario`, `tab`, `diario`, `votar`, `amuletos`;
  `me`/`do` son texto libre.

## /dar, /invocar, /abrir (Lib)

- `/dar [jugador] <cosa> [cantidad]` (alias `rgive`, permiso `rpgrolllib.dar`, op). Sin jugador, para ti.
- `/invocar <criatura> [cantidad] [jugador]` (alias `rsummon`, permiso `rpgrolllib.invocar`, op).
- `/abrir <menú> [jugador]` (alias `open`, para todos; a otro jugador, `rpgrolllib.abrir.otros`). Los menús
  son la tabla `abrir:` del config de la Lib (menú → comando que se ejecuta como el jugador) y solo se
  ofrecen los que tienen su comando en el servidor.
- `<cosa>` es `tipo:id` o un id suelto si solo un tipo de RPGRoll lo tiene (si no, Minecraft). Con Tab:
  primero los tipos; al escribir, ids de cualquier tipo.
- Cada módulo registra su tipo con `Things.give(...)` / `Things.summon(...)` (ServicesManager: se da de baja
  solo al deshabilitar el módulo): `item`, `gema` (Items), `mueble` (Furniture), `spawner`, `maquina`
  (Machines: `cantera`, máquinas de área, hornos `<horno>.<nivel>`), `libro` (`<encantamiento>_<nivel>`),
  `estacion` (Enchantments), `llave`, `lucky` (Crates), `extra` (`reforzador`, `llave_en_blanco`),
  `mob` (Mobs, para /invocar), `minecraft` (la Lib).
- `/dar` pide una pieza nueva por pila (no clona: hay ítems con datos propios por ejemplar).

## Ojo al desplegar

- Los módulos que llaman a `Things` necesitan la Lib nueva (si no, NoClassDefFoundError al arrancar).
- Comprobar en el servidor que `Bukkit.getCommandMap().getCommand` ve los comandos Brigadier (si no,
  `/abrir` no ofrecería ningún menú) y que los alias nuevos no chocan con EssentialsX.
