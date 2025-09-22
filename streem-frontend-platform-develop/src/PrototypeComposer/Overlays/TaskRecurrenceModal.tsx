import { BaseModal, FormGroup } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { removeTaskRecurrence, setTaskRecurrence } from '#PrototypeComposer/Tasks/actions';
import { Task } from '#types';
import { apiGetTaskRecurrence } from '#utils/apiUrls';
import { InputTypes } from '#utils/globalTypes';
import { request } from '#utils/request';
import { getSummary } from '#utils/summaryUtils';
import { calculateSecondsFromDuration } from '#utils/timeUtils';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import React, { FC, useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

export interface TaskRecurrenceModalProps {
  task: Task;
  isReadOnly: boolean;
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
        color: #000;
        display: flex;
        font-size: 14px;
        justify-content: flex-start;
        letter-spacing: 0.16px;
        line-height: 16px;
        margin: 0px;
        margin-bottom: 8px;
      }

      .custom-label-bold {
        font-weight: 600;
        font-size: 16px;
      }

      .optional-badge {
        color: #999999;
        font-size: 12px;
        margin-left: 4px;
      }

      .form-group {
        padding: 0;
        margin-bottom: 16px;

        :last-of-type {
          margin-bottom: 0;
        }
      }

      .task-recurrence {
        display: flex;
        flex-direction: column;
        gap: 16px;
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

      .remove-recurrence {
        color: red;
        font-size: 14px;
        line-height: 16px;
        cursor: pointer;
      }
    }
  }
`;

const TaskRecurrenceModal: FC<CommonOverlayProps<TaskRecurrenceModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { task, isReadOnly },
}) => {
  const [recurrence, setRecurrence] = useState<any>(null);
  const [editMode, setEditMode] = useState(false);
  const dispatch = useDispatch();

  const { enableRecurrence } = task || {};

  const form = useForm<{
    startDateDuration: Record<string, number>;
    positiveStartDateToleranceDuration: Record<string, number>;
    negativeStartDateToleranceDuration: Record<string, number>;
    startDateInterval: number;
    dueDateDuration: Record<string, number>;
    positiveDueDateToleranceDuration: Record<string, number>;
    negativeDueDateToleranceDuration: Record<string, number>;
    dueDateInterval: number;
    positiveStartDateToleranceInterval: number;
    negativeStartDateToleranceInterval: number;
    positiveDueDateToleranceInterval: number;
    negativeDueDateToleranceInterval: number;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      startDateDuration: { day: 0, hour: 0, minute: 0 },
      positiveStartDateToleranceDuration: { day: 0, hour: 0, minute: 0 },
      negativeStartDateToleranceDuration: { day: 0, hour: 0, minute: 0 },
      startDateInterval: 0,
      dueDateDuration: { day: 0, hour: 0, minute: 0 },
      positiveDueDateToleranceDuration: { day: 0, hour: 0, minute: 0 },
      negativeDueDateToleranceDuration: { day: 0, hour: 0, minute: 0 },
      dueDateInterval: 0,
      positiveStartDateToleranceInterval: 0,
      negativeStartDateToleranceInterval: 0,
      positiveDueDateToleranceInterval: 0,
      negativeDueDateToleranceInterval: 0,
    },
  });

  const {
    handleSubmit,
    register,
    setValue,
    errors,
    watch,
    reset,
    formState: { isDirty },
  } = form;

  register('startDateInterval', {
    required: false,
  });

  register('dueDateInterval', {
    required: false,
  });

  register('positiveStartDateToleranceInterval', {
    required: false,
  });

  register('negativeStartDateToleranceInterval', {
    required: false,
  });

  register('positiveDueDateToleranceInterval', {
    required: false,
  });

  register('negativeDueDateToleranceInterval', {
    required: false,
  });

  const {
    startDateDuration,
    dueDateDuration,
    startDateInterval,
    dueDateInterval,
    positiveStartDateToleranceDuration,
    negativeStartDateToleranceDuration,
    positiveDueDateToleranceDuration,
    negativeDueDateToleranceDuration,
    positiveStartDateToleranceInterval,
    negativeStartDateToleranceInterval,
    positiveDueDateToleranceInterval,
    negativeDueDateToleranceInterval,
  } = watch([
    'startDateDuration',
    'dueDateDuration',
    'startDateInterval',
    'dueDateInterval',
    'positiveStartDateToleranceDuration',
    'negativeStartDateToleranceDuration',
    'positiveDueDateToleranceDuration',
    'negativeDueDateToleranceDuration',
    'positiveStartDateToleranceInterval',
    'negativeStartDateToleranceInterval',
    'positiveDueDateToleranceInterval',
    'negativeDueDateToleranceInterval',
  ]);

  const getRecurrence = async (taskId: string) => {
    try {
      const { data } = await request('GET', apiGetTaskRecurrence(taskId));
      if (data) {
        setRecurrence(data);
        setEditMode(true);
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

  const onSetTaskRecurrence = (data: any) => {
    dispatch(
      setTaskRecurrence({
        taskId: task.id,
        startDateDuration: data.startDateDuration,
        startDateInterval: data.startDateInterval,
        dueDateDuration: data.dueDateDuration,
        dueDateInterval: data.dueDateInterval,
        positiveStartDateToleranceDuration: data.positiveStartDateToleranceDuration,
        negativeStartDateToleranceDuration: data.negativeStartDateToleranceDuration,
        positiveDueDateToleranceDuration: data.positiveDueDateToleranceDuration,
        negativeDueDateToleranceDuration: data.negativeDueDateToleranceDuration,
        positiveStartDateToleranceInterval: data.positiveStartDateToleranceInterval,
        negativeStartDateToleranceInterval: data.negativeStartDateToleranceInterval,
        positiveDueDateToleranceInterval: data.positiveDueDateToleranceInterval,
        negativeDueDateToleranceInterval: data.negativeDueDateToleranceInterval,
        updating: data.updating,
      }),
    );
  };

  const onRemoveTaskRecurrence = () => {
    dispatch(removeTaskRecurrence(task.id));
  };

  const updateDueDateInterval = (duration: Record<string, number>, intervalToBeSet: string) => {
    const durationSeconds = calculateSecondsFromDuration(duration);
    if (
      durationSeconds === 0 &&
      (intervalToBeSet === 'startDateInterval' || intervalToBeSet === 'dueDateInterval')
    ) {
      if (intervalToBeSet === 'startDateInterval') {
        setValue('positiveStartDateToleranceDuration.day', 0);
        setValue('positiveStartDateToleranceDuration.hour', 0);
        setValue('positiveStartDateToleranceDuration.minute', 0);
        setValue('negativeStartDateToleranceDuration.day', 0);
        setValue('negativeStartDateToleranceDuration.hour', 0);
        setValue('negativeStartDateToleranceDuration.minute', 0);
      } else {
        setValue('positiveDueDateToleranceDuration.day', 0);
        setValue('positiveDueDateToleranceDuration.hour', 0);
        setValue('positiveDueDateToleranceDuration.minute', 0);
        setValue('negativeDueDateToleranceDuration.day', 0);
        setValue('negativeDueDateToleranceDuration.hour', 0);
        setValue('negativeDueDateToleranceDuration.minute', 0);
      }
    }
    setValue(intervalToBeSet, durationSeconds, {
      shouldValidate: true,
    });
  };

  const { day: startDay, hour: startHour, minute: startMinute } = startDateDuration || {};
  const { day: dueDay, hour: dueHour, minute: dueMinute } = dueDateDuration || {};
  const {
    day: positiveStartDay,
    hour: positiveStartHour,
    minute: positiveStartMinute,
  } = positiveStartDateToleranceDuration || {};
  const {
    day: negativeStartDay,
    hour: negativeStartHour,
    minute: negativeStartMinute,
  } = negativeStartDateToleranceDuration || {};
  const {
    day: positiveEndDay,
    hour: positiveEndHour,
    minute: positiveEndMinute,
  } = positiveDueDateToleranceDuration || {};
  const {
    day: negativeEndDay,
    hour: negativeEndHour,
    minute: negativeEndMinute,
  } = negativeDueDateToleranceDuration || {};

  useEffect(() => {
    if (enableRecurrence && recurrence) {
      reset({
        startDateDuration: recurrence?.startDateDuration,
        startDateInterval: recurrence?.startDateInterval,
        dueDateDuration: recurrence?.dueDateDuration,
        dueDateInterval: recurrence?.dueDateInterval,
        positiveStartDateToleranceDuration: recurrence?.positiveStartDateToleranceDuration,
        negativeStartDateToleranceDuration: recurrence?.negativeStartDateToleranceDuration,
        positiveDueDateToleranceDuration: recurrence?.positiveDueDateToleranceDuration,
        negativeDueDateToleranceDuration: recurrence?.negativeDueDateToleranceDuration,
        positiveStartDateToleranceInterval: recurrence?.positiveStartDateToleranceInterval,
        negativeStartDateToleranceInterval: recurrence?.negativeStartDateToleranceInterval,
        positiveDueDateToleranceInterval: recurrence?.positiveDueDateToleranceInterval,
        negativeDueDateToleranceInterval: recurrence?.negativeDueDateToleranceInterval,
      });
    }
  }, [recurrence]);

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
    if (dueDateDuration) {
      updateDueDateInterval(
        positiveStartDateToleranceDuration,
        'positiveStartDateToleranceInterval',
      );
    }
  }, [positiveStartDay, positiveStartHour, positiveStartMinute]);

  useEffect(() => {
    if (startDateDuration) {
      updateDueDateInterval(
        negativeStartDateToleranceDuration,
        'negativeStartDateToleranceInterval',
      );
    }
  }, [negativeStartDay, negativeStartHour, negativeStartMinute]);

  useEffect(() => {
    if (dueDateDuration) {
      updateDueDateInterval(positiveDueDateToleranceDuration, 'positiveDueDateToleranceInterval');
    }
  }, [positiveEndDay, positiveEndHour, positiveEndMinute]);

  useEffect(() => {
    if (startDateDuration) {
      updateDueDateInterval(negativeDueDateToleranceDuration, 'negativeDueDateToleranceInterval');
    }
  }, [negativeEndDay, negativeEndHour, negativeEndMinute]);

  useEffect(() => {
    if (task?.id) {
      getRecurrence(task?.id);
    }
  }, []);

  const validateRecurrenceForm = useCallback(() => {
    let isValid = true;
    if (dueDateInterval > 0 && startDateInterval <= 0) {
      isValid = false;
    }
    return editMode ? !isValid || !isDirty : !isValid;
  }, [dueDateInterval, startDateInterval, editMode, isDirty]);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title="Task Recurrence"
        primaryText={`${editMode ? 'Update' : 'Save'}`}
        closeModal={closeOverlay}
        showSecondary={false}
        modalFooterOptions={
          <div
            onClick={() => {
              onRemoveTaskRecurrence();
            }}
            className="remove-recurrence"
          >
            {enableRecurrence ? 'Remove Task Recurrence' : ''}
          </div>
        }
        disabledPrimary={validateRecurrenceForm()}
        onPrimary={handleSubmit((data) =>
          onSetTaskRecurrence({ ...data, updating: recurrence ? true : false }),
        )}
        showFooter={!isReadOnly}
      >
        <div>
          {!enableRecurrence && isReadOnly ? (
            <div>Task Recurrence is not set.</div>
          ) : (
            <div className="task-recurrence">
              <div>
                <div style={{ display: 'flex' }}>
                  <p className="custom-label custom-label-bold">Start Task After </p>
                  <div className="optional-badge">optional</div>
                </div>
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
                <p className="custom-label" style={{ color: '#6F6F6F', fontSize: '12px' }}>
                  This is the minimum time interval after which the next task instance should be
                  started.
                </p>
              </div>

              <div>
                <p className="custom-label">Positive Start Tolerance (+ve)</p>
                <div className="start-after-section">
                  <FormGroup
                    key="start-after-section"
                    inputs={[
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Day',
                          label: 'Day',
                          id: 'positiveStartDateToleranceDuration.day',
                          name: 'positiveStartDateToleranceDuration.day',
                          error: !!errors?.positiveStartDateToleranceDuration?.day,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/, // Regex pattern to allow only positive integers, no decimals allowed
                          }),
                          disabled: isReadOnly || !startDateInterval,
                        },
                      },
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Hour',
                          label: 'Hour',
                          id: 'positiveStartDateToleranceDuration.hour',
                          name: 'positiveStartDateToleranceDuration.hour',
                          error: !!errors?.positiveStartDateToleranceDuration?.hour,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/,
                          }),
                          disabled: isReadOnly || !startDateInterval,
                        },
                      },
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Minute',
                          label: 'Minute',
                          id: 'positiveStartDateToleranceDuration.minute',
                          name: 'positiveStartDateToleranceDuration.minute',
                          error: !!errors?.positiveStartDateToleranceDuration?.minute,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/,
                          }),
                          disabled: isReadOnly || !startDateInterval,
                        },
                      },
                    ]}
                  />
                </div>
                <p className="custom-label" style={{ color: '#6F6F6F', fontSize: '12px' }}>
                  The exception is not triggered if the task is started within the time limit set by
                  this field.
                </p>
              </div>

              <div>
                <p className="custom-label">Negative Start Tolerance (-ve)</p>
                <div className="start-after-section">
                  <FormGroup
                    key="start-after-section"
                    inputs={[
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Day',
                          label: 'Day',
                          id: 'negativeStartDateToleranceDuration.day',
                          name: 'negativeStartDateToleranceDuration.day',
                          error: !!errors?.negativeStartDateToleranceDuration?.day,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/, // Regex pattern to allow only positive integers, no decimals allowed
                          }),
                          disabled: isReadOnly || !startDateInterval,
                        },
                      },
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Hour',
                          label: 'Hour',
                          id: 'negativeStartDateToleranceDuration.hour',
                          name: 'negativeStartDateToleranceDuration.hour',
                          error: !!errors?.negativeStartDateToleranceDuration?.hour,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/,
                          }),
                          disabled: isReadOnly || !startDateInterval,
                        },
                      },
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Minute',
                          label: 'Minute',
                          id: 'negativeStartDateToleranceDuration.minute',
                          name: 'negativeStartDateToleranceDuration.minute',
                          error: !!errors?.negativeStartDateToleranceDuration?.minute,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/,
                          }),
                          disabled: isReadOnly || !startDateInterval,
                        },
                      },
                    ]}
                  />
                </div>
                <p className="custom-label" style={{ color: '#6F6F6F', fontSize: '12px' }}>
                  The exception is not triggered if the task is started within the time limit set by
                  this field.
                </p>
              </div>

              <hr
                style={{
                  border: '1px solid #e9e9e9',
                  margin: 0,
                }}
              />

              <div>
                <div style={{ display: 'flex' }}>
                  <p className="custom-label custom-label-bold">Task Due After </p>
                  <div className="optional-badge">optional</div>
                </div>
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
                <p className="custom-label" style={{ color: '#6F6F6F', fontSize: '12px' }}>
                  This is the time within which the task should be completed, after which it will be
                  marked as overdue.
                </p>
              </div>

              <div>
                <p className="custom-label">Positive End Tolerance (+ve)</p>
                <div className="due-after-section">
                  <FormGroup
                    key="due-after-section"
                    inputs={[
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Day',
                          label: 'Day',
                          id: 'positiveDueDateToleranceDuration.day',
                          name: 'positiveDueDateToleranceDuration.day',
                          error: !!errors?.positiveDueDateToleranceDuration?.day,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/,
                          }),
                          disabled: isReadOnly || !dueDateInterval,
                        },
                      },
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Hour',
                          label: 'Hour',
                          id: 'positiveDueDateToleranceDuration.hour',
                          name: 'positiveDueDateToleranceDuration.hour',
                          error: !!errors?.positiveDueDateToleranceDuration?.hour,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/,
                          }),
                          disabled: isReadOnly || !dueDateInterval,
                        },
                      },
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Minute',
                          label: 'Minute',
                          id: 'positiveDueDateToleranceDuration.minute',
                          name: 'positiveDueDateToleranceDuration.minute',
                          error: !!errors?.positiveDueDateToleranceDuration?.minute,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/,
                          }),
                          disabled: isReadOnly || !dueDateInterval,
                        },
                      },
                    ]}
                  />
                </div>
                <p className="custom-label" style={{ color: '#6F6F6F', fontSize: '12px' }}>
                  The exception is not triggered if the task is started within the time limit set by
                  this field.
                </p>
              </div>

              <div>
                <p className="custom-label">Negative End Tolerance (-ve)</p>
                <div className="due-after-section">
                  <FormGroup
                    key="due-after-section"
                    inputs={[
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Day',
                          label: 'Day',
                          id: 'negativeDueDateToleranceDuration.day',
                          name: 'negativeDueDateToleranceDuration.day',
                          error: !!errors?.negativeDueDateToleranceDuration?.day,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/,
                          }),
                          disabled: isReadOnly || !dueDateInterval,
                        },
                      },
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Hour',
                          label: 'Hour',
                          id: 'negativeDueDateToleranceDuration.hour',
                          name: 'negativeDueDateToleranceDuration.hour',
                          error: !!errors?.negativeDueDateToleranceDuration?.hour,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/,
                          }),
                          disabled: isReadOnly || !dueDateInterval,
                        },
                      },
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          placeholder: 'Minute',
                          label: 'Minute',
                          id: 'negativeDueDateToleranceDuration.minute',
                          name: 'negativeDueDateToleranceDuration.minute',
                          error: !!errors?.negativeDueDateToleranceDuration?.minute,
                          defaultValue: 0,
                          ref: register({
                            required: true,
                            valueAsNumber: true,
                            min: 0,
                            pattern: /^\d+$/,
                          }),
                          disabled: isReadOnly || !dueDateInterval,
                        },
                      },
                    ]}
                  />
                </div>
                <p className="custom-label" style={{ color: '#6F6F6F', fontSize: '12px' }}>
                  The exception is not triggered if the task is completed after the time limit set
                  by this field.
                </p>
              </div>
              {startDateInterval ||
              dueDateInterval ||
              positiveStartDateToleranceInterval ||
              negativeStartDateToleranceInterval ||
              positiveDueDateToleranceInterval ||
              negativeDueDateToleranceInterval ? (
                <div className="scheduler-summary">
                  <h4>Summary</h4>
                  <ReadOnlyGroup
                    className="read-only-group"
                    minWidth={'200px'}
                    items={[
                      {
                        label: 'Start Task After',
                        value: getSummary(startDateDuration),
                      },
                      {
                        label: 'Positive Start Tolerance',
                        value: getSummary(positiveStartDateToleranceDuration),
                      },
                      {
                        label: 'Negative Start Tolerance',
                        value: getSummary(negativeStartDateToleranceDuration),
                      },
                    ]}
                  />
                  <hr
                    style={{
                      border: '1px solid #e9e9e9',
                      marginTop: 4,
                      marginBottom: 16,
                    }}
                  />
                  <ReadOnlyGroup
                    className="read-only-group"
                    minWidth={'200px'}
                    items={[
                      {
                        label: 'Task Due After',
                        value: getSummary(dueDateDuration),
                      },
                      {
                        label: 'Positive End Tolerance',
                        value: getSummary(positiveDueDateToleranceDuration),
                      },
                      {
                        label: 'Negative End Tolerance',
                        value: getSummary(negativeDueDateToleranceDuration),
                      },
                    ]}
                  />
                </div>
              ) : null}
            </div>
          )}
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default TaskRecurrenceModal;
