module.exports = function (api) {
  api.cache(true);

  return {
    presets: ['@babel/react', '@babel/env', '@babel/typescript'],
    plugins: [
      [
        'babel-plugin-styled-components',
        {
          pure: true,
        },
      ],
      [
        'module-resolver',
        {
          root: ['.'],
          alias: {
            '#i18n': './src/i18n',
            '#assets': './src/assets',
            '#components': './src/components',
            '#PrototypeComposer': './src/PrototypeComposer',
            '#modules': './src/modules',
            '#store': './src/store',
            '#utils': './src/utils',
            '#views': './src/views',
            '#services': './src/services',
            '#hooks': './src/hooks',
            '#types': './src/types',
            test: ['./test'],
            'test-utils': './test/__setup__/test-utils',
          },
        },
      ],
    ],
  };
};
