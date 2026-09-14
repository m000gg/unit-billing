/*
 * Copyright 2026 Vladyslav Livandovskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE TABLE ledger_entries (
                                id UUID PRIMARY KEY,
                                subscriber_id UUID NOT NULL REFERENCES application_users(id),
                                amount NUMERIC(19, 4) NOT NULL,
                                type VARCHAR(50) NOT NULL,
                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                description TEXT,
                                original_entry_id UUID REFERENCES ledger_entries(id),
                                source VARCHAR(50) NOT NULL,
                                performed_by_admin UUID REFERENCES admins(id),
                                user_currency VARCHAR(3) NOT NULL,
                                base_currency VARCHAR(3) NOT NULL,
                                exchange_rate_source VARCHAR(50),
                                exchange_rate NUMERIC(19, 6) NOT NULL,
                                amount_in_base_currency NUMERIC(19, 4) NOT NULL

);

CREATE INDEX idx_ledger_entries_subscriber_id_created_at
    ON ledger_entries (subscriber_id, created_at DESC);