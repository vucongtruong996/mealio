-- create user only if not exists
DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'mealio') THEN
      CREATE ROLE mealio WITH LOGIN PASSWORD 'mealio' SUPERUSER;
   END IF;
END
$$;

ALTER DATABASE mealio OWNER TO mealio;

CREATE SCHEMA IF NOT EXISTS menu_schema;
CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE SCHEMA IF NOT EXISTS order_schema;
CREATE SCHEMA IF NOT EXISTS inventory_schema;
CREATE SCHEMA IF NOT EXISTS ai_schema;