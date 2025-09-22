const ignoredFiles = require('react-dev-utils/ignoredFiles');
const paths = require('./paths');

const config = {
  open: true,
  compress: true,
  static: {
    directory: paths.appPublic,
    watch: {
      ignored: ignoredFiles(paths.appSrc),
    },
  },
  allowedHosts: 'auto',
  client: {
    logging: 'warn',
    overlay: false,
  },
  historyApiFallback: true,
};

module.exports = config;
