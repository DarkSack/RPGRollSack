# 🛡️ RPGRoll Framework

> **Nombre final pendiente**

Un framework RPG para **Minecraft Java Edition 1.21.1 (Paper)** diseñado para transformar un servidor vanilla en una experiencia de rol completa inspirada en juegos como **Dungeons & Dragons**, MMORPGs clásicos y sistemas RPG modernos.

> ⚠️ **Estado del proyecto:** En desarrollo activo (Alpha)

---

# ✨ Características

El objetivo del proyecto es proporcionar una base sólida para crear servidores RPG altamente personalizables.

## Sistema RPG

- 🎭 Clases
- 🧬 Razas
- ⚒️ Trabajos (Jobs)
- ⭐ Sistema de niveles
- 📈 Experiencia (XP)
- ❤️ Estadísticas del jugador
- 💎 Traits
- ⚔️ Skills
- 🎒 Objetos únicos
- 🛡️ Equipamiento RPG
- ✨ Encantamientos personalizados
- 📜 Ítems completamente configurables
- 🎲 Bonificaciones por raza
- ⚡ Habilidades especiales por clase

Cada **raza** y cada **clase** otorgarán efectos pasivos, habilidades únicas y modificadores de estadísticas.

---

# 🚀 Roadmap

Estas son algunas de las características planeadas para futuras versiones.

- ✅ Sistema de clases
- ✅ Sistema de razas
- ✅ Sistema de estadísticas
- ✅ Sistema de experiencia
- 🚧 Sistema de trabajos
- 🚧 Objetos únicos
- 🚧 Encantamientos personalizados
- 🚧 Habilidades activas
- 🚧 Árboles de talentos
- 🚧 NPCs RPG
- 🚧 Quests
- 🚧 Dungeons
- 🚧 Bosses personalizados
- 🚧 Economía integrada mediante Vault
- 🚧 Sistema de profesiones
- 🚧 Sistema de atributos avanzados
- 🚧 API pública para desarrolladores
- 🚧 Compatibilidad con plugins externos

---

# 📦 Compatibilidad

| Software  | Versión                |
| --------- | ---------------------- |
| Minecraft | **1.21.1**             |
| Paper     | ✅                     |
| Java      | **21**                 |
| Vault     | Opcional (Soft Depend) |

---

# 🔧 Instalación

1. Descarga el archivo `.jar`.
2. Colócalo dentro de la carpeta:

```
plugins/
```

3. Reinicia el servidor.

---

# 🎮 Comandos

Comando principal:

```
/rpg
```

Alias:

```
/rpgroll
/dnd
```

---

## 👤 Comandos de Jugador

| Comando        | Descripción                 | Permiso                  |
| -------------- | --------------------------- | ------------------------ |
| `/rpg create`  | Crear personaje             | `rpgroll.player.create`  |
| `/rpg stats`   | Ver estadísticas            | `rpgroll.player.stats`   |
| `/rpg mystats` | Ver estadísticas detalladas | `rpgroll.player.mystats` |
| `/rpg level`   | Ver nivel y experiencia     | `rpgroll.player.level`   |
| `/rpg class`   | Ver o cambiar clase         | `rpgroll.player.class`   |
| `/rpg race`    | Ver o cambiar raza          | `rpgroll.player.race`    |
| `/rpg jobs`    | Ver trabajos                | `rpgroll.player.jobs`    |
| `/rpg skills`  | Ver habilidades             | `rpgroll.player.skills`  |
| `/rpg traits`  | Ver traits                  | `rpgroll.player.traits`  |

---

## 👑 Comandos de Administrador

| Comando                           | Descripción                      | Permiso                 |
| --------------------------------- | -------------------------------- | ----------------------- |
| `/rpg reload`                     | Recargar configuración           | `rpgroll.admin.reload`  |
| `/rpg addxp <jugador> <cantidad>` | Agregar experiencia              | `rpgroll.admin.addxp`   |
| `/rpg levelup <jugador>`          | Subir nivel (Debug)              | `rpgroll.admin.levelup` |
| `/rpg gui`                        | Abrir interfaces en modo preview | `rpgroll.admin.gui`     |

---

# 🔐 Permisos

## Jugador

```
rpgroll.player.*
```

Incluye:

- rpgroll.player.create
- rpgroll.player.stats
- rpgroll.player.mystats
- rpgroll.player.level
- rpgroll.player.class
- rpgroll.player.race
- rpgroll.player.jobs
- rpgroll.player.skills
- rpgroll.player.traits

---

## Administrador

```
rpgroll.admin.*
```

Incluye:

- rpgroll.admin.reload
- rpgroll.admin.addxp
- rpgroll.admin.levelup
- rpgroll.admin.gui

---

## Acceso total

```
rpgroll.*
```

---

# 🧩 Integraciones

Actualmente soporta:

- Vault (Soft Depend)

En el futuro se planea compatibilidad con múltiples plugins de economía, NPCs y administración.

---

# 🛠️ Tecnologías

- Java 21
- Gradle
- Paper API 1.21.1
- Adventure API
- SQLite
- Vault API

---

# 📖 Filosofía del proyecto

RPGRoll no busca ser simplemente un plugin con niveles.

El objetivo es convertirse en un **Framework RPG** que permita a cualquier servidor construir su propio mundo de fantasía mediante sistemas modulares, altamente configurables y fáciles de extender.

La idea es que prácticamente cualquier mecánica RPG pueda implementarse utilizando este framework.

---

# ❤️ Estado del desarrollo

Actualmente el proyecto se encuentra en una fase temprana de desarrollo.

Las APIs internas y algunas funcionalidades pueden cambiar antes de la versión **1.0**.

---

# 📄 Licencia

Licencia pendiente.

---

## 👨‍💻 Autor

**Sack**

Framework RPG para servidores Paper inspirado en Dungeons & Dragons y MMORPGs.
