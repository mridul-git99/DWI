import i18n from 'i18next';
import * as en from './en';
import { initReactI18next } from 'react-i18next';

// Sort All Translation keys Alphabetically.
export const resources = {
  en,
};

i18n.use(initReactI18next).init({
  lng: 'en',
  ns: ['userManagement', 'auth'],
  resources,
});
