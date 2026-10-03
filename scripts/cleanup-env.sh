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
# Reverts EVERYTHING done by setup-env.sh. DESTRUCTIVE: the database,
# the application directory (jar + .env), users and firewall rules are
# removed permanently.
#
set -euo pipefail

# ---------- Config (Interactive) ----------
echo "=== Configuration ==="
echo "Press ENTER to accept the default values in brackets."

read -r -p "PostgreSQL version [18]: " input_pg
POSTGRES_VERSION="${input_pg:-18}"

read -r -p "Java version [21]: " input_java
JAVA_VERSION="${input_java:-21}"

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

if [[ "${FORCE:-0}" != "1" ]]; then
    echo "This will permanently delete: PostgreSQL ${POSTGRES_VERSION} with ALL data,"
    echo "Java ${JAVA_VERSION}, ${APP_DIR}, users '${SERVICE_USER}' and '${DEPLOY_USER}'"
    echo "(incl. /home/${DEPLOY_USER}), the systemd unit, the sudoers file and ufw."
    read -r -p "Type 'yes' to continue: " answer
    [[ "$answer" == "yes" ]] || { echo "Aborted."; exit 1; }
fi

echo "=== 1. Remove CI/CD deploy user and sudoers rule ==="
sudo rm -f "/etc/sudoers.d/${DEPLOY_USER}-deploy"
if id -u "$DEPLOY_USER" >/dev/null 2>&1; then
    sudo pkill -u "$DEPLOY_USER" || true
    sleep 1
    sudo userdel -r "$DEPLOY_USER" || true
fi

echo "=== 2. Remove systemd service, app user and app directory ==="
sudo systemctl stop "${SERVICE_NAME}" 2>/dev/null || true
sudo systemctl disable "${SERVICE_NAME}" 2>/dev/null || true
sudo rm -f "/etc/systemd/system/${SERVICE_NAME}.service"
sudo rm -f "/etc/systemd/system/multi-user.target.wants/${SERVICE_NAME}.service"
sudo systemctl daemon-reload
sudo systemctl reset-failed "${SERVICE_NAME}" 2>/dev/null || true

sudo rm -rf "$APP_DIR"

if id -u "$SERVICE_USER" >/dev/null 2>&1; then
    sudo userdel "$SERVICE_USER" || true
fi
if getent group "$SERVICE_USER" >/dev/null 2>&1; then
    sudo groupdel "$SERVICE_USER" || true
fi

echo "=== 3. Firewall (ufw): reset, disable, remove ==="
if command -v ufw >/dev/null 2>&1; then
    sudo ufw --force disable || true
    sudo ufw --force reset || true
fi
sudo apt-get purge -y ufw || true

echo "=== 4. Remove PostgreSQL ${POSTGRES_VERSION} (DB, user, config, data) ==="
sudo systemctl stop postgresql 2>/dev/null || true
sudo apt-get purge -y \
    "postgresql-${POSTGRES_VERSION}" \
    "postgresql-client-${POSTGRES_VERSION}" \
    postgresql-common \
    postgresql-client-common || true
sudo rm -rf "/etc/postgresql" "/var/lib/postgresql" "/var/log/postgresql" "/etc/postgresql-common"

echo "=== 5. Remove Java (OpenJDK ${JAVA_VERSION}) ==="
sudo apt-get purge -y \
    "openjdk-${JAVA_VERSION}-jdk" \
    "openjdk-${JAVA_VERSION}-jdk-headless" \
    "openjdk-${JAVA_VERSION}-jre" \
    "openjdk-${JAVA_VERSION}-jre-headless" || true

echo "=== 6. Remove orphaned dependencies ==="
sudo apt-get autoremove -y --purge
sudo apt-get clean

echo "=== Done. Everything from setup-env.sh has been removed (except the apt upgrade). ==="