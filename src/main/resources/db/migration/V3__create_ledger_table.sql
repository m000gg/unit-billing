CREATE TABLE ledger_entries (
                                id UUID PRIMARY KEY,
                                subscriber_id UUID NOT NULL REFERENCES application_users(id),
                                amount NUMERIC(19, 4) NOT NULL,
                                type VARCHAR(50) NOT NULL,
                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                description TEXT,
                                original_entry_id UUID REFERENCES ledger_entries(id),
                                source VARCHAR(50) NOT NULL,
                                performed_by_admin UUID REFERENCES admins(id)
);

CREATE INDEX idx_ledger_entries_subscriber_id_created_at
    ON ledger_entries (subscriber_id, created_at DESC);