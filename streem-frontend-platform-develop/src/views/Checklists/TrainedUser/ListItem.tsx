import React, { useMemo } from 'react';
import { Button, Checkbox, RoleTag } from '#components';
import { Avatar, parseEntityData } from '#components/shared/Avatar';
import styled from 'styled-components';
import { User } from '#store/users/types';

type ListItemProps = {
  user: User;
  selected?: boolean;
  partialSelected?: boolean;
  onClick?: (checked: boolean) => void;
  checkboxDisabled?: boolean;
  onRemove?: () => void;
  buttonDisabled?: boolean;
  userSelectionDisabled?: boolean;
  isGroup?: boolean;
  showRoleTag: boolean;
};

const ListItemWrapper = styled.div`
  align-items: center;
  border-bottom: 1px solid #eeeeee;
  display: flex;
  padding: 8px 0;

  .checkbox-input {
    margin-top: -19px;

    label.container {
      color: #333333;
      font-weight: bold;
    }
  }

  .avatar {
    margin-right: 16px;
  }

  .user,
  .group {
    &-detail {
      display: flex;
      flex-direction: column;
    }

    &-id {
      color: #999999;
      font-size: 12px;
      line-height: 1.33;
      letter-spacing: 0.32px;
      text-align: left;
    }

    &-name {
      color: #333333;
      font-size: 20px;
      font-weight: 600;
      line-height: 1.2;
      text-align: left;
    }
  }
`;

export const ListItem = ({
  user,
  selected,
  onClick,
  checkboxDisabled,
  onRemove,
  buttonDisabled,
  isGroup = false,
  showRoleTag,
}: ListItemProps) => {
  const entity = useMemo(() => parseEntityData(user, isGroup), [user, isGroup]);
  return (
    <ListItemWrapper>
      {onClick && (
        <Checkbox
          onClick={(checked) => onClick?.(!!checked)}
          checked={selected}
          disabled={checkboxDisabled}
        />
      )}
      <Avatar user={entity} size="large" allowMouseEvents={false} isGroup={isGroup} />
      <div className="user-detail">
        <span className="user-id">{entity.employeeId}</span>
        <span className="user-name">{entity.name}</span>
      </div>
      {showRoleTag && entity.roles && <RoleTag roles={entity.roles} />}
      {onRemove && (
        <Button
          style={{ marginLeft: !entity.roles ? 'auto' : 'unset' }}
          variant="textOnly"
          color="gray"
          disabled={buttonDisabled}
          onClick={onRemove}
        >
          x
        </Button>
      )}
    </ListItemWrapper>
  );
};
