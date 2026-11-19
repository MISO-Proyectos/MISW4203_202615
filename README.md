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
Se lee de `local.properties` (en la raíz del proyecto). Git lo ignora, así que no hay que tocar archivos del repo.
- Emulador de Android Studio: no hay que hacer nada, el valor por defecto es `http://10.0.2.2:3000/`.
- Dispositivo físico (mismo wifi): agrega esta línea a `local.properties` (créalo si no existe y no borres `sdk.dir`):
  `vinilos.baseUrl=http://<IP-de-tu-computador>:3000/`
- Si la URL no termina en `/`, el build se lo agrega (Retrofit lo pide).
- Si cambias `local.properties`, vuelve a compilar (Run o `.\gradlew.bat installDebug`).

En Android 17 (API 37) la app pide al abrir el permiso de red local (aparece como «dispositivos cercanos»). Hay que aceptarlo; si no, el catálogo muestra el error de conexión.

## Pruebas
- Pruebas unitarias: `.\gradlew.bat testDebugUnitTest`
- Pruebas E2E con Espresso (con el emulador abierto, las animaciones desactivadas y el backend recién levantado con los cuatro álbumes por defecto):
  `.\gradlew.bat connectedDebugAndroidTest`
- Reporte: `app/build/reports/androidTests/connected/debug/index.html`
- Estrategia y casos de prueba: ver la wiki.

### Preparar el backend para correr todas las pruebas
Los datos por defecto del backend entran con ids fijos (100 a 103) y la secuencia de PostgreSQL arranca en 1; sin este paso, las pruebas que crean datos chocan con esos ids (error de llave duplicada) después de unas pocas corridas.

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
El APK está en la carpeta `releases/` del repositorio (`releases/vinilos-v2.0.0.apk`) y también en la sección Releases:
https://github.com/MISO-Proyectos/MISW4203_202615/releases

Para instalarlo en un emulador o en un celular con Android 5.0 o superior hay que tener el backend corriendo. Ojo: el APK usa `10.0.2.2`, que solo sirve en el emulador. En un celular físico hay que compilar de nuevo con la IP del computador (ver «URL del backend»).

## Ramas (GitFlow)
- `main`: versiones publicadas (release `v2.0.0`).
- `develop`: integración.
- `feature/HUxx-Tn-descripcion`: una rama por tarea; se integra a `develop` con pull request revisado por otra persona.
- `release/vX.Y.Z`: se crea desde `develop` para preparar una versión (por ejemplo, agregar el APK); luego se integra a `main` y vuelve a `develop`.
