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

# ---------- Config ----------
POSTGRES_VERSION="${POSTGRES_VERSION:-18}"
JAVA_VERSION="${JAVA_VERSION:-21}"
DOMAIN="${DOMAIN:-unit-billing.xyz}"
DB_NAME="${DB_NAME:-unit-billing}"
DB_USER="${DB_USER:-unitbillingadmin}"
: "${DB_PASS:?DB_PASS must be set}"
: "${CERTBOT_EMAIL:?CERTBOT_EMAIL must be set}"
: "${INITIAL_ADMIN_EMAIL:?INITIAL_ADMIN_EMAIL must be set}"
: "${INITIAL_ADMIN_PASSWORD:?INITIAL_ADMIN_PASSWORD must be set}"

log() { echo "=== $* ==="; }
trap 'echo "FAILED at line $LINENO: $BASH_COMMAND" >&2' ERR

echo "=== 1. System update ==="
sudo apt update && sudo apt upgrade -y

echo "=== 2. Install PostgreSQL ==="
sudo apt install -y postgresql-$POSTGRES_VERSION

echo "=== 3. Postgres: create app DB/user ==="
if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}'" | grep -q 1; then
    sudo -u postgres psql -c "CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASS}';"
fi

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" | grep -q 1; then
    sudo -u postgres psql -c "CREATE DATABASE \"${DB_NAME}\" OWNER ${DB_USER};"
fi

echo "=== 4. Postgres: localhost only + scram-sha-256 auth ==="
PG_CONF_DIR=$(sudo -u postgres psql -tAc "SHOW config_file;" | xargs | xargs dirname)
PG_HBA_FILE=$(sudo -u postgres psql -tAc "SHOW hba_file;" | xargs)

sudo sed -i -E "s/^#?listen_addresses\s*=.*/listen_addresses = 'localhost'/" "${PG_CONF_DIR}/postgresql.conf"
sudo sed -i -E 's/^(local\s+all\s+all\s+)peer/\1scram-sha-256/g' "$PG_HBA_FILE"
sudo systemctl restart postgresql

echo "=== 5. Install Java (OpenJDK ${JAVA_VERSION}) ==="
sudo apt install -y openjdk-${JAVA_VERSION}-jdk

echo "=== 6. Nginx reverse proxy (monolith on :8080) ==="
sudo apt-get install -y nginx

sudo tee /etc/nginx/sites-available/unit-billing > /dev/null <<NGINX
server {
    listen 80;
    server_name ${DOMAIN};

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGINX

sudo ln -sf /etc/nginx/sites-available/unit-billing /etc/nginx/sites-enabled/unit-billing
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl restart nginx
sudo systemctl enable nginx

echo "=== 7. Firewall (ufw) ==="
sudo apt-get install -y ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw --force enable

echo "=== 8. Installing Certbot & SSL ==="
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d "${DOMAIN}" --non-interactive --agree-tos -m "${CERTBOT_EMAIL}" --redirect

echo "=== 9. Provisioning app user, directory and systemd service ==="
SERVICE_USER="unitbilling"
APP_DIR="/opt/unit-billing"
SERVICE_NAME="unit-billing"

if ! id -u "$SERVICE_USER" >/dev/null 2>&1; then
    sudo useradd --system --no-create-home --shell /usr/sbin/nologin "$SERVICE_USER"
fi

sudo mkdir -p "$APP_DIR"
sudo chown "$SERVICE_USER:$SERVICE_USER" "$APP_DIR"

sudo tee /etc/systemd/system/${SERVICE_NAME}.service > /dev/null <<EOF
[Unit]
Description=Unit Billing Spring Boot Application
After=syslog.target network.target postgresql.service

[Service]
User=${SERVICE_USER}
Environment="INITIAL_ADMIN_EMAIL=${INITIAL_ADMIN_EMAIL}"
Environment="INITIAL_ADMIN_PASSWORD=${INITIAL_ADMIN_PASSWORD}"
ExecStart=/usr/bin/java -jar ${APP_DIR}/${SERVICE_NAME}.jar --spring.profiles.active=prod
SuccessExitStatus=143

Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable ${SERVICE_NAME}

echo "=== Done. Server ${DOMAIN} fully ready (infra + service unit). Next: run deploy.sh via Jenkins. ==="