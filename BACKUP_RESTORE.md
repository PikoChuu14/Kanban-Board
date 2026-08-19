# Backup and restore

## Administrator UI

Administrators can use **Settings → Backups & Archives** instead of opening pgAdmin or browsing server folders. **Create Backup Now** invokes `pg_dump` in custom format. The application obtains database settings from its configured runtime properties and passes the password only through the child-process environment; it is not included in command output or application logs.

Installed mode uses `C:\ProgramData\FlowOps\backups`. Override it with `BACKUP_DIRECTORY`; local development uses `DEV_BACKUP_DIRECTORY` or `backups-dev`. `backup-metadata.json` records the type, reason, source database, creation time, and status. Files created by older installers are still discovered and shown with inferred metadata.

UI restore requires the exact `RESTORE` confirmation and always creates a `PRE_RESTORE` safety backup first. It writes a constrained request inside the backup directory and starts the environment-specific restore helper. Installed mode uses `restore-request.ps1`, which verifies the source, stops the Windows service, restores, and restarts the service. Local development uses `scripts\restore-dev.ps1` with the active dev datasource and PostgreSQL credentials; it performs an atomic restore and reports completion in `backend\runtime\restore-status.json`. Tests keep restore disabled.

## Command-line fallback

Run `scripts\backup.bat`. A new PostgreSQL custom-format file is written to `backups\` and existing backups are not overwritten. Keep daily backups and copy them to a separate protected location.

To restore, stop the application, run `scripts\restore.bat backups\flowops-YYYY-MM-DD-HHMM.dump`, confirm the warning, then start the application. Restore replaces current data. Test recovery periodically on a spare database/server.

The scripts use `config\pgpass.conf` so the password is not repeated in each script. Protect this file with Windows permissions; it contains the database password.
