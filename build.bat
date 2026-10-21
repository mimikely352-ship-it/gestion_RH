@echo off
rem Wrapper qui force JAVA_HOME sur le JDK 17 pour cette invocation d'Ant,
rem sans toucher au JAVA_HOME global de la machine (qui peut pointer sur un autre JDK).
rem Ajuste le chemin ci-dessous si ton JDK 17 est installe ailleurs.
setlocal
if "%SIRH_JDK17_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
) else (
    set "JAVA_HOME=%SIRH_JDK17_HOME%"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
call ant %*
endlocal
