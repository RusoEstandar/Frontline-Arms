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
