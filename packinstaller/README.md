# Instalador de packs

Reparte el contenido de un pack en las carpetas de los plugins que el servidor
tenga instalados, y omite los que no.

Un pack está organizado así, y esa estructura es exactamente la del destino:

```
reino-no-muerto/
  LEEME.md                              (documentación: no se instala)
  RPGRoll-Mobs/mobs/bosses/lich.yml  →  plugins/RPGRoll-Mobs/mobs/bosses/lich.yml
  RPGRoll-Magic/spells/drenar.yml    →  plugins/RPGRoll-Magic/spells/drenar.yml
  RPGRoll-Traps/traps/pinchos.yml    →  se omite si RPGRoll-Traps no está
```

## Para el comprador

Doble clic en `RPGRoll-PackInstaller-1.0.0.jar`. **No hay nada que instalar**: quien
corre Paper ya tiene Java, porque el servidor no arranca sin él.

En un servidor sin escritorio —un VPS por SSH, que es donde vive media
comunidad— el mismo archivo funciona desde la consola:

```
java -jar RPGRoll-PackInstaller.jar <pack> <carpeta-plugins> [--instalar]
```

Sin `--instalar` solo enseña lo que haría. Ese es el valor por defecto a
propósito: en una consola no hay ventana de confirmación, así que la primera
ejecución tiene que ser inofensiva.

El pack puede ser una carpeta o su `.zip`, para que nadie tenga que
descomprimir primero. También se puede **arrastrar el zip sobre la ventana**,
que es el gesto natural con algo recién descargado: si lo que se suelta es una
carpeta llamada `plugins` se toma como destino, y cualquier otra cosa como el
pack.

Si el zip envuelve el pack en una carpeta —que es justo lo que hace «Enviar a
→ Carpeta comprimida» de Windows, y casi cualquier herramienta gráfica— se
entra en ella sola y se avisa. Sin eso no se reconocería ninguna carpeta de
plugin y no se copiaría **nada**, con un mensaje que suena a que el pack está
roto. Solo se entra cuando no hay duda posible: una única carpeta en la raíz,
que no es ningún plugin conocido, y dentro de ella sí aparece alguno.

## Qué valida

Todo esto ocurre **antes** de tocar el disco. El peor final posible es una lista
de problemas en pantalla y un servidor idéntico a como estaba.

| Comprobación | Por qué |
|---|---|
| La carpeta destino parece un `plugins/` | Volcar 116 archivos sueltos en la carpeta equivocada se limpia a mano, uno por uno |
| Existe, es carpeta y se puede escribir | Fallar a mitad de copia deja el pack instalado por la mitad |
| Espacio libre, por el doble de lo que ocupa | Los respaldos ocupan aparte |
| Rutas del zip que se escapan (`../../`) | Los packs se descargan de internet, que es justo el escenario del *zip slip* |
| El destino final cae dentro de `plugins/` | Un enlace simbólico puede sacar la ruta fuera sin que aparezca ningún `..` |
| Nombres de carpeta que no son ningún plugin | Una errata como `RPGRoll-Mob` no se instalaría nunca **y nadie se enteraría** |
| Nombres que solo difieren en mayúsculas | Funciona en el Windows de quien armó el pack y falla en el Linux del comprador |
| Archivos ya presentes e idénticos | Reinstalar el mismo pack no debe tocar nada ni generar respaldos inútiles |
| Que el plan siga siendo el revisado | Cambiar la ruta después de *Revisar* instalaría algo que nadie ha visto |

Que falte un addon **no es un error**: nadie compra los 24. Se omite y se dice
cuál. Un nombre desconocido sí avisa, pero tampoco impide instalar — si mañana
sale un addon nuevo, un instalador viejo tiene que seguir sirviendo.

## Qué hace al instalar

- Escribe a un temporal y luego mueve. Si el proceso muere a mitad, el archivo
  original queda intacto en vez de convertirse en medio archivo que el plugin no
  puede leer al arrancar.
- Guarda una copia de todo lo que reemplaza en
  `rpgroll-respaldo-<fecha>/`, **junto a** `plugins/` y no dentro: ahí dentro el
  servidor intentaría cargarla como si fuera un plugin. Conserva la estructura,
  así que restaurar es arrastrar la carpeta de vuelta.
- No crea la carpeta de un plugin que no está instalado. Crearla haría pensar
  que sí lo está.

## Por qué Java y no otra cosa

El comprador corre Paper, que exige Java: ya lo tiene, sin excepción. Un
ejecutable en Python o Node obligaría a instalar un intérprete solo para copiar
archivos.

Swing viene dentro del JDK, así que esto es **un solo `.jar` sin dependencias**
(60 KB) que se abre con doble clic. Nada que instalar, ningún permiso de
administrador, ningún antivirus preguntando por un `.exe` desconocido.

Se compila para **Java 17** aunque el resto del repo apunte a 25. Esto no corre
en el servidor sino en la máquina del comprador, que puede tener cualquier cosa;
bajar el objetivo no cuesta nada —no se usa ninguna API posterior— y evita el
peor final para un instalador: un doble clic que no hace nada y un mensaje sobre
versiones de clase que nadie sabe interpretar.

## Distribución

No va dentro del zip de plugins: no es un plugin. Se acompaña al pack, o se
publica como descarga suelta en la tienda.

```
./gradlew :packinstaller:jar
```

Deja `build/libs/RPGRoll-PackInstaller-<versión>.jar`. La versión va en el
nombre y en el manifiesto: cuando alguien reporte un problema, la primera
pregunta es cuál tiene.

## Sobre la ventana

Los botones y las barras de desplazamiento se dibujan a mano. El aspecto nativo
de Windows pinta los botones en gris claro y no deja cambiarlo: sobre un fondo
oscuro se ve como un error, no como una decisión.

El informe es HTML dentro de un `JEditorPane`, porque un `JTextArea` no admite
color y lo único que se quiere saber de un vistazo es qué se instala y qué se
omite. El HTML de Swing es antiguo —viene a ser HTML 3.2— y tiene una rareza
que cuesta encontrar: **descarta el espacio que precede a un `<b>`**, también
escrito como `&nbsp;`, y también si se mete dentro de la etiqueta. "la carpeta
plugins" se lee "la carpetaplugins". Por eso no hay negritas a media frase;
donde sí las hay es al principio de línea, que es seguro.

Dos pruebas dibujan la ventana y el informe en `build/*.png`. No comprueban el
aspecto —eso hay que mirarlo— pero sí que ambos se montan sin reventar, que es
el fallo de interfaz más caro: no aparece al compilar, solo al abrir.
