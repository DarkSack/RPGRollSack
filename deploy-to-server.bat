@echo off
setlocal enabledelayedexpansion

rem Copia los jars compilados de RPGRoll (core + los 10 addons) a la
rem carpeta plugins/ de tu servidor de pruebas, y borra plugins/.paper-remapped
rem para que Paper no reuse una version vieja cacheada de esos jars.
rem
rem Uso: deploy-to-server.bat "C:\ruta\a\tu\servidor\plugins"
rem Si no le pasas la carpeta como argumento, te la pregunta.
rem NO reconstruye el proyecto - corre "gradlew build" antes si necesitas
rem jars nuevos. Lo unico que borra es la carpeta .paper-remapped de destino.

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
set "MODULES=core ascension chat crates dungeons enchantments guilds items mobs npcs quests sackeffects effects"

if exist "%DEST_DIR%\.paper-remapped\" (
    rd /s /q "%DEST_DIR%\.paper-remapped"
    echo Borrado: %DEST_DIR%\.paper-remapped
) else (
    echo No habia .paper-remapped que borrar.
)

echo.
echo Copiando jars a: %DEST_DIR%
echo.

set /a COPIED=0
set /a MISSING=0

for %%M in (%MODULES%) do (
    set "FOUND=0"
    if exist "%ROOT%%%M\build\libs\" (
        for %%F in ("%ROOT%%%M\build\libs\*.jar") do (
            set "NAME=%%~nxF"
            set "STRIPPED=!NAME:-plain.jar=.jar!"
            if "!STRIPPED!"=="!NAME!" (
                copy /Y "%%F" "%DEST_DIR%\" >nul
                echo   OK    %%~nxF
                set "FOUND=1"
                set /a COPIED+=1
            )
        )
    )
    if "!FOUND!"=="0" (
        echo   FALTA modulo %%M ^(no hay build en build\libs^)
        set /a MISSING+=1
    )
)

echo.
echo Listo: !COPIED! jar^(s^) copiado^(s^), !MISSING! modulo^(s^) sin build.

endlocal
