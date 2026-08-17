# Kanban history snapshots

## Endpoints

- `GET /api/history/dates`
- `GET /api/history/{date}?type=START_OF_DAY|END_OF_DAY` (defaults to `END_OF_DAY`)
- `GET /api/history/{date}/users/{userId}?type=START_OF_DAY|END_OF_DAY`
- With the `dev` Spring profile only, ADMIN may call `POST /api/dev/snapshots/start` or `/end`.

History is read-only. STAFF can see only their own assigned snapshots. MANAGER can see users and tasks in their own department. ADMIN can see all snapshots. The authenticated JWT principal is used for authorization.

## Manual scenarios

1. Set Task A to `DOING`, assigned to John. Trigger `/api/dev/snapshots/start`; verify the returned/history row is `DOING` and John. Move the live task to `REVIEW`; the morning row remains `DOING`.
2. Trigger `/api/dev/snapshots/end` after the live task is `REVIEW`; verify morning is `DOING` and evening is `REVIEW`.
3. Move the live task from `REVIEW` to `DOING` after the evening trigger; verify the live task is `DOING` while the evening row is still `REVIEW`.
4. Snapshot Task B assigned to John, reassign it to Farid, then take the evening snapshot. Morning must contain John and evening Farid.
5. Rename a live project or user after a snapshot. The snapshot must retain the original stored name.
6. Trigger the same type twice. The second call returns the existing completed batch; there must be one batch and one row per task.
7. An old `DONE` task is excluded. A task updated today, including one completed today, is included.
8. A manager can query users in their department but receives `403` for another department. STAFF receives `403` for another user.
9. To test recovery, remove/omit an expected batch in a development database and run the recovery service or wait for its 15-minute check. The batch has `recovered=true`, intended `scheduledFor`, and actual `capturedAt`.

## SQL verification

```sql
select id, snapshot_date, snapshot_type, scheduled_for, captured_at,
       recovered, status, task_count
from snapshot_batches
order by snapshot_date desc, snapshot_type;
```

```sql
select ts.task_id, ts.title, ts.status, ts.workload,
       ts.assignee_name as assignee, ts.board_name as board,
       ts.department_name as department,
       sb.snapshot_date, sb.snapshot_type
from task_snapshots ts
join snapshot_batches sb on sb.id = ts.batch_id
order by sb.snapshot_date desc, sb.snapshot_type, ts.position;
```

```sql
select batch_id, task_id, count(*)
from task_snapshots
group by batch_id, task_id
having count(*) > 1;
```

The last query must return zero rows.

## MVP limitations

Recovery captures current live state after a missed cutoff and explicitly marks it recovered; it cannot reconstruct the exact historical cutoff without event sourcing. There is no calendar UI, AFTER_HOURS type, working-day configuration, audit log, export, or notification support yet.
