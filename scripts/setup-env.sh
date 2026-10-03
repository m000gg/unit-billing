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

read -r -p "PostgreSQL version [18]: " input_pg
POSTGRES_VERSION="${input_pg:-18}"

read -r -p "Java version [21]: " input_java
JAVA_VERSION="${input_java:-21}"

read -r -p "Nginx VM IP (Required): " input_nginx_ip
NGINX_VM_IP="${input_nginx_ip:?NGINX_VM_IP must be set}"

read -r -p "Database Name [unit-billing]: " input_dbname
DB_NAME="${input_dbname:-unit-billing}"

read -r -p "Database User [unitbillingadmin]: " input_dbuser
DB_USER="${input_dbuser:-unitbillingadmin}"

read -r -s -p "Database Password (Required): " input_dbpass
echo ""
DB_PASS="${input_dbpass:?DB_PASS must be set}"

read -r -p "Initial Admin Email (Required): " input_admin_email
INITIAL_ADMIN_EMAIL="${input_admin_email:?INITIAL_ADMIN_EMAIL must be set}"

read -r -s -p "Initial Admin Password (Required): " input_admin_pass
echo ""
INITIAL_ADMIN_PASSWORD="${input_admin_pass:?INITIAL_ADMIN_PASSWORD must be set}"

read -r -p "Service user [unitbilling]: " input_suser
SERVICE_USER="${input_suser:-unitbilling}"

read -r -p "Deploy user [deployer]: " input_duser
DEPLOY_USER="${input_duser:-deployer}"

read -r -p "App directory [/opt/unit-billing]: " input_appdir
APP_DIR="${input_appdir:-/opt/unit-billing}"

read -r -p "Service name [unit-billing]: " input_sname
SERVICE_NAME="${input_sname:-unit-billing}"
echo "---------------------------"

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

echo "=== 6. Firewall (ufw): SSH only, web ports are opened by setup-https.sh ==="
sudo apt-get install -y ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH
sudo ufw allow from "$NGINX_VM_IP" to any port 8080 proto tcp
sudo ufw --force enable

echo "=== 7. Provisioning app user, directory, env file and systemd service ==="
ENV_FILE="${APP_DIR}/.env"

if ! id -u "$SERVICE_USER" >/dev/null 2>&1; then
    sudo useradd --system --no-create-home --shell /usr/sbin/nologin "$SERVICE_USER"
fi

sudo mkdir -p "$APP_DIR"

sudo tee "$ENV_FILE" > /dev/null <<EOF
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASS}
INITIAL_ADMIN_EMAIL=${INITIAL_ADMIN_EMAIL}
INITIAL_ADMIN_PASSWORD=${INITIAL_ADMIN_PASSWORD}
EOF

sudo chown -R "$SERVICE_USER:$SERVICE_USER" "$APP_DIR"
sudo chmod 775 "$APP_DIR"
sudo chmod 600 "$ENV_FILE"

sudo tee /etc/systemd/system/${SERVICE_NAME}.service > /dev/null <<EOF
[Unit]
Description=Unit Billing Spring Boot Application
After=syslog.target network.target postgresql.service

[Service]
User=${SERVICE_USER}
EnvironmentFile=${ENV_FILE}
ExecStart=/usr/bin/java -jar ${APP_DIR}/${SERVICE_NAME}.jar --spring.profiles.active=prod
SuccessExitStatus=143

Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable ${SERVICE_NAME}

echo "=== 8. Provisioning CI/CD deploy user (visudo) ==="
if ! id -u "$DEPLOY_USER" >/dev/null 2>&1; then
    sudo useradd -m -s /bin/bash "$DEPLOY_USER"
fi

sudo usermod -aG "$SERVICE_USER" "$DEPLOY_USER"

SUDOERS_FILE="/etc/sudoers.d/${DEPLOY_USER}-deploy"
SUDOERS_TMP="$(mktemp)"

cat > "$SUDOERS_TMP" <<EOF
${DEPLOY_USER} ALL=(root) NOPASSWD: /bin/systemctl stop ${SERVICE_NAME}, /bin/mv ${APP_DIR}/${SERVICE_NAME}.jar.new ${APP_DIR}/${SERVICE_NAME}.jar, /bin/chown ${SERVICE_USER}\:${SERVICE_USER} ${APP_DIR}/${SERVICE_NAME}.jar, /bin/systemctl start ${SERVICE_NAME}, /bin/systemctl status ${SERVICE_NAME} --no-pager
EOF

if ! sudo visudo -cf "$SUDOERS_TMP"; then
    rm -f "$SUDOERS_TMP"
    echo "Invalid sudoers, aborting" >&2
    exit 1
fi

sudo install -m 0440 -o root -g root "$SUDOERS_TMP" "$SUDOERS_FILE"
rm -f "$SUDOERS_TMP"

echo "=== Done. Environment ready (Postgres, Java, service unit, deploy user). Next: run setup-https.sh, then set up Jenkins SSH key and run deploy.sh ==="