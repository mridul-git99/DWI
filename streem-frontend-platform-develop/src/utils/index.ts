import { Constraint, filterPageTypeEnum, MandatoryParameter } from '#types';
import { get } from 'lodash';
import { InputTypes } from './globalTypes';

export const setKeepPersistedData = (value = '') => {
  localStorage.setItem('keepPersistedData', value);
};

export const openLinkInNewTab = (link: string) => {
  setKeepPersistedData('true');
  window.open(link, '_blank');
};

export const labelByConstraint = (
  inputType?: InputTypes | MandatoryParameter,
  pageType?: filterPageTypeEnum,
) => {
  switch (pageType) {
    case filterPageTypeEnum.CONFIGURE_TASK_CONDITIONS:
      switch (inputType) {
        case InputTypes.DATE:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };

        case InputTypes.DATE_TIME:
        case InputTypes.TIME:
          return {
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };

        case InputTypes.SINGLE_SELECT:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
          };

        default:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
      }

    case filterPageTypeEnum.OBJECTS_FILTERS_DRAWER:
      switch (inputType) {
        case InputTypes.DATE:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
        case InputTypes.DATE_TIME:
        case InputTypes.TIME:
          return {
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
        case InputTypes.SINGLE_LINE:
        case InputTypes.MULTI_LINE:
          return {
            [Constraint.LIKE]: 'contains',
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
          };
        case InputTypes.SINGLE_SELECT:
        case InputTypes.ONE_TO_ONE:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.ANY]: 'in',
            [Constraint.NIN]: 'not in',
          };
        case InputTypes.MULTI_SELECT:
        case InputTypes.ONE_TO_MANY:
          return {
            [Constraint.ALL]: 'is equal to',
            [Constraint.NOT_ALL]: 'is not equal to',
            [Constraint.ANY]: 'in',
            [Constraint.NIN]: 'not in',
          };
        default:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
      }

    case filterPageTypeEnum.JOB_LOGS_FILTERS_DRAWER:
      switch (inputType) {
        case InputTypes.DATE:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
        case InputTypes.DATE_TIME:
        case InputTypes.TIME:
          return {
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
        case InputTypes.SINGLE_LINE:
        case InputTypes.MULTI_LINE:
          return {
            [Constraint.LIKE]: 'contains',
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
          };
        case InputTypes.SINGLE_SELECT:
        case InputTypes.ONE_TO_ONE:
        case MandatoryParameter.RESOURCE:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.ANY]: 'in',
            [Constraint.NIN]: 'not in',
          };
        case InputTypes.MULTI_SELECT:
        case InputTypes.ONE_TO_MANY:
        case MandatoryParameter.MULTI_RESOURCE:
        case MandatoryParameter.MULTISELECT:
          return {
            [Constraint.ALL]: 'is equal to',
            [Constraint.NOT_ALL]: 'is not equal to',
            [Constraint.ANY]: 'in',
            [Constraint.NIN]: 'not in',
          };
        case MandatoryParameter.YES_NO:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
          };
        default:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
      }

    case filterPageTypeEnum.RESOURCE_FILTERS:
      switch (inputType) {
        case InputTypes.SINGLE_LINE:
        case InputTypes.MULTI_LINE:
        case InputTypes.SINGLE_SELECT:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
          };

        case InputTypes.ONE_TO_ONE:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.ANY]: 'in',
            [Constraint.NIN]: 'not in',
          };
        case InputTypes.ONE_TO_MANY:
        case InputTypes.MULTI_SELECT:
          return {
            [Constraint.ALL]: 'is equal to',
            [Constraint.NOT_ALL]: 'is not equal to',
            [Constraint.ANY]: 'in',
            [Constraint.NIN]: 'not in',
          };
        case InputTypes.DATE_TIME:
          return {
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };

        default:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
      }

    default:
      switch (inputType) {
        case InputTypes.DATE:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
        case InputTypes.DATE_TIME:
        case InputTypes.TIME:
          return {
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
        case InputTypes.SINGLE_LINE:
        case InputTypes.MULTI_LINE:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
          };
        case InputTypes.SINGLE_SELECT:
        case InputTypes.MULTI_SELECT:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
          };
        case InputTypes.ONE_TO_ONE:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.ANY]: 'in',
            [Constraint.NIN]: 'not in',
          };
        case InputTypes.ONE_TO_MANY:
          return {
            [Constraint.ALL]: 'is equal to',
            [Constraint.NOT_ALL]: 'is not equal to',
            [Constraint.ANY]: 'in',
            [Constraint.NIN]: 'not in',
          };
        default:
          return {
            [Constraint.EQ]: 'is equal to',
            [Constraint.NE]: 'is not equal to',
            [Constraint.LT]: 'is less than',
            [Constraint.GT]: 'is greater than',
            [Constraint.LTE]: 'is less than equal to',
            [Constraint.GTE]: 'is greater than equal to',
          };
      }
  }
};

export const validateNumber = (number: string | number) => {
  if (['0', 0].includes(number) || number) {
    return true;
  }
  return false;
};

export const transformDataToOptions = (
  data: any[],
  labelKey: string[],
  valueKey: string[],
): any[] => {
  return data.map((item) => {
    return {
      label: get(item, labelKey),
      value: get(item, valueKey),
    };
  });
};
