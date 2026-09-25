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

SERVICE_NAME="unit-billing"
SERVICE_USER="unitbilling"
APP_DIR="/opt/unit-billing"

log() { echo -e "\n=== $* ==="; }
trap 'echo "FAILED at line $LINENO: $BASH_COMMAND" >&2' ERR

if [ ! -f "${APP_DIR}/${SERVICE_NAME}.jar.new" ]; then
    echo "ERROR: ${APP_DIR}/${SERVICE_NAME}.jar.new not found — did Jenkins scp the artifact?" >&2
    exit 1
fi

log "Deploying new jar"
sudo systemctl stop "$SERVICE_NAME"
sudo mv "${APP_DIR}/${SERVICE_NAME}.jar.new" "${APP_DIR}/${SERVICE_NAME}.jar"
sudo chown "$SERVICE_USER:$SERVICE_USER" "${APP_DIR}/${SERVICE_NAME}.jar"
sudo systemctl start "$SERVICE_NAME"
sudo systemctl status "$SERVICE_NAME" --no-pager