# FlowOps Deployment

## Central server architecture

Install the full FlowOps server on **one** always-on Windows server or dedicated PC. That machine alone hosts the Spring Boot service, PostgreSQL, backups, restore tools, and production configuration. Do not run the server installer on staff, manager, or administrator workstations.

Client devices connect through the server URL:

- server machine: `http://localhost:8080`
- company devices during testing: `http://SERVER-LAN-IP:8080`
- preferred LAN name: `http://flowops-server:8080`
- future recommended name: `https://flowops.company.local`

Set `APP_BASE_URL` (or `app.base-url` in `C:\ProgramData\FlowOps\config\application.properties`) to the stable address employees use. Use an internal DNS hostname, a static/reserved server IP, or a DHCP reservation; do not save an automatically detected DHCP address as permanent configuration. Activation links use only this configured value and never the request host or a detected network adapter. In production, a missing, invalid, localhost, loopback, or wildcard value disables activation-link generation and shows the administrator a configuration error instead of creating a misleading link.

For an installed Windows server, the exact configuration procedure is:

1. Open `C:\ProgramData\FlowOps\config\application.properties` as an administrator.
2. Set `app.base-url=http://flowops-server:8080` (or the stable reserved-IP/HTTPS URL selected by IT), with no trailing slash.
3. Restart the **FlowOps** Windows service.
4. Open **System → Client Access** and confirm **Company address: Configured** and **Activation links use: http://flowops-server:8080**.

`APP_BASE_URL=http://flowops-server:8080` is the environment-variable equivalent for deployments that manage service environment variables. The installed properties file is usually simpler and is retained across upgrades. The development profile intentionally uses `http://localhost:5173`; this exception is not enabled in production.

The handover runtime is one `app\flowops.jar` process plus PostgreSQL. Java 21 is required; Node, npm, Maven, Git, VS Code and Laragon are not required on the server.

Install PostgreSQL, create database `flowops` and dedicated user `flowops_user`, then grant that user ownership of the database. Edit only `config\application.properties`. Run `scripts\setup.bat`, then `scripts\start.bat`. Rebranded upgrades continue using database names and credentials already recorded in their retained configuration.

The installer allows inbound TCP 8080 on Windows Private and Domain profiles only. Public-profile access is intentionally not enabled. Find the server IPv4 address with `ipconfig`, or open **Client Access** as an administrator. Detected addresses are diagnostic/testing candidates only and never replace `APP_BASE_URL`. Users on the same company network open `http://SERVER-IP:8080`; phones must be connected to the same Wi-Fi. Production uses same-origin `/api` calls, so phones never call their own localhost. PostgreSQL remains local to the server and its port must not be opened company-wide.

The production profile uses Hibernate `update`, never `create-drop`. Demo data is profile-only and disabled. Logs are in `logs\flowops.log` with size/history rotation. For upgrades, stop the app, replace the JAR, back up the database, and start it again.

HTTPS can later be added with IIS/Nginx and a trusted company certificate. Browser installation support is generally restricted to secure contexts (HTTPS, with localhost as a development exception), so a plain LAN IP over HTTP may provide normal web access while limiting installability, service workers, and future push notifications.

## Windows client installation

Build the lightweight package with `scripts\build-client-installer.bat`, then give `dist\installer\FlowOps-Client-Setup.exe` to company IT. Run this client installer on each staff PC, enter the configured company URL, and optionally use **Test Connection**. A failed test does not prevent installation so setup can be prepared before a network or server is available.

The client package installs only the FlowOps icon, PowerShell launcher, URL configuration, Start Menu shortcut, and optional desktop shortcut. It does not install Java, Spring Boot, PostgreSQL, a Windows service, server backups, database credentials, JWT secrets, or any second backend. The URL is stored per user at `%LOCALAPPDATA%\FlowOps Client\server-url.txt`.

Launching **FlowOps** checks standard Microsoft Edge installation locations first and opens `msedge.exe --app="<FLOWOPS_URL>"`. It then checks standard Chrome locations and uses the same app-mode argument. If neither browser is found, it opens the URL in the Windows default browser. App mode removes normal tabs and the address bar; this behavior must be confirmed on a representative staff PC.

To change the server later, open **Start Menu → FlowOps → Configure FlowOps Client**, enter the new HTTP/HTTPS URL, and click OK. The backend and database are unaffected and do not need reinstalling.

## PWA and mobile installation

- Windows (Edge/Chrome): the lightweight FlowOps Client above is recommended during HTTP LAN deployment. Browser PWA installation remains available when the browser permits it.
- Android (Chrome): open the server URL and choose **Install app** or **Add to Home screen**.
- iPhone/iPad (Safari): open the server URL, tap **Share**, choose **Add to Home Screen**, and launch FlowOps from the Home Screen.

All authoritative tasks, reports, accounts, and notifications remain in central PostgreSQL. The PWA caches only its application shell and static assets; `/api` responses are network-only and offline editing is not provided.
