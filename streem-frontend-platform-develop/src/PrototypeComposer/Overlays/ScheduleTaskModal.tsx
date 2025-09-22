import { BaseModal, FormGroup } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { removeTaskSchedule, setTaskSchedule } from '#PrototypeComposer/Tasks/actions';
import { useTypedSelector } from '#store/helpers';
import { ScheduledTaskCondition, ScheduledTaskType, Task } from '#types';
import { apiTaskSchedule } from '#utils/apiUrls';
import { InputTypes } from '#utils/globalTypes';
import { request } from '#utils/request';
import { getSummary } from '#utils/summaryUtils';
import { calculateSecondsFromDuration } from '#utils/timeUtils';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import React, { FC, useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

export interface ScheduleTaskModalProps {
  task: Task;
  isReadOnly: boolean;
}

interface TaskListOption {
  label: string;
  value: string;
}

const Wrapper = styled.div.attrs({})`
  .modal {
    min-width: 700px !important;
    max-width: 720px !important;

    .modal-body {
      padding: 24px !important;
      overflow: auto;

      .custom-label {
        align-items: center;
        color: #525252;
        display: flex;
        font-size: 12px;
        justify-content: flex-start;
        letter-spacing: 0.32px;
        line-height: 1.33;
        margin: 0px;
        margin-bottom: 8px;
      }

      .form-group {
        padding: 0;
        margin-bottom: 16px;

        :last-of-type {
          margin-bottom: 0;
        }
      }

      .task-schedule {
        display: flex;
        flex-direction: column;
        gap: 16px;
        margin-top: 16px;
      }

      .due-after-section,
      .start-after-section {
        display: flex;
        margin-bottom: 16px;
        .form-group {
          flex-direction: row;
          gap: 0.8%;
          width: 100%;
          > div {
            margin-bottom: 0;
            width: 16%;
            input {
              width: calc(100% - 32px);
            }
          }
        }
      }

      .scheduler-summary {
        border-top: 1.5px solid #e0e0e0;
        h4 {
          font-size: 14px;
          font-weight: bold;
          line-height: 1.14;
          letter-spacing: 0.16px;
          color: #161616;
          margin-block: 16px;
        }
        .read-only-group {
          padding: 0;
          font-size: 14px;
          color: #525252;
          .read-only {
            margin-bottom: 16px;
          }
        }
      }
    }

    .modal-footer {
      flex-direction: row;
      justify-content: space-between;
      align-items: center;

      .remove-schedule {
        color: red;
        font-size: 14px;
        line-height: 16px;
        cursor: pointer;
      }
    }
  }
`;

const ScheduleTaskModal: FC<CommonOverlayProps<ScheduleTaskModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { task = {}, isReadOnly },
}) => {
  const [schedule, setSchedule] = useState<any>(null);
  const [tasksList, setTasksList] = useState<TaskListOption[]>([]);
  const {
    tasks: { tasksOrderInStage },
  } = useTypedSelector((state) => state.prototypeComposer);
  const dispatch = useDispatch();

  const { enableScheduling, id } = task;

  const getTasksList = () => {
    const tasks = [];
    let stageOrder = 1;

    for (const stageId in tasksOrderInStage) {
      const stageTasks = tasksOrderInStage[stageId];

      for (let i = 0; i < stageTasks.length; i++) {
        const taskId = stageTasks[i];
        const label = `Task ${stageOrder}.${i + 1}`;

        tasks.push({
          label,
          value: taskId,
        });
      }

      stageOrder++;
    }
    setTasksList(tasks);
  };

  const form = useForm<{
    type: ScheduledTaskType;
    referencedTaskId: string | null;
    condition: ScheduledTaskCondition | null;
    startDateDuration: Record<string, number>;
    startDateInterval: number;
    dueDateDuration: Record<string, number>;
    dueDateInterval: number;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      referencedTaskId: null,
      startDateDuration: { day: 0, hour: 0, minute: 0 },
      startDateInterval: 0,
      dueDateDuration: { day: 0, hour: 0, minute: 0 },
      dueDateInterval: 0,
    },
  });

  const {
    handleSubmit,
    register,
    setValue,
    errors,
    watch,
    reset,
    control,
    formState: { isDirty, isValid },
  } = form;

  const validateInterval = (value: number) => {
    if (value > 0) {
      return true;
    }
    return false;
  };

  register('startDateInterval', {
    required: true,
    validate: validateInterval,
  });

  register('dueDateInterval', {
    required: true,
    validate: validateInterval,
  });

  const {
    type,
    referencedTaskId,
    condition,
    startDateDuration,
    dueDateDuration,
    startDateInterval,
    dueDateInterval,
  } = watch([
    'type',
    'referencedTaskId',
    'condition',
    'startDateDuration',
    'dueDateDuration',
    'startDateInterval',
    'dueDateInterval',
  ]);

  const getTaskSchedule = async (taskId: string) => {
    try {
      const { data } = await request('GET', apiTaskSchedule(taskId));
      if (data) {
        setSchedule(data);
      }
    } catch (error) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: typeof error !== 'string' ? 'Oops! Please Try Again.' : error,
        }),
      );
    }
  };

  const onScheduleTask = (data: any) => {
    dispatch(
      setTaskSchedule({
        taskId: id,
        type,
        referencedTaskId,
        condition,
        scheduledTaskId: id,
        startDateDuration: data.startDateDuration,
        startDateInterval: data.startDateInterval,
        dueDateDuration: data.dueDateDuration,
        dueDateInterval: data.dueDateInterval,
      }),
    );
  };

  const onRemoveTaskSchedule = () => {
    dispatch(removeTaskSchedule(id));
  };

  const updateDueDateInterval = (duration: Record<string, number>, intervalToBeSet: string) => {
    const durationSeconds = calculateSecondsFromDuration(duration);
    setValue(intervalToBeSet, durationSeconds, {
      shouldValidate: true,
    });
  };

  const scheduledTaskDetails = () => {
    const referenceTask = tasksList.find((t) => t.value === referencedTaskId);
    const conditionLabel =
      condition === ScheduledTaskCondition.START ? 'is Started' : 'is Completed';

    return `after ${
      type === ScheduledTaskType.JOB ? 'Job' : referenceTask?.label
    } ${conditionLabel}`;
  };

  const { day: startDay, hour: startHour, minute: startMinute } = startDateDuration || {};
  const { day: dueDay, hour: dueHour, minute: dueMinute } = dueDateDuration || {};

  useEffect(() => {
    if (enableScheduling && schedule) {
      reset({
        type: schedule?.type,
        referencedTaskId: schedule?.referencedTaskId,
        condition: schedule?.condition,
        startDateDuration: schedule?.startDateDuration,
        startDateInterval: schedule?.startDateInterval,
        dueDateDuration: schedule?.dueDateDuration,
        dueDateInterval: schedule?.dueDateInterval,
      });
    }
  }, [schedule]);

  useEffect(() => {
    if (dueDateDuration) {
      updateDueDateInterval(dueDateDuration, 'dueDateInterval');
    }
  }, [dueDay, dueHour, dueMinute]);

  useEffect(() => {
    if (startDateDuration) {
      updateDueDateInterval(startDateDuration, 'startDateInterval');
    }
  }, [startDay, startHour, startMinute]);

  useEffect(() => {
    if (id) {
      getTaskSchedule(id);
    }
    getTasksList();
  }, []);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title="Schedule a Task"
        primaryText="Save"
        closeModal={closeOverlay}
        showSecondary={false}
        modalFooterOptions={
          <div
            onClick={() => {
              onRemoveTaskSchedule();
            }}
            className="remove-schedule"
          >
            {enableScheduling ? 'Remove Schedule' : ''}
          </div>
        }
        disabledPrimary={!isDirty || !isValid}
        onPrimary={handleSubmit((data) => onScheduleTask(data))}
        showFooter={!isReadOnly}
      >
        <div>
          {!enableScheduling && isReadOnly ? (
            <div>Task is not scheduled.</div>
          ) : (
            <div className="schedule">
              <Controller
                control={control}
                name="type"
                key="type"
                defaultValue={type || null}
                shouldUnregister={false}
                rules={{
                  required: true,
                }}
                render={({ value }) => (
                  <FormGroup
                    inputs={[
                      {
                        type: InputTypes.SINGLE_SELECT,
                        props: {
                          id: 'scheduleTaskType',
                          label: 'When',
                          options: [
                            {
                              label: 'Job',
                              value: ScheduledTaskType.JOB,
                            },
                            {
                              label: 'Task',
                              value: ScheduledTaskType.TASK,
                            },
                          ],
                          isDisabled: isReadOnly,
                          value: value
                            ? [
                                {
                                  label: value === ScheduledTaskType.JOB ? 'Job' : 'Task',
                                  value: value,
                                },
                              ]
                            : null,
                          placeholder: 'Select',
                          onChange: (option: { value: string }) => {
                            reset({
                              type: option.value,
                              condition: null,
                            });
                          },
                        },
                      },
                    ]}
                  />
                )}
              />
              {type && type === ScheduledTaskType.TASK && (
                <Controller
                  control={control}
                  name="referencedTaskId"
                  key="referencedTaskId"
                  defaultValue={referencedTaskId}
                  shouldUnregister={false}
                  rules={{
                    required: true,
                  }}
                  render={({ onChange, value }) => (
                    <FormGroup
                      inputs={[
                        {
                          type: InputTypes.SINGLE_SELECT,
                          props: {
                            id: 'referencedTaskId',
                            options: tasksList.filter((t) => t.value !== task.id),
                            isDisabled: isReadOnly,
                            value: value ? tasksList.find((t) => t.value === value) : null,
                            placeholder: 'Select',
                            onChange: (option: { value: string }) => {
                              onChange(option.value);
                            },
                          },
                        },
                      ]}
                    />
                  )}
                />
              )}
              <Controller
                control={control}
                name="condition"
                key="condition"
                defaultValue={condition || null}
                shouldUnregister={false}
                rules={{
                  required: true,
                }}
                render={({ onChange, value }) => (
                  <FormGroup
                    inputs={[
                      {
                        type: InputTypes.SINGLE_SELECT,
                        props: {
                          id: 'condition',
                          label: 'Condition',
                          options: [
                            {
                              label: 'Is Started',
                              value: ScheduledTaskCondition.START,
                            },
                            type === ScheduledTaskType.TASK
                              ? {
                                  label: 'Completed',
                                  value: ScheduledTaskCondition.COMPLETE,
                                }
                              : null,
                          ].filter(Boolean),
                          isDisabled: isReadOnly,
                          value: value
                            ? [
                                {
                                  label:
                                    value === ScheduledTaskCondition.START
                                      ? 'Is Started'
                                      : 'Completed',
                                  value: value,
                                },
                              ]
                            : null,
                          placeholder: 'Select',
                          onChange: (option: { value: string }) => {
                            onChange(option.value);
                          },
                        },
                      },
                    ]}
                  />
                )}
              />
              {type && condition && (
                <div className="task-schedule">
                  <div>
                    <p className="custom-label">Schedule Start Time </p>
                    <div className="start-after-section">
                      <FormGroup
                        key="start-after-section"
                        inputs={[
                          {
                            type: InputTypes.NUMBER,
                            props: {
                              placeholder: 'Day',
                              label: 'Day',
                              id: 'startDateDuration.day',
                              name: 'startDateDuration.day',
                              error: !!errors?.startDateDuration?.day,
                              defaultValue: 0,
                              ref: register({
                                required: true,
                                valueAsNumber: true,
                                min: 0,
                                pattern: /^\d+$/, // Regex pattern to allow only positive integers, no decimals allowed
                              }),
                              disabled: isReadOnly,
                            },
                          },
                          {
                            type: InputTypes.NUMBER,
                            props: {
                              placeholder: 'Hour',
                              label: 'Hour',
                              id: 'startDateDuration.hour',
                              name: 'startDateDuration.hour',
                              error: !!errors?.startDateDuration?.hour,
                              defaultValue: 0,
                              ref: register({
                                required: true,
                                valueAsNumber: true,
                                min: 0,
                                pattern: /^\d+$/,
                              }),
                              disabled: isReadOnly,
                            },
                          },
                          {
                            type: InputTypes.NUMBER,
                            props: {
                              placeholder: 'Minute',
                              label: 'Minute',
                              id: 'startDateDuration.minute',
                              name: 'startDateDuration.minute',
                              error: !!errors?.startDateDuration?.minute,
                              defaultValue: 0,
                              ref: register({
                                required: true,
                                valueAsNumber: true,
                                min: 0,
                                pattern: /^\d+$/,
                              }),
                              disabled: isReadOnly,
                            },
                          },
                        ]}
                      />
                    </div>
                    <p className="custom-label" style={{ color: '#6F6F6F' }}>
                      This is the minimum time interval after which task should be started
                    </p>
                  </div>

                  <div>
                    <p className="custom-label">Task Due After </p>
                    <div className="due-after-section">
                      <FormGroup
                        key="due-after-section"
                        inputs={[
                          {
                            type: InputTypes.NUMBER,
                            props: {
                              placeholder: 'Day',
                              label: 'Day',
                              id: 'dueDateDuration.day',
                              name: 'dueDateDuration.day',
                              error: !!errors?.dueDateDuration?.day,
                              defaultValue: 0,
                              ref: register({
                                required: true,
                                valueAsNumber: true,
                                min: 0,
                                pattern: /^\d+$/,
                              }),
                              disabled: isReadOnly,
                            },
                          },
                          {
                            type: InputTypes.NUMBER,
                            props: {
                              placeholder: 'Hour',
                              label: 'Hour',
                              id: 'dueDateDuration.hour',
                              name: 'dueDateDuration.hour',
                              error: !!errors?.dueDateDuration?.hour,
                              defaultValue: 0,
                              ref: register({
                                required: true,
                                valueAsNumber: true,
                                min: 0,
                                pattern: /^\d+$/,
                              }),
                              disabled: isReadOnly,
                            },
                          },
                          {
                            type: InputTypes.NUMBER,
                            props: {
                              placeholder: 'Minute',
                              label: 'Minute',
                              id: 'dueDateDuration.minute',
                              name: 'dueDateDuration.minute',
                              error: !!errors?.dueDateDuration?.minute,
                              defaultValue: 0,
                              ref: register({
                                required: true,
                                valueAsNumber: true,
                                min: 0,
                                pattern: /^\d+$/,
                              }),
                              disabled: isReadOnly,
                            },
                          },
                        ]}
                      />
                    </div>
                    <p className="custom-label" style={{ color: '#6F6F6F' }}>
                      This is the time within which the task should be completed, after which it
                      will be marked as overdue.
                    </p>
                  </div>

                  {startDateInterval || dueDateInterval ? (
                    <div className="scheduler-summary">
                      <h4>Summary</h4>
                      <ReadOnlyGroup
                        className="read-only-group"
                        items={[
                          {
                            label: 'Schedule Start time',
                            value: `${getSummary(startDateDuration)} ${scheduledTaskDetails()}`,
                          },
                          {
                            label: 'Task Due After',
                            value: getSummary(dueDateDuration),
                          },
                        ]}
                      />
                    </div>
                  ) : null}
                </div>
              )}
            </div>
          )}
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default ScheduleTaskModal;
