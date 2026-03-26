@echo off
setlocal
cd /d %~dp0
echo --- Zomtopia Derleniyor ve Baslatiliyor ---
if exist out rmdir /s /q out
mkdir out

:: Quoted forward-slashes for javac @file to handle spaces and avoid backslash escapes
powershell -Command "Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' } | Out-File -FilePath sources.txt -Encoding ascii"

javac -d out @sources.txt
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo DERLEME HATASI! Lutfen Java JDK'nin yuklu ve PATH'e ekli oldugundan emin olun.
    if exist sources.txt del sources.txt
    pause
    exit /b %ERRORLEVEL%
)

if exist sources.txt del sources.txt
echo --- Baslatiliyor ---
java -cp out com.zomtopia.main.GameApp
pause
