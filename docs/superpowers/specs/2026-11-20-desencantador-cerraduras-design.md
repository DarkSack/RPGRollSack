# Desencantador y cerraduras con código — diseño

Aprobado por Sack el 2026-11-20.

## Por qué

- Quitar un encantamiento concreto que no se quiere sin perder el resto (la muela vanilla los borra todos y no toca los de RPGRoll).
- Dar valor a un mineral nuevo y raro del mundo de recursos.
- Proteger cofres y puertas con un código que se puede compartir (llave o teclado), sin depender solo del claim.

## Desencantador (RPGRoll-Enchantments)

- Se abre con Mayús + clic derecho en una muela (grindstone) vanilla con el ítem en la mano principal. El clic normal sigue siendo la muela vanilla.
- El menú enseña el ítem y un botón por cada encantamiento (vanilla y de RPGRoll).
  - Clic izquierdo: paga dinero (Vault).
  - Clic derecho: paga esquirlas de olvidita.
- El encantamiento sale en un libro a su nivel (libro vanilla o libro de RPGRoll) y el ítem conserva los demás.
- Precio por nivel (config): 1500 monedas o 1 esquirla. Tesoro (vanilla `isTreasure`, RPGRoll LEGENDARY o superior) y maldiciones cuestan el doble.
- La esquirla se reconoce por el id de RPGRoll-Items (`rpgroll-items:item-id`), sin depender del módulo (Items ya depende de Enchantments).

## Mineral olvidita (RPGRoll-Items + pack)

- `mena_de_olvidita`: solo en el mundo `recursos`, en deepslate, vetas pequeñas y raras (~1 cada 3 chunks).
- Suelta `esquirla_de_olvidita`, que no tiene receta ni otro uso. La tienda no la vende.
- Textura nueva en el pack (catálogo de infra/texturas) y `srp publish`.

## Cerraduras (RPGRoll-Extras, junto al Reforzador)

- Bloques: cofres (también dobles), barriles, puertas de madera y de hierro, trampillas, puertas de valla.
- Cerrar: con una llave en blanco, Mayús + clic derecho en un bloque propio (o sin claim ajeno). Teclado de 4 dígitos para el código; la llave queda grabada.
- Abrir: el dueño siempre; los demás con la llave de ese código o tecleándolo. La puerta de hierro también se abre así.
- Copias: llave grabada + llave en blanco en la mesa de crafteo = 2 grabadas.
- Seguridad: 3 fallos = 60 s sin poder teclear; tolvas no meten ni sacan; explosiones no rompen; solo el dueño rompe (admins con permiso).
- Quitar: el dueño, Mayús + clic con la llave, opción «Quitar cerradura».

## Decisiones mías (Claude)

- Precios, nombres (olvidita) y tasa de la mena.
- Muela vanilla en vez de bloque propio: sin textura ni receta nuevas.
- Cerraduras en Extras porque el Reforzador ya guarda dueño por bloque.
