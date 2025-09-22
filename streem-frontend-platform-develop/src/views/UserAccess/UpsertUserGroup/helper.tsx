import React from 'react';
import { useFormContext } from 'react-hook-form';
import { GroupDetails } from './GroupDetails';
import { ReasonDetails } from './ReasonDetails';
import { UserDetailsContainer } from './UserDetailsContainer';

export const createGroupConfig = ({
  readOnly,
  isDirtyForm,
  userList,
  id,
  usersId,
  setUsersId,
  userRemovalReason,
  setUserRemovalReason,
}: any) => {
  const {
    formState: { isDirty },
  } = useFormContext();

  const config = [
    {
      label: 'Add Group Details',
      view: <GroupDetails readOnly={readOnly} />,
    },
    {
      label: 'Add Users',
      view: (
        <UserDetailsContainer
          userList={userList}
          id={id}
          isDirtyForm={isDirtyForm}
          usersId={usersId}
          setUsersId={setUsersId}
          userRemovalReason={userRemovalReason}
          setUserRemovalReason={setUserRemovalReason}
          readOnly={readOnly}
        />
      ),
    },
    ...(isDirty || isDirtyForm.current
      ? [
          {
            label: 'Reason',
            view: <ReasonDetails readOnly={readOnly} />,
          },
        ]
      : []),
  ];

  return config;
};
