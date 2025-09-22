const { pathsToModuleNameMapper } = require('ts-jest');
const { compilerOptions } = require('./tsconfig');

module.exports = {
  roots: ['<rootDir>'],
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  testRegex: '/test/.*?\\.(test|spec)\\.(ts|tsx)?$',
  setupFiles: ['<rootDir>/test/__setup__/setupFiles.ts'],
  setupFilesAfterEnv: ['<rootDir>/test/__setup__/setupFilesAfterEnv.ts'],
  collectCoverage: false,
  collectCoverageFrom: ['src/**/*.{ts,tsx}'],
  // coverageThreshold: {
  //   global: {
  //     branches: 90,
  //     functions: 90,
  //     lines: 90,
  //     statements: 90,
  //   },
  // },
  moduleDirectories: ['node_modules', 'src', 'test/__setup__', __dirname],
  moduleFileExtensions: ['js', 'jsx', 'ts', 'tsx', 'json'],
  modulePaths: [compilerOptions.baseUrl],
  moduleNameMapper: {
    '\\.(css|scss)$': '<rootDir>/test/__mocks__/styleMock.ts',
    '\\.(jpe?g|png|gif|ttf|eot|woff|md)$': '<rootDir>/test/__mocks__/fileMock.ts',
    '\\.svg$': '<rootDir>/test/__mocks__/svgMock.ts',
    ...pathsToModuleNameMapper(compilerOptions.paths, { prefix: '<rootDir>/' }),
  },
  transform: {
    '^.+\\.(ts|tsx|js|jsx)?$': [
      'ts-jest',
      {
        diagnostics: false,
      },
    ],
  },
};
