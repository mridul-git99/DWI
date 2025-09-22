import { v4 as uuidv4 } from 'uuid';

export const filtersToQueryParams = (data: any[]) => {
  let filters = data.map((currData: any) => ({
    field: currData.key,
    op: currData.constraint,
    values: ['startedAt', 'createdAt', 'modifiedAt'].includes(currData.key)
      ? [parseInt(currData.value)]
      : Array.isArray(currData.value)
      ? currData.value
      : [currData.value],
  }));

  return filters;
};

export const queryParamsToFilters = (data: any, parameterListById: any) => {
  const displayNameEnums = {
    startedAt: 'Job Started At',
    createdAt: 'Job Created At',
    modifiedAt: 'modifiedAt',
    endedAt: 'Job Ended At',
    state: 'Job State',
  };
  let filters = data?.map((currData: any) => {
    if (['startedAt', 'createdAt', 'modifiedAt', 'endedAt', 'state'].includes(currData.field)) {
      return {
        value: currData.values[0],
        key: currData.field,
        id: uuidv4(),
        constraint: currData.op,
        displayName: displayNameEnums[currData.field],
      };
    } else {
      const parameterId = currData.field?.split('.')[1];
      return {
        value: currData.values[0],
        key: currData.field,
        id: uuidv4(),
        constraint: currData.op,
        displayName: parameterListById[parameterId]?.label,
      };
    }
  });

  return filters;
};

export const parseFilterObject = (filterObject: any) => {
  return filterObject ? JSON.parse(filterObject) : {};
};

export const filterFieldsFromQueryParams = (queryParams: any) => {
  const parsedParams = parseFilterObject(queryParams);
  const filterFields = Object.values(parsedParams)
    .flatMap((value) => {
      if (Array.isArray(value)) {
        return value;
      } else if (typeof value === 'object' && value !== null && 'value' in value) {
        return value.value;
      }
      return [];
    })
    .filter((value) => typeof value === 'object' && value !== null);
  return filterFields;
};

export const getFilterInfoFromQueryParams = (
  queryParams: any,
  key: string,
  infoType: 'label' | 'value',
) => {
  const parsedParams = parseFilterObject(queryParams);
  if (parsedParams[key] && parsedParams[key][infoType]) {
    return parsedParams[key][infoType];
  }

  return null;
};

export const modifyFilters = (filters: any, key: string, action: string, data: any) => {
  const parsedFilters = parseFilterObject(filters);
  if (action === 'add') {
    if (parsedFilters[key]) {
      parsedFilters[key] = { ...parsedFilters[key], ...data };
    } else {
      parsedFilters[key] = data;
    }
  } else if (action === 'remove' && parsedFilters[key]) {
    delete parsedFilters[key];
  }

  return parsedFilters;
};

export const isKeyPresent = (filters: any, key: string) => {
  const parsedFilters = parseFilterObject(filters);
  return parsedFilters.hasOwnProperty(key);
};

export const formatFilters = (filters: any[]) => {
  return filters?.map((item) => ({
    ...item,
    value: Array.isArray(item.value) ? item.value : [item.value],
  }));
};
