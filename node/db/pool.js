const { Pool } = require('pg');

const pool = new Pool({
  host: process.env.POSTGRES_HOST ?? 'localhost',
  port: Number(process.env.POSTGRES_PORT ?? 5432),
  user: process.env.POSTGRES_USER ?? 'app',
  password: process.env.POSTGRES_PASSWORD ?? 'secret',
  database: process.env.POSTGRES_DB ?? 'drones'
});

module.exports = pool;
