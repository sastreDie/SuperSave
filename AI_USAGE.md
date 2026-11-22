# Registro de uso de IA (Nivel 2)

Para el apéndice del reporte final. Herramienta: Claude Code (Claude Opus 5.5).
Regla: la IA escribe código de poca importancia o repetitivo; el equipo escribe
a mano la funcionalidad medular. Cada aporte de la IA se registra aquí.

| Fecha | Qué hizo la IA | Por qué | Revisión / cambios del equipo |
|---|---|---|---|
| 2026-11-22 | Revisó la propuesta; sugirió una fórmula de presupuesto semanal y un orden de implementación; redactó `DESIGN.md` con las decisiones del equipo | Planeación y documentación; las decisiones son de Diego | Pendiente |
| 2026-11-22 | Instaló GitHub CLI y Android Studio (winget); inicializó el repositorio git; escribió el `.gitignore` de Android | Configuración del entorno y código repetitivo | Diego aprobó las instalaciones |
| 2026-11-22 | Registró nuevas decisiones (D12 a D16) en `DESIGN.md`; propuso un borrador del modelo de datos (`DATA_MODEL.md`) | Documentación y propuesta; el modelo de datos lo decide el equipo | Pendiente |
| 2026-11-22 | Registró las decisiones D17 a D19 en `DESIGN.md`; reescribió el borrador de la fórmula con segmentos de semana; detectó un conflicto entre D4 y D17 (pago a fin de mes dentro de una semana dividida) | Documentación y análisis del diseño | Pendiente |
| 2026-11-22 | Registró D20 a D23; actualizó `DATA_MODEL.md` (v2) con las decisiones de Diego; propuso el "fondo de ahorro" para evitar que el ahorro del mes anterior se cuente como dinero disponible | Documentación y detección de un error de diseño | Pendiente |
| 2026-11-22 | Esqueleto de la app sobre la plantilla de Android Studio (creada por Diego): dependencias de Compose, Room, Glance, WorkManager y KSP (`libs.versions.toml`, `build.gradle.kts`); `MainActivity.kt` con una pantalla de prueba; `ui/theme/Theme.kt`; archivos de textos en inglés y español; `resources.properties` | Configuración y código repetitivo (Nivel 2). Las versiones se consultaron en Google Maven; se bajó el BOM de Compose a 2026.09.00 porque 2026.11 requiere compileSdk 37.1 | Pendiente: revisar el código del esqueleto |
