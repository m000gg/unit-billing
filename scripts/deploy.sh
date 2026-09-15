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
