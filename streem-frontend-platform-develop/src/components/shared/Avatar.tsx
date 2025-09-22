import { User as UserType, Users } from '#store/users/types';
import { getFullName, getInitials } from '#utils/stringUtils';
import { Popover, makeStyles } from '@material-ui/core';
import React, { FC, MouseEvent, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import styled, { css } from 'styled-components';
import { closeOverlayAction, openOverlayAction } from '../OverlayContainer/actions';
import { OverlayNames, CommonOverlayProps } from '../OverlayContainer/types';
import GroupPersonIcon from '#assets/svg/group-person-icon.svg';

type User = Pick<UserType, 'id' | 'firstName' | 'lastName' | 'employeeId'>;

type Props = {
  color?: string;
  size?: 'small' | 'medium' | 'large';
  borderColor?: string;
  backgroundColor?: string;
  user: User;
  allowMouseEvents?: boolean;
  groupAvatar?: boolean;
  tip?: boolean;
  isGroup?: boolean;
};

const Wrapper = styled.div.attrs({
  className: 'avatar',
})<Pick<Props, 'size' | 'color' | 'borderColor' | 'backgroundColor' | 'tip'>>`
  align-items: center;
  background-color: ${({ backgroundColor }) => `${backgroundColor}`};
  border: 1px solid ${({ borderColor }) => `${borderColor}`};
  position: relative;
  top: ${({ tip = false }) => (tip === true ? '10px' : '0')};
  border-radius: 50%;
  color: ${({ color }) => `${color}`};
  display: flex;
  justify-content: center;

  .subscript-icon {
    position: absolute;
    bottom: -4px;
    right: -4px;
  }

  ${({ size }) => {
    switch (size) {
      case 'small':
        return css`
          font-size: 12px;
          line-height: 12px;
          height: 24px;
          width: 24px;
        `;

      case 'medium':
        return css`
          font-size: 12px;
          line-height: 12px;
          height: 32px;
          width: 32px;
        `;
      case 'large':
        return css`
          font-size: 16px;
          line-height: 16px;
          height: 40px;
          width: 40px;
        `;
      default:
        return null;
    }
  }}
`;

const useStyles = makeStyles({
  paper: {
    marginTop: '5px',
    overflow: 'auto',
    padding: '8px',
    maxHeight: '50%',

    '&::before': {
      content: '""',
      display: 'block',
      width: '0',
      height: '0',
      position: 'absolute',
      borderLeft: '5px solid transparent',
      borderRight: '5px solid transparent',
      borderBottom: '5px solid #FFF',
      left: 'calc(50% - 5px)',
      top: '-5px',
    },
  },
  title: {
    fontSize: '20px',
    color: '#00000',
    marginTop: '8px',
  },
  id: {
    fontWeight: 600,
    fontSize: '8px',
    color: '#00000',
  },
  wrapper: {
    display: 'flex',
    flexDirection: 'column',
    padding: '8px',
  },
});

export const parseEntityData = (entity: any, isGroup: boolean) => {
  if (isGroup) {
    return {
      id: entity.id || entity.userGroupId,
      name: entity.userGroupName || entity.name,
    };
  } else {
    return {
      id: entity.id,
      name: getFullName(entity),
      roles: entity.roles,
      employeeId: entity.employeeId,
      ...entity,
    };
  }
};

const parseEntitiesData = (entities: any[], isGroup: boolean) => {
  return {
    entities: entities.map((entity) => parseEntityData(entity, isGroup)),
  };
};

export const Avatar: FC<Props> = ({
  color = '#59afff',
  size = 'medium',
  borderColor = '#ffffff',
  backgroundColor = '#f2f2f2',
  user,
  allowMouseEvents = true,
  isGroup = false,
}) => {
  const dispatch = useDispatch();
  const entity = useMemo(() => parseEntityData(user, isGroup), [user, isGroup]);
  return (
    <Wrapper
      color={color}
      size={size}
      borderColor={borderColor}
      backgroundColor={backgroundColor}
      data-testid="avatar-wrapper"
      {...(allowMouseEvents
        ? {
            onMouseEnter: (event: MouseEvent) => {
              dispatch(
                openOverlayAction({
                  type: OverlayNames.AVATAR_DETAIL,
                  popOverAnchorEl: event.currentTarget,
                  props: { users: [entity], isGroup },
                }),
              );
            },
            onMouseLeave: () => {
              dispatch(closeOverlayAction(OverlayNames.AVATAR_DETAIL));
            },
          }
        : {})}
    >
      {getInitials(entity.name)}
      {isGroup && <img className="subscript-icon" src={GroupPersonIcon} />}
    </Wrapper>
  );
};

export type AvatarExtrasProps = Pick<
  Props,
  'size' | 'color' | 'borderColor' | 'backgroundColor'
> & {
  users: User[];
  hideMouseHover?: boolean;
  userCount?: number;
  isGroup: boolean;
};

export const AvatarExtras: FC<AvatarExtrasProps> = ({
  color = '#59afff',
  size = 'medium',
  borderColor = '#ffffff',
  backgroundColor = '#f2f2f2',
  users,
  hideMouseHover = true,
  userCount,
  isGroup = false,
}) => {
  const dispatch = useDispatch();
  const { entities } = useMemo(() => parseEntitiesData(users, isGroup), [users]);
  return (
    <Wrapper
      color={color}
      size={size}
      borderColor={borderColor}
      backgroundColor={backgroundColor}
      data-testid="avatar-extras-wrapper"
      onMouseEnter={(event: MouseEvent) => {
        hideMouseHover &&
          dispatch(
            openOverlayAction({
              type: OverlayNames.AVATAR_DETAIL,
              popOverAnchorEl: event.currentTarget,
              props: { users: entities, isGroup },
            }),
          );
      }}
      onMouseLeave={
        hideMouseHover && users.length === 1
          ? () => {
              dispatch(closeOverlayAction(OverlayNames.AVATAR_DETAIL));
            }
          : undefined
      }
    >
      + {users.length || userCount}
    </Wrapper>
  );
};

export const AssignedUserDetailsPopover: FC<
  CommonOverlayProps<{
    users: Users;
    isGroup?: boolean;
  }>
> = ({ closeOverlay, popOverAnchorEl, props: { users: data, isGroup } }) => {
  const classes = useStyles();
  return (
    <Popover
      id={`assignedGroupDetailsPopOver`}
      open={!!popOverAnchorEl}
      anchorEl={popOverAnchorEl}
      onClose={closeOverlay}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'center',
      }}
      transformOrigin={{
        vertical: 'top',
        horizontal: 'center',
      }}
      classes={{
        paper: classes.paper,
      }}
      {...(data.length === 1 && {
        style: { pointerEvents: 'none' },
      })}
    >
      {data.map((item: any, index: number) => (
        <div className={classes.wrapper} key={`assignedGroupDetailsPopOver_${item.id || index}`}>
          {!isGroup && <span className={classes.id}>{item.employeeId || item.id}</span>}
          <span className={classes.title}>{item.name}</span>
        </div>
      ))}
    </Popover>
  );
};
