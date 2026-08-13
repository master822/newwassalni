'use strict';

const path = require('path');
const fs = require('fs');
const dotenv = require('dotenv');

const envPath = path.join(__dirname, '..', '.env');

if (fs.existsSync(envPath)) {
    dotenv.config({
        path: envPath,
        override: true
    });
}

console.log('[ENV] DATABASE_URL loaded:', !!process.env.DATABASE_URL);

if (process.env.DATABASE_URL) {
    try {
        const u = new URL(process.env.DATABASE_URL);

        console.log('[ENV] DB host:', u.hostname);
        console.log('[ENV] DB port:', u.port || '5432');
        console.log('[ENV] DB name:', u.pathname.replace('/', ''));
    } catch {
        console.log('[ENV] DATABASE_URL exists but URL parsing failed');
    }
}
