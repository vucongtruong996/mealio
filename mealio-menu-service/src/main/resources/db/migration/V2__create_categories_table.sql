CREATE TABLE categories
(
    id UUID PRIMARY KEY,

    name VARCHAR(100) NOT NULL UNIQUE,

    description VARCHAR(500)
);