# Troubleshooting

Java not installed: install a 64-bit Java 21 runtime and run setup again.

PostgreSQL or password errors: confirm the service is running, database/user names match `config\application.properties`, and the user can connect in pgAdmin.

Port 8080 in use: run `netstat -ano | findstr :8080`, stop the owning application, or change `server.port` and the firewall rule.

Phone cannot connect: confirm both devices are on the same LAN, use the server IPv4 from `ipconfig`, allow TCP 8080 through Windows Firewall, and do not use `localhost` on the phone.

Application errors: read `logs\flowops.log`. Backup errors usually mean PostgreSQL client tools are not installed or not on PATH.
