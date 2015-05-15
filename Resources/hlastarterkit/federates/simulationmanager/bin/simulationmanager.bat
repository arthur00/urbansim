@echo off
title Simulation Manager
setlocal

IF "%PRTI1516E_HOME%"=="" echo WARNING! PRTI1516E_HOME environment variable has not been set

rem ***********************************************************************
rem * CONNECTING TO THE RTI USING THE JAVA API                            *
rem * ----------------------------------------                            *
rem * Specify RTI jar-file(s) when connecting through the RTI:s Java API: *
rem ***********************************************************************
set RTI_JAR=%PRTI1516E_HOME%\lib\prti1516e.jar

..\..\..\jre\bin\java -cp "..\lib\master.jar;%RTI_JAR%;%CLASSPATH%" se.pitch.hlatutorial.master.Master

pause
endlocal