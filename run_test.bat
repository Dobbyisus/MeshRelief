@echo off
echo.
echo ========================================
echo  MESHRELIEF NETWORK TEST RUNNER
echo ========================================
echo.

cd /d "%~dp0"

REM Compile all classes and test
echo [1/2] Compiling all Java files...
javac -source 1.8 -target 1.8 -d out ^
    core/model/Packet.java ^
    core/p2p/Peer.java ^
    core/store/SeenCache.java ^
    core/transport/Transport.java ^
    core/transport/MockTransport.java ^
    core/routing/Router.java ^
    core/routing/FloodRouter.java ^
    meshrelief_test1.java

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo [OK] Compilation successful
echo.
echo [2/2] Running flood routing test (A ^<--^> B ^<--^> C)...
echo.

REM Run the test
java -cp out meshrelief_test1

echo.
pause
