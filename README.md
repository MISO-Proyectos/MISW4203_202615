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
   - Con Docker (lo más fácil): `docker compose up --build` dentro de la carpeta `BackVynils` (con PostgreSQL 16: ver «Preparar el backend para correr todas las pruebas», porque `postgres:latest` ya es la 18 y no arranca), o
   - Sin Docker: tener PostgreSQL en `localhost:5432` (base `vinyls`, usuario y clave `postgres`) y Node.js 12 (el backend es viejo y con versiones más nuevas de Node puede fallar). Luego `npm install` y `npm run start` dentro de la carpeta `BackVynils`.
4. Verificar que `http://localhost:3000/albums` responde en el navegador. Al arrancar con la base vacía, el backend carga solo cuatro álbumes por defecto.
5. Ejecutar en un emulador (botón Run) o por consola: `.\gradlew.bat installDebug`

### URL del backend
Se lee de `local.properties` (en la raíz del proyecto). Git lo ignora, así que no hay que tocar archivos del repo.
- Emulador de Android Studio: no hay que hacer nada, el valor por defecto es `http://10.0.2.2:3000/`.
- Dispositivo físico (mismo wifi): agrega esta línea a `local.properties` (créalo si no existe y no borres `sdk.dir`):
  `vinilos.baseUrl=http://<IP-de-tu-computador>:3000/`
- Si la URL no termina en `/`, el build se lo agrega (Retrofit lo pide).
- Si cambias `local.properties`, vuelve a compilar (Run o `.\gradlew.bat installDebug`).

En Android 17 (API 37) la app pide al abrir el permiso de red local (aparece como «dispositivos cercanos»). Hay que aceptarlo; si no, el catálogo muestra el error de conexión.

## Pruebas

Las pruebas que usan emulador (E2E, accesibilidad y reconocimiento) necesitan las animaciones desactivadas y el backend recién recreado (ver «Preparar el backend para correr todas las pruebas», más abajo).

- Pruebas unitarias: `.\gradlew.bat testDebugUnitTest` (reporte: `app/build/reports/tests/testDebugUnitTest/index.html`)
- Análisis estático: `.\gradlew.bat lintDebug` (reporte: `app/build/reports/lint-results-debug.html`)
- Pruebas E2E con Espresso, todas: `.\gradlew.bat connectedDebugAndroidTest` (reporte: `app/build/reports/androidTests/connected/debug/index.html`)

`connectedDebugAndroidTest` no acepta `--tests`. Para correr una sola clase se usa el argumento de instrumentación:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=co.edu.uniandes.misw4203.vinilos.albums.AddToCollectionE2ETest"
```

Para varias clases, se separan con coma.

### Accesibilidad

Las pruebas usan *Accessibility Test Framework* (`espresso-accessibility`, solo en pruebas):

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=co.edu.uniandes.misw4203.vinilos.accesibilidad.AccesibilidadCatalogoE2ETest,co.edu.uniandes.misw4203.vinilos.accesibilidad.AccesibilidadColeccionistasE2ETest,co.edu.uniandes.misw4203.vinilos.accesibilidad.AccesibilidadFavoritosE2ETest,co.edu.uniandes.misw4203.vinilos.collectors.CollectorDetailAccessibilityTest"
```

La revisión manual con Accessibility Scanner y TalkBack está en la wiki.

### Reconocimiento

Se hizo en un emulador con Android 17 (API 37). Monkey, con las semillas 42, 43 y 44:

```powershell
adb shell am force-stop co.edu.uniandes.misw4203.vinilos
adb logcat -c
adb shell monkey -p co.edu.uniandes.misw4203.vinilos -s 42 --throttle 200 --pct-syskeys 0 -v -v 5000
```

El APK *debug* trae LeakCanary, y Monkey también abre su pantalla. La exploración sistemática (Droidbot y recorrido propio) y los resultados están en la wiki.

### Preparar el backend para correr todas las pruebas
Los datos por defecto del backend entran con ids fijos (100 a 103) y la secuencia de PostgreSQL arranca en 1; sin este paso, las pruebas que crean datos chocan con esos ids (error de llave duplicada) después de unas pocas corridas.

**Antes, fija PostgreSQL en la versión 16.** El `docker-compose.yml` de BackVynils usa `image: postgres` sin versión, y desde que `postgres:latest` es la 18 la base no arranca con la carpeta `./pgdata` (el contenedor `db` sale con error y el backend queda con `getaddrinfo ENOTFOUND db`). BackVynils no es nuestro y no se modifica: crea, dentro de la carpeta `BackVynils`, el archivo `docker-compose.override.yml` (Docker Compose lo lee solo, junto al `docker-compose.yml`) con este contenido:
```yaml
services:
  db:
    image: postgres:16
```
Si ya intentaste levantar la base con la 18, borra `pgdata` (el script de abajo ya lo hace); los datos que tenía se pierden.

Desde PowerShell, dentro de la carpeta `BackVynils`:
```powershell
docker compose down
Remove-Item -Recurse -Force .\pgdata
docker compose up --build -d
# Esperar a que responda: hasta 40 intentos de 3 s (2 minutos)
$listo = $false
for ($i = 1; $i -le 40 -and -not $listo; $i++) {
  try { $listo = (Invoke-WebRequest http://localhost:3000/albums -UseBasicParsing).StatusCode -eq 200 } catch { Start-Sleep -Seconds 3 }
}
if (-not $listo) { Write-Error "El backend no respondió en http://localhost:3000/albums después de 2 minutos. Revisa los logs con: docker compose logs web db"; return }
# Dejar las secuencias por encima de los ids por defecto
foreach ($t in 'album','collector','collector_album','comment','performer','performer_prize','prize','track') {
  docker compose exec -T db psql -U postgres -d vinyls -c "SELECT setval(pg_get_serial_sequence('$t','id'), 1000);"
}
```
Si prefieres ver los logs, quita `-d` de `docker compose up` y corre el `setval` desde otra terminal una vez que `http://localhost:3000/albums` responda.

## APK
El APK de la versión `3.0.0` está en `releases/vinilos-v3.0.0.apk` y también en la sección Releases:
https://github.com/MISO-Proyectos/MISW4203_202615/releases

Es de variante *debug*: incluye LeakCanary y StrictMode.

Para instalarlo en un emulador o en un celular con Android 5.0 o superior hay que tener el backend corriendo. Ojo: el APK usa `10.0.2.2`, que solo sirve en el emulador. En un celular físico hay que compilar de nuevo con la IP del computador (ver «URL del backend»).

## Ramas (GitFlow)
- `main`: versiones publicadas (release `v3.0.0`). Solo recibe ramas `release/*`.
- `develop`: integración.
- `feature/HUxx-Tn-descripcion`: una rama por tarea; se integra a `develop` con pull request revisado por otra persona.
- `release/vX.Y.Z`: se crea desde `develop` para preparar una versión (por ejemplo, agregar el APK); luego se integra a `main` y vuelve a `develop`.

## Documentación
- [Sprint 3](https://github.com/MISO-Proyectos/MISW4203_202615/wiki/Sprint-3)
- [Estrategia de pruebas](https://github.com/MISO-Proyectos/MISW4203_202615/wiki/Estrategia-de-Pruebas-Sprint-3)
- [Pruebas de reconocimiento](https://github.com/MISO-Proyectos/MISW4203_202615/wiki/Pruebas-de-Reconocimiento)
- [Accesibilidad](https://github.com/MISO-Proyectos/MISW4203_202615/wiki/Accesibilidad)
- [Micro-optimizaciones](https://github.com/MISO-Proyectos/MISW4203_202615/wiki/Micro-optimizaciones-Sprint-3)
- [Perfilamiento](https://github.com/MISO-Proyectos/MISW4203_202615/wiki/Perfilamiento-Sprint-3)
