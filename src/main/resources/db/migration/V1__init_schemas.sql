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

CREATE TABLE application_users (
                                   id UUID PRIMARY KEY,
                                   first_name VARCHAR(255),
                                   last_name VARCHAR(255),
                                   email VARCHAR(255) NOT NULL UNIQUE,
                                   phone VARCHAR(255),
                                   password VARCHAR(255),
                                   balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
                                   country VARCHAR(255),
                                   city VARCHAR(255),
                                   region VARCHAR(255),
                                   user_currency VARCHAR(3),
                                   street VARCHAR(255),
                                   house_number VARCHAR(255),
                                   apartment VARCHAR(255),
                                   postal_code VARCHAR(255),
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   is_deleted BOOLEAN DEFAULT FALSE,
                                   version BIGINT NOT NULL DEFAULT 0
);


