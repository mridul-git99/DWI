import { DEFAULT_PAGINATION } from '#utils/constants';
import { Pageable, ResponseError, ResponseObj } from '#utils/globalTypes';
import { request } from '#utils/request';
import { Method } from 'axios';
import { keyBy } from 'lodash';
import { useEffect, useState } from 'react';

interface UseRequestConfig {
  url: string;
  method?: Method;
  queryParams?: Record<string, any>;
  body?: Record<string, any>;
  fetchOnInit?: boolean;
}

type TStatus = 'init' | 'loading' | 'success' | 'error' | 'loading-next';
type TFetchData = (params?: {
  page?: Pageable['page'];
  size?: Pageable['pageSize'];
  customQueryParams?: Record<string, any>;
  customBody?: Record<string, any>;
}) => Promise<void>;
type TFetchNext = () => void;

interface UseRequestResult<T> {
  data: T[] | null;
  dataById: Record<string, T>;
  pagination: Pageable | null;
  fetchData: TFetchData;
  fetchNext: TFetchNext;
  errors: ResponseError[];
  status: TStatus;
}

const useRequest = <T = unknown>({
  url,
  method = 'GET',
  queryParams = {},
  body = {},
  fetchOnInit = true,
}: UseRequestConfig): UseRequestResult<T> => {
  const [data, setData] = useState<T[] | null>(null);
  const [dataById, setDataById] = useState<Record<string, T>>({});
  const [pagination, setPagination] = useState<Pageable | null>(null);
  const [errors, setErrors] = useState<ResponseError[]>([]);
  const [status, setStatus] = useState<TStatus>('init');

  const fetchData: TFetchData = async ({
    page = DEFAULT_PAGINATION.page,
    size = DEFAULT_PAGINATION.pageSize,
    customQueryParams = {},
    customBody = {},
  } = {}) => {
    if (!['loadingNext', 'loading'].includes(status)) {
      setStatus('loading');
      const {
        data: responseData,
        errors: responseErrors,
        pageable,
      }: ResponseObj<T[]> = await request(method, url, {
        params: { ...queryParams, ...customQueryParams, page, size },
        data: { ...body, ...customBody },
      });

      if (responseErrors && responseErrors.length > 0) {
        setErrors(responseErrors);
        setStatus('error');
        return;
      }

      if (responseData) {
        if (page > 1 && Array.isArray(responseData)) {
          setData((prevData) =>
            prevData ? [...(prevData as T[]), ...responseData] : responseData,
          );
        } else {
          setData(responseData);
        }

        if (pageable) setPagination(pageable);

        if (Array.isArray(responseData)) {
          setDataById((prev) => ({ ...prev, ...keyBy(responseData, 'id') }));
        } else {
          setDataById({ [(responseData as any).id]: responseData });
        }
      }

      setStatus('success');
    }
  };

  const fetchNext: TFetchNext = () => {
    if (pagination && !pagination?.last) {
      fetchData({ page: pagination.page + 1 });
    }
  };

  useEffect(() => {
    if (fetchOnInit) {
      fetchData();
    }
  }, []);

  return {
    data,
    dataById,
    pagination,
    fetchData,
    fetchNext,
    errors,
    status,
  };
};

export default useRequest;
