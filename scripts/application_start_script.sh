#!/usr/bin/env bash
set -euo pipefail

sudo tee /etc/systemd/system/unitbilling.service > /dev/null << 'EOF'
[Unit]
Description=Unit Billing Spring Boot Application
After=syslog.target network.target postgresql.service

[Service]
User=vboxuser

#ff: right path & name
ExecStart=/usr/bin/java -jar /opt/unit-billing/app.jar
SuccessExitStatus=143

Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF


sudo systemctl daemon-reload

sudo systemctl enable unitbilling


sudo systemctl start unitbilling
