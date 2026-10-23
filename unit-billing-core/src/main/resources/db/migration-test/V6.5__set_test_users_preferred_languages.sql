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

UPDATE application_users
SET preferred_language = CASE id
                             WHEN '550e8400-e29b-41d4-a716-446655440001'::uuid THEN 'en'
                             WHEN '550e8400-e29b-41d4-a716-446655440002'::uuid THEN 'en'
                             WHEN '550e8400-e29b-41d4-a716-446655440003'::uuid THEN 'uk'
                             ELSE preferred_language
    END
WHERE id IN (
             '550e8400-e29b-41d4-a716-446655440001'::uuid,
             '550e8400-e29b-41d4-a716-446655440002'::uuid,
             '550e8400-e29b-41d4-a716-446655440003'::uuid
    );