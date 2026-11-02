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
3. Levantar el backend. Es el proyecto BackVynils que entregó el curso (NestJS + PostgreSQL): https://github.com/TheSoftwareDesignLab/BackVynils. No viene dentro de este repositorio, así que hay que clonarlo en otra carpeta: `git clone https://github.com/TheSoftwareDesignLab/BackVynils.git`
   - Con Docker (lo más fácil): `docker compose up --build` dentro de la carpeta `BackVynils`, o
   - Sin Docker: tener PostgreSQL en `localhost:5432` (base `vinyls`, usuario y clave `postgres`) y Node.js 12 (el backend es viejo y con versiones más nuevas de Node puede fallar). Luego `npm install` y `npm run start` dentro de la carpeta `BackVynils`.
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
El APK está en la carpeta `releases/` del repositorio (`releases/vinilos-v1.0.0.apk`) y también en la sección Releases:
https://github.com/MISO-Proyectos/MISW4203_202615/releases

Para instalarlo en un emulador o en un celular con Android 5.0 o superior hay que tener el backend corriendo. Ojo: el APK usa `10.0.2.2`, que solo sirve en el emulador. En un celular físico hay que compilar de nuevo con la IP del computador (ver «URL del backend»).

## Ramas (GitFlow)
- `main`: versiones publicadas (release `v1.0.0`).
- `develop`: integración.
- `feature/HUxx-Tn-descripcion`: una rama por tarea; se integra a `develop` con pull request revisado por otra persona.
- `release/vX.Y.Z`: se crea desde `develop` para preparar una versión (por ejemplo, agregar el APK); luego se integra a `main` y vuelve a `develop`.
