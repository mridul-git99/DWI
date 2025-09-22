import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { BaseModal } from '#components/shared/BaseModal';
import { getUserName, User, UserGroup } from '#services/users';
import { Error } from '#utils/globalTypes';
import React, { FC } from 'react';
import styled from 'styled-components';

type TAssignmentError = Error & {
  userId: User['id'] | null;
  userGroupId: string | null;
};

type TAssignmentInfoProps = {
  errors: TAssignmentError[];
  assignedUsers: User[];
  unassignedUsers: User[];
  assignedUserGroups: UserGroup[];
  unassignedUserGroups: UserGroup[];
};

const Wrapper = styled.div`
  .modal {
    max-width: 600px !important;
  }

  .errors-info {
    display: flex;
    flex-direction: column;
    gap: 16px;
    max-height: 500px;
  }

  .error-item {
    color: #333333;
    font-size: 14px;
    letter-spacing: 0.16px;
    line-height: 1.43;
  }
`;

const AssignmentInfo: FC<CommonOverlayProps<TAssignmentInfoProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: {
    errors = [],
    assignedUsers = [],
    unassignedUsers = [],
    assignedUserGroups = [],
    unassignedUserGroups = [],
  } = {},
}) => {
  const ErrorInfo = (error: TAssignmentError) => {
    const { userId, userGroupId } = error;

    const UserOrUserGroup = userId
      ? [...assignedUsers, ...unassignedUsers].find((user) => user?.userId === userId)
      : userGroupId
      ? [...assignedUserGroups, ...unassignedUserGroups].find(
          (userGroup) => userGroup?.userGroupId === userGroupId,
        )
      : null;

    const name = UserOrUserGroup ? getName(UserOrUserGroup, userId ? 'user' : 'usergroup') : '';

    return (
      <div className="error-item">
        <span>{name}</span> {error?.message}
      </div>
    );
  };

  const getName = (user: User | UserGroup, entity: 'user' | 'usergroup') => {
    return entity === 'user'
      ? getUserName({
          user,
          withEmployeeId: true,
        })
      : (user as UserGroup)?.userGroupName;
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title={'Action could not be taken for the following users'}
        showFooter={false}
      >
        <div className="errors-info">
          {(errors || []).map((error) => {
            return <ErrorInfo key={error.id} {...error} />;
          })}
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default AssignmentInfo;
