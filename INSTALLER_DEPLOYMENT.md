# Installer deployment (technical)

There are two separate Inno Setup packages. `FlowOps-Setup-<version>.exe` is the full server installer and belongs on one central server only. `FlowOps-Client-Setup.exe` is the lightweight employee-PC launcher and never includes the backend, Java, PostgreSQL, a service, backups, or secrets.

The server installer places the JAR and a private Java 21 runtime under `C:\Program Files\FlowOps`, while writable configuration, secrets, logs, backups and runtime state live under `C:\ProgramData\FlowOps`.

The service wrapper is WinSW. Place the approved `WinSW-x64.exe` at `installer\prerequisites\WinSW-x64.exe`; the build renames it to `FlowOps.exe` in the payload. The official PostgreSQL installer must be placed at `installer\prerequisites\postgresql-installer.exe` to enable the automatic PostgreSQL option. The repository does not download or silently fetch either binary.

Set the release version in `VERSION.txt`, then run `scripts\build-installer.bat`. It builds React, packages the Spring Boot JAR, creates a trimmed Java runtime with `jlink`, prepares the payload, and calls `ISCC.exe`. For version `1.1.0`, the output is `dist\installer\FlowOps-Setup-1.1.0.exe`.

The wizard detects PostgreSQL services or `psql.exe`. For an existing server it collects administrator credentials only during setup. For automatic PostgreSQL installation it uses the official prerequisite and a generated temporary postgres password. A fresh install creates `flowops` and `flowops_user`, generates the application DB password and JWT secret, writes protected ProgramData config, creates the first admin bootstrap values, and deletes temporary admin/setup files. An upgrade reads the retained configuration and continues using its existing database name, user, password, and JWT secret.

The installer creates the Private/Domain-profile firewall rule `FlowOps` for TCP 8080 (never the Public profile), installs and starts the `FlowOps` service, and creates Start Menu shortcuts for opening, backing up, restoring, and reading documentation. This remains a server installer and must be run on one always-on machine, not every workstation. Uninstall stops/removes the service and firewall rule but retains ProgramData data, secrets and backups by default. Pre-rebrand test installs are detected through their legacy service and paths; persistent data is copied to the new ProgramData directory before the old service and application binaries are removed.

The server shortcuts are named **Open FlowOps Server** and **FlowOps Server** and continue to open `localhost`; they do not control the service. Build the independent employee package with `scripts\build-client-installer.bat`. It produces `dist\installer\FlowOps-Client-Setup.exe`, installs per user without elevation, asks for the central server URL, and offers a connection test. The installed **FlowOps** shortcut uses Edge/Chrome app mode, while **Configure FlowOps Client** changes `%LOCALAPPDATA%\FlowOps Client\server-url.txt` without reinstalling anything.

Database startup is protected by WinSW restart-on-failure. If PostgreSQL starts a few seconds later after reboot, the service retries rather than requiring a manual daily start. A clean-machine, reboot, PostgreSQL-install, and phone/LAN test still require a Windows test environment and are not claimed here.
