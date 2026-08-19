# Backup and restore

Run `scripts\backup.bat`. A new PostgreSQL custom-format file is written to `backups\` and existing backups are not overwritten. Keep daily backups and copy them to a separate protected location.

To restore, stop the application, run `scripts\restore.bat backups\kovax-flowops-YYYY-MM-DD-HHMM.dump`, confirm the warning, then start the application. Restore replaces current data. Test recovery periodically on a spare database/server.

The scripts use `config\pgpass.conf` so the password is not repeated in each script. Protect this file with Windows permissions; it contains the database password.
