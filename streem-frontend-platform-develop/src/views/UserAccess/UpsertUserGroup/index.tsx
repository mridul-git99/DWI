import React, { useState } from 'react';
import styled from 'styled-components';
import { UpsertUserGroupConfig } from './UpsertUserGroupConfig';
import { FormProvider, useForm } from 'react-hook-form';
import { getErrorMsg, request } from '#utils/request';
import { apiUserGroups } from '#utils/apiUrls';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { useDispatch } from 'react-redux';
import { useNavigate } from '@reach/router';

const Wrapper = styled.div`
  margin: 0 auto;
  width: 100%;

  .footer-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
    background-color: #ffffff;
    position: absolute;
    padding: 20px;
    flex: 1;
    bottom: 0;
    left: 0;
    width: -webkit-fill-available;
  }
`;

export const UpsertUserGroupContainer = ({
  id,
  prefillData,
  isEdit,
  userList = [],
  readOnly = false,
}: any) => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [usersId, setUsersId] = useState(userList);
  const [userRemovalReason, setUserRemovalReason] = useState({});

  const form = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      groupName: prefillData?.name || '',
      groupDescription: prefillData?.description || '',
      groupReason: '',
    },
  });

  const { getValues } = form;

  const handleUpsertPayload = async () => {
    const { groupName, groupDescription, groupReason } = getValues();
    const payload = {
      name: groupName,
      description: groupDescription,
      reason: groupReason,
      active: true,
    };
    const addedUsers = usersId.filter((item) => !userList.includes(item));
    const deletedUsers = userList.filter((item) => !usersId.includes(item));
    let response;
    if (isEdit) {
      response = await request('PATCH', apiUserGroups(id), {
        data: {
          ...payload,
          assignedUserIds: addedUsers || [],
          removedUser: (deletedUsers || []).map((userId) => {
            return { userId: userId, reason: userRemovalReason?.[userId] || '' };
          }),
          ...(usersId.length === 0 && { unAssignAllUsers: true }),
        },
      });
      if (response.status === 'OK') {
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: 'Group Updated',
            detail: `${groupName} group has been updated with changes.`,
          }),
        );
        navigate('/users?tab=1');
      } else {
        dispatch(
          showNotification({
            type: NotificationType.ERROR,
            msg: getErrorMsg(response?.errors),
          }),
        );
      }
    } else {
      response = await request('POST', apiUserGroups(), {
        data: { ...payload, userIds: usersId },
      });
      if (response.status === 'OK') {
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: 'New Group Added',
            detail: `You have successfully created the group '${groupName}'.`,
          }),
        );
        navigate('/users?tab=1');
      } else {
        dispatch(
          showNotification({
            type: NotificationType.ERROR,
            msg: getErrorMsg(response?.errors),
          }),
        );
      }
    }
  };

  return (
    <Wrapper>
      <FormProvider {...form}>
        <UpsertUserGroupConfig
          handleUpsertPayload={handleUpsertPayload}
          userList={userList}
          usersId={usersId}
          setUsersId={setUsersId}
          userRemovalReason={userRemovalReason}
          setUserRemovalReason={setUserRemovalReason}
          id={id}
          readOnly={readOnly}
          isEdit={isEdit}
        />
      </FormProvider>
    </Wrapper>
  );
};
