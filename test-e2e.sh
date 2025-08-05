#!/usr/bin/env bash
set -euo pipefail

# Centralised, timestamped logging helper
log() {
  echo -e "📋 $(date '+%H:%M:%S') — $1"
}

# Helper: pretty-print JSON only if jq is available
pp_json() {
  if command -v jq >/dev/null 2>&1; then
    jq .
  else
    cat
  fi
}

# ───────── 0. Build & Up ─────────
export JIRA_MOCK=true              # active JiraMockConfig
docker compose up -d --build       # compile & (re)lance tous les services
sleep 5                            # petit temps pour que Spring démarre

# ───────── 1. Unit-Tests Reporting ─────────
log "Tests unitaires Reporting-Service…"
docker compose run --rm reporting-service \
       mvn -q -pl reporting-service test
log "Unit tests OK"

# ───────── 2. Variables utiles ─────────
source .env                         # Récupère les ports
BASE_REP="http://localhost:${REPORTING_SERVICE_PORT}/api/reporting"
BASE_CHART="http://localhost:${CHART_SERVICE_PORT}/api/charts"
BASE_EMAIL="http://localhost:${EMAIL_SERVICE_PORT}/api/emails"
PROJECT_KEY="SCRUM"
IMG_PATH="/tmp/chart.png"

# ───────── 3. Reporting ─────────
log "Rapport mensuel ${PROJECT_KEY}"
curl -s "${BASE_REP}/monthly?projectKey=${PROJECT_KEY}" | pp_json | tee report.json
log "Report saved to report.json"

# ───────── 4. Chart ─────────
log "Chart mensuel"
curl -s "${BASE_CHART}/monthly/summary?projectKey=${PROJECT_KEY}" -o "${IMG_PATH}"
log "Chart sauvé → ${IMG_PATH}"

# ───────── 5. Email ─────────
log "Envoi e-mail…"
curl -s -X POST "${BASE_EMAIL}/send/chart" \
     -F "request={\"to\":\"${MAIL_USERNAME}\",\"subject\":\"Chart ${PROJECT_KEY}\",\"templateData\":{\"projectKey\":\"${PROJECT_KEY}\",\"chartType\":\"monthly\"},\"priority\":\"LOW\"};type=application/json" \
     -F "file=@${IMG_PATH};type=image/png" | pp_json

# ───────── 6. Attente & Vérification ─────────
log "Attente de 10 s pour traitement en arrière-plan"
sleep 10
log "Test terminé. Vérifiez votre boîte mail (${MAIL_USERNAME}) et les derniers logs du service e-mail :"
docker compose logs --tail=20 email-service

log "Rapport JSON : report.json"
log "Chart PNG    : ${IMG_PATH}"
