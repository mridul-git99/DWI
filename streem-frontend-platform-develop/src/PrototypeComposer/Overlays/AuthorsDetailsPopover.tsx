import React, { FC } from 'react';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { User } from '#store/users/types';
import { Popover } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles({
  popover: {
    pointerEvents: 'none',
  },
  paper: {
    marginTop: '5px',
    overflow: 'visible',
    padding: '24px',

    '&::before': {
      content: '""',
      display: 'block',
      width: '0',
      height: '0',
      position: 'absolute',
      borderLeft: '5px solid transparent',
      borderRight: '5px solid transparent',
      borderBottom: '5px solid #FFF',
      left: 'calc(100% - 5px)',
      top: '-5px',
    },
  },
  title: {
    fontSize: '14px',
    color: '#00000',
    marginTop: '8px',
    textTransform: 'capitalize',
  },
  wrapper: {
    display: 'flex',
  },
  heading: {
    fontSize: '14px',
    color: '#00000',
    fontWeight: 600,
  },
});

export const AuthorsDetailsPopover: FC<
  CommonOverlayProps<{
    users: User[];
  }>
> = ({ closeOverlay, popOverAnchorEl, props: { users } }) => {
  const classes = useStyles();

  return (
    <Popover
      id={`authorsDetailsPopOver`}
      open={!!popOverAnchorEl}
      anchorEl={popOverAnchorEl}
      onClose={closeOverlay}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'center',
      }}
      transformOrigin={{
        vertical: 'top',
        horizontal: 'right',
      }}
      className={classes.popover}
      classes={{
        paper: classes.paper,
      }}
    >
      <span className={classes.heading}>Authors</span>
      {users.map((user: User, index: number) => (
        <div className={classes.wrapper} key={`authorsDetailsPopOver_${user.id}`}>
          <span className={classes.title}>{`${index + 1}. ${user.firstName} ${user.lastName} ID: ${
            user.employeeId
          }`}</span>
        </div>
      ))}
    </Popover>
  );
};
