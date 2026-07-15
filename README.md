# RPGRoll

**Framework RPG modular para PaperMC (Minecraft Java 1.21.1)**

RPGRoll no es un plugin RPG más: es un **framework** pensado para construir contenido de rol (razas, clases, habilidades, profesiones, quests, NPCs y más) definiendo YAML, sin tocar código Java para cada nueva pieza de contenido.

---

## ✨ Filosofía del proyecto

> No estamos desarrollando un plugin pequeño. Estamos desarrollando un framework.

RPGRoll se construye priorizando:

- 🧼 **Código limpio** y principios SOLID
- 🧩 **Separación de responsabilidades** entre módulos
- 🏗️ **Arquitectura desacoplada** y extensible
- 📈 **Escalabilidad** a largo plazo
- 🔧 **Mantenibilidad** pensada para años de desarrollo, no para un solo release

---

## 🛠️ Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| API | Paper API 1.21.1 |
| Build | Gradle (Kotlin DSL) |
| Base de datos | SQLite (implementación actual) — preparado para MySQL y PostgreSQL |
| Configuración | YAML |
| Migraciones | SQL versionado (estilo Flyway) |

---

## 🎯 Objetivo final

Construir algo cercano a un pequeño **motor de videojuegos RPG**, donde el contenido del servidor se define casi por completo mediante configuración:

```
plugins/
└── RPGRoll/
    ├── config/
    ├── classes/
    ├── races/
    ├── skills/
    ├── professions/
    ├── items/
    ├── quests/
    ├── spells/
    ├── lang/
    ├── database/
    │   ├── rpgroll.db
    │   └── migrations/
    └── cache/
```

Los administradores de servidor podrán crear y balancear contenido **únicamente editando archivos YAML**.

---

## 📦 Estado del desarrollo

### ✅ Sprint 1 — Infraestructura (COMPLETADO)

Base del framework: arranque, registro de servicios y sistema de configuración propio.

- **`Bootstrap`** — Inicia el framework. No contiene lógica de negocio, solo registra servicios.
- **`ServiceRegistry`** — Registro interno de servicios del framework.
- **Sistema de configuración propio**, independiente de `JavaPlugin#getConfig()`:
  - `config/ConfigManager` — copia, carga y registra configuraciones.
  - `config/ConfigRegistry` — mantiene todos los YAML cargados en memoria.
  - `config/ConfigFile` — record que representa un archivo de configuración.
  - `creator/DirectoryCreator` — crea automáticamente las carpetas del framework (`classes`, `races`, `skills`, `lang`, `database`, `professions`, `items`, `quests`).
  - `creator/ResourceCopier` — copia los recursos base (`config.yml`, `database.yml`, `gameplay.yml`, `lang/es_MX.yml`) desde el jar.
  - `loader/YamlLoader` — carga de archivos YAML.
  - `migration/ConfigMigrator` y `migration/VersionChecker` — migración y verificación de versiones de configuración.

### 🔄 Sprint 2 — Persistencia (80–90%)

Sistema de base de datos y migraciones propio, inspirado en Flyway.

- **`database/DatabaseProvider`** *(interfaz)* — `connect()`, `disconnect()`, `isConnected()`, `getConnection()`.
- **`database/provider/SQLiteProvider`** — implementación de `DatabaseProvider` responsable únicamente de abrir/cerrar la conexión SQLite.
- **`database/DatabaseManager`** — coordina el acceso a la base de datos. No contiene SQL, no crea tablas, no conoce el motor concreto; selecciona el proveedor según `database.yml`.
- **`database/Migration`** *(record)* — representa una migración (`version`, `filename`, `path`).
- **`database/MigrationRegistry`** — detecta y ordena automáticamente las migraciones en `database/migrations`.
- **`database/MigrationExecutor`** — ejecuta el SQL de cada archivo de migración (ej. `V2__create_players.sql`).
- **`database/DatabaseMigrator`** — conoce el `schema_version` actual, determina las migraciones pendientes y ejecuta únicamente esas.

#### Flujo de migraciones

```
Servidor inicia
      │
      ▼
MigrationRegistry
      │
      ▼
DatabaseMigrator
      │
      ▼
¿Existe V1? ──Sí──▶ ¿Existe V2? ──No──▶ Ejecutar ──▶ Guardar versión
```

#### Tabla de control de versión

```sql
CREATE TABLE schema_version (
    version    INTEGER PRIMARY KEY,
    applied_at INTEGER NOT NULL
);
```

Garantiza que ninguna migración ya aplicada vuelva a ejecutarse.

---

## 🗄️ Base de datos

Actualmente se usa **SQLite**, con una base de datos independiente por instalación del plugin:

```
plugins/RPGRoll/database/rpgroll.db
```

No se comparte entre servidores.

### `database.yml`

```yaml
provider: SQLITE

sqlite:
  file: rpgroll.db

mysql:
  host: localhost
  port: 3306
  database: rpgroll
  username: root
  password: ""
```

> Por ahora solo el proveedor `SQLITE` está operativo. La estructura ya contempla MySQL y PostgreSQL.

### Migraciones SQL

Ubicadas en `resources/database/migrations` (**nunca** dentro de `src/main/java`):

```
V1__create_schema_version.sql
V2__create_players.sql
V3__create_player_stats.sql
```

---

## ⚙️ Configuración

El `ConfigManager` expone las configuraciones cargadas de forma directa, sin depender de `plugin.getConfig()`:

```java
configManager.getConfig("database"); // devuelve un YamlConfiguration
```

---

## 🧭 Arquitectura futura

```
database/
├── provider/
│   ├── SQLiteProvider
│   ├── MySQLProvider
│   └── PostgreSQLProvider
├── repository/
├── entity/
└── migration/
```

Próximos módulos planeados: sistema de razas, clases, habilidades, profesiones (con recompensas por bloque, protección anti-farm y persistencia), integración de economía vía Vault, quests y NPCs.

---

## 📋 Requisitos

- Java 21
- Servidor PaperMC 1.21.1
- Gradle (incluido vía wrapper)

## 🚀 Build

```bash
./gradlew build
```

El `.jar` generado se coloca en la carpeta `plugins/` del servidor.

---

## 📄 Licencia

Por definir.