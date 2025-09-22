'use strict';

const fs = require('fs');
const path = require('path');

let yogaPath = require.resolve('yoga-layout-prebuilt');

if (!yogaPath) {
  console.log("couldn't find react-pdf path");
  process.exit(0);
}

yogaPath = path.resolve(path.dirname(yogaPath), '../');
yogaPath = path.join(yogaPath, 'build/Release/nbind.js');
const nbindBuffer = fs.readFileSync(yogaPath);
const it = nbindBuffer.indexOf(Buffer.from(' 268435456'));

if (it === -1) {
  process.exit(0);
}

/* Patch 256 MB to 1 GB */
nbindBuffer.write('1073741824', it);
fs.writeFileSync(yogaPath, nbindBuffer);
