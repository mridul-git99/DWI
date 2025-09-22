import { LoadingContainer } from '#components';
import { useQueryParams } from '#hooks/useQueryParams';
import { apiGetAllUsersIdByGroupId } from '#utils/apiUrls';
import { request } from '#utils/request';
import React, { useEffect, useState } from 'react';
import { UpsertUserGroupContainer } from './index';

export const EditUserGroupContainer = ({ id }: any) => {
  const { getQueryParam } = useQueryParams();
  const [data, setData] = useState({ status: 'init' });

  const readOnly = !!getQueryParam('readOnly');

  const getUserGroupData = async () => {
    const { data: groupData, errors } = await request('GET', apiGetAllUsersIdByGroupId(id), {});
    if (errors) {
      setData({ status: 'error', errors });
      return;
    }
    setData({ ...groupData, status: 'success' });
  };

  useEffect(() => {
    getUserGroupData();
  }, [id]);

  if (!data || data.status !== 'success') {
    return <LoadingContainer loading={true} />;
  }

  return (
    <UpsertUserGroupContainer
      prefillData={data}
      userList={data?.allUserIds}
      isEdit={true}
      id={id}
      readOnly={readOnly}
    />
  );
};
