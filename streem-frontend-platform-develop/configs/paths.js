'use strict';

const path = require('path');
const fs = require('fs');

const appDirectory = fs.realpathSync(process.cwd());
const resolveApp = (relativePath) => path.resolve(appDirectory, relativePath);

const moduleFileExtensions = ['web.mjs', 'mjs', 'web.js', 'js', 'web.ts', 'ts', 'web.tsx', 'tsx', 'json', 'web.jsx', 'jsx'];

// Resolve file paths in the same order as webpack
const resolveModule = (resolveFn, filePath) => {
  const extension = moduleFileExtensions.find((extension) => fs.existsSync(resolveFn(`${filePath}.${extension}`)));

  if (extension) {
    return resolveFn(`${filePath}.${extension}`);
  }

  return resolveFn(`${filePath}.js`);
};

// config after eject: we're in ./config/
module.exports = {
  dotenv: resolveApp('.env'),
  appPath: resolveApp('.'),
  dist: resolveApp('build'),
  babelConfig: resolveApp('babel.config.js'),
  appPackageJson: resolveApp('package.json'),
  packageLockFile: resolveApp('package-lock.json'),
  yarnLockFile: resolveApp('yarn.lock'),
  appNodeModules: resolveApp('node_modules'),
  appBuild: resolveApp('dist'),
  appSrc: resolveApp('src'),
  appIndex: resolveModule(resolveApp, 'src/index'),
  appPublic: resolveApp('public'),
  appHtml: resolveApp('public/index.html'),
  appTsConfig: resolveApp('tsconfig.json'),
};

module.exports.moduleFileExtensions = moduleFileExtensions;
