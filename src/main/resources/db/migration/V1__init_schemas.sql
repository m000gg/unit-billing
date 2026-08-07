CREATE TABLE application_users (
                                   id UUID PRIMARY KEY,
                                   first_name VARCHAR(255),
                                   last_name VARCHAR(255),
                                   email VARCHAR(255) NOT NULL UNIQUE,
                                   phone VARCHAR(255),
                                   password VARCHAR(255),
                                   balance NUMERIC NOT NULL,
                                   country VARCHAR(255),
                                   city VARCHAR(255),
                                   region VARCHAR(255),
                                   street VARCHAR(255),
                                   house_number VARCHAR(255),
                                   apartment VARCHAR(255),
                                   postal_code VARCHAR(255),
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   is_deleted BOOLEAN DEFAULT FALSE
);


CREATE TABLE ledger_entries (
                                id UUID PRIMARY KEY,
                                subscriber_id UUID NOT NULL,
                                amount NUMERIC(19, 4) NOT NULL,
                                type VARCHAR(50) NOT NULL,
                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                description TEXT
);