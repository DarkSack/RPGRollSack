@echo off
setlocal enabledelayedexpansion

rem Copia los jars compilados de RPGRoll (la Lib, el core y todos los addons)
rem a la carpeta plugins/ de tu servidor de pruebas, y borra plugins/.paper-remapped
rem para que Paper no reuse una version vieja cacheada de esos jars.
rem
rem Uso: deploy-to-server.bat "C:\ruta\a\tu\servidor\plugins"
rem Si no le pasas la carpeta como argumento, te la pregunta.
rem NO reconstruye el proyecto - corre "gradlew build" antes si necesitas
rem jars nuevos. Solo copia la version actual (la de core\build.gradle.kts) y
rem borra de destino .paper-remapped y las versiones anteriores de esos mismos
rem plugins: con dos versiones del mismo jar, Paper carga solo una.

set "DEST_DIR=%~1"
if "%DEST_DIR%"=="" set /p DEST_DIR="Carpeta plugins/ de destino: "

if "%DEST_DIR%"=="" (
    echo No se especifico ninguna carpeta destino. Cancelado.
    exit /b 1
)

if not exist "%DEST_DIR%\" (
    echo.
    echo La carpeta destino no existe: %DEST_DIR%
    echo Creala primero, o revisa la ruta.
    exit /b 1
)

set "ROOT=%~dp0"
rem La lista es addonModuleNames de build.gradle.kts mas recipes y machines
rem (que van aparte del zip): si agregas un modulo nuevo y te olvidas de
rem ponerlo aqui, el servidor de pruebas se queda con la version vieja de ese
rem plugin y no avisa nadie.
set "MODULES=common core npcs crates enchantments quests items ascension mobs dungeons guilds chat fx effects magic seasons fishing sackresourcepack ranching workers economy crafting tab extras traps pass furniture disasters recipes machines"

rem Todos los modulos comparten version (checkReleaseVersions): vale la de core.
set "VERSION="
for /f tokens^=2^ delims^=^" %%V in ('findstr /b "version" "%ROOT%core\build.gradle.kts"') do set "VERSION=%%V"
if "%VERSION%"=="" (
    echo No se pudo leer la version de %ROOT%core\build.gradle.kts. Cancelado.
    exit /b 1
)

if exist "%DEST_DIR%\.paper-remapped\" (
    rd /s /q "%DEST_DIR%\.paper-remapped"
    echo Borrado: %DEST_DIR%\.paper-remapped
) else (
    echo No habia .paper-remapped que borrar.
)

echo.
echo Copiando jars %VERSION% a: %DEST_DIR%
echo.

set /a COPIED=0
set /a MISSING=0
set /a FAILED=0

for %%M in (%MODULES%) do (
    set "FOUND=0"
    rem *-VERSION.jar deja fuera el -plain de core/npcs y los jars de versiones viejas.
    for %%F in ("%ROOT%%%M\build\libs\*-%VERSION%.jar") do (
        set "FOUND=1"
        set "NAME=%%~nxF"
        set "PREFIX=!NAME:-%VERSION%.jar=!"
        for /f "delims=" %%O in ('dir /b "%DEST_DIR%\*.jar" 2^>nul ^| findstr /b /r /i /c:"!PREFIX!-[0-9][0-9.]*\.jar$"') do (
            if /i not "%%O"=="!NAME!" (
                del "%DEST_DIR%\%%O" && echo   QUITO %%O ^(version anterior^)
            )
        )
        copy /Y "%%F" "%DEST_DIR%\" >nul
        if errorlevel 1 (
            echo   ERROR %%~nxF ^(no se pudo copiar: servidor encendido?^)
            set /a FAILED+=1
        ) else (
            echo   OK    %%~nxF
            set /a COPIED+=1
        )
    )
    if "!FOUND!"=="0" (
        echo   FALTA modulo %%M ^(no hay jar %VERSION% en build\libs^)
        set /a MISSING+=1
    )
)

echo.
echo Listo: !COPIED! jar^(s^) copiado^(s^), !MISSING! modulo^(s^) sin build, !FAILED! error^(es^).
if !FAILED! gtr 0 exit /b 1

endlocal
