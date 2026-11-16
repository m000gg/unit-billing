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

package com.m000gg.billing.plugin.db;

import com.m000gg.plugin.api.db.PluginDatabaseException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Pure-Java SQL tokenizer and validator that restricts plugin runtime queries and
 * Flyway migrations to the plugin's own {@code plugin_<id>} schema.
 */
@Component
public class PluginSqlValidator {

    private static final int MAX_CACHE_SIZE = 4096;

    private static final Set<String> ALLOWED_RUNTIME_VERBS = Set.of(
            "select", "with", "insert", "update", "delete"
    );

    private static final Set<String> FORBIDDEN_RUNTIME_KEYWORDS = Set.of(
            "reset", "call", "copy", "create", "alter", "drop", "truncate",
            "grant", "revoke", "execute", "prepare", "deallocate", "vacuum",
            "analyze", "cluster", "lock", "listen", "notify", "unlisten",
            "discard", "reindex", "refresh", "comment", "import", "load"
    );

    private static final Set<String> ALLOWED_MIGRATION_CREATE_TARGETS = Set.of(
            "table", "index", "sequence"
    );

    private static final Set<String> ALLOWED_MIGRATION_ALTER_DROP_TARGETS = Set.of(
            "table", "index", "sequence"
    );

    private static final Set<String> FORBIDDEN_SCHEMAS = Set.of(
            "core", "public", "pg_catalog", "information_schema", "pg_toast", "pg_temp"
    );

    private static final Set<String> FORBIDDEN_FUNCTIONS = Set.of(
            "set_config", "current_setting", "query_to_xml", "query_to_json",
            "cursor_to_xml", "table_to_xml"
    );

    private static final Set<String> RELATION_KEYWORDS = Set.of(
            "from", "join", "into", "update", "table", "references", "sequence"
    );

    private static final Set<String> FROM_CLAUSE_TERMINATORS = Set.of(
            "where", "group", "having", "order", "limit", "offset", "fetch",
            "union", "intersect", "except", "for", "window", "returning",
            "set", "values", "on", "using"
    );

    private final ConcurrentMap<String, Boolean> validatedRuntimeCache = new ConcurrentHashMap<>();

    public void validateRuntimeSql(String sql, String allowedSchema) {
        if (sql == null || sql.isBlank()) {
            throw new PluginDatabaseException("SQL statement must not be empty");
        }
        String normalizedSchema = normalizeSchema(allowedSchema);
        String cacheKey = normalizedSchema + "\0" + sql;
        if (validatedRuntimeCache.containsKey(cacheKey)) {
            return;
        }

        List<Token> tokens = tokenize(sql);
        if (tokens.isEmpty()) {
            throw new PluginDatabaseException("SQL statement must not be empty");
        }

        List<Token> effectiveTokens = stripOptionalTrailingSemicolon(tokens);
        validateRuntimeStatement(effectiveTokens, normalizedSchema);

        if (validatedRuntimeCache.size() >= MAX_CACHE_SIZE) {
            validatedRuntimeCache.clear();
        }
        validatedRuntimeCache.put(cacheKey, Boolean.TRUE);
    }

    public void validateMigrationSql(String sql, String allowedSchema) {
        if (sql == null) {
            throw new PluginDatabaseException("Migration SQL must not be null");
        }
        String normalizedSchema = normalizeSchema(allowedSchema);
        List<Token> tokens = tokenize(sql);
        if (tokens.isEmpty()) {
            return;
        }

        List<List<Token>> statements = splitBySemicolon(tokens);
        for (List<Token> statement : statements) {
            validateMigrationStatement(statement, normalizedSchema);
        }
    }

    private void validateRuntimeStatement(List<Token> tokens, String allowedSchema) {
        Token first = tokens.getFirst();
        if (first.type() != TokenType.WORD) {
            throw new PluginDatabaseException("Unsupported SQL statement start");
        }

        String verb = first.lower();
        if (!ALLOWED_RUNTIME_VERBS.contains(verb)) {
            throw new PluginDatabaseException(
                    "SQL command '" + first.text() + "' is not allowed for plugins");
        }

        for (Token token : tokens) {
            if (token.type() == TokenType.WORD && FORBIDDEN_RUNTIME_KEYWORDS.contains(token.lower())) {
                throw new PluginDatabaseException(
                        "Forbidden SQL keyword '" + token.text() + "' in plugin query");
            }
        }

        validateFunctionsAndSchemas(tokens, allowedSchema, false);
    }

    private void validateMigrationStatement(List<Token> tokens, String allowedSchema) {
        Token first = tokens.getFirst();
        if (first.type() != TokenType.WORD) {
            throw new PluginDatabaseException("Unsupported migration SQL statement");
        }

        String verb = first.lower();
        boolean isCreateIndex = false;
        boolean isComment = false;

        switch (verb) {
            case "create" -> {
                String target = nextTargetAfterModifiers(tokens, 1, Set.of("unique"));
                if (target == null || !ALLOWED_MIGRATION_CREATE_TARGETS.contains(target)) {
                    throw new PluginDatabaseException(
                            "Unsupported CREATE target in plugin migration: " + target);
                }
                isCreateIndex = "index".equals(target);
            }
            case "alter", "drop" -> {
                String target = nextWordLower(tokens, 1);
                if (target == null || !ALLOWED_MIGRATION_ALTER_DROP_TARGETS.contains(target)) {
                    throw new PluginDatabaseException(
                            "Unsupported " + first.text() + " target in plugin migration: " + target);
                }
            }
            case "comment" -> isComment = true;
            case "insert", "update", "delete", "select", "with", "truncate" -> {
                // Allowed data backfill / cleanup statements inside plugin schema.
            }
            default -> throw new PluginDatabaseException(
                    "SQL command '" + first.text() + "' is not allowed in plugin migrations");
        }

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type() != TokenType.WORD) {
                continue;
            }
            String lower = token.lower();
            if ("copy".equals(lower) || "grant".equals(lower)
                    || "revoke".equals(lower) || "execute".equals(lower) || "prepare".equals(lower)) {
                throw new PluginDatabaseException(
                        "Forbidden SQL keyword '" + token.text() + "' in plugin migration");
            }
            // Allow ALTER TABLE ... SET NOT NULL / SET DEFAULT, forbid SET search_path etc.
            if ("set".equals(lower) && i == 0) {
                throw new PluginDatabaseException("SET statements are not allowed in plugin migrations");
            }
        }

        validateFunctionsAndSchemas(tokens, allowedSchema, isCreateIndex || isComment);
    }

    private void validateFunctionsAndSchemas(
            List<Token> tokens,
            String allowedSchema,
            boolean onIntroducesRelation
    ) {
        boolean expectingRelation = false;
        boolean inFromClause = false;
        int fromParenDepth = -1;
        int parenDepth = 0;

        for (int i = 0; i < tokens.size(); i++) {
            Token current = tokens.get(i);

            if (current.type() == TokenType.LPAREN) {
                parenDepth++;
                expectingRelation = false;
                continue;
            }
            if (current.type() == TokenType.RPAREN) {
                if (inFromClause && parenDepth == fromParenDepth) {
                    inFromClause = false;
                    fromParenDepth = -1;
                }
                parenDepth = Math.max(0, parenDepth - 1);
                expectingRelation = false;
                continue;
            }

            if (current.type() == TokenType.COMMA) {
                if (inFromClause && parenDepth == fromParenDepth) {
                    expectingRelation = true;
                }
                continue;
            }

            if (current.type() == TokenType.WORD) {
                String lower = current.lower();

                // Check function calls: word followed by '('
                if (i + 1 < tokens.size() && tokens.get(i + 1).type() == TokenType.LPAREN) {
                    if (isForbiddenFunction(lower)) {
                        throw new PluginDatabaseException(
                                "Forbidden SQL function '" + current.text() + "'");
                    }
                }

                if ("from".equals(lower)) {
                    expectingRelation = true;
                    inFromClause = true;
                    fromParenDepth = parenDepth;
                    continue;
                }
                if (RELATION_KEYWORDS.contains(lower)
                        || (onIntroducesRelation && "on".equals(lower))) {
                    expectingRelation = true;
                    continue;
                }
                if (expectingRelation && ("only".equals(lower) || "if".equals(lower)
                        || "not".equals(lower) || "exists".equals(lower)
                        || "concurrently".equals(lower))) {
                    continue;
                }
                if (inFromClause && parenDepth == fromParenDepth
                        && FROM_CLAUSE_TERMINATORS.contains(lower)) {
                    inFromClause = false;
                    fromParenDepth = -1;
                    expectingRelation = false;
                    continue;
                }
            }

            if (current.isIdentifier()) {
                // Check for 3-part names (catalog.schema.table) or schema.object
                if (i + 2 < tokens.size()
                        && tokens.get(i + 1).type() == TokenType.DOT
                        && tokens.get(i + 2).isIdentifier()) {

                    if (i + 4 < tokens.size() && tokens.get(i + 3).type() == TokenType.DOT) {
                        throw new PluginDatabaseException(
                                "Cross-catalog qualified names are not allowed");
                    }

                    String qualifier = current.lower();
                    if (isForbiddenQualifier(qualifier, allowedSchema)) {
                        throw new PluginDatabaseException(
                                "Access to schema '" + current.text() + "' is not allowed; only '"
                                        + allowedSchema + "' is permitted");
                    }
                    if (expectingRelation && !qualifier.equals(allowedSchema)) {
                        throw new PluginDatabaseException(
                                "Access to schema '" + current.text() + "' is not allowed; only '"
                                        + allowedSchema + "' is permitted");
                    }

                    expectingRelation = false;
                    i += 2; // Skip '.object'
                    continue;
                }

                expectingRelation = false;
            } else {
                expectingRelation = false;
            }
        }
    }

    private static boolean isForbiddenQualifier(String qualifierLower, String allowedSchema) {
        if (FORBIDDEN_SCHEMAS.contains(qualifierLower) || qualifierLower.startsWith("pg_")) {
            return true;
        }
        return qualifierLower.startsWith("plugin_") && !qualifierLower.equals(allowedSchema);
    }

    private static boolean isForbiddenFunction(String funcLower) {
        return funcLower.startsWith("pg_")
                || funcLower.startsWith("dblink")
                || funcLower.startsWith("lo_")
                || FORBIDDEN_FUNCTIONS.contains(funcLower);
    }

    private static List<Token> stripOptionalTrailingSemicolon(List<Token> tokens) {
        int end = tokens.size();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).type() == TokenType.SEMICOLON) {
                if (i != tokens.size() - 1) {
                    throw new PluginDatabaseException("Multiple SQL statements are not allowed");
                }
                end = i;
            }
        }
        if (end == 0) {
            throw new PluginDatabaseException("SQL statement must not be empty");
        }
        return tokens.subList(0, end);
    }

    private static List<List<Token>> splitBySemicolon(List<Token> tokens) {
        List<List<Token>> statements = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).type() == TokenType.SEMICOLON) {
                if (i > start) {
                    statements.add(tokens.subList(start, i));
                }
                start = i + 1;
            }
        }
        if (start < tokens.size()) {
            statements.add(tokens.subList(start, tokens.size()));
        }
        return statements;
    }

    private static String nextWordLower(List<Token> tokens, int index) {
        if (index < tokens.size() && tokens.get(index).type() == TokenType.WORD) {
            return tokens.get(index).lower();
        }
        return null;
    }

    private static String nextTargetAfterModifiers(List<Token> tokens, int startIndex, Set<String> modifiers) {
        int i = startIndex;
        while (i < tokens.size() && tokens.get(i).type() == TokenType.WORD
                && modifiers.contains(tokens.get(i).lower())) {
            i++;
        }
        return nextWordLower(tokens, i);
    }

    private static String normalizeSchema(String allowedSchema) {
        if (allowedSchema == null || allowedSchema.isBlank()) {
            throw new IllegalArgumentException("allowedSchema must not be blank");
        }
        return allowedSchema.toLowerCase(Locale.ROOT);
    }

    static List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        int len = sql.length();
        int i = 0;

        while (i < len) {
            char c = sql.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Line comment: -- ...
            if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
                i += 2;
                while (i < len && sql.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }

            // Block comment: /* ... */ (PostgreSQL supports nesting)
            if (c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                i += 2;
                int depth = 1;
                while (i < len && depth > 0) {
                    if (sql.charAt(i) == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                        depth++;
                        i += 2;
                    } else if (sql.charAt(i) == '*' && i + 1 < len && sql.charAt(i + 1) == '/') {
                        depth--;
                        i += 2;
                    } else {
                        i++;
                    }
                }
                if (depth > 0) {
                    throw new PluginDatabaseException("Unterminated SQL block comment");
                }
                continue;
            }

            // Escape string literal: E'...' / e'...'
            if ((c == 'E' || c == 'e') && i + 1 < len && sql.charAt(i + 1) == '\'') {
                i = consumeSingleQuotedLiteral(sql, i + 2, true);
                tokens.add(new Token(TokenType.STRING_LITERAL, "''"));
                continue;
            }

            // Standard string literal: '...'
            if (c == '\'') {
                i = consumeSingleQuotedLiteral(sql, i + 1, false);
                tokens.add(new Token(TokenType.STRING_LITERAL, "''"));
                continue;
            }

            // PostgreSQL dollar-quoted string: $tag$...$tag$
            if (c == '$') {
                String tag = matchDollarTag(sql, i);
                if (tag != null) {
                    int end = sql.indexOf(tag, i + tag.length());
                    if (end < 0) {
                        throw new PluginDatabaseException("Unterminated dollar-quoted SQL literal");
                    }
                    i = end + tag.length();
                    tokens.add(new Token(TokenType.STRING_LITERAL, "$$"));
                    continue;
                }
            }

            // Quoted identifier: "..."
            if (c == '"') {
                StringBuilder ident = new StringBuilder();
                i++;
                boolean closed = false;
                while (i < len) {
                    char ch = sql.charAt(i);
                    if (ch == '"') {
                        if (i + 1 < len && sql.charAt(i + 1) == '"') {
                            ident.append('"');
                            i += 2;
                        } else {
                            closed = true;
                            i++;
                            break;
                        }
                    } else {
                        ident.append(ch);
                        i++;
                    }
                }
                if (!closed) {
                    throw new PluginDatabaseException("Unterminated quoted SQL identifier");
                }
                tokens.add(new Token(TokenType.QUOTED_IDENT, ident.toString()));
                continue;
            }

            // Unquoted identifier or keyword
            if (isIdentStart(c)) {
                int start = i;
                i++;
                while (i < len && isIdentPart(sql.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(TokenType.WORD, sql.substring(start, i)));
                continue;
            }

            // Numeric literal
            if (Character.isDigit(c)) {
                int start = i;
                i++;
                while (i < len && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '.')) {
                    i++;
                }
                tokens.add(new Token(TokenType.NUMBER, sql.substring(start, i)));
                continue;
            }

            switch (c) {
                case '.' -> tokens.add(new Token(TokenType.DOT, "."));
                case ';' -> tokens.add(new Token(TokenType.SEMICOLON, ";"));
                case '(' -> tokens.add(new Token(TokenType.LPAREN, "("));
                case ')' -> tokens.add(new Token(TokenType.RPAREN, ")"));
                case ',' -> tokens.add(new Token(TokenType.COMMA, ","));
                default -> tokens.add(new Token(TokenType.OPERATOR, String.valueOf(c)));
            }
            i++;
        }

        return tokens;
    }

    private static int consumeSingleQuotedLiteral(String sql, int start, boolean allowBackslashEscape) {
        int len = sql.length();
        int i = start;
        while (i < len) {
            char ch = sql.charAt(i);
            if (allowBackslashEscape && ch == '\\') {
                i += 2;
                continue;
            }
            if (ch == '\'') {
                if (i + 1 < len && sql.charAt(i + 1) == '\'') {
                    i += 2;
                } else {
                    return i + 1;
                }
            } else {
                i++;
            }
        }
        throw new PluginDatabaseException("Unterminated SQL string literal");
    }

    private static String matchDollarTag(String sql, int start) {
        int len = sql.length();
        int i = start + 1;
        if (i < len && sql.charAt(i) == '$') {
            return "$$";
        }
        if (i >= len || !isIdentStart(sql.charAt(i))) {
            return null;
        }
        i++;
        while (i < len && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
            i++;
        }
        if (i < len && sql.charAt(i) == '$') {
            return sql.substring(start, i + 1);
        }
        return null;
    }

    private static boolean isIdentStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return isIdentStart(c) || (c >= '0' && c <= '9') || c == '$';
    }

    enum TokenType {
        WORD,
        QUOTED_IDENT,
        STRING_LITERAL,
        NUMBER,
        DOT,
        SEMICOLON,
        LPAREN,
        RPAREN,
        COMMA,
        OPERATOR
    }

    record Token(TokenType type, String text) {
        String lower() {
            return text.toLowerCase(Locale.ROOT);
        }

        boolean isIdentifier() {
            return type == TokenType.WORD || type == TokenType.QUOTED_IDENT;
        }
    }
}
