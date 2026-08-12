'use strict';

const { spawn } = require('child_process');

const children = [
  spawn(process.execPath, ['src/server.js'], {
    stdio: 'inherit',
    env: process.env
  }),
  spawn(process.execPath, ['src/bot.js'], {
    stdio: 'inherit',
    env: process.env
  })
];

function shutdown() {
  for (const child of children) {
    try { child.kill('SIGTERM'); } catch {}
  }
  process.exit(0);
}

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

for (const child of children) {
  child.on('exit', code => {
    if (code !== 0) {
      console.error(`Process exited with code ${code}`);
    }
  });
}
