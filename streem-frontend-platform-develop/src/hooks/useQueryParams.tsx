import { navigate, useLocation, NavigateOptions } from '@reach/router';
import React, { createContext, FC, ReactNode, useCallback, useContext, useMemo } from 'react';

type TCommonProps = {
  shouldNavigate?: boolean;
  navigateOptions?: NavigateOptions<{}>;
  persistQueryParams?: (queryString: string) => void;
  prefixUrl?: string;
};

type TUpdateQueryParams = (
  options: TCommonProps & {
    newParams?: Record<string, any>;
    shouldClear?: boolean;
    paramsToRemove?: string[];
  },
) => void | URLSearchParams;

type TPostAction = (options: TCommonProps & { params: URLSearchParams }) => void | URLSearchParams;

type TGetQueryParam = (key: string, defaultvalue?: any) => string | undefined;

type TSetQueryParam = (
  options: TCommonProps & { key: string; value: string },
) => void | URLSearchParams;

type TRemoveQueryParam = (options: TCommonProps & { key: string }) => void | URLSearchParams;

type TQueryParamsContextType = {
  updateQueryParams: TUpdateQueryParams;
  getQueryParam: TGetQueryParam;
  setQueryParam: TSetQueryParam;
  removeQueryParam: TRemoveQueryParam;
};

const QueryParamsContext = createContext<TQueryParamsContextType | null>(null);

export const QueryParamsProvider: FC<{
  children: ReactNode;
}> = ({ children }) => {
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);

  const postAction: TPostAction = useCallback(
    ({
      params,
      shouldNavigate = true,
      navigateOptions = {},
      persistQueryParams,
      prefixUrl = '',
    }) => {
      const queryString = `${prefixUrl}?${params.toString()}`;

      if (persistQueryParams) {
        persistQueryParams(queryString);
      }
      if (shouldNavigate) {
        navigate(queryString, { state: location?.state, ...navigateOptions });
      } else {
        return params;
      }
    },
    [location?.state, navigate],
  );

  const updateQueryParams: TUpdateQueryParams = useCallback(
    ({ newParams = {}, shouldClear = false, paramsToRemove = [], ...rest }) => {
      const params = shouldClear ? new URLSearchParams() : queryParams;

      paramsToRemove.forEach((param) => {
        params.delete(param);
      });

      Object.entries(newParams).forEach(([k, v]) => {
        params.set(k, typeof v === 'object' ? JSON.stringify(v) : v);
      });

      postAction({ params, ...rest });
    },
    [queryParams],
  );

  const getQueryParam: TGetQueryParam = useCallback(
    (key, defaultvalue) => {
      return queryParams.get(key) || defaultvalue;
    },
    [queryParams],
  );

  const setQueryParam: TSetQueryParam = useCallback(
    ({ key, value, ...rest }) => {
      queryParams.set(key, value.toString());
      postAction({ params: queryParams, ...rest });
    },
    [queryParams],
  );

  const removeQueryParam: TRemoveQueryParam = useCallback(
    ({ key, ...rest }) => {
      queryParams.delete(key);
      postAction({ params: queryParams, ...rest });
    },
    [queryParams],
  );

  const value: TQueryParamsContextType = useMemo(
    () => ({ updateQueryParams, getQueryParam, setQueryParam, removeQueryParam }),
    [getQueryParam, updateQueryParams, setQueryParam, removeQueryParam],
  );

  return <QueryParamsContext.Provider value={value}>{children}</QueryParamsContext.Provider>;
};

export const useQueryParams = () => {
  const context = useContext(QueryParamsContext);

  if (!context) {
    throw new Error('useQueryParams must be used within a QueryParamsProvider');
  }

  return context;
};
