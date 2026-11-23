# Registro de uso de IA (Nivel 2)

Para el apéndice del reporte final. Herramienta: Claude Code (Claude Opus 5.5).
Regla: la IA escribe código de poca importancia o repetitivo; el equipo escribe
a mano la funcionalidad medular. Cada aporte de la IA se registra aquí.

| Qué hizo la IA | Por qué | Revisión / cambios del equipo |
|---|---|---|
| Revisó la propuesta; sugirió una fórmula de presupuesto semanal y un orden de implementación; redactó `DESIGN.md` con las decisiones del equipo | Planeación y documentación; las decisiones son de Diego | Pendiente |
| Instaló GitHub CLI y Android Studio (winget); inicializó el repositorio git; escribió el `.gitignore` de Android | Configuración del entorno y código repetitivo | Diego aprobó las instalaciones |
| Registró nuevas decisiones (D12 a D16) en `DESIGN.md`; propuso un borrador del modelo de datos (`DATA_MODEL.md`) | Documentación y propuesta; el modelo de datos lo decide el equipo | Pendiente |
| Registró las decisiones D17 a D19 en `DESIGN.md`; reescribió el borrador de la fórmula con segmentos de semana; detectó un conflicto entre D4 y D17 (pago a fin de mes dentro de una semana dividida) | Documentación y análisis del diseño | Pendiente |
| Registró D20 a D23; actualizó `DATA_MODEL.md` (v2) con las decisiones de Diego; propuso el "fondo de ahorro" para evitar que el ahorro del mes anterior se cuente como dinero disponible | Documentación y detección de un error de diseño | Pendiente |
| Esqueleto de la app sobre la plantilla de Android Studio (creada por Diego): dependencias de Compose, Room, Glance, WorkManager y KSP (`libs.versions.toml`, `build.gradle.kts`); `MainActivity.kt` con una pantalla de prueba; `ui/theme/Theme.kt`; archivos de textos en inglés y español; `resources.properties` | Configuración y código repetitivo (Nivel 2). Las versiones se consultaron en Google Maven; se bajó el BOM de Compose a 2026.09.00 porque 2026.11 requiere compileSdk 37.1 | Pendiente: revisar el código del esqueleto |

## Notas sobre la autoría del código

Algunos archivos tienen comentarios como "Written by Claude" o "UI by Claude".
Esos comentarios describen la versión actual del archivo, no la primera.

- **Primera versión, escrita a mano por el equipo:** la lógica de la pantalla
  principal (cómo se calcula el presupuesto de la semana y cómo un gasto lo
  afecta al revisarlo y registrarlo en "Check & record an expense"), el código
  de Kotlin de esa versión y las primeras 2 o 3 funciones y tablas de datos
  (por ejemplo `freeMoney`, `daysLeftInMonth` y `startOfWeek`).
- **Con la IA, a partir de esa base y de los lineamientos del equipo:** las
  versiones posteriores a la primera (corrección de bugs y cambios de diseño),
  la interfaz tipo tablero, todos los archivos XML, las funciones básicas o
  repetitivas (días del mes y de la semana, presupuestos) y el resto de las
  tablas de datos.
- **Pruebas unitarias:** todas las pruebas de JUnit (`app/src/test`) las escribió
  la IA, a partir de las fórmulas y los ejemplos que definió el equipo en
  `DESIGN.md`.
- **Consultas:** durante la programación el equipo le hizo consultas a la IA
  sobre Kotlin, Compose y las bibliotecas de Jetpack.

Por eso archivos como `engine/Weigh.kt`, `engine/Money.kt` o `ui/HomeScreen.kt`
dicen que los escribió Claude: son las versiones reescritas después de la
primera, que fue del equipo.

## Commits

Los commits de este repositorio también los manejó Claude Code: preparó los
cambios, escribió los mensajes de commit y los subió a GitHub, con la aprobación
del equipo. Por eso varios commits aparecen con autor `unknown` en lugar de un
integrante del equipo.
