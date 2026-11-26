RPGRoll-Structures: formato de una estructura
=============================================

Cada estructura es un archivo YAML en esta carpeta (plugins/RPGRoll-Structures/structures/).
El nombre del archivo es el id (solo a-z, 0-9, _ y -): caseta.yml -> id "caseta".
El esquemático va en la carpeta hermana schematics/ (.nbt de estructura de Minecraft, o .schem de
WorldEdit/FAWE; para leer .schem hace falta WorldEdit o FastAsyncWorldEdit instalado).
Tras cambiar algo: /structuresadmin validate (revisa sin aplicar) y /structuresadmin reload.

Todas las claves (solo name, category, tier, schematic y materials son obligatorias):

  name: {es: "Caseta", en: "Hut"}   # es es obligatorio
  category: casas                   # id de categories.yml
  tier: 1                           # nivel de mesa mínimo: 1 Constructor, 2 Ingeniero, 3 Arquitecto
  schematic: caseta.nbt             # archivo dentro de schematics/
  icon: minecraft:oak_door          # opcional
  money: 0                          # opcional; con Vault se cobra entero al confirmar (sin Vault no se puede confirmar)
  permission: rpgrollstructures.build.caseta   # opcional; por defecto build.<id>
  materials:
    - {item: oak_planks, amount: 40}              # vanilla o de otro módulo (rpgroll:...)
    - {any_of: [cobblestone, stone], amount: 20}  # cualquiera de la lista
    - {item: glass_pane, amount: 4, optional: true}
  build:
    mode: progressive      # progressive (por ciclos) o instant
    order: bottom_up       # bottom_up, layers, center_out, stages o custom
    custom: [OAK_PLANKS]   # solo con order: custom
    blocks-per-cycle: 16
    ticks-per-cycle: 20
  placement:
    ground: true           # exige suelo firme
    liquids: false         # permite construir sobre líquidos
    worlds: []             # vacío = todos
    biomes: []             # vacío = todos
    replace: [GRASS_BLOCK] # bloques que la obra puede reemplazar

Un archivo con errores no entra al catálogo; el motivo sale en el log y en /structuresadmin validate.
