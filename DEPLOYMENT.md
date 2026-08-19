# Kovax FlowOps Deployment

The handover runtime is one `app\kovax-flowops.jar` process plus PostgreSQL. Java 21 is required; Node, npm, Maven, Git, VS Code and Laragon are not required on the server.

Install PostgreSQL, create database `kovax_flowops` and dedicated user `kovax_user`, then grant that user ownership of the database. Edit only `config\application.properties`. Run `scripts\setup.bat`, then `scripts\start.bat`.

Allow inbound TCP 8080 in Windows Firewall. Find the server IPv4 address with `ipconfig`. Users on the same company network open `http://SERVER-IP:8080`; phones must be connected to the same Wi-Fi. Production uses same-origin `/api` calls, so phones never call their own localhost.

The production profile uses Hibernate `update` for this first internal deployment, never `create-drop`. Demo data is profile-only and disabled. Logs are in `logs\kovax-flowops.log` with size/history rotation. For upgrades, stop the app, replace the JAR, back up the database, and start it again.

HTTPS can later be added with IIS/Nginx and a company certificate; it is recommended for remote access and browser push features.
