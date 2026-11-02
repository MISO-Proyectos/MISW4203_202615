# Vinilos · App móvil Android

Aplicación Android para consultar el catálogo de álbumes, artistas y coleccionistas de Vinilos.
Proyecto del curso MISW4203 Ingeniería de software para aplicaciones móviles (Uniandes), Grupo 4.

## Equipo
| Integrante | GitHub |
|---|---|
| Fredy Alberto Varón Guzmán | @fredy-alberto-varonguzman |
| Javier Felipe Ramírez Páez | @jframirezp |
| Sebastián Guerrero Ríos | @sguerrero3 |
| Eduard Alonso Villamil Avellaneda | @EduardVillamilUA |

Wiki con la planeación, el diseño arquitectónico y la estrategia de pruebas:
https://github.com/MISO-Proyectos/MISW4203_202615/wiki

## Requisitos
- Android Studio (versión estable más reciente)
- JDK 17 o superior (Android Studio lo descarga automáticamente)
- Android SDK instalado desde Android Studio (SDK Manager)
- Emulador o dispositivo con Android 5.0 (API 21) o superior
- Backend REST de Vinilos en ejecución (puerto 3000)

## Cómo construir y ejecutar
1. Clonar: `git clone https://github.com/MISO-Proyectos/MISW4203_202615.git`
2. Abrir la carpeta en Android Studio (File > Open) y esperar a que termine Gradle Sync.
3. Levantar el backend (proyecto NestJS + PostgreSQL, carpeta `BackVynils`):
   - Con Docker: `docker compose up --build` dentro de `BackVynils`, o
   - Sin Docker: tener PostgreSQL en `localhost:5432` (base `vinyls`, usuario y clave `postgres`), y ejecutar `npm install` y `npm run start`.
4. Verificar que `http://localhost:3000/albums` responde en el navegador. Al arrancar con la base vacía, el backend carga solo cuatro álbumes por defecto.
5. Ejecutar en un emulador (botón Run) o por consola: `.\gradlew.bat installDebug`

### URL del backend
Está definida en `app/build.gradle.kts` (`BASE_URL`) y debe terminar en `/`.
- Emulador de Android Studio: `http://10.0.2.2:3000/` (valor por defecto)
- Dispositivo físico: `http://<IP-de-tu-computador>:3000/` (mismo wifi)

En Android 17 (API 37) la app pide al abrir el permiso de red local (aparece como «dispositivos cercanos»). Hay que aceptarlo; si no, el catálogo muestra el error de conexión.

## Pruebas
- Pruebas unitarias: `.\gradlew.bat testDebugUnitTest`
- Pruebas E2E con Espresso (con el emulador abierto, las animaciones desactivadas y el backend recién levantado con los cuatro álbumes por defecto):
  `.\gradlew.bat connectedDebugAndroidTest`
- Reporte: `app/build/reports/androidTests/connected/debug/index.html`
- Estrategia y casos de prueba: ver la wiki.

## APK
El APK de cada versión está en la sección Releases del repositorio:
https://github.com/MISO-Proyectos/MISW4203_202615/releases

## Ramas (GitFlow)
`main` (versiones publicadas), `develop` (integración) y `feature/HUxx-Tn-descripcion` (cada tarea).
Toda integración a `develop` se hace por pull request con *squash and merge*.
