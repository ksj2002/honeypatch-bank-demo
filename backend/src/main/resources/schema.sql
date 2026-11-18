CREATE TABLE users (
    id       VARCHAR(50)  PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    name     VARCHAR(50)  NOT NULL
);

CREATE TABLE customers (
    id      BIGINT      PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    balance BIGINT      NOT NULL
);

CREATE TABLE decoy_customers (
    id      BIGINT      PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    balance BIGINT      NOT NULL
);