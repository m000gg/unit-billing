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

# ---------- Config (Interactive) ----------
echo "=== Configuration ==="
echo "Press ENTER to accept the default values in brackets."

read -r -p "Domain name [unit-billing.xyz]: " input_domain
DOMAIN="${input_domain:-unit-billing.xyz}"

read -r -p "Nginx site name [unit-billing]: " input_sitename
SITE_NAME="${input_sitename:-unit-billing}"

read -r -p "Certbot Email (Required): " input_email
CERTBOT_EMAIL="${input_email:?CERTBOT_EMAIL must be set}"

read -r -p "App Upstream IP [10.0.0.5]: " input_upstream
APP_UPSTREAM_IP="${input_upstream:-10.0.0.5}"
echo "---------------------------"

log() { echo "=== $* ==="; }
trap 'echo "FAILED at line $LINENO: $BASH_COMMAND" >&2' ERR

echo "=== 1. Nginx reverse proxy (monolith on :8080) ==="
sudo apt-get install -y nginx

sudo tee /etc/nginx/sites-available/${SITE_NAME} > /dev/null <<NGINX
server {
    listen 80;
    server_name ${DOMAIN};

    location / {
        proxy_pass http://${APP_UPSTREAM_IP}:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGINX

sudo ln -sf /etc/nginx/sites-available/${SITE_NAME} /etc/nginx/sites-enabled/${SITE_NAME}
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl restart nginx
sudo systemctl enable nginx

echo "=== 2. Firewall (ufw): open 80/443 ==="
sudo ufw allow 'Nginx Full'

echo "=== 3. Installing Certbot & SSL ==="
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d "${DOMAIN}" --non-interactive --agree-tos -m "${CERTBOT_EMAIL}" --redirect

echo "=== Done. https://${DOMAIN} is served by nginx with a Let's Encrypt certificate ==="