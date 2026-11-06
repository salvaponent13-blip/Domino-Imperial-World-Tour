# Compila la app desde PowerShell, en un solo comando.
#
#   tools\build.ps1                 -> APK de depuracion (app/build/outputs/apk/debug)
#   tools\build.ps1 -Target apk     -> APK de release firmada (hace falta el keystore)
#   tools\build.ps1 -Target aab     -> AAB firmado para Play (hace falta el keystore)
#
# Existe porque Gradle necesita JAVA_HOME 17 en esta maquina (Android Studio lo pone solo, la
# consola no) y porque asi no hay que recordar las rutas de salida.
param(
    [ValidateSet('debug', 'apk', 'aab')]
    [string]$Target = 'debug'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    # JDK 17: el toolchain del proyecto pide exactamente 17, y el PATH de la consola suele
    # apuntar a 21. Se busca el primero que haya instalado.
    if (-not ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe')) -and
              ((& (Join-Path $env:JAVA_HOME 'bin\java.exe') -version 2>&1 | Select-Object -First 1) -match '17'))) {
        $jdk17 = Get-ChildItem 'C:\Program Files\Microsoft' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($jdk17) { $env:JAVA_HOME = $jdk17.FullName }
    }
    Write-Host "JAVA_HOME = $env:JAVA_HOME"

    $task = switch ($Target) {
        'debug' { ':app:assembleDebug' }
        'apk'   { ':app:assembleRelease' }
        'aab'   { ':app:bundleRelease' }
    }

    & .\gradlew.bat $task --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle termino con codigo $LASTEXITCODE" }

    $out = switch ($Target) {
        'debug' { 'app\build\outputs\apk\debug\app-debug.apk' }
        'apk'   { 'app\build\outputs\apk\release\app-release.apk' }
        'aab'   { 'app\build\outputs\bundle\release\app-release.aab' }
    }
    if (Test-Path $out) {
        $file = Get-Item $out
        Write-Host ("LISTO: {0} ({1:N1} MB)" -f $file.FullName, ($file.Length / 1MB))
    } else {
        Write-Warning "Gradle dijo OK pero no encuentro $out"
    }
} finally {
    Pop-Location
}
