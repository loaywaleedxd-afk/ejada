#!/usr/bin/env bash
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080,8081,8082,8083,8084 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { try { Stop-Process -Id \$_ -Force } catch {} }"
echo "stopped all services (ports 8080-8084)"
