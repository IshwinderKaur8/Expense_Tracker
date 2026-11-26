# Runbook

## Start / stop
- Start: `docker compose up -d`      Stop: `docker compose down`  (never add `-v`: it deletes the database)
- Status: `docker compose ps`        Logs: `docker compose logs -f app`

## Backup (daily via Task Scheduler)
`docker compose run --rm backup`  -> `backups/expense_tracker_<timestamp>.sql.gz`
Copy the `backups/` folder off this machine regularly. Never commit it.

## Restore
1. `docker compose stop app`
2. `docker compose run --rm -e BACKUP_FILE=<file name> restore`
3. `docker compose start app`
Verify row counts and log in. (Last tested: <date>)

## Update to a new version
`git pull` then `docker compose up -d --build`   (take a backup first)

## Login locked out
Five wrong passwords block that account+address for 15 minutes. Restarting the app clears the block.