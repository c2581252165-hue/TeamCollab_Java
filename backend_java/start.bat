@echo off
set JAVA_HOME=D:\uiiiii\jdk
set PATH=%JAVA_HOME%\bin;%PATH%
java -jar target\backend-0.0.1-SNAPSHOT.jar > backend.log 2>&1
