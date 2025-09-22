import { Avatar, AvatarExtras } from '#components';
import { User } from '#services/users/types';
import React from 'react';
import styled from 'styled-components';

const Wrapper = styled.div.attrs({
  className: 'assignments',
})`
  display: flex;
  margin-left: 16px;

  > .avatar:nth-child(1n) {
    margin-right: -5px;
  }
`;

type Props = {
  users: User[];
  count?: number;
  userCount?: number;
  isGroup?: boolean;
};

const AssigneeList = ({ users, count = 4, userCount = 0, isGroup = false }: Props) => {
  return (
    <Wrapper>
      {users.slice(0, count).map((user) => (
        <Avatar user={user} key={user.id || user.userId || user.userGroupId} isGroup={isGroup} />
      ))}

      {users.length > count && (
        <AvatarExtras
          users={users.slice(count)}
          hideMouseHover={!userCount}
          userCount={userCount - count}
          borderColor="#ffffff"
          backgroundColor="#eeeeee"
          isGroup={isGroup}
        />
      )}
    </Wrapper>
  );
};

export default AssigneeList;
