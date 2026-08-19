# Administrator guide

Log in with the first administrator created by setup. Existing authorization boundaries remain in force: only an `ADMIN` can manage company-wide accounts or database backups.

## User Management

Open **Users** in the administrator sidebar. The page lists all accounts and can be filtered by Pending Activation, Active, or Disabled; search matches names and email addresses.

To onboard an employee:

1. Select **Add User**.
2. Enter the employee's name and unique email address.
3. Choose a department from the live departments table and select `STAFF`, `MANAGER`, or `ADMIN` (the default is `STAFF`).
4. Save the user. The account starts as `PENDING_ACTIVATION` and its activation link is copied when browser clipboard permission is available.
5. If necessary, select **Copy Activation Link** in the user row and send it through an approved company channel. The link expires after 48 hours by default and generating a replacement invalidates the previous link.

Employees choose their own password on the public activation page. Tokens are random, stored only as SHA-256 hashes, expire, and work once. Raw tokens and passwords are never logged.

Use **Edit** to change a user's name, department, role, or status. Department and role changes do not rewrite historical tasks or reports. Use **Disable** for offboarding and **Reactivate** to restore access. Accounts are deliberately not deleted: old task ownership and reporting attribution must remain intact. The application refuses to disable or demote the final active administrator.

## Data Management

Open **Settings**, then **Backups & Archives**. `ADMIN` users can:

- create a PostgreSQL custom-format backup immediately;
- download/export an individual `.backup` or `.dump` file;
- open the configured backup folder when the browser is running on the Windows server;
- view installer-archived database names as read-only restore sources;
- permanently delete an unused backup after confirmation; and
- request a controlled restore.

Installed backups are stored under `C:\ProgramData\FlowOps\backups`. Development uses `backups-dev` unless `DEV_BACKUP_DIRECTORY` is set. The configured directory is normalized and download/delete operations accept filenames only, preventing arbitrary filesystem access.

Restore is intentionally delegated to the installed Windows helper. Before queuing it, FlowOps creates a new `PRE_RESTORE` safety backup. The helper then stops the `FlowOps` service, runs `pg_restore`, restarts the service, and writes status under the ProgramData runtime directory. If helper execution fails, the safety backup is retained. Restore is disabled in development and automated tests.
