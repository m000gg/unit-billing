#!/usr/bin/env bash
set -euo pipefail

POSTGRES_VERSION=18
JAVA_VERSION=21
DOMAIN="unit-billing.xyz"


echo "=== 1. System update ==="
sudo apt update && sudo apt upgrade -y

echo "=== 2. Install PostgreSQL ==="
sudo apt install -y postgresql-$POSTGRES_VERSION

echo "=== 3. Postgres: listen on localhost only, create app DB/user ==="
DB_NAME="${DB_NAME:-unitbilling}"
DB_USER="${DB_USER:-unitbillingadmin}"
DB_PASS="${DB_PASS:-changeme}"

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}'" | grep -q 1; then
    sudo -u postgres psql -c "CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASS}';"
fi

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" | grep -q 1; then
    sudo -u postgres psql -c "CREATE DATABASE \"${DB_NAME}\" OWNER ${DB_USER};"
fi

echo "=== 4. Postgres: ensure local password authentication (scram-sha-256) ==="
PG_HBA_FILE=$(sudo -u postgres psql -tAc "SHOW hba_file;" | xargs)


sudo sed -i -E 's/^(local\s+all\s+all\s+)peer/\1scram-sha-256/g' "$PG_HBA_FILE" || true
sudo systemctl reload postgresql

echo "=== 5. Install Java (OpenJDK ${JAVA_VERSION}) ==="
sudo apt install -y openjdk-${JAVA_VERSION}-jdk

echo "=== 6. Nginx reverse proxy for admin/client apps ==="
sudo apt-get install -y nginx

sudo tee /etc/nginx/sites-available/unit-billing > /dev/null <<NGINX
server {
    listen 80;
    server_name ${DOMAIN};

    location /admin/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /client/ {
        proxy_pass http://127.0.0.1:8081/;
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
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw --force enable


echo "=== 8. Installing Certbot & SSL ==="
sudo apt-get install -y certbot python3-certbot-nginx

sudo certbot --nginx -d "${DOMAIN}" --non-interactive --agree-tos -m nonameb281@gmail.com --redirect

echo "=== SSL setup complete for ${DOMAIN} ==="

echo "=== Done. Infra ready (PostgreSQL + Java ${JAVA_VERSION} + Nginx), no app deployed yet. ==="
