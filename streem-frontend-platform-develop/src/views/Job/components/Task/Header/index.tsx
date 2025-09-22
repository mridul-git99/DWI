import BulkVerificationIcon from '#assets/svg/BulkVerificationIcon';
import ScheduleTask from '#assets/svg/ScheduleTask';
import SoloTaskLock from '#assets/svg/SoloTaskLock';
import TaskDependencyIcon from '#assets/svg/TaskDependencyIcon';
import TaskExecutorLock from '#assets/svg/TaskExecutorLock';
import TaskRecurrence from '#assets/svg/TaskRecurrence';
import configureActionsIcon from '#assets/svg/configure-actions.svg';
import repeatTaskIcon from '#assets/svg/repeat-task.svg';
import { AssigneeList, StyledMenu } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import Tooltip from '#components/shared/Tooltip';
import checkPermission, { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { TaskAction, TaskExecutionType, TaskPauseReasons } from '#types';
import { InputTypes } from '#utils/globalTypes';
import { formatDateTime, formatDuration, getEpochTimeDifference } from '#utils/timeUtils';
import AssignBulkPeerVerification from '#views/Job/components/Task/Parameters/Verification/AssignBulkPeerVerification';
import Timer from '#views/Job/components/Task/Timer';
import { jobActions } from '#views/Job/jobStore';
import { useJobStateToFlags } from '#views/Job/utils';
import { MenuItem } from '@material-ui/core';
import { Error as ErrorIcon, MoreVert, PanTool } from '@material-ui/icons';
import React, { FC, MouseEvent, useState } from 'react';
import { useDispatch } from 'react-redux';
import ReasonTag from './ReasonTag';
import { Wrapper } from './styles';

type HeaderProps = {
  isUserAssignedToJob: boolean;
};

const Header: FC<HeaderProps> = ({ isUserAssignedToJob }) => {
  const dispatch = useDispatch();
  const {
    timerState: { limitCrossed },
    isInboxView,
    activeTask: task,
    errors: { taskErrors },
  } = useTypedSelector((state) => state.job);
  const { userId } = useTypedSelector((state) => state.auth);
  const { isJobStarted, isTaskCompleted, isBlocked, isTaskStarted, isTaskPaused } =
    useJobStateToFlags();

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [requestPeerJobDrawerVisible, setRequestPeerJobDrawerVisible] = useState<boolean>(false);

  const {
    isTaskAssigned: isUserAssignedToTask,
    stageOrderTree,
    taskExecution: {
      state,
      reason,
      correctionEnabled,
      assignees,
      userGroupAssignees,
      type,
      id: taskExecutionId,
      continueRecurrence,
      pauseReasons,
      recurringPrematureStartReason,
      recurringOverdueCompletionReason,
      recurringExpectedStartedAt,
      recurringExpectedDueAt,
      schedulingExpectedStartedAt,
      schedulingExpectedDueAt,
      schedulePrematureStartReason,
      scheduleOverdueCompletionReason,
      scheduleTaskSummary,
      startedBy,
      startedAt,
      endedBy,
      endedAt,
      orderTree: taskExecutionOrderTree,
      audit,
    },
    taskRecurrence,
    canSkipTask,
    enableRecurrence,
    enableScheduling,
    hasPrerequisites,
    hasExecutorLock,
    hasBulkVerification,
    orderTree,
    soloTask,
    pendingSelfVerificationParameters,
    pendingPeerVerificationParameters,
    executedParametersWithPeerVerification,
  } = task;

  // Calculate parameters with pending peer verification that current user initiated
  const parametersInitiatedByUser = Array.from(task.parameters.values()).filter((param: any) => {
    const response = param.response;
    if (!response) return false;

    // Check if current user initiated this parameter
    const isInitiator = userId === response?.audit?.modifiedBy?.id;
    if (!isInitiator) return false;

    // Check if parameter has pending peer verification
    const peerVerifications =
      response.parameterVerifications?.filter((v: any) => v.verificationType === 'PEER') || [];

    const hasPendingPeerVerification = peerVerifications.some(
      (v: any) => v.verificationStatus === 'PENDING',
    );

    return hasPendingPeerVerification;
  });

  const {
    negativeStartDateToleranceInterval,
    positiveStartDateToleranceInterval,
    positiveDueDateToleranceInterval,
    negativeDueDateToleranceInterval,
  } = taskRecurrence || {};

  const handleClose = () => setAnchorEl(null);

  const handleClick = (event: MouseEvent<HTMLDivElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const reasonTitle = () => {
    switch (state) {
      case 'COMPLETED':
        if (limitCrossed) return 'Delayed completion of Timed Task';
        return 'Early completion of Timed Task';
      case 'SKIPPED':
        return 'Task Skipped';
      case 'COMPLETED_WITH_EXCEPTION':
        return 'Completed With Exception';
    }
  };

  const handleEnableErrorCorrection = (closeModal: () => void) => {
    dispatch(jobActions.enableErrorCorrectionOnTask({ taskExecutionId }));
    closeModal();
  };

  const showMenuActions = () => {
    if (checkPermission(['inbox', 'bypassMenuValidations'])) {
      return true;
    }

    const isErrorCorrectionVisible = isTaskCompleted && !correctionEnabled;

    const isCweVisible =
      !isTaskCompleted &&
      !isBlocked &&
      checkPermission(['inbox', 'completeWithException']) &&
      (isUserAssignedToTask || (soloTask && isUserAssignedToJob));

    const isSkipVisible = canSkipTask && !isTaskStarted && !isBlocked && isUserAssignedToTask;

    const menuOptionsVisible =
      isErrorCorrectionVisible ||
      isCweVisible ||
      isSkipVisible ||
      parametersInitiatedByUser.length > 0;

    return isInboxView && isJobStarted && isUserAssignedToJob && menuOptionsVisible;
  };

  const handleRecurringTaskCreation = (action: TaskAction, reason: string) => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.TASK_RECURRENCE_EXECUTION_MODAL,
        props: {
          onPrimary: () => {
            dispatch(
              jobActions.performTaskAction({
                id: taskExecutionId,
                reason,
                action,
                continueRecurrence: true,
              }),
            );
          },
          onSecondary: () => {
            dispatch(
              openOverlayAction({
                type: OverlayNames.END_TASK_RECURRENCE_MODAL,
                props: {
                  onPrimary: () => {
                    dispatch(
                      jobActions.performTaskAction({
                        id: taskExecutionId,
                        reason,
                        action,
                        continueRecurrence: false,
                      }),
                    );
                  },
                },
              }),
            );
          },
        },
      }),
    );
  };

  const formatToleranceDateRange = ({ startDate, negativeTolerance, positiveTolerance }: any) => {
    const start = negativeTolerance
      ? formatDateTime({
          value: startDate - negativeTolerance,
          type: InputTypes.TIME,
        })
      : formatDateTime({ value: startDate, type: InputTypes.TIME });

    const end = positiveTolerance
      ? formatDateTime({
          value: startDate + positiveTolerance,
          type: InputTypes.TIME,
        })
      : formatDateTime({ value: startDate, type: InputTypes.TIME });

    return <>{negativeTolerance || positiveTolerance ? `${start} - ${end}` : `${start}`}</>;
  };

  return (
    <Wrapper>
      <div className="task-header">
        <div className="task-config">
          <div className="wrapper">
            <div className="task-name">{`${stageOrderTree}.${orderTree}${
              taskExecutionOrderTree === 1 ? '' : `.${taskExecutionOrderTree - 1}`
            } ${task.name}`}</div>
            <Timer state={state} id={taskExecutionId} />
          </div>
        </div>
        <div className="task-info">
          <div className="left-section">
            {task.hasStop && (
              <div>
                <Tooltip title={'Stop Added'} arrow>
                  <PanTool className="icon" style={{ color: '#f2c94c' }} />
                </Tooltip>
              </div>
            )}
            {type === TaskExecutionType.REPEAT && (
              <div>
                <Tooltip title={'Repeated Task'} arrow>
                  <img src={repeatTaskIcon} />
                </Tooltip>
              </div>
            )}
            {enableRecurrence && (
              <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                <Tooltip
                  title={
                    type === TaskExecutionType.MASTER
                      ? 'This is a recurring task. A new instance of this task can be created once this task is completed.'
                      : 'This an instance of a recurring task.'
                  }
                  arrow
                  textAlignment="left"
                >
                  <span style={{ color: continueRecurrence ? '#161616' : '#BBBBBB' }}>
                    <TaskRecurrence />
                  </span>
                </Tooltip>
                {type === TaskExecutionType.RECURRING ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    {recurringExpectedStartedAt && (
                      <span>
                        Start Time :{' '}
                        {formatToleranceDateRange({
                          startDate: recurringExpectedStartedAt,
                          negativeTolerance: negativeStartDateToleranceInterval,
                          positiveTolerance: positiveStartDateToleranceInterval,
                        })}
                      </span>
                    )}
                    {recurringExpectedDueAt && (
                      <span>
                        Due At :{' '}
                        {formatToleranceDateRange({
                          startDate: recurringExpectedDueAt,
                          negativeTolerance: negativeDueDateToleranceInterval,
                          positiveTolerance: positiveDueDateToleranceInterval,
                        })}
                      </span>
                    )}
                  </div>
                ) : null}
              </div>
            )}
            {task.timed && (
              <div className="time-info" style={{ padding: '6px 15px' }}>
                <div>
                  {task.timerOperator === 'NOT_LESS_THAN' ? (
                    <>
                      <span>
                        Perform task in NLT {task.minPeriod && formatDuration(task.minPeriod)}{' '}
                      </span>
                      <span>
                        Max Time limit - {task.maxPeriod && formatDuration(task?.maxPeriod)}
                      </span>
                    </>
                  ) : (
                    <span>Complete under {task.maxPeriod && formatDuration(task?.maxPeriod)}</span>
                  )}
                </div>
              </div>
            )}
            {task.automations.length > 0 && (
              <div
                onClick={() => {
                  dispatch(
                    openOverlayAction({
                      type: OverlayNames.AUTOMATION_TASK_MODAL,
                      props: {
                        taskId: task.id,
                        taskExecutionId: task?.taskExecution?.id,
                      },
                    }),
                  );
                }}
                style={{ display: 'flex', cursor: 'pointer', gap: '10px', color: '#1D84FF' }}
              >
                <Tooltip title={'Automation Task'} arrow>
                  <img src={configureActionsIcon} />
                </Tooltip>
                <span style={{ margin: '5px' }}>Automations ({task.automations.length})</span>
              </div>
            )}
            {type === TaskExecutionType.MASTER && enableScheduling && (
              <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                <Tooltip
                  title={scheduleTaskSummary ? scheduleTaskSummary : 'Task is scheduled'}
                  arrow
                >
                  <span style={{ color: '#161616' }}>
                    <ScheduleTask />
                  </span>
                </Tooltip>
                {schedulingExpectedStartedAt && schedulingExpectedDueAt ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <span>
                      Start Time -{' '}
                      {formatDateTime({
                        value: schedulingExpectedStartedAt,
                        type: InputTypes.TIME,
                      })}
                    </span>
                    <span>
                      End Time -{' '}
                      {formatDateTime({ value: schedulingExpectedDueAt, type: InputTypes.TIME })}
                    </span>
                  </div>
                ) : null}
              </div>
            )}
            {hasBulkVerification && (
              <div>
                <Tooltip title={'Bulk Verification'} arrow>
                  <div className="icon-bg">
                    <BulkVerificationIcon />
                  </div>
                </Tooltip>
              </div>
            )}

            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>Assignees:</span>
              {assignees && assignees.length > 0 && (
                <div className="task-assignees">
                  <AssigneeList users={assignees} count={4} />
                </div>
              )}
              {userGroupAssignees && userGroupAssignees.length > 0 && (
                <div className="task-assignees">
                  <AssigneeList users={userGroupAssignees} count={4} isGroup={true} />
                </div>
              )}
            </div>
            {soloTask && (
              <div>
                <Tooltip title={'Solo Task Locked'} arrow>
                  <div className="icon-bg">
                    <SoloTaskLock />
                  </div>
                </Tooltip>
              </div>
            )}

            {hasPrerequisites && (
              <Tooltip title={'View Dependencies'} arrow>
                <div
                  onClick={() => {
                    dispatch(
                      openOverlayAction({
                        type: OverlayNames.VIEW_TASK_DEPENDENCY_MODAL,
                        props: {
                          taskId: task.id,
                          taskName: `Task ${stageOrderTree}.${orderTree}`,
                          hasPrerequisites,
                        },
                      }),
                    );
                  }}
                >
                  <TaskDependencyIcon className="icon" style={{ color: '#525252' }} />
                </div>
              </Tooltip>
            )}
            {hasExecutorLock && (
              <Tooltip title={'Task Executor Lock'} arrow>
                <div
                  onClick={() => {
                    dispatch(
                      openOverlayAction({
                        type: OverlayNames.TASK_EXECUTOR_LOCK_DETAILS,
                        props: {
                          taskId: task.id,
                        },
                      }),
                    );
                  }}
                >
                  <div className="icon-bg">
                    <TaskExecutorLock className="icon" style={{ color: '#1D84FF' }} />
                  </div>
                </div>
              </Tooltip>
            )}
          </div>
          <div className="right-section" style={{ paddingRight: 16 }}>
            {showMenuActions() && (
              <>
                <div onClick={handleClick} className="more">
                  <MoreVert />
                </div>

                <StyledMenu
                  id="task-error-correction"
                  anchorEl={anchorEl}
                  keepMounted
                  disableEnforceFocus
                  open={Boolean(anchorEl)}
                  onClose={handleClose}
                  style={{ marginTop: 30 }}
                >
                  {isTaskCompleted && !correctionEnabled && (
                    <MenuItem
                      onClick={() => {
                        handleEnableErrorCorrection(handleClose);
                      }}
                    >
                      Enable Corrections
                    </MenuItem>
                  )}
                  {!isBlocked && hasBulkVerification && (
                    <>
                      {pendingSelfVerificationParameters.length > 0 && (
                        <MenuItem
                          onClick={() => {
                            handleClose();
                            dispatch(
                              openOverlayAction({
                                type: OverlayNames.BULK_SELF_VERIFICATION_MODAL,
                              }),
                            );
                          }}
                        >
                          Bulk Self Verification
                        </MenuItem>
                      )}
                      {executedParametersWithPeerVerification.length > 0 && (
                        <MenuItem
                          onClick={() => {
                            handleClose();
                            setRequestPeerJobDrawerVisible(true);
                          }}
                        >
                          Request Peer Verification
                        </MenuItem>
                      )}
                      {pendingPeerVerificationParameters.length > 0 && (
                        <MenuItem
                          onClick={() => {
                            handleClose();
                            dispatch(
                              openOverlayAction({
                                type: OverlayNames.BULK_PEER_VERIFICATION_MODAL,
                              }),
                            );
                          }}
                        >
                          Bulk Peer Verification
                        </MenuItem>
                      )}
                    </>
                  )}
                  {isFeatureAllowed('sameSessionVerification') &&
                    !isTaskCompleted &&
                    hasBulkVerification &&
                    parametersInitiatedByUser.length > 1 && (
                      <MenuItem
                        onClick={() => {
                          handleClose();
                          dispatch(
                            openOverlayAction({
                              type: OverlayNames.BULK_SAME_SESSION_VERIFICATION_MODAL,
                              props: {
                                parameters: parametersInitiatedByUser,
                              },
                            }),
                          );
                        }}
                      >
                        Bulk Same Session Verification
                      </MenuItem>
                    )}
                  {isTaskCompleted && !enableRecurrence && isFeatureAllowed('repeatTask') && (
                    <MenuItem
                      onClick={() => {
                        handleClose();
                        dispatch(
                          openOverlayAction({
                            type: OverlayNames.CONFIRMATION_MODAL,
                            props: {
                              onPrimary: () => dispatch(jobActions.repeatTask({ id: task.id })),
                              primaryText: 'Yes',
                              secondaryText: 'No',
                              title: 'Repeat Task',
                              body: (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                                  <div>Are you sure you want to repeat the task ?</div>
                                  <div>
                                    Please note, the task will be executed with the same resource
                                    parameters as used in the master task.
                                  </div>
                                </div>
                              ),
                            },
                          }),
                        );
                      }}
                    >
                      Repeat the Task
                    </MenuItem>
                  )}
                  {type === TaskExecutionType.REPEAT && !isTaskStarted && (
                    <MenuItem
                      onClick={() => {
                        handleClose();
                        dispatch(jobActions.removeRepeatTask({ taskExecutionId }));
                      }}
                    >
                      Remove Repeated Task
                    </MenuItem>
                  )}
                  {enableRecurrence && continueRecurrence && (
                    <MenuItem
                      onClick={() => {
                        handleClose();
                        dispatch(
                          openOverlayAction({
                            type: OverlayNames.END_TASK_RECURRENCE_MODAL,
                            props: {
                              onPrimary: () => {
                                dispatch(jobActions.endTaskRecurrence({ taskExecutionId }));
                              },
                            },
                          }),
                        );
                      }}
                    >
                      End Recurrence
                    </MenuItem>
                  )}
                  {!isBlocked && (
                    <>
                      {canSkipTask && !isTaskStarted && (
                        <MenuItem
                          onClick={() => {
                            handleClose();
                            dispatch(
                              openOverlayAction({
                                type: OverlayNames.REASON_MODAL,
                                props: {
                                  modalTitle: 'Skip Task',
                                  modalDesc: 'Provide the details for skipping the task',
                                  onSubmitHandler: (reason: string, closeModal: () => void) => {
                                    if (enableRecurrence && continueRecurrence) {
                                      handleRecurringTaskCreation(TaskAction.SKIP, reason);
                                    } else {
                                      dispatch(
                                        jobActions.performTaskAction({
                                          id: taskExecutionId,
                                          reason,
                                          action: TaskAction.SKIP,
                                        }),
                                      );
                                    }
                                    closeModal();
                                  },
                                },
                              }),
                            );
                          }}
                        >
                          Skip the task
                        </MenuItem>
                      )}
                      {!isTaskCompleted && checkPermission(['inbox', 'completeWithException']) && (
                        <MenuItem
                          onClick={() => {
                            handleClose();
                            dispatch(
                              openOverlayAction({
                                type: OverlayNames.REASON_MODAL,
                                props: {
                                  modalTitle: 'Complete with Exception',
                                  modalDesc: 'Provide the details for Exception',
                                  onSubmitHandler: (reason: string, closeModal: () => void) => {
                                    if (enableRecurrence && continueRecurrence) {
                                      handleRecurringTaskCreation(
                                        TaskAction.COMPLETE_WITH_EXCEPTION,
                                        reason,
                                      );
                                    } else {
                                      dispatch(
                                        jobActions.performTaskAction({
                                          id: taskExecutionId,
                                          reason,
                                          action: TaskAction.COMPLETE_WITH_EXCEPTION,
                                        }),
                                      );
                                    }
                                    closeModal();
                                  },
                                },
                              }),
                            );
                          }}
                        >
                          Complete with Exception
                        </MenuItem>
                      )}
                    </>
                  )}
                </StyledMenu>
              </>
            )}
          </div>
        </div>

        {taskErrors && taskErrors.length > 0 && (
          <div className="task-error">
            {taskErrors.map((currError) => (
              <div className="task-error-wrapper">
                <ErrorIcon className="task-error-icon" />
                {currError}
              </div>
            ))}
          </div>
        )}

        {!isTaskCompleted &&
          recurringExpectedDueAt &&
          type === TaskExecutionType.RECURRING &&
          getEpochTimeDifference(recurringExpectedDueAt + positiveDueDateToleranceInterval) ===
            'LATE' && <div className="task-banner task-overdue-banner">Task is overdue</div>}
        {!isTaskCompleted &&
        enableScheduling &&
        schedulingExpectedDueAt &&
        getEpochTimeDifference(schedulingExpectedDueAt) === 'LATE' ? (
          <div className="task-banner task-overdue-banner">Task is overdue</div>
        ) : (
          !isTaskStarted &&
          enableScheduling &&
          schedulingExpectedStartedAt &&
          (getEpochTimeDifference(schedulingExpectedStartedAt) === 'ON TIME' ||
            getEpochTimeDifference(schedulingExpectedStartedAt) === 'LATE') && (
            <div className="task-banner scheduled-task-banner">Task is ready to start</div>
          )
        )}
        <div className="reason-tags">
          {isTaskPaused && (
            <ReasonTag
              startedBy={audit?.modifiedBy}
              startedAt={audit?.modifiedAt}
              reason={
                pauseReasons?.[pauseReasons.length - 1]?.comment ??
                TaskPauseReasons?.[
                  pauseReasons?.[pauseReasons.length - 1]
                    ?.taskPauseReason as keyof typeof TaskPauseReasons
                ] ??
                ''
              }
              modalTitle="Task Paused"
              badgeText="Task Paused"
            />
          )}

          {reason && (
            <ReasonTag
              startedBy={endedBy}
              startedAt={endedAt}
              reason={reason}
              modalTitle={reasonTitle()}
              badgeText={reasonTitle()}
            />
          )}

          {(recurringPrematureStartReason || schedulePrematureStartReason) && (
            <ReasonTag
              startedBy={startedBy}
              startedAt={startedAt}
              reason={recurringPrematureStartReason || schedulePrematureStartReason}
              reasonType="start"
              modalTitle={
                recurringPrematureStartReason
                  ? 'Early start for recurring task'
                  : 'Early start for scheduled task'
              }
              badgeText={
                recurringPrematureStartReason
                  ? 'Early start for recurring task'
                  : 'Early start for scheduled task'
              }
            />
          )}

          {(recurringOverdueCompletionReason || scheduleOverdueCompletionReason) && (
            <ReasonTag
              startedBy={endedBy}
              startedAt={endedAt}
              reason={recurringOverdueCompletionReason || scheduleOverdueCompletionReason}
              reasonType="end"
              modalTitle={
                recurringOverdueCompletionReason
                  ? 'Delayed completion for recurring task'
                  : 'Delayed completion for scheduled task'
              }
              badgeText={
                recurringOverdueCompletionReason
                  ? 'Delayed completion for recurring task'
                  : 'Delayed completion for scheduled task'
              }
            />
          )}

          {requestPeerJobDrawerVisible && (
            <AssignBulkPeerVerification
              onCloseDrawer={setRequestPeerJobDrawerVisible}
              isReadOnly={!isUserAssignedToTask}
            />
          )}
        </div>
      </div>
    </Wrapper>
  );
};

export default Header;
