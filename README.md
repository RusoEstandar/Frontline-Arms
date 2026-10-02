# Frontline Arms (Forge 1.20.1 · 47.4.10)

Mod de armas, mecanicas y equipamiento tacticos inspirado en shooters modernos.
**Todo el contenido es original** (codigo, nombres, texturas placeholder, sonidos vanilla). No incluye nada de Activision.

## Que trae esta primera version

| Area | Contenido |
|---|---|
| Armas | P9 (pistola, semi), VX-9 (subfusil), AR-14 (fusil de asalto), M12 (escopeta), KR-50 (francotirador con mira) |
| Municion | 9mm, 5.56, 12ga, .50 (se consume de tu inventario al recargar) |
| Mecanicas | Disparo hitscan server-side, apuntar (ADS) con zoom, retroceso, dispersion segun movimiento/salto/agachado/ADS, dano a la cabeza, caida de dano por distancia, cargador por arma, recarga (cambiar de arma la cancela), sin disparar al correr |
| Equipamiento | Granada de fragmentacion, flashbang (ciega/ralentiza segun distancia y orientacion), granada de humo (nube ~15 s). Rebotan y tienen mecha |
| HUD | Municion cargador/reserva con colores, mira telescopica del KR-50 |

## Controles

| Accion | Tecla |
|---|---|
| Disparar | Clic izquierdo (mantener en automaticas) |
| Apuntar | Clic derecho (mantener) |
| Recargar | R (reasignable) |
| Lanzar granada | Clic derecho con la granada en la mano (hotbar) |

Todo esta en la pestana creativa **Frontline Arms** (o `/give @s frontline:ar14_rifle`). Aun no hay recetas de crafteo.

## Compilar

Requisitos: **JDK 17**.

Este zip no incluye el *Gradle wrapper* (`gradlew`). Dos opciones:

1. **Recomendada:** descarga el MDK oficial de Forge 1.20.1-47.4.10, y copia dentro de el
   mi carpeta `src/` y los archivos `build.gradle`, `gradle.properties`, `settings.gradle`
   (sustituyendo los suyos). Luego `./gradlew build`.
2. Con Gradle 8.x instalado: en esta carpeta `gradle wrapper --gradle-version 8.8` y despues `./gradlew build`.

El `.jar` queda en `build/libs/`. Para probar en desarrollo: `./gradlew runClient`.

### Opcion C: que GitHub lo compile (sin instalar nada)

1. Crea un repositorio en GitHub y sube **el contenido** de esta carpeta (incluida `.github/workflows/build.yml`).
   Si la carpeta oculta `.github` no se sube, crea el archivo a mano: *Add file -> Create new file* con el nombre `.github/workflows/build.yml`.
2. Pestana **Actions** -> workflow *Build mod* -> se ejecuta solo al subir (o *Run workflow*).
3. Al terminar, abre la ejecucion y descarga el artefacto **frontline-arms-jar** (contiene el `.jar`).
4. Si falla, copia el final del log (el error de `javac`) y pegalo para corregirlo.

## Modelos 3D

Las 5 armas y las 3 granadas usan modelos 3D de bloques propios (`assets/frontline/models/item/*.json`),
que comparten la textura `textures/item/palette.png` (9 colores). Los ajustes de posicion en mano/inventario
estan en el bloque `"display"` de cada JSON. Si un arma se ve girada o descentrada, ajusta ahi `rotation`/`translation`/`scale`
(o abre el JSON en Blockbench, pestana Display, para verlo en vivo). Las municiones siguen siendo sprites planos.

## Armas en primera persona y animaciones

Las armas se dibujan en primera persona con un renderizador propio (`client/GunRenderer.java`) a partir de datos:
`assets/frontline/guns/<arma>.json` define las piezas (cuerpo, corredera/cerrojo/bombeo, cargador, manos, fogonazo),
las poses (cadera, apuntado, sprint) y las animaciones por fotogramas clave (`draw`, `shoot`, `reload`, `reload_empty`, `inspect`).
Ademas hay balanceo al andar, respiracion, inercia al girar, corredera retenida con el cargador vacio y fogonazo al disparar.

- **Tecla H:** inspeccionar el arma (reasignable).
- **F3 + T** recarga los modelos en el juego: puedes editar un JSON de `guns/` y verlo sin reiniciar (en desarrollo con `runClient`).
- Los JSON se generan con `python3 tools/gen_guns.py` (ahi estan las medidas de cada arma y los parametros de animacion).
  Necesita Python 3 y `pip install pillow`. Si quieres tocar a mano un JSON, puedes hacerlo directamente.
- Los modelos de `models/item/` se usan solo en inventario, suelo y tercera persona.

## Estado

**Sin compilar ni probar.** Lo escribi sin acceso a las librerias de Forge, asi que puede haber errores de compilacion
(nombres de API, imports). Si `./gradlew build` falla, pega el error y lo corrijo.

## Ajustar el balance

Todas las estadisticas estan en `ModItems.java` (dano, cadencia, cargador, dispersion, retroceso, zoom, sonido).

## Siguientes pasos previstos

- Estadisticas en JSON (requiere sincronizar datos al cliente)
- Claymore y C4, tecla dedicada para letales/tacticos, cocinar granadas
- Accesorios (miras, silenciador, cargador extendido), perks, rachas de bajas
- Pantalla blanca/sordera real para la flashbang, tracers, animaciones, sonidos y modelos propios
- Recetas de crafteo
