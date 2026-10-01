CREATE TABLE categories
(
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL,

    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);