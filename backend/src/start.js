'use strict';

const dns = require('dns');

dns.setDefaultResultOrder('ipv4first');

require('./env');

console.log('========================================');
console.log('🚀 WASALNI BACKEND STARTING');
console.log('========================================');

console.log('🌐 Starting API server...');
require('./server');

console.log('🤖 Starting Telegram bot...');
require('./bot');

console.log('========================================');
console.log('✅ WASALNI SERVER + BOT STARTED');
console.log('========================================');
