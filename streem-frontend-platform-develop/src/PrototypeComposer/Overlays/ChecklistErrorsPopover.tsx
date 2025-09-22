import closeIcon from '#assets/svg/close-icon.svg';
import errorIcon from '#assets/svg/error-indicator.svg';
import navigateIcon from '#assets/svg/external-link-icon.svg';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store/helpers';
import { Error } from '#utils/globalTypes';
import { Popover } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { navigate } from '@reach/router';
import React, { FC } from 'react';

const useStyles = makeStyles({
  popover: {},
  paper: {
    marginTop: '16px',
    overflowY: 'auto',
    overflowX: 'hidden',
    padding: '16px',
    borderRadius: '0px',
    boxShadow: '0px 2px 6px 0px rgba(0,0,0,0.25)',
    cursor: 'pointer',
    width: '397px',
    maxHeight: '500px',
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  title: {
    fontSize: '16px',
    color: '#525252',
    fontWeight: 700,
  },
  desc: {
    fontSize: '14px',
    color: '#525252',
    fontWeight: 400,
  },
  errors: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },
  error: {
    width: '100%',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: '8px',
  },
  errorMessages: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
    justifyContent: 'flex-start',
    gap: '2px',
    width: '100%',
  },
  errorEntity: {
    fontSize: '14px',
    color: '#161616',
    fontWeight: 700,
  },
  errorMessage: {
    fontSize: '14px',
    color: '#161616',
    fontWeight: 400,
  },
});

export const ChecklistErrorsPopover: FC<
  CommonOverlayProps<{ errors: (Error & { title: string })[] }>
> = ({ closeOverlay, popOverAnchorEl, props: { errors } }) => {
  const classes = useStyles();

  const {
    stages: { listById: stageListById },
    tasks: { listById: taskListById },
    parameters: { parameterOrderInTaskInStage },
  } = useTypedSelector((state) => state.prototypeComposer);

  const handleNavigation = (error: Error) => {
    // E449 - Process contains parameters that are not mapped
    if (error.code === 'E449') {
      navigate(`?tab=1`);
    } else if (error.entity === 'stage') {
      navigate(`?stageId=${error.id}`);
    } else if (error.entity === 'task') {
      const stage = stageListById[taskListById[error.id]?.stageId];
      navigate(`?stageId=${stage.id}&taskId=${error.id}`);
    } else if (error.entity === 'parameter') {
      let stageId = '';
      let taskId = '';
      Object.entries(parameterOrderInTaskInStage).every(([_stageId, parameterOrderInTask]) => {
        Object.entries(parameterOrderInTask).every(([_taskId, parameterIds]) => {
          if (parameterIds.includes(error.id)) {
            stageId = _stageId;
            taskId = _taskId;
            return false;
          }
        });
        return !stageId;
      });
      if (stageId && taskId) {
        navigate(`?stageId=${stageId}&taskId=${taskId}`);
      }
    }
  };

  return (
    <Popover
      id={`checklist-errors-popover`}
      open={!!popOverAnchorEl}
      anchorEl={popOverAnchorEl}
      onClose={closeOverlay}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'right',
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
      <div className={classes.errorContainer}>
        <div className={classes.header}>
          <span className={classes.title}>Errors</span>
          <img onClick={closeOverlay} src={closeIcon} />
        </div>
        <div className={classes.desc}>
          There are following errors at following stages, kindly rectify them to proceed
        </div>
        <div className={classes.errors}>
          {errors.map((error, index) => (
            <div key={index} className={classes.error}>
              <img src={errorIcon}></img>
              <div className={classes.errorMessages}>
                <span className={classes.errorEntity}>{error.title}</span>
                <span className={classes.errorMessage}>{error.message}</span>
              </div>
              <img
                onClick={() => {
                  handleNavigation(error);
                  closeOverlay();
                }}
                src={navigateIcon}
              ></img>
            </div>
          ))}
        </div>
      </div>
    </Popover>
  );
};
