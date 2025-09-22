import { BaseModal, FormGroup } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { InputTypes } from '#utils/globalTypes';
import React, { FC, useEffect, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { useTypedSelector } from '#store/helpers';
import { useForm, Controller } from 'react-hook-form';
import styled from 'styled-components';
import { components } from 'react-select';
import Option from '#components/shared/DropdownCheckboxOption';
import { getErrorMsg, request } from '#utils/request';
import { apiRemoveExecutorLock, apiExecutorLock } from '#utils/apiUrls';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { updateTask } from '#PrototypeComposer/Tasks/actions';
import { getStageTaskOptions } from '#utils/stringUtils';

export interface TaskExecutorLockModalProps {
  taskId: string;
  isReadOnly: boolean;
  hasExecutorLock: boolean;
}

const Wrapper = styled.div.attrs({})`
  .modal {
    width: 576px !important;

    .modal-body {
      padding: 24px !important;
      overflow: auto;
      font-size: 14px;
      color: #525252;

      .executor-lock {
        p {
          margin: 0px 0px 16px 0px;
        }
      }

      .form-group {
        padding: 0;
        margin-bottom: 16px;

        .react-custom-select {
          .label {
            font-size: 14px;
            color: #161616;
          }
        }

        :last-of-type {
          margin-bottom: 0;
        }
      }
    }

    .modal-footer {
      flex-direction: row;
      justify-content: space-between;
      align-items: center;

      .remove {
        color: red;
        font-size: 14px;
        line-height: 16px;
        cursor: pointer;
      }
    }
  }
`;

const NestedOption = (props: any) => {
  return (
    <div>
      <components.Option {...props}>
        <div style={{ padding: '2px 0px 2px 16px' }}>{props.label}</div>
      </components.Option>
    </div>
  );
};

const TaskExecutorLock: FC<CommonOverlayProps<TaskExecutorLockModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { taskId, isReadOnly, hasExecutorLock },
}) => {
  const {
    tasks: { tasksOrderInStage, listById: tasksListById },
    stages: { listById: stagesListById },
  } = useTypedSelector((state) => state.prototypeComposer);

  const task = tasksListById[taskId];

  const dispatch = useDispatch();

  const form = useForm<{
    mandatoryExecutor: string | null;
    excludedExecutors: string[] | null;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      mandatoryExecutor: null,
      excludedExecutors: null,
    },
  });

  const {
    handleSubmit,
    watch,
    reset,
    control,
    formState: { isDirty, isValid },
  } = form;

  const { mandatoryExecutor, excludedExecutors } = watch([
    'mandatoryExecutor',
    'excludedExecutors',
  ]);

  const options = useMemo(() => {
    return getStageTaskOptions(stagesListById, tasksListById, tasksOrderInStage, taskId);
  }, [stagesListById, tasksListById, tasksOrderInStage, taskId]);

  const filteredExcludedExecutorsOptions = useMemo(() => {
    if (!mandatoryExecutor) {
      return options;
    }

    return options.map((stage) => ({
      ...stage,
      options: stage?.options.filter((option) => option?.value !== mandatoryExecutor),
    }));
  }, [options, mandatoryExecutor]);

  const filteredMandatoryExecutorOptions = useMemo(() => {
    if (!excludedExecutors || excludedExecutors.length === 0) {
      return options;
    }
    return options.map((stage) => ({
      ...stage,
      options: stage?.options.filter((option) => !excludedExecutors?.includes(option.value)),
    }));
  }, [options, excludedExecutors]);

  const getTaskExecutorLockDetails = async () => {
    const { data } = await request('GET', apiExecutorLock(taskId));

    if (data) {
      // Updating the `hasExecutorLock` in the case of no data.
      // ( handling when user deletes a task and it clears out excecutor lock of other tasks )
      // Temporary fix until we have polling or any other mechanism to know the taskIds for which executor lock details are empty after deletion of some task.
      if (!data.length) {
        dispatch(updateTask({ ...task, hasExecutorLock: false }));
        return;
      }

      const executorList = data.filter((item: any) => item.condition === 'EQ');
      const nonExecutorList = data.filter((item: any) => item.condition === 'NIN');

      reset({
        mandatoryExecutor: executorList[0]?.taskId,
        excludedExecutors: nonExecutorList?.map((item: any) => item?.taskId),
      });
    }
  };

  const setTaskExecutorLock = async (executorData: {
    mandatoryExecutor: string | null;
    excludedExecutors: string[] | null;
  }) => {
    const { mandatoryExecutor, excludedExecutors } = executorData;

    if (!mandatoryExecutor && !excludedExecutors?.length) {
      removeTaskExecutorLock();
      return;
    }

    const { data, errors } = await request('POST', apiExecutorLock(taskId), {
      data: {
        hasToBeExecutorId: mandatoryExecutor,
        cannotBeExecutorIds: excludedExecutors || [],
      },
    });

    if (data) {
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Task Executor Lock set successfully',
        }),
      );
      dispatch(updateTask({ ...task, hasExecutorLock: true }));
    } else if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }
    closeOverlay();
  };

  const removeTaskExecutorLock = async () => {
    const { data, errors } = await request('PATCH', apiRemoveExecutorLock(taskId));

    if (data) {
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Task Executor Lock removed successfully',
        }),
      );
      dispatch(updateTask({ ...task, hasExecutorLock: false }));
      closeOverlay();
    } else if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }
  };

  const getTaskOption = (taskId: string) => {
    const task = tasksListById[taskId];

    const { id, name, orderTree: taskOrderTree, stageId } = task;

    const stage = stagesListById[stageId];

    const { orderTree: stageOrderTree } = stage;

    return {
      label: `Task ${stageOrderTree}.${taskOrderTree} : ${name}`,
      value: id,
    };
  };

  useEffect(() => {
    if (hasExecutorLock) {
      getTaskExecutorLockDetails();
    }
  }, []);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title="Set Task Executor Lock"
        primaryText="Save"
        closeModal={closeOverlay}
        showSecondary={true}
        secondaryText="Cancel"
        onSecondary={closeOverlay}
        modalFooterOptions={
          <div
            onClick={() => {
              if (hasExecutorLock) {
                removeTaskExecutorLock();
              }
            }}
            className="remove"
          >
            {hasExecutorLock ? 'Remove Executor Lock' : ''}
          </div>
        }
        disabledPrimary={!isDirty || !isValid}
        onPrimary={handleSubmit((executorData) => {
          setTaskExecutorLock(executorData);
        })}
        showFooter={!isReadOnly}
      >
        <div>
          {!hasExecutorLock && isReadOnly ? (
            <div>No Task Executor Lock configured.</div>
          ) : (
            <div className="executor-lock">
              <p>Specify the tasks that user HAS to execute & CANNOT execute</p>
              <Controller
                control={control}
                name="mandatoryExecutor"
                key="mandatoryExecutor"
                defaultValue={mandatoryExecutor}
                shouldUnregister={false}
                render={({ onChange, value }) => (
                  <FormGroup
                    inputs={[
                      {
                        type: InputTypes.SINGLE_SELECT,
                        props: {
                          id: 'taskExecutor',
                          label: 'HAS to be executor of task',
                          options: filteredMandatoryExecutorOptions,
                          isDisabled: isReadOnly,
                          menuPortalTarget: document.body,
                          menuPosition: 'fixed',
                          maxMenuHeight: 250,
                          placeholder: 'Select a Task',
                          components: { Option: NestedOption },
                          isClearable: true,
                          value: value ? getTaskOption(value) : null,
                          onChange: (option: { value: string }) => {
                            onChange(option?.value ? option.value : null);
                          },
                        },
                      },
                    ]}
                  />
                )}
              />
              <Controller
                control={control}
                name="excludedExecutors"
                key="excludedExecutors"
                defaultValue={excludedExecutors}
                shouldUnregister={false}
                render={({ onChange, value }) => (
                  <FormGroup
                    inputs={[
                      {
                        type: InputTypes.MULTI_SELECT,
                        props: {
                          id: 'nonTaskExecutor',
                          label: 'CANNOT be executor of tasks',
                          options: filteredExcludedExecutorsOptions,
                          isDisabled: isReadOnly,
                          closeMenuOnSelect: false,
                          hideSelectedOptions: false,
                          placeholder: 'Select Multiple Tasks',
                          menuPortalTarget: document.body,
                          menuPosition: 'fixed',
                          maxMenuHeight: 250,
                          value: value ? value.map((value: string) => getTaskOption(value)) : null,
                          components: { Option },
                          onChange: (value: any[]) => {
                            onChange(value.length > 0 ? value.map((v) => v.value) : null);
                          },
                        },
                      },
                    ]}
                  />
                )}
              />
            </div>
          )}
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default TaskExecutorLock;
