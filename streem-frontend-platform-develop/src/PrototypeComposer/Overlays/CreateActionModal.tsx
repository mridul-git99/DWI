import { BaseModal, FormGroup } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { nonEmptyStringRegex } from '#utils/constants';
import { InputTypes } from '#utils/globalTypes';
import React, { Dispatch, FC, SetStateAction, useEffect, useMemo, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import styled from 'styled-components';
import { apiCreateOrEditAction } from '#utils/apiUrls';
import { getErrorMsg, request } from '#utils/request';
import { TriggerType } from '#types/actionsAndEffects';
import { useDispatch } from 'react-redux';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';

type TCreateActionModalProps = {
  isReadOnly: boolean;
  action?: Record<string, any>;
  actionList: Record<string, any>[];
  setRefetchActions: Dispatch<SetStateAction<boolean>>;
};

const Wrapper = styled.div.attrs({})`
  .modal {
    width: 480px !important;

    .modal-body {
      padding: 24px !important;
      overflow: auto;
      font-size: 14px;
      color: #525252;
      display: flex;
      flex-direction: column;

      .form-group {
        padding: 0;
        margin-bottom: 16px;

        :last-of-type {
          margin-bottom: 0;
        }
      }
    }

    .modal-footer {
      flex-direction: row-reverse !important;
    }
  }
`;

const triggerTypeOptions = [
  { value: TriggerType.START_TASK, label: 'When Task is Started' },
  { value: TriggerType.COMPLETE_TASK, label: 'When Task is Completed' },
];

const CreateActionModal: FC<CommonOverlayProps<TCreateActionModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { isReadOnly, action, actionList, setRefetchActions },
}) => {
  const dispatch = useDispatch();
  const tasksOrderInStage = useTypedSelector(
    (state) => state.prototypeComposer.tasks.tasksOrderInStage,
  );

  const taskslistbyid = useTypedSelector((state) => state.prototypeComposer.tasks.listById);
  const checklistId = useTypedSelector((state) => state.prototypeComposer.data?.id);

  const [tasksList, setTasksList] = useState<any[]>([]);
  const [tasksWithActions, setTasksWithActions] = useState<any>({
    start: {},
    complete: {},
  });

  const form = useForm<{
    name: string;
    description: string;
    successMessage?: string;
    failureMessage?: string;
    triggerType: TriggerType | null;
    triggerEntityId: string | null;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      name: action?.name || '',
      description: action?.description || '',
      successMessage: action?.successMessage || '',
      failureMessage: action?.failureMessage || '',
      triggerType: action?.triggerType || null,
      triggerEntityId: action?.triggerEntityId || null,
    },
  });

  const {
    handleSubmit,
    register,
    watch,
    control,
    formState: { isDirty, isValid },
  } = form;

  const { triggerType } = watch(['triggerType']);

  const sortTaskActions = () => {
    actionList.forEach((action: any) => {
      if (action?.triggerType === TriggerType.START_TASK) {
        tasksWithActions.start[action?.triggerEntityId] = true;
      } else if (action?.triggerType === TriggerType.COMPLETE_TASK) {
        tasksWithActions.complete[action?.triggerEntityId] = true;
      }
    });
  };

  const getTasksList = () => {
    const tasks = [];
    let stageOrder = 1;

    for (const stageId in tasksOrderInStage) {
      const stageTasks = tasksOrderInStage[stageId];

      for (let i = 0; i < stageTasks.length; i++) {
        const taskId = stageTasks[i];
        const taskName = taskslistbyid[taskId].name;
        const label = `Task ${stageOrder}.${i + 1} : ${taskName}`;

        tasks.push({
          label,
          value: taskId,
        });
      }

      stageOrder++;
    }
    setTasksList(tasks);
  };

  const filteredTasks = useMemo(() => {
    if (triggerType === TriggerType.START_TASK) {
      return tasksList.filter((task) => !tasksWithActions.start[task.value]);
    } else if (triggerType === TriggerType.COMPLETE_TASK) {
      return tasksList.filter((task) => !tasksWithActions.complete[task.value]);
    } else {
      return tasksList;
    }
  }, [triggerType, tasksList, tasksWithActions]);

  const onCreateAction = async (data: any) => {
    const { name, description, triggerType, triggerEntityId, successMessage, failureMessage } =
      data;

    const { data: responseData, errors } = await request(
      action ? 'PATCH' : 'POST',
      action ? apiCreateOrEditAction(action.id) : apiCreateOrEditAction(),
      {
        data: {
          name,
          triggerType,
          triggerEntityId,
          ...(description && { description }),
          ...(successMessage && { successMessage }),
          ...(failureMessage && { failureMessage }),
          checklistId,
        },
      },
    );

    if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }

    if (responseData) {
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Action ${action ? 'updated' : 'created'} successfully`,
        }),
      );
      setRefetchActions((prev) => !prev);
      closeOverlay();
    }
  };

  useEffect(() => {
    getTasksList();
    sortTaskActions();
  }, []);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title={`${action ? 'Edit' : 'Create'} Action`}
        closeModal={closeOverlay}
        primaryText={action ? 'Update' : 'Save'}
        showSecondary={false}
        showFooter={!isReadOnly}
        disabledPrimary={!isValid || !isDirty}
        onPrimary={handleSubmit((data) => onCreateAction(data))}
      >
        <div>
          <FormGroup
            inputs={[
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  placeholder: 'Write here',
                  label: 'Action Name',
                  id: 'name',
                  name: 'name',
                  disabled: isReadOnly,
                  ref: register({
                    required: true,
                    pattern: nonEmptyStringRegex,
                  }),
                },
              },
              {
                type: InputTypes.MULTI_LINE,
                props: {
                  placeholder: 'Write here',
                  label: 'Description',
                  id: 'description',
                  name: 'description',
                  disabled: isReadOnly,
                  optional: true,
                  ref: register({
                    pattern: nonEmptyStringRegex,
                  }),
                  rows: 3,
                  maxRows: 6,
                },
              },
            ]}
          />
          <Controller
            control={control}
            name="triggerType"
            key="triggerType"
            shouldUnregister={false}
            rules={{
              required: true,
            }}
            render={({ value, onChange }) => (
              <FormGroup
                inputs={[
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'triggerType',
                      label: 'When',
                      options: triggerTypeOptions,
                      isDisabled: isReadOnly,
                      value: value
                        ? triggerTypeOptions.find((option) => option.value === value)
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
          <Controller
            control={control}
            name="triggerEntityId"
            key="triggerEntityId"
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
                      id: 'triggerEntityId',
                      label: 'Task',
                      options: filteredTasks,
                      isDisabled: isReadOnly || !triggerType,
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
          <FormGroup
            inputs={[
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  placeholder: 'Write here',
                  label: 'Success Message',
                  id: 'successMessage',
                  name: 'successMessage',
                  disabled: isReadOnly,
                  optional: true,
                  ref: register({
                    pattern: nonEmptyStringRegex,
                  }),
                },
              },
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  placeholder: 'Write here',
                  label: 'Failure Message',
                  id: 'failureMessage',
                  name: 'failureMessage',
                  optional: true,
                  disabled: isReadOnly,
                  ref: register({
                    pattern: nonEmptyStringRegex,
                  }),
                },
              },
            ]}
          />
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default CreateActionModal;
