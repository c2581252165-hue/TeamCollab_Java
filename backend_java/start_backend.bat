@echo off
taskkill /F /IM java.exe
cd /d d:\ruanjian\backend_java
set "JAVA_HOME=D:\uiiiii\jdk"
set "PATH=D:\uiiiii\jdk\bin;%PATH%"
mvn spring-boot:run
