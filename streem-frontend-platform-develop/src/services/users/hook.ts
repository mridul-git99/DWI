import { useTypedSelector } from '#store/helpers';
import { useEffect } from 'react';
import { useDispatch } from 'react-redux';

import { fetch } from './actions';
import { fetchUsersParams, UserState, useUsersType, useUsersReturnType } from './types';
import { DEFAULT_PAGE_SIZE } from '#utils/constants';

const defaultParams = (includeSorting = true): fetchUsersParams => {
  return {
    page: 0,
    size: DEFAULT_PAGE_SIZE,
    ...(includeSorting ? { sort: 'firstName,asc' } : {}),
  };
};

const useUsers = ({
  userState = UserState.ACTIVE,
  params = defaultParams(),
}: useUsersType): useUsersReturnType => {
  const { pageable, users, usersById } = useTypedSelector((state) => state.usersService[userState]);

  const dispatch = useDispatch();

  useEffect(() => {
    if (!users.length) {
      dispatch(fetch({ initialCall: true, params, type: userState }));
    }
  }, []);

  const loadMore = () => {
    if (!pageable.last) {
      dispatch(
        fetch({
          initialCall: false,
          type: userState,
          params: {
            filters: params.filters,
            page: pageable.page + 1,
            size: pageable.pageSize,
            sort: params.sort,
          },
        }),
      );
    }
  };

  const loadAgain = ({ newParams = params }) => {
    dispatch(
      fetch({
        initialCall: true,
        type: userState,
        params: {
          filters: newParams.filters,
          page: 0,
          size: newParams.size,
          sort: newParams.sort,
        },
      }),
    );
  };

  return { loadMore, users, usersById, loadAgain };
};

export { defaultParams, useUsers };
