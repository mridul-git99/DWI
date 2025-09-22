import '@testing-library/jest-dom';
// import 'jest-localstorage-mock';
// import 'jest-extended';
// import 'jest-styled-components';

// import { enableFetchMocks } from 'jest-fetch-mock';

// enableFetchMocks();

jest.mock('uuid', () => ({ v4: () => '123456789' }));
jest.mock('../../src/utils/request');
