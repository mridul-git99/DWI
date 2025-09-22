import { generateUserSearchFilters } from '#utils/smartFilterUtils';
import { useState } from 'react';
import { createFetchList } from './useFetchData';
import { FilterOperators } from '#utils/globalTypes';

export function getDataSetById(_userDetailsUrl: any, _params = {}) {
  const [state, setState] = useState<{
    fetchDataCount: number;
    ids: string[];
    isLast: boolean;
  }>({
    fetchDataCount: 0,
    ids: [],
    isLast: false,
  });

  const { fetchDataCount, isLast } = state;

  const { list, listById, status, reset, fetchNext, filters, pagination } = createFetchList(
    _userDetailsUrl,
    {},
    false,
  );

  const fetchData = ({ ids = [], searchQuery }: { ids: string[]; searchQuery?: string }) => {
    if (searchQuery) {
      setState((prev) => ({ ...prev, fetchDataCount: 0, ids, isLast: false }));
      reset({
        params: {
          filters: generateUserSearchFilters(FilterOperators.LIKE, searchQuery),
        },
      });
    } else {
      const values = ids.slice(0, 10);
      setState((prev) => ({
        ...prev,
        fetchDataCount: values.length,
        ids,
        isLast: values.length === ids.length,
      }));
      if (values.length) {
        reset({
          params: {
            filters: {
              op: FilterOperators.AND,
              fields: [{ field: 'id', op: FilterOperators.ANY, values }],
            },
          },
        });
      }
    }
  };

  const _fetchNext = ({ ids }: any) => {
    if (isLast) return;
    const filteringById = filters?.find((field) => field.field === 'id');
    if (filteringById) {
      const values = ids.slice(fetchDataCount, fetchDataCount + 10);
      setState((prev) => {
        const _fetchDataCount = prev.fetchDataCount + values.length;
        return {
          ...prev,
          fetchDataCount: _fetchDataCount,
          ids,
          isLast: ids.length <= _fetchDataCount,
        };
      });
      fetchNext(
        {
          page: 0,
          filters: {
            op: FilterOperators.AND,
            fields: [{ field: 'id', op: FilterOperators.ANY, values }],
          },
        },
        true,
      );
    } else {
      setState((prev) => ({ ...prev, fetchDataCount: 0 }));
      fetchNext();
    }
  };

  return {
    list: list,
    listById,
    status,
    isLast,
    pagination,
    fetchData,
    fetchNext: _fetchNext,
  };
}
