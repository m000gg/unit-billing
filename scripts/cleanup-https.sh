#!/usr/bin/env bash
#
# Copyright 2026 Vladyslav Livandovskyi
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
set -euo pipefail

# ---------- Config (must match setup-https.sh) ----------
DOMAIN="${DOMAIN:-unit-billing.xyz}"
SITE_NAME="unit-billing"
SITE_AVAILABLE="/etc/nginx/sites-available/${SITE_NAME}"
SITE_ENABLED="/etc/nginx/sites-enabled/${SITE_NAME}"
DRY_RUN="${DRY_RUN:-0}"

log() { echo "=== $* ==="; }
trap 'echo "FAILED at line $LINENO: $BASH_COMMAND" >&2' ERR

run() {
    if [[ "$DRY_RUN" == "1" ]]; then
        echo "[dry-run] $*"
    else
        "$@"
    fi
}

if [[ "${FORCE:-0}" != "1" && "$DRY_RUN" != "1" ]]; then
    echo "This will remove for domain '${DOMAIN}' ONLY:"
    echo "  - ${SITE_AVAILABLE} and its symlink in sites-enabled"
    echo "  - the Let's Encrypt certificate '${DOMAIN}'"
    [[ "${REMOVE_FW_RULE:-0}" == "1" ]] && echo "  - ufw rule 'Nginx Full'"
    read -r -p "Type 'yes' to continue: " answer
    [[ "$answer" == "yes" ]] || { echo "Aborted."; exit 1; }
fi

echo "=== 1. Nginx: remove site '${SITE_NAME}' ==="
if sudo test -e "$SITE_AVAILABLE" || sudo test -L "$SITE_ENABLED"; then

    if sudo test -e "$SITE_AVAILABLE" && ! sudo grep -Eq "server_name[[:space:]]+${DOMAIN//./\\.}[[:space:];]" "$SITE_AVAILABLE"; then
        echo "REFUSING: ${SITE_AVAILABLE} exists but does not contain server_name ${DOMAIN}." >&2
        echo "It does not look like the file created by setup-https.sh." >&2
        exit 1
    fi

    BACKUP_DIR="$(mktemp -d)"
    if [[ "$DRY_RUN" != "1" ]]; then
        sudo test -e "$SITE_AVAILABLE" && sudo cp -a "$SITE_AVAILABLE" "$BACKUP_DIR/"
    fi

    run sudo rm -f "$SITE_ENABLED"
    run sudo rm -f "$SITE_AVAILABLE"

    if [[ "$DRY_RUN" != "1" ]]; then
        if sudo nginx -t; then
            sudo systemctl reload nginx
            rm -rf "$BACKUP_DIR"
        else
            echo "nginx -t failed after removal, rolling back..." >&2
            if [[ -e "$BACKUP_DIR/${SITE_NAME}" ]]; then
                sudo cp -a "$BACKUP_DIR/${SITE_NAME}" "$SITE_AVAILABLE"
                sudo ln -sf "$SITE_AVAILABLE" "$SITE_ENABLED"
            fi
            echo "Rolled back. Backup kept in ${BACKUP_DIR}" >&2
            exit 1
        fi
    else
        echo "[dry-run] sudo nginx -t && sudo systemctl reload nginx"
    fi
else
    echo "Nothing to do: nginx site '${SITE_NAME}' not found."
fi

echo "=== 2. Certbot: delete certificate '${DOMAIN}' only ==="
if sudo test -f "/etc/letsencrypt/renewal/${DOMAIN}.conf"; then
    if sudo grep -rIl --include='*' "letsencrypt/live/${DOMAIN}/" /etc/nginx 2>/dev/null | grep -q .; then
        echo "SKIPPING cert deletion: still referenced by remaining nginx config:" >&2
        sudo grep -rIl "letsencrypt/live/${DOMAIN}/" /etc/nginx >&2 || true
    else
        run sudo certbot delete --cert-name "${DOMAIN}" --non-interactive
    fi
else
    echo "Nothing to do: no certbot certificate named '${DOMAIN}'."
fi

echo "=== 3. Firewall (ufw): 'Nginx Full' rule ==="
if [[ "${REMOVE_FW_RULE:-0}" == "1" ]]; then
    run sudo ufw delete allow 'Nginx Full' || true
else
    echo "Skipped (other sites on this host likely need 80/443). Set REMOVE_FW_RULE=1 to delete it."
fi

echo "=== Done. Removed only the '${DOMAIN}' site and certificate. nginx, certbot and everything else untouched. ==="