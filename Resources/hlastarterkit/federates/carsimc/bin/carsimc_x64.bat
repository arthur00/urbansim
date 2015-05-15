@echo off
title Car Sim C x64
setlocal

IF "%PRTI1516E_HOME%"=="" echo WARNING! PRTI1516E_HOME environment variable has not been set

rem ********************************************
rem * CONNECTING TO THE RTI USING THE C++ API  *
rem ********************************************

set LIB_DIR=../lib/win/x64
set PATH=%LIB_DIR%;%PRTI1516E_HOME%\jre\bin\server;%PRTI1516E_HOME%\lib\vc100_64;%PATH%

CarSimC_x64.exe

pause
endlocal