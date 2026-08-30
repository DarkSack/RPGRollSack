# Servidor de licencias (ventas propias)

Valida las compras del canal **directo** — Ko-fi, Patreon, transferencia — donde no
hay un marketplace que inyecte la clave en la descarga ni exponga una API de
verificación.

No hace falta para voxel.shop: ese canal se valida solo contra la API del
marketplace (`provider: voxel-shop` en `license.yml`).

Sin dependencias — solo módulos nativos de Node. Corre en un VPS, en
Render/Railway/Fly, o detrás de nginx.

## Emitir una licencia

```bash
node server.js issue "comprador@example.com"
```

Eso cubre todo el ecosistema. Para una licencia acotada a ciertos módulos:

```bash
node server.js issue "comprador@example.com" rpgroll-magic rpgroll-economy
```

Imprime la clave (`RPGR-XXXXX-XXXXX-XXXXX-XXXXX`), que le pasás al comprador. Él la
pega en `plugins/RPGRoll/license.yml`:

```yaml
provider: self-hosted
key: 'RPGR-XXXXX-XXXXX-XXXXX-XXXXX'
resource-id: 'rpgroll'
endpoint: 'https://licencias.tu-dominio.com/verify'
```

## Revocar

```bash
node server.js revoke RPGR-XXXXX-XXXXX-XXXXX-XXXXX "Reembolsada el 2026-08-30"
```

Tiene efecto inmediato, sin reiniciar el servidor: cada consulta relee el archivo.
El motivo que escribas se le muestra al comprador en la consola del servidor.

```bash
node server.js list
```

## Levantar el servicio

```bash
PORT=8080 LICENSE_DB=/var/lib/rpgroll/licenses.json node server.js
```

Ponelo detrás de HTTPS (nginx, Caddy o el proxy de tu hosting). El plugin acepta
cualquier URL, pero mandar claves de licencia por HTTP plano las expone.

## Contrato

`POST /verify` con `application/x-www-form-urlencoded`:

| Campo      | Contenido                                                     |
| ---------- | ------------------------------------------------------------- |
| `license`  | La clave del comprador                                        |
| `resource` | Qué producto se está validando (el `resource-id` del plugin)   |

Respuesta:

```json
{ "valid": true,  "status": "active" }
{ "valid": false, "status": "revoked",     "message": "..." }
{ "valid": false, "status": "unknown",     "message": "..." }
{ "valid": false, "status": "not-covered", "message": "..." }
```

## Cómo interpreta el plugin cada respuesta

Esto es lo importante del diseño, y es deliberado:

| Situación                                | Estado    | Qué pasa                                        |
| ---------------------------------------- | --------- | ----------------------------------------------- |
| `valid: true`                            | VALID     | Arranca normalmente                             |
| `valid: false` (revocada / desconocida)  | INVALID   | **Bloquea de inmediato**, sin período de gracia  |
| Servidor caído, HTTP 5xx, cuerpo ilegible| UNKNOWN   | Período de gracia: usa la última validación OK   |

Un problema tuyo (servidor caído) **nunca** bloquea a un comprador legítimo: se
apoya en la última validación exitosa durante 7 días. Por eso el servidor devuelve
`400` cuando falta un parámetro, en vez de `valid: false` — un error de forma no es
una licencia inválida.

La contracara, que conviene tener presente: si tu servidor está inalcanzable, una
licencia **revocada** sigue funcionando hasta que se agote esa ventana de gracia. Es
el costo inevitable de tolerar cortes; la alternativa (bloquear ante cualquier fallo
de red) castiga a los compradores legítimos por un problema del vendedor.

## Almacenamiento

Un `licenses.json` plano, suficiente para una operación de un solo vendedor. Las
escrituras son atómicas (se escribe un `.tmp` y se renombra), así que un corte a
mitad no deja el archivo truncado. **Respaldalo**: es la única fuente de verdad de
quién compró qué.

Las claves se comparan en tiempo constante para que el tiempo de respuesta no filtre
cuántos caracteres iniciales de una clave son correctos.
