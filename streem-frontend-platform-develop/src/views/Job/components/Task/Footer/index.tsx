import {
  AutomationActionActionType,
  AutomationActionTriggerType,
  TimerOperator,
} from '#PrototypeComposer/checklist.types';
import { Button } from '#components';
import { closeOverlayAction, openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store/helpers';
import { StoreTask, TaskAction, TaskExecutionStates, TaskExecutionType } from '#types';
import { getEpochTimeDifference } from '#utils/timeUtils';
import { jobActions } from '#views/Job/jobStore';
import { navigateToTaskExecution, startJob, useJobStateToFlags } from '#views/Job/utils';
import { CompletedJobStates, JobStateEnum } from '#views/Jobs/ListView/types';
import {
  ArrowBack,
  ArrowForward,
  CheckCircleOutlined,
  ReportProblemOutlined,
} from '@material-ui/icons';
import PauseIcon from '@material-ui/icons/Pause';
import PlayArrowIcon from '@material-ui/icons/PlayArrow';
import React, { FC } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const Wrapper = styled.div.attrs({
  className: 'task-buttons',
})`
  display: flex;
  height: 80px;
  margin-top: auto;
  box-shadow: 0 2px 4px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  grid-area: task-footer;

  .primary-action {
    flex: 1;
    padding: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-inline: 1px solid #e0e0e0;
    gap: 4px;

    button {
      min-width: 50%;
      max-width: 80%;
      > svg {
        height: 16px;
      }
    }

    span {
      font-weight: 700;
      font-size: 14px;
      line-height: 16px;
      letter-spacing: 0.16px;
      color: #161616;
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  button {
    margin: unset;
    padding-inline: 24px;
  }

  .error-correction-action {
    display: flex;
    justify-content: center;
    > button {
      flex: 1;
      width: 300px;
    }
  }
`;

type FooterProps = {
  task: StoreTask;
  isUserAssignedToTask: boolean;
};

const Footer: FC<FooterProps> = () => {
  const dispatch = useDispatch();
  const {
    id: jobId,
    expectedStartDate: jobExpectedStartDate,
    state: jobState,
    isInboxView,
    pendingTasks,
    code,
    timerState,
    updating,
    activeTask: task,
    taskExecutions,
  } = useTypedSelector((state) => state.job);

  const showCJFExceptionBanner = useTypedSelector((state) => state.job.showCJFExceptionBanner);
  const forceCwe = useTypedSelector((state) => state.job.forceCwe);

  const { isJobStarted, isTaskPaused, isTaskStarted, isTaskCompleted } = useJobStateToFlags();

  const {
    taskExecution: {
      state: taskExecutionState,
      correctionEnabled,
      id: taskExecutionId,
      continueRecurrence,
      recurringExpectedStartedAt,
      recurringExpectedDueAt,
      schedulingExpectedDueAt,
      schedulingExpectedStartedAt,
      type,
    },
    isTaskAssigned: isUserAssignedToTask,
  } = task;

  const { previous, next } = taskExecutions.get(taskExecutionId) || {};

  const { negativeStartDateToleranceInterval, positiveDueDateToleranceInterval } =
    task.taskRecurrence || {};

  const handleOnNextTask = () => {
    if (next) navigateToTaskExecution(jobId, next);
  };

  const handleOnPreviousTask = () => {
    if (previous) navigateToTaskExecution(jobId, previous);
  };

  const handleRecurringTaskCompletion = (params?: {
    reason?: string;
    scheduleOverdueCompletionReason?: string;
  }) => {
    const { reason = '', scheduleOverdueCompletionReason = '' } = params || {};
    if (
      type === TaskExecutionType.RECURRING &&
      recurringExpectedDueAt &&
      getEpochTimeDifference(recurringExpectedDueAt + positiveDueDateToleranceInterval) === 'LATE'
    ) {
      dispatch(
        openOverlayAction({
          type: OverlayNames.REASON_MODAL,
          props: {
            modalTitle: 'Recurring Task Overdue',
            modalDesc: 'This recurring task is overdue. Kindly provide reason for delay.',
            onSubmitHandler: (recurringOverdueCompletionReason: string, closeModal: () => void) => {
              if (continueRecurrence) {
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.TASK_RECURRENCE_EXECUTION_MODAL,
                    props: {
                      onPrimary: () => {
                        preCompleteTask({
                          reason,
                          continueRecurrence: true,
                          recurringOverdueCompletionReason,
                          scheduleOverdueCompletionReason,
                        });
                      },
                      onSecondary: () => {
                        dispatch(
                          openOverlayAction({
                            type: OverlayNames.END_TASK_RECURRENCE_MODAL,
                            props: {
                              onPrimary: () => {
                                preCompleteTask({
                                  reason,
                                  continueRecurrence: false,
                                  recurringOverdueCompletionReason,
                                  scheduleOverdueCompletionReason,
                                });
                              },
                            },
                          }),
                        );
                      },
                    },
                  }),
                );
              } else {
                preCompleteTask({
                  reason,
                  recurringOverdueCompletionReason,
                  scheduleOverdueCompletionReason,
                });
              }
              closeModal();
            },
          },
        }),
      );
    } else {
      if (continueRecurrence) {
        dispatch(
          openOverlayAction({
            type: OverlayNames.TASK_RECURRENCE_EXECUTION_MODAL,
            props: {
              onPrimary: () => {
                preCompleteTask({
                  reason,
                  continueRecurrence: true,
                  scheduleOverdueCompletionReason,
                });
              },
              onSecondary: () => {
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.END_TASK_RECURRENCE_MODAL,
                    props: {
                      onPrimary: () => {
                        preCompleteTask({
                          reason,
                          continueRecurrence: false,
                          scheduleOverdueCompletionReason,
                        });
                      },
                    },
                  }),
                );
              },
            },
          }),
        );
      } else {
        preCompleteTask({
          reason,
          scheduleOverdueCompletionReason,
        });
      }
    }
  };

  const openAutomationModal = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.AUTOMATION_TASK_MODAL,
        props: {
          taskId: task.id,
          taskExecutionId: task?.taskExecution?.id,
          initialTab: '1',
        },
      }),
    );
  };

  const preCompleteTask = (params?: {
    reason?: string;
    continueRecurrence?: boolean;
    recurringOverdueCompletionReason?: string;
    scheduleOverdueCompletionReason?: string;
  }) => {
    const {
      reason = '',
      continueRecurrence = false,
      recurringOverdueCompletionReason = '',
      scheduleOverdueCompletionReason = '',
    } = params || {};
    const handleCompleteTask = (createObjectAutomation: any[] = []) => {
      dispatch(
        jobActions.performTaskAction({
          id: taskExecutionId,
          action: TaskAction.COMPLETE,
          reason,
          ...(createObjectAutomation.length > 0 && {
            createObjectAutomations: createObjectAutomation,
          }),
          continueRecurrence,
          recurringOverdueCompletionReason,
          scheduleOverdueCompletionReason,
          openAutomationModal,
        }),
      );
    };

    if (task.automations?.length) {
      const createObjectAutomation = (task.automations || []).find(
        (automation) =>
          (automation.actionType === AutomationActionActionType.CREATE_OBJECT ||
            automation.actionType === AutomationActionActionType.BULK_CREATE_OBJECT) &&
          automation.triggerType === AutomationActionTriggerType.TASK_COMPLETED,
      );
      if (createObjectAutomation) {
        dispatch(
          openOverlayAction({
            type: OverlayNames.AUTOMATION_ACTION,
            props: {
              objectTypeId: createObjectAutomation.actionDetails.objectTypeId,
              createObjectAutomationDetail: createObjectAutomation,
              onDone: (createObjectData: any) => {
                const createObjectAutomations = [
                  {
                    automationId: createObjectAutomation.id,
                    entityObjectValueRequest: createObjectData,
                  },
                ];
                handleCompleteTask(createObjectAutomations);
                dispatch(closeOverlayAction(OverlayNames.AUTOMATION_ACTION));
              },
              setLoadingState: () => {},
            },
          }),
        );
      } else {
        handleCompleteTask();
      }
    } else {
      handleCompleteTask();
    }
  };

  const onCompleteTask = async () => {
    if (task.timed && (timerState.earlyCompletion || timerState.limitCrossed)) {
      let modalTitle, modalDesc;
      if (timerState.limitCrossed) {
        modalTitle = 'Delayed completion';
        modalDesc = 'State your reason for delay';
      } else if (task.timerOperator === TimerOperator.NOT_LESS_THAN && timerState.earlyCompletion) {
        modalTitle = 'Early completion';
        modalDesc = 'State your reason for early completion';
      }

      if (task.enableScheduling && getEpochTimeDifference(schedulingExpectedDueAt) === 'LATE') {
        dispatch(
          openOverlayAction({
            type: OverlayNames.REASON_MODAL,
            props: {
              modalTitle,
              modalDesc,
              onSubmitHandler: (reason: string, closeModal: () => void) => {
                closeModal();
                setTimeout(() => {
                  dispatch(
                    openOverlayAction({
                      type: OverlayNames.REASON_MODAL,
                      props: {
                        modalTitle: 'Scheduled Task Overdue',
                        modalDesc:
                          'This task is overdue. Kindly provide the reason for the delay in its execution.',
                        onSubmitHandler: (
                          scheduleOverdueCompletionReason: string,
                          closeModal: () => void,
                        ) => {
                          if (task.enableRecurrence) {
                            closeModal();
                            setTimeout(() => {
                              handleRecurringTaskCompletion({
                                reason,
                                scheduleOverdueCompletionReason,
                              });
                            }, 0);
                          } else {
                            preCompleteTask({ reason, scheduleOverdueCompletionReason });
                            closeModal();
                          }
                        },
                      },
                    }),
                  );
                }, 0);
              },
            },
          }),
        );
      } else if (task.enableRecurrence) {
        dispatch(
          openOverlayAction({
            type: OverlayNames.REASON_MODAL,
            props: {
              modalTitle,
              modalDesc,
              onSubmitHandler: (reason: string, closeModal: () => void) => {
                closeModal();
                setTimeout(() => {
                  handleRecurringTaskCompletion({ reason });
                }, 0);
              },
            },
          }),
        );
      } else {
        dispatch(
          openOverlayAction({
            type: OverlayNames.REASON_MODAL,
            props: {
              modalTitle,
              modalDesc,
              onSubmitHandler: (reason: string, closeModal: () => void) => {
                preCompleteTask({ reason });
                closeModal();
              },
            },
          }),
        );
      }
    } else if (
      task.enableScheduling &&
      getEpochTimeDifference(schedulingExpectedDueAt) === 'LATE'
    ) {
      if (task.enableRecurrence) {
        dispatch(
          openOverlayAction({
            type: OverlayNames.REASON_MODAL,
            props: {
              modalTitle: 'Scheduled Task Overdue',
              modalDesc:
                'This task is overdue. Kindly provide the reason for the delay in its execution.',
              onSubmitHandler: (
                scheduleOverdueCompletionReason: string,
                closeModal: () => void,
              ) => {
                closeModal();
                setTimeout(() => {
                  handleRecurringTaskCompletion({ scheduleOverdueCompletionReason });
                }, 0);
              },
            },
          }),
        );
      } else {
        dispatch(
          openOverlayAction({
            type: OverlayNames.REASON_MODAL,
            props: {
              modalTitle: 'Scheduled Task Overdue',
              modalDesc:
                'This task is overdue. Kindly provide the reason for the delay in its execution.',
              onSubmitHandler: (
                scheduleOverdueCompletionReason: string,
                closeModal: () => void,
              ) => {
                preCompleteTask({ scheduleOverdueCompletionReason });
                closeModal();
              },
            },
          }),
        );
      }
    } else if (task.enableRecurrence) {
      handleRecurringTaskCompletion();
    } else {
      preCompleteTask();
    }
  };

  const onStartTask = (params?: {
    recurringPrematureStartReason?: string;
    schedulePrematureStartReason?: string;
  }) => {
    const { recurringPrematureStartReason, schedulePrematureStartReason = '' } = params || {};
    const handleStartTask = (createObjectAutomation: any[] = []) => {
      dispatch(
        jobActions.performTaskAction({
          id: taskExecutionId,
          action: TaskAction.START,
          openAutomationModal,
          ...(createObjectAutomation.length > 0 && {
            createObjectAutomations: createObjectAutomation,
          }),
          recurringPrematureStartReason,
          schedulePrematureStartReason,
        }),
      );
    };
    if (task.automations?.length) {
      const createObjectAutomation = (task.automations || []).find(
        (automation) =>
          (automation.actionType === AutomationActionActionType.CREATE_OBJECT ||
            automation.actionType === AutomationActionActionType.BULK_CREATE_OBJECT) &&
          automation.triggerType === AutomationActionTriggerType.TASK_STARTED,
      );
      if (createObjectAutomation) {
        dispatch(
          openOverlayAction({
            type: OverlayNames.AUTOMATION_ACTION,
            props: {
              objectTypeId: createObjectAutomation.actionDetails.objectTypeId,
              createObjectAutomationDetail: createObjectAutomation,
              onDone: (createObjectData: any) => {
                const createObjectAutomations = [
                  {
                    automationId: createObjectAutomation.id,
                    entityObjectValueRequest: createObjectData,
                  },
                ];
                handleStartTask(createObjectAutomations);
                dispatch(closeOverlayAction(OverlayNames.AUTOMATION_ACTION));
              },
              setLoadingState: () => {},
            },
          }),
        );
      } else {
        handleStartTask();
      }
    } else {
      handleStartTask();
    }
  };

  const handleForceCwe = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.COMPLETE_JOB_WITH_EXCEPTION,
        props: { jobId, code },
      }),
    );
  };

  let primaryActionProps: React.ComponentProps<typeof Button> = {};
  let primaryActionLabel = '';

  if (isInboxView) {
    if (!isTaskCompleted) {
      if (forceCwe && !(jobState in CompletedJobStates)) {
        primaryActionLabel = 'Complete with Exception';
        primaryActionProps = {
          onClick: () => handleForceCwe(),
        };
      } else if (jobState === JobStateEnum.ASSIGNED && isUserAssignedToTask && !task.parentTaskId) {
        primaryActionLabel = 'Start Job';
        primaryActionProps = {
          onClick: () => startJob(jobId!, jobExpectedStartDate!),
        };
      } else if (isJobStarted && isUserAssignedToTask && !isTaskStarted) {
        primaryActionLabel = 'Start task';
        primaryActionProps = {
          onClick: () => {
            if (
              task.enableScheduling &&
              getEpochTimeDifference(schedulingExpectedStartedAt) === 'EARLY'
            ) {
              dispatch(
                openOverlayAction({
                  type: OverlayNames.REASON_MODAL,
                  props: {
                    modalTitle: 'Start the Task',
                    modalDesc:
                      'Are you sure you want to start the task before it’s start time ? Please provide a reason for it.',
                    onSubmitHandler: (reason: string, closeModal: () => void) => {
                      onStartTask({ schedulePrematureStartReason: reason });
                      closeModal();
                    },
                  },
                }),
              );
            } else if (
              task?.enableRecurrence &&
              type === TaskExecutionType.RECURRING &&
              getEpochTimeDifference(
                recurringExpectedStartedAt - negativeStartDateToleranceInterval,
              ) === 'EARLY'
            ) {
              dispatch(
                openOverlayAction({
                  type: OverlayNames.REASON_MODAL,
                  props: {
                    modalTitle: 'Start the Task',
                    modalDesc:
                      'Are you sure you want to start the task before it’s start time ? Please provide a reason for it.',
                    onSubmitHandler: (reason: string, closeModal: () => void) => {
                      onStartTask({ recurringPrematureStartReason: reason });
                      closeModal();
                    },
                  },
                }),
              );
            } else {
              onStartTask();
            }
          },
        };
      } else if (isUserAssignedToTask && !(jobState in CompletedJobStates)) {
        primaryActionLabel = 'Complete Task';
        primaryActionProps = {
          onClick: onCompleteTask,
        };
      }
    } else if (!(jobState in CompletedJobStates) && !pendingTasks.size) {
      primaryActionLabel = 'Complete Job';
      primaryActionProps = {
        onClick: () => {
          if (jobId && code) dispatch(jobActions.completeJob({ jobId, details: { code } }));
        },
      };
    }
  }

  const togglePauseResume = async (reason = '', comment = '') => {
    dispatch(
      jobActions.togglePauseResume({
        id: taskExecutionId,
        reason,
        comment,
        isTaskPaused,
      }),
    );
  };

  const PauseResumeButton = () => {
    const iconShow = (state: TaskExecutionStates) => {
      switch (state) {
        case 'PAUSED':
          return <PlayArrowIcon />;
        case 'IN_PROGRESS':
          return <PauseIcon />;
      }
    };

    return (
      <Button
        variant="primary"
        style={{ minWidth: 'unset', width: '48px' }}
        onClick={() => {
          if (isTaskPaused) {
            togglePauseResume();
          } else {
            dispatch(
              openOverlayAction({
                type: OverlayNames.TASK_PAUSE_REASON_MODAL,
                props: {
                  onSubmitHandler: togglePauseResume,
                },
              }),
            );
          }
        }}
      >
        {iconShow(taskExecutionState)}
      </Button>
    );
  };

  return (
    <>
      {!(jobState in CompletedJobStates) && showCJFExceptionBanner && (
        <div
          style={{
            display: 'flex',
            justifyContent: 'center',
            background: '#FF6B6B29',
            color: '#DA1E28',
            padding: '8px',
            fontSize: '12px',
          }}
        >
          {forceCwe
            ? `Since the CJF parameter is not executed, this job cannot be executed. Please complete this job with exception`
            : `This Job contains CJF Parameters with Pending Exception Approval`}
        </div>
      )}

      <Wrapper>
        <Button variant="textOnly" onClick={handleOnPreviousTask} disabled={!previous}>
          <ArrowBack />
        </Button>

        <div className="primary-action">
          {!!correctionEnabled && isInboxView ? (
            <div className="error-correction-action">
              <Button
                variant="secondary"
                color="red"
                onClick={() => {
                  dispatch(jobActions.cancelErrorCorrectionOnTask({ taskExecutionId }));
                }}
              >
                Disable Corrections
              </Button>
            </div>
          ) : primaryActionLabel ? (
            <>
              {jobState === 'IN_PROGRESS' &&
                ['IN_PROGRESS', 'PAUSED'].includes(taskExecutionState) &&
                PauseResumeButton()}
              <Button
                variant="primary"
                {...primaryActionProps}
                disabled={isTaskPaused || updating}
                loading={updating}
              >
                {primaryActionLabel}
              </Button>
            </>
          ) : null}
          {!(jobState in CompletedJobStates) && (
            <>
              {!primaryActionLabel && taskExecutionState === 'COMPLETED' && !correctionEnabled && (
                <span>
                  <CheckCircleOutlined style={{ color: '#24A148' }} /> Task Completed
                </span>
              )}
              {!primaryActionLabel &&
                taskExecutionState === 'COMPLETED_WITH_EXCEPTION' &&
                !correctionEnabled && (
                  <span>
                    <ReportProblemOutlined style={{ color: '#F1C21B' }} /> Task Completed with
                    Exception
                  </span>
                )}
            </>
          )}
        </div>
        <Button variant="textOnly" onClick={handleOnNextTask} disabled={!next}>
          <ArrowForward />
        </Button>
      </Wrapper>
    </>
  );
};

export default Footer;
