# Optional Windows service

The package does not bundle third-party service binaries. Use an approved WinSW or NSSM installation. Configure internal and display service name **FlowOps**, working directory as the deployment folder, executable `java.exe`, arguments `-jar app\flowops.jar --spring.profiles.active=prod --spring.config.additional-location=optional:file:config\`, and log files under `logs\`. Set startup type Automatic and make it depend on PostgreSQL. Test stop/start and reboot before handover.
