#!/usr/bin/env bash
set -a
source .env
set +a

mkdir -p logs

start() {
  name="$1"; url="$2"
  ( cd "services/$name" && DB_URL="$url" nohup ./mvnw -q spring-boot:run > "../../logs/$name.log" 2>&1 & )
  echo "starting $name"
}

start auth-service      "jdbc:postgresql://$DB_HOST/auth_db?sslmode=require"
start wallet-service    "jdbc:postgresql://$DB_HOST/wallet_db?sslmode=require"
start inventory-service "jdbc:postgresql://$DB_HOST/inventory_db?sslmode=require"
start shop-service      "jdbc:postgresql://$DB_HOST/shop_db?sslmode=require"
start api-gateway       ""

echo
echo "All 5 services are starting (logs in ./logs/). Give them ~40-60s."
echo "Check readiness:   grep -l 'Started .*Application' logs/*.log"
echo "Then open a public URL to the gateway:   npx localtunnel --port 8080"
