import { LoadingContainer } from '#components';
import checkPermission, { RoleIdByName } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { fetchFacilities } from '#store/facilities/actions';
import { fetchSelectedUser } from '#store/users/actions';
import { User } from '#store/users/types';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import ManageUser from './ManageUser';
import { PAGE_TYPE, ViewUserProps } from './types';

type InitialState = {
  isEditable: boolean;
  isAccountOwner: boolean;
};

const EditUserContainer: FC<ViewUserProps> = ({ id, pageType }) => {
  const dispatch = useDispatch();
  const { selectedUser, selectedUserLoading } = useTypedSelector((state) => state.users);
  const { list } = useTypedSelector((state) => state.facilities);

  const [state, setState] = useState<InitialState | undefined>();

  useEffect(() => {
    dispatch(fetchFacilities());
    dispatch(fetchSelectedUser(id));
    if (pageType === PAGE_TYPE.ADD) {
      setState({
        isEditable: true,
        isAccountOwner: false,
      });
    }
  }, []);

  useEffect(() => {
    if (selectedUser?.id) {
      const isAccountOwner = !!(
        selectedUser.roles && selectedUser.roles[0].id === RoleIdByName.ACCOUNT_OWNER
      );
      const isSystemAdmin = !!(
        selectedUser.roles && selectedUser.roles[0].id === RoleIdByName.SYSTEM_ADMIN
      );

      const isEditable = checkPermission(['usersAndAccess', 'selectedUser', 'form', 'editable'])
        ? isAccountOwner
          ? checkPermission(['usersAndAccess', 'editAccountOwner'])
          : true
        : false;

      setState({
        isAccountOwner,
        isEditable:
          pageType === PAGE_TYPE.PROFILE
            ? isAccountOwner || isSystemAdmin
              ? true
              : false
            : isEditable,
      });
    }
  }, [selectedUser?.id]);

  return (
    <LoadingContainer
      loading={selectedUserLoading || !list?.length || !state}
      component={
        <ManageUser
          user={selectedUser as User}
          facilities={list!}
          isEditable={!!state?.isEditable}
          isAccountOwner={!!state?.isAccountOwner}
          pageType={pageType}
        />
      }
    />
  );
};

export default EditUserContainer;
