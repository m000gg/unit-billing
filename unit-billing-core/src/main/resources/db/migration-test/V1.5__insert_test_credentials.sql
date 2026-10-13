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

INSERT INTO application_users (
    id,
    first_name,
    last_name,
    email,
    phone,
    password,
    balance,
    country,
    city,
    region,
    street,
    house_number,
    apartment,
    postal_code,
    is_deleted,
    user_currency
) VALUES
      (
          '550e8400-e29b-41d4-a716-446655440001',
          'John',
          'Doe',
          'john.doe@example.com',
          '+15550199',
          '$2a$12$wwFwCF6vxcQMZu9QizKZJOPmDaacESu5Od58jdAwv4h6tF7XZ44mC', -- hashed password ("password123")
          500.00,
          'USA',
          'New York',
          'NY',
          'Broadway',
          '100',
          '4B',
          '10001',
          FALSE,
       'USD'
      ),
      (
          '550e8400-e29b-41d4-a716-446655440002',
          'Alice',
          'Smith',
          'alice.smith@example.com',
          '+15550188',
          '$2a$12$wwFwCF6vxcQMZu9QizKZJOPmDaacESu5Od58jdAwv4h6tF7XZ44mC',
          1250.50,
          'USA',
          'San Francisco',
          'CA',
          'Market Street',
          '500',
          '12A',
          '94105',
          FALSE,
         'USD'
      ),
      (
          '550e8400-e29b-41d4-a716-446655440003',
          'Michael',
          'Johnson',
          'michael.johnson@example.com',
          '+4420794609',
          '$2a$12$wwFwCF6vxcQMZu9QizKZJOPmDaacESu5Od58jdAwv4h6tF7XZ44mC',
          3200.00,
          'United Kingdom',
          'London',
          'Greater London',
          'Baker Street',
          '221',
          'B',
          'NW1 6XE',
          FALSE,
       'GBP'
      );