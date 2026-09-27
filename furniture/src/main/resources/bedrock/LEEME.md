# Bedrock: RPGRoll-Furniture

Los muebles son ItemDisplay. Geyser no los enseña por sí mismo: hace falta la extensión
**GeyserDisplayEntity** (https://github.com/GeyserExtensionists/GeyserDisplayEntity, AGPL-3.0).

## Instalación (en el servidor donde corre Geyser: el proxy o el propio Paper)

1. `GeyserDisplayEntity-<versión>.jar` → `plugins/Geyser-*/extensions/`
2. `GeyserDisplayEntityPack.mcpack` (de la misma release) → `plugins/Geyser-*/packs/`
3. De esta carpeta:
   - `Geyser/packs/rpgroll-furniture.mcpack` → `plugins/Geyser-*/packs/`
   - `Geyser/custom_mappings/rpgroll-furniture.json` → `plugins/Geyser-*/custom_mappings/`
4. Reiniciar Geyser (el proxy). Los jugadores de Bedrock descargan los packs al entrar.

## Qué se ve en Bedrock

- Los muebles, con su modelo, giro y versión (madera, color). El ítem en el inventario, con icono.
- Asientos, almacenes, luz, estados y estaciones funcionan igual (los hace el servidor).
- Los objetos puestos en mesas y repisas son ítems vanilla: con la opción
  `hide-unmapped-vanilla-displays` de la extensión activada (lo normal) **no se ven** en Bedrock.
- La altura al sentarse puede quedar algo alta o baja: la corrige el fork de wlsgunzz de la
  extensión (`seat-offset-y`) o `seats.height-offset` en el config.yml del plugin (afecta a todos).
