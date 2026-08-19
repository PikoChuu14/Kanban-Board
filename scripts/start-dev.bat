@echo off
setlocal
cd /d "%~dp0..\backend"
set SPRING_PROFILES_ACTIVE=dev
set SERVER_PORT=8081
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kanban_db
echo Starting Kovax FlowOps development backend on http://localhost:8081
call mvnw.cmd spring-boot:run
endlocal
