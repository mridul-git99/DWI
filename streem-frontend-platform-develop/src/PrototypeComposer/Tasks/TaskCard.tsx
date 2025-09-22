import ParameterTaskView from '#PrototypeComposer/Parameters/TaskViews';
import { copyEntities } from '#PrototypeComposer/actions';
import { EntityType } from '#PrototypeComposer/types';
import BulkVerificationIcon from '#assets/svg/BulkVerificationIcon';
import ConfigureActionsIcon from '#assets/svg/ConfigureActionsIcon';
import ScheduleTask from '#assets/svg/ScheduleTask';
import SoloTaskLock from '#assets/svg/SoloTaskLock';
import TaskDependencyIcon from '#assets/svg/TaskDependencyIcon';
import TaskExecutorLock from '#assets/svg/TaskExecutorLock';
import TaskInterlocks from '#assets/svg/TaskInterlocks';
import TaskMediaIcon from '#assets/svg/TaskMediaIcon';
import TaskRecurrence from '#assets/svg/TaskRecurrence';
import TimedTaskIcon from '#assets/svg/TimedTaskIcon';
import {
  ImageGallery,
  ImageUploadButton,
  LoadingContainer,
  NestedSelect,
  Textarea,
} from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import Tooltip from '#components/shared/Tooltip';
import { useWindowWidth } from '#hooks/useWindowWidth';
import { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store/helpers';
import { MandatoryParameter, NonMandatoryParameter, TargetEntityType } from '#types';
import { apiGetParameters, apiMapParameterToTask, apiUpdateTask } from '#utils/apiUrls';
import { DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, ResponseObj } from '#utils/globalTypes';
import { request } from '#utils/request';
import {
  DndContext,
  DragEndEvent,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { restrictToVerticalAxis } from '@dnd-kit/modifiers';
import { SortableContext, arrayMove, verticalListSortingStrategy } from '@dnd-kit/sortable';
import {
  AddCircleOutline,
  ArrowDropDown,
  ArrowDropUp,
  DeleteOutlined,
  Error as ErrorIcon,
  FileCopyOutlined,
} from '@material-ui/icons';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import { debounce } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { addNewParameterSuccess, toggleNewParameter } from '../Activity/actions';
import DynamicTaskDrawer from './DynamicTaskDrawer';
import {
  addBulkVerification,
  addSoloTaskLock,
  deleteTask,
  reOrderParameters,
  reOrderTask,
  removeBulkVerification,
  removeSoloTaskLock,
  setActiveTask,
  updateTaskName,
} from './actions';
import { AddActivityItemWrapper, TaskCardWrapper } from './styles';
import { ProcessConfigurations, ProcessLevelOptions, TaskCardProps, TaskTypeEnum } from './types';

const AddActivity = () => {
  return (
    <AddActivityItemWrapper>
      <div className="label">
        <AddCircleOutline /> Add Activity
      </div>
      <ArrowDropDown />
    </AddActivityItemWrapper>
  );
};

export const getPrototypeIcon = (type: string) => {
  switch (type) {
    case ProcessLevelOptions.DEPENDENCIES:
      return <TaskDependencyIcon className="icon" />;
    case ProcessLevelOptions.AUTOMATION:
      return <ConfigureActionsIcon className="icon" />;
    case ProcessLevelOptions.EXECUTOR_LOCK:
      return <TaskExecutorLock className="icon" />;
    case ProcessLevelOptions.RECURRENCE:
      return <TaskRecurrence className="icon" />;
    case ProcessLevelOptions.INTERLOCKS:
      return <TaskInterlocks className="icon" />;
    case ProcessLevelOptions.SCHEDULE:
      return <ScheduleTask className="icon" />;
    case ProcessLevelOptions.SOLO_TASK:
      return <SoloTaskLock className="icon" />;
    case ProcessLevelOptions.TIMED:
      return <TimedTaskIcon className="icon" />;
    case ProcessLevelOptions.BULK_VERIFICATION:
      return <BulkVerificationIcon className="icon" />;
    default:
      return;
  }
};

const DeleteTaskContent = (props: { taskId: string }) => {
  const { taskId } = props;

  const [response, setResponse] = useState<any>(null);

  const fetchTask = async (taskId: string) => {
    const { data, errors } = await request('GET', apiUpdateTask(taskId));
    if (data) {
      setResponse(data);
    }

    if (errors) {
      setResponse(true);
    }
  };

  useEffect(() => {
    fetchTask(taskId);
  }, []);

  return (
    <LoadingContainer
      loading={response === null}
      component={
        <>
          {response?.hasDependents && (
            <div>
              This task is set as a prerequisite. Deleting this task will also delete it's
              prerequisite status.
            </div>
          )}
          {response?.referencedTaskExecutorLock && (
            <div>This task has been set as a prerequisite for task executor lock.</div>
          )}
          <div>Are you sure you want to Delete this Task?</div>
        </>
      }
    />
  );
};

const TaskCard: FC<
  TaskCardProps & { isFirstTask: boolean; isLastTask: boolean; isReadOnly: boolean }
> = ({ task, index, isFirstTask, isLastTask, isActive, isReadOnly }) => {
  const dispatch = useDispatch();
  const {
    data,
    parameters: { parameterOrderInTaskInStage, listById },
    stages: { activeStageId, listOrder },
    tasks: { activeTaskId },
    errors: checklistErrors,
  } = useTypedSelector((state) => state.prototypeComposer);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const stageIndex = listOrder.indexOf(activeStageId as string);
  const { count } = useWindowWidth();
  const sensors = useSensors(useSensor(PointerSensor));

  const {
    id: taskId,
    maxPeriod = 0,
    medias,
    minPeriod = 0,
    name,
    timed,
    timerOperator,
    automations,
    enableScheduling,
    enableRecurrence,
    hasPrerequisites,
    interlocks,
    soloTask,
    hasExecutorLock,
    hasBulkVerification,
  } = task;

  const taskConditions = interlocks?.validations?.resourceParameterValidations?.length || 0;

  const handleDragEnd = (e: DragEndEvent) => {
    const { active, over } = e;
    if (data?.id && activeStageId && over && active.id !== over.id) {
      const taskParameters = parameterOrderInTaskInStage[activeStageId][taskId];
      const oldIndex = taskParameters.indexOf(active.id as string);
      const newIndex = taskParameters.indexOf(over.id as string);
      dispatch(
        reOrderParameters({
          checklistId: data.id,
          stageId: activeStageId,
          taskId,
          orderedIds: arrayMove(taskParameters, oldIndex, newIndex),
        }),
      );
    }
  };

  const onChildChange = async (option: any) => {
    switch (option.value) {
      case 'add-new-parameter':
        dispatch(
          toggleNewParameter({ action: 'task', title: 'Create a New Process Parameter', taskId }),
        );
        break;
      case 'add-text':
        dispatch(
          toggleNewParameter({
            action: 'task',
            title: 'Create a New Instruction',
            type: NonMandatoryParameter.INSTRUCTION,
            taskId,
          }),
        );
        break;
      case 'add-material':
        dispatch(
          toggleNewParameter({
            action: 'task',
            title: 'Create a New Instruction',
            type: NonMandatoryParameter.MATERIAL,
            taskId,
          }),
        );
        break;
      case 'checklist':
        dispatch(
          toggleNewParameter({
            action: 'task',
            title: 'Create a New Checklist',
            type: MandatoryParameter.CHECKLIST,
            taskId,
          }),
        );
        break;
      default:
        if (activeStageId) {
          const parametersInTask = parameterOrderInTaskInStage[activeStageId][taskId];
          const maxOrderTree =
            listById?.[parametersInTask?.[parametersInTask?.length - 1]]?.orderTree ?? 0;
          const response: ResponseObj<any> = await request(
            'PATCH',
            apiMapParameterToTask(data!.id, taskId),
            {
              data: {
                parameterId: option.id,
                orderTree: maxOrderTree + 1,
              },
            },
          );
          if (response?.data) {
            dispatch(
              addNewParameterSuccess({
                parameter: response.data,
                stageId: activeStageId,
                taskId,
              }),
            );
            dispatch(
              showNotification({
                type: NotificationType.SUCCESS,
                msg: 'Parameter added',
                detail: response.data.label,
              }),
            );
          }
        }
        break;
    }
  };

  // E211 = 'TASK_SHOULD_HAVE_ATLEAST_ONE_EXECUTABLE_PARAMETER'
  const noParameterError =
    checklistErrors.find((error) => error.code === 'E211' && error.id === task.id) ?? undefined;

  // E225 = 'TASK_AUTOMATION_INVALID_MAPPED_PARAMETERS',
  const archiveParameterError = checklistErrors.find(
    (error) => error.code === 'E225' && error.id === task.id,
  );

  // Note - E246 = 'TASK_BULK_VERIFICATION_INVALID_MAPPED_PARAMETERS',
  const bulkVerificationError = checklistErrors.find(
    (error) => error.code === 'E246' && error.id === task.id,
  );

  const TooltipComponent = ({ title, children, onClick, smallSection = false }: any) => (
    <Tooltip title={title} arrow>
      <div className={`${smallSection ? '' : 'task-config-control-item'}`} onClick={onClick}>
        {children}
      </div>
    </Tooltip>
  );

  const processLevelIconComponent = ({
    smallSection = false,
    selected = false,
    tag = '',
    isShowDetails = false,
  }: any) => {
    return (
      <div
        className={`${!smallSection ? 'wrap-container' : 'task-config-control-item small-icon'} ${
          selected ? 'selected-nested-selector' : ''
        } configure-options`}
      >
        {getPrototypeIcon(ProcessLevelOptions[tag])}{' '}
        {!smallSection && (
          <span className="text-container">
            {ProcessLevelOptions[tag]} {isShowDetails && `(${selected})`}
          </span>
        )}
      </div>
    );
  };

  const onClickHandler = (option: string) => {
    switch (option) {
      case 'TIMED':
        dispatch(
          openOverlayAction({
            type: OverlayNames.TIMED_TASK_CONFIG,
            props: { maxPeriod, minPeriod, taskId, timerOperator, isReadOnly },
          }),
        );
        break;
      case 'AUTOMATION':
        dispatch(
          openOverlayAction({
            type: OverlayNames.CONFIGURE_ACTIONS,
            props: {
              checklistId: data?.id,
              isReadOnly,
            },
          }),
        );
        break;
      case 'RECURRENCE':
        dispatch(
          openOverlayAction({
            type: OverlayNames.TASK_RECURRENCE_MODAL,
            props: {
              task,
              isReadOnly,
            },
          }),
        );
        break;
      case 'SCHEDULE':
        dispatch(
          openOverlayAction({
            type: OverlayNames.SCHEDULE_TASK_MODAL,
            props: {
              task,
              isReadOnly,
            },
          }),
        );
        break;
      case 'EXECUTOR_LOCK':
        dispatch(
          openOverlayAction({
            type: OverlayNames.TASK_EXECUTOR_LOCK,
            props: {
              taskId,
              isReadOnly,
              hasExecutorLock,
            },
          }),
        );
        break;
      case 'INTERLOCKS':
        dispatch(
          openOverlayAction({
            type: OverlayNames.CONFIGURE_TASK_CONDITIONS,
            props: {
              checklistId: data?.id,
              isReadOnly,
            },
          }),
        );
        break;
      case 'SOLO_TASK':
        if (!isReadOnly) {
          if (soloTask) {
            dispatch(removeSoloTaskLock(taskId));
          } else {
            dispatch(addSoloTaskLock(taskId));
          }
        }
        break;
      case 'DEPENDENCIES':
        if (!isReadOnly) {
          dispatch(
            openOverlayAction({
              type: OverlayNames.TASK_DEPENDENCY_MODAL,
              props: {
                taskId,
                taskName: `Task ${stageIndex + 1}.${index + 1}`,
                hasPrerequisites,
              },
            }),
          );
        } else {
          dispatch(
            openOverlayAction({
              type: OverlayNames.VIEW_TASK_DEPENDENCY_MODAL,
              props: {
                taskId,
                taskName: `Task ${stageIndex + 1}.${index + 1}`,
                hasPrerequisites,
              },
            }),
          );
        }
        break;
      case ProcessConfigurations.BULK_VERIFICATION:
        if (!isReadOnly) {
          if (hasBulkVerification) {
            dispatch(removeBulkVerification(taskId));
          } else {
            dispatch(addBulkVerification(taskId));
          }
        }
        break;
      default:
        break;
    }
  };

  const copyTaskDetails = () => {
    dispatch(
      copyEntities({
        elementId: taskId,
        type: EntityType.TASK,
        checklistId: data?.id,
        stageId: activeStageId,
      }),
    );
  };

  if (activeStageId) {
    const taskParameters = parameterOrderInTaskInStage[activeStageId][taskId];
    const hasMedias = !!medias.length;

    const anyParameterHasError = useMemo(() => {
      return taskParameters.some((parameterId) => !!listById[parameterId]?.errors?.length);
    }, [taskParameters, listById]);

    return (
      <TaskCardWrapper
        isActive={isActive}
        onClick={() => {
          if (activeTaskId !== taskId) {
            dispatch(setActiveTask(taskId));
          }
        }}
        isReadOnly={isReadOnly}
        hasError={!!archiveParameterError?.code || anyParameterHasError}
      >
        <div className="task-header">
          {!isReadOnly && (
            <div className="order-control">
              <ArrowDropUp
                className="icon"
                fontSize="small"
                onClick={(event) => {
                  event.stopPropagation();
                  if (!isFirstTask) {
                    dispatch(
                      reOrderTask({
                        from: index,
                        to: index - 1,
                        id: taskId,
                        activeStageId: activeStageId,
                      }),
                    );
                  }
                }}
              />
              <ArrowDropDown
                className="icon"
                fontSize="small"
                onClick={(event) => {
                  event.stopPropagation();
                  if (!isLastTask) {
                    dispatch(
                      reOrderTask({
                        from: index,
                        to: index + 1,
                        id: taskId,
                        activeStageId: activeStageId,
                      }),
                    );
                  }
                }}
              />
            </div>
          )}

          <div className="task-name">
            Task {stageIndex + 1}.{index + 1}
          </div>
          {!isReadOnly && (
            <div className="task-actions">
              {isFeatureAllowed('copyElement') && (
                <FileCopyOutlined
                  className="icon task-icon"
                  onClick={(event) => {
                    event.stopPropagation();
                    dispatch(
                      openOverlayAction({
                        type: OverlayNames.COPY_ENTITY_MODAL,
                        props: {
                          onPrimary: copyTaskDetails,
                          title: 'Copy Task',
                          body: 'Are you sure you want to copy this Task?',
                        },
                      }),
                    );
                  }}
                />
              )}
              <DeleteOutlined
                className="icon task-icon"
                onClick={(event) => {
                  event.stopPropagation();
                  dispatch(
                    openOverlayAction({
                      type: OverlayNames.CONFIRMATION_MODAL,
                      props: {
                        onPrimary: () => dispatch(deleteTask(taskId)),
                        title: 'Delete Task',
                        body: <DeleteTaskContent taskId={taskId} />,
                      },
                    }),
                  );
                }}
              />
            </div>
          )}
        </div>
        <div className="task-body">
          <div className="task-config">
            {!!archiveParameterError?.code && (
              <div className="task-error-wrapper">
                <ErrorIcon className="task-error-icon" />
                {archiveParameterError.message}
              </div>
            )}

            {bulkVerificationError && (
              <div className="task-error-wrapper">
                <ErrorIcon className="task-error-icon" />
                {bulkVerificationError?.message}
              </div>
            )}

            <Textarea
              defaultValue={name}
              //E210 = 'TASK_NAME_CANNOT_BE_EMPTY',
              error={!task.name && task.errors.find((error) => error.code === 'E210')?.message}
              label={task.type === TaskTypeEnum.SUBPROCESS ? 'Process Name' : 'Name the task'}
              disabled={isReadOnly || task.type === TaskTypeEnum.SUBPROCESS}
              onBlur={(e) => {
                const value = e.target.value;
                dispatch(updateTaskName({ id: taskId, name: value }));
              }}
            />

            <div className="task-config-control">
              <TooltipComponent title={'Timed Condition'} onClick={() => onClickHandler('TIMED')}>
                {processLevelIconComponent({ selected: timed, tag: 'TIMED' })}
              </TooltipComponent>

              <TooltipComponent title={'Media'}>
                <div className={`wrap-container ${hasMedias ? 'selected-nested-selector' : ''}`}>
                  <ImageUploadButton
                    onUploadSuccess={(fileData) => {
                      dispatch(
                        openOverlayAction({
                          type: OverlayNames.TASK_MEDIA,
                          props: {
                            mediaDetails: {
                              ...fileData,
                              name: '',
                              description: '',
                            },
                            taskId: taskId,
                          },
                        }),
                      );
                    }}
                    onUploadError={(error) => {
                      console.error('error in fileUpload :: ', error);
                    }}
                    acceptedTypes={['image/*', '.png', '.jpg', '.jpeg']}
                    icon={() => (
                      <div style={{ marginRight: '8px' }}>
                        <TaskMediaIcon className="icon" />
                      </div>
                    )}
                    disabled={isReadOnly}
                    label={<span className="text-container">Media</span>}
                  />
                </div>
              </TooltipComponent>

              {count > 2 && (
                <TooltipComponent
                  title={'Task Automations'}
                  onClick={() => onClickHandler('AUTOMATION')}
                >
                  {processLevelIconComponent({
                    selected: automations.length,
                    tag: 'AUTOMATION',
                    isShowDetails: automations.length > 0,
                  })}
                </TooltipComponent>
              )}

              {count > 3 && (
                <TooltipComponent
                  title={'Dependencies'}
                  onClick={() => onClickHandler('DEPENDENCIES')}
                >
                  {processLevelIconComponent({ selected: hasPrerequisites, tag: 'DEPENDENCIES' })}
                </TooltipComponent>
              )}

              {automations.length > 0 && count < 3 && (
                <TooltipComponent
                  title={'Task Automations'}
                  onClick={() => onClickHandler('AUTOMATION')}
                  smallSection={true}
                >
                  {processLevelIconComponent({
                    selected: automations.length,
                    tag: 'AUTOMATION',
                    isShowDetails: true,
                    smallSection: true,
                  })}
                </TooltipComponent>
              )}

              {hasPrerequisites && count < 4 && (
                <TooltipComponent
                  smallSection={true}
                  title={!isReadOnly ? 'Add Dependency' : 'View Dependency'}
                  onClick={() => onClickHandler('DEPENDENCIES')}
                >
                  {processLevelIconComponent({
                    smallSection: true,
                    selected: hasPrerequisites,
                    tag: 'DEPENDENCIES',
                  })}
                </TooltipComponent>
              )}

              {taskConditions > 0 && (
                <TooltipComponent smallSection={true} title={'Parameter Conditions'} arrow>
                  <div
                    className="task-config-control-item small-icon selected-nested-selector"
                    onClick={() => onClickHandler('INTERLOCKS')}
                  >
                    {processLevelIconComponent({
                      selected: taskConditions > 0,
                      tag: 'INTERLOCKS',
                      smallSection: true,
                    })}
                  </div>
                </TooltipComponent>
              )}

              {enableRecurrence && isFeatureAllowed('recurringTask') && (
                <TooltipComponent smallSection={true} title={'Task Recurrence'} arrow>
                  <div
                    className="task-config-control-item small-icon selected-nested-selector"
                    onClick={() => onClickHandler('RECURRENCE')}
                  >
                    {processLevelIconComponent({
                      selected: enableRecurrence,
                      tag: 'RECURRENCE',
                      smallSection: true,
                    })}
                  </div>
                </TooltipComponent>
              )}

              {enableScheduling && (
                <TooltipComponent smallSection={true} title={'Task Scheduling'} arrow>
                  <div
                    className="task-config-control-item small-icon selected-nested-selector"
                    onClick={() => onClickHandler('SCHEDULE')}
                  >
                    {processLevelIconComponent({
                      selected: enableScheduling,
                      tag: 'SCHEDULE',
                      smallSection: true,
                    })}
                  </div>
                </TooltipComponent>
              )}

              {soloTask && (
                <TooltipComponent smallSection={true} title={'Solo Task Lock'} arrow>
                  <div
                    className="task-config-control-item small-icon selected-nested-selector"
                    onClick={() => onClickHandler('SOLO_TASK')}
                  >
                    {processLevelIconComponent({
                      selected: soloTask,
                      tag: 'SOLO_TASK',
                      smallSection: true,
                    })}
                  </div>
                </TooltipComponent>
              )}

              {hasExecutorLock && (
                <TooltipComponent smallSection={true} title={'Task Executor Lock'} arrow>
                  <div
                    className="task-config-control-item small-icon selected-nested-selector"
                    onClick={() => onClickHandler('EXECUTOR_LOCK')}
                  >
                    {processLevelIconComponent({
                      selected: hasExecutorLock,
                      tag: 'EXECUTOR_LOCK',
                      smallSection: true,
                    })}
                  </div>
                </TooltipComponent>
              )}

              {hasBulkVerification && (
                <TooltipComponent smallSection={true} title={'Bulk Verification'} arrow>
                  <div
                    className="task-config-control-item small-icon selected-nested-selector"
                    onClick={() => onClickHandler(ProcessConfigurations.BULK_VERIFICATION)}
                  >
                    {processLevelIconComponent({
                      selected: hasBulkVerification,
                      tag: ProcessConfigurations.BULK_VERIFICATION,
                      smallSection: true,
                    })}
                  </div>
                </TooltipComponent>
              )}

              <div className="accordian-icon">
                <NestedSelect
                  id="process-configuration"
                  items={{
                    ...(count < 3 && {
                      AUTOMATION: {
                        label: processLevelIconComponent({
                          selected: automations.length,
                          tag: 'AUTOMATION',
                          isShowDetails: automations.length > 0,
                        }),
                      },
                    }),
                    ...(count < 4 && {
                      DEPENDENCIES: {
                        label: processLevelIconComponent({
                          selected: hasPrerequisites,
                          tag: 'DEPENDENCIES',
                        }),
                      },
                    }),
                    INTERLOCKS: {
                      label: processLevelIconComponent({
                        selected: taskConditions,
                        tag: 'INTERLOCKS',
                        isShowDetails: taskConditions > 0,
                      }),
                    },
                    ...(isFeatureAllowed('recurringTask') && {
                      RECURRENCE: {
                        label: processLevelIconComponent({
                          selected: enableRecurrence,
                          tag: 'RECURRENCE',
                        }),
                      },
                    }),
                    SCHEDULE: {
                      label: processLevelIconComponent({
                        selected: enableScheduling,
                        tag: 'SCHEDULE',
                      }),
                    },
                    SOLO_TASK: {
                      label: processLevelIconComponent({ selected: soloTask, tag: 'SOLO_TASK' }),
                    },
                    EXECUTOR_LOCK: {
                      label: processLevelIconComponent({
                        selected: hasExecutorLock,
                        tag: 'EXECUTOR_LOCK',
                      }),
                    },
                    BULK_VERIFICATION: {
                      label: processLevelIconComponent({
                        selected: hasBulkVerification,
                        tag: ProcessConfigurations.BULK_VERIFICATION,
                      }),
                    },
                  }}
                  onChildChange={(option) => onClickHandler(option.value)}
                  label={() => (
                    <div className="small-icon">
                      <ArrowDropDownIcon />
                    </div>
                  )}
                />
              </div>
            </div>
          </div>

          {hasMedias && (
            <ImageGallery
              medias={medias}
              onClickHandler={(media) => {
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.TASK_MEDIA,
                    props: {
                      taskId,
                      mediaDetails: media,
                      disableNameInput: false,
                      disableDescInput: false,
                    },
                  }),
                );
              }}
            />
          )}

          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
            modifiers={[restrictToVerticalAxis]}
          >
            <SortableContext items={taskParameters} strategy={verticalListSortingStrategy}>
              <div className="parameter-list">
                {taskParameters?.map((parameterId, index) => {
                  const parameter = listById[parameterId];
                  return (
                    <ParameterTaskView
                      parameter={parameter}
                      key={`${parameterId}-${index}`}
                      taskId={taskId}
                      isReadOnly={isReadOnly}
                    />
                  );
                })}
              </div>
            </SortableContext>
          </DndContext>
        </div>

        {noParameterError ? <div className="task-error">{noParameterError?.message}</div> : null}
        {!isReadOnly && (
          <div className="task-footer">
            <NestedSelect
              id="add-parameter-selector"
              width="100%"
              label={AddActivity}
              items={{
                parameters: {
                  label: 'Parameters',
                  items: {
                    'add-new-parameter': {
                      label: 'Add New',
                    },
                    'existing-parameter': {
                      label: 'Choose from Existing',
                      fetchItems: async (pageNumber?: number, query = '') => {
                        if (typeof pageNumber === 'number') {
                          try {
                            const { data: resData, pageable }: ResponseObj<any[]> = await request(
                              'GET',
                              apiGetParameters(data!.id),
                              {
                                params: {
                                  page: pageNumber,
                                  sort: 'createdAt,desc',
                                  size: DEFAULT_PAGE_SIZE,
                                  filters: {
                                    op: FilterOperators.AND,
                                    fields: [
                                      {
                                        field: 'targetEntityType',
                                        op: FilterOperators.EQ,
                                        values: [TargetEntityType.UNMAPPED],
                                      },
                                      {
                                        field: 'archived',
                                        op: FilterOperators.EQ,
                                        values: [false],
                                      },
                                      ...(query
                                        ? [
                                            {
                                              field: 'label',
                                              op: FilterOperators.LIKE,
                                              values: [query],
                                            },
                                          ]
                                        : []),
                                    ],
                                  },
                                },
                              },
                            );
                            if (resData && pageable) {
                              return {
                                options: resData.map((item) => ({
                                  ...item,
                                  value: item.id,
                                })),
                                pageable,
                              };
                            }
                          } catch (e) {
                            console.error('Error while fetching existing unmapped parameters', e);
                          }
                        }
                        return {
                          options: [],
                        };
                      },
                    },
                  },
                },
                instructions: {
                  label: 'Instruction',
                  items: {
                    'add-text': {
                      label: 'Add Text',
                    },
                    'add-material': {
                      label: 'Add Material',
                    },
                  },
                },
                checklist: {
                  label: 'Checklist',
                },
              }}
              popOutProps={{ filterOption: () => true }}
              onChildChange={onChildChange}
            />
          </div>
        )}
        {isDrawerOpen && <DynamicTaskDrawer onCloseDrawer={setIsDrawerOpen} />}
      </TaskCardWrapper>
    );
  }
  return null;
};

export default TaskCard;
