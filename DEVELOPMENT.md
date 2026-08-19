# Local development

The development environment is intentionally separate from the installed FlowOps application.

Frontend:

```text
npm run dev
http://localhost:5173
```

Run it from `frontend`. Vite proxies relative `/api` requests to `http://localhost:8081`.

Backend:

```powershell
.\scripts\start-dev.ps1
```

Alternatively, run `scripts\start-dev.bat`. The backend uses Spring profile `dev`, port `8081`, and the existing PostgreSQL database `kanban_db`. The expected local PostgreSQL role is `postgres`; it is not the database name. Put a local password (if required) in the ignored file `backend\config\dev-secrets.properties`, based on `backend\config\dev-secrets.properties.example`.

Do not create, rename, drop, or reset `kanban_db`. Do not use `flowops_user`, `flowops`, or `C:\ProgramData\FlowOps\config\secrets.properties` for development. The dev profile has its own JWT secret source and does not import installed production configuration.

When testing a production release:

```text
build installer
install/upgrade
desktop shortcut opens http://localhost:8080
database: flowops
profile: prod
```

The installed Windows service `FlowOps` remains on port `8080`. Developers should not edit files inside `C:\Program Files\FlowOps`; those are generated installed artifacts.
