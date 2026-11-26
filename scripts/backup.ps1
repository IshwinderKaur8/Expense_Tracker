# Takes a database backup of the local Docker stack
Set-Location (Split-Path -Parent $PSScriptRoot)
docker compose run --rm backup