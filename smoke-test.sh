#!/usr/bin/env bash
set +e

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"
PY="$(command -v python3 || command -v python)"
[ -z "$PY" ] && { echo "python3 (or python) is required"; exit 2; }
AJSON="{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}"

PASS=0; FAIL=0
g(){ "$PY" -c "import sys,json;print(json.load(sys.stdin)$1)" 2>/dev/null; }
ck(){ if [ "$3" = "$2" ]; then echo "  PASS  $1 ($3)"; PASS=$((PASS+1)); else echo "  FAIL  $1 expected $2 got $3"; FAIL=$((FAIL+1)); fi; }
ckn(){ "$PY" -c "import sys;sys.exit(0 if abs(float('$3' or 'nan')-$2)<0.001 else 1)" 2>/dev/null && { echo "  PASS  $1 ($3)"; PASS=$((PASS+1)); } || { echo "  FAIL  $1 expected $2 got $3"; FAIL=$((FAIL+1)); }; }
code(){ local m="$1" url="$2" tok="$3" body="$4"; local a=(-s --max-time 90 -o /tmp/smk_b -w "%{http_code}" -X "$m" "$BASE_URL$url"); [ -n "$tok" ] && a+=(-H "Authorization: Bearer $tok"); [ -n "$body" ] && a+=(-H 'Content-Type: application/json' -d "$body"); curl "${a[@]}"; }
hit(){ curl -s -o /dev/null -w '%{http_code}' --max-time 30 ${2:+-H "Authorization: Bearer $2"} "$BASE_URL$1"; }
wait_up(){ local path="$1" tok="$2" ok="${3:-200}" i c; for i in $(seq 1 50); do c=$(hit "$path" "$tok"); case ",$ok," in *",$c,"*) return 0;; esac; sleep 4; done; return 1; }

echo "== Ejada E2E smoke against $BASE_URL =="
echo "warming services (a cold free-tier service can take ~3 min to boot)..."
curl -s -o /dev/null --max-time 8 "$BASE_URL/api/shop/products" &
curl -s -o /dev/null --max-time 8 "$BASE_URL/api/inventory/products" &
curl -s -o /dev/null --max-time 8 -X POST "$BASE_URL/api/auth/login" -H 'Content-Type: application/json' -d "$AJSON" &
wait
wait_up "/actuator/health" "" "200" && echo "  gateway ready" || echo "  gateway not ready"
wait_up "/api/shop/products" "" "200" && echo "  shop ready" || echo "  shop not ready"
wait_up "/api/inventory/products" "" "200" && echo "  inventory ready" || echo "  inventory not ready"
ATOK=""
for i in $(seq 1 50); do ATOK=$(curl -s --max-time 30 -X POST "$BASE_URL/api/auth/login" -H 'Content-Type: application/json' -d "$AJSON" | g "['accessToken']"); [ -n "$ATOK" ] && break; sleep 4; done
[ -n "$ATOK" ] && echo "  auth ready" || echo "  auth not ready"
wait_up "/api/wallet/wallets/999999" "$ATOK" "200,403,404" && echo "  wallet ready" || echo "  wallet not ready"

echo
echo "-- admin --"
[ -n "$ATOK" ] && { echo "  PASS  admin login"; PASS=$((PASS+1)); } || { echo "  FAIL  admin login"; FAIL=$((FAIL+1)); }
SKU="SMOKE-$(date +%s)"
PROD=$(curl -s --max-time 90 -X POST "$BASE_URL/api/shop/products" -H "Authorization: Bearer $ATOK" -H 'Content-Type: application/json' \
  -d "{\"sku\":\"$SKU\",\"name\":\"Smoke Widget\",\"price\":20,\"initialStock\":30}")
PID=$(echo "$PROD" | g "['id']")
[ -n "$PID" ] && { echo "  PASS  create product id=$PID"; PASS=$((PASS+1)); } || { echo "  FAIL  create product: $PROD"; FAIL=$((FAIL+1)); }
INVP=$(curl -s --max-time 60 "$BASE_URL/api/inventory/products/sku/$SKU")
ckn "product synced to inventory (qty 30)" 30 "$(echo "$INVP" | g "['quantityAvailable']")"
IID=$(echo "$INVP" | g "['id']")
ck "update product price (PUT)" 200 "$(code PUT "/api/shop/products/$PID" "$ATOK" "{\"sku\":\"$SKU\",\"name\":\"Smoke Widget\",\"price\":22}")"
ck "adjust inventory stock +10" 200 "$(code POST "/api/inventory/products/$IID/stock" "$ATOK" '{"delta":10}')"
ck "create user as admin" 201 "$(code POST "/api/auth/admin/users" "$ATOK" "{\"username\":\"a$SKU\",\"email\":\"a$SKU@x.com\",\"password\":\"secret123\",\"fullName\":\"A\",\"role\":\"ROLE_USER\"}")"

echo
echo "-- shopper --"
U="smk$(date +%s)"
REG=$(curl -s --max-time 90 -X POST "$BASE_URL/api/auth/register" -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"email\":\"$U@x.com\",\"password\":\"secret123\",\"fullName\":\"Smoke\"}")
TOK=$(echo "$REG" | g "['accessToken']"); UID2=$(echo "$REG" | g "['user']['id']")
[ -n "$TOK" ] && { echo "  PASS  register userId=$UID2"; PASS=$((PASS+1)); } || { echo "  FAIL  register: $REG"; FAIL=$((FAIL+1)); }
ck "deposit 100" 200 "$(code POST "/api/wallet/wallets/$UID2/deposit" "$TOK" '{"amount":100}')"
ckn "balance == 100" 100 "$(curl -s --max-time 60 "$BASE_URL/api/wallet/wallets/$UID2" -H "Authorization: Bearer $TOK" | g "['balance']")"
ck "browse products (public)" 200 "$(code GET "/api/shop/products" '' '')"
ck "add to cart x2" 200 "$(code POST "/api/shop/carts/$UID2/items" "$TOK" "{\"productId\":$PID,\"quantity\":2}")"
CO=$(code POST "/api/shop/orders/checkout/$UID2" "$TOK" '')
ST=$(cat /tmp/smk_b | g "['status']")
if [ "$CO" = "201" ] && [ "$ST" = "PAID" ]; then echo "  PASS  checkout (PAID)"; PASS=$((PASS+1)); OID=$(cat /tmp/smk_b | g "['id']"); else echo "  FAIL  checkout got $CO status=$ST | $(head -c 140 /tmp/smk_b)"; FAIL=$((FAIL+1)); fi
ckn "balance debited to 60 (100 - 2*20)" 60 "$(curl -s --max-time 60 "$BASE_URL/api/wallet/wallets/$UID2" -H "Authorization: Bearer $TOK" | g "['balance']")"
ck "order detail" 200 "$(code GET "/api/shop/orders/$OID" "$TOK")"
ck "ownership: other user's wallet -> 403" 403 "$(code GET "/api/wallet/wallets/1" "$TOK")"
ck "RBAC: non-admin create product -> 403" 403 "$(code POST "/api/shop/products" "$TOK" "{\"sku\":\"z$SKU\",\"name\":\"x\",\"price\":1}")"

echo
echo "=================================="
echo "  RESULT: PASS=$PASS  FAIL=$FAIL"
echo "=================================="
[ "$FAIL" -eq 0 ]
