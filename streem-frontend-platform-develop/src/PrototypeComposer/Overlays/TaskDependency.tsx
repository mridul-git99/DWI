import { updateTask } from '#PrototypeComposer/Tasks/actions';
import { BaseModal, TextInput } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import NestedCheckboxList from '#components/shared/NestedCheckboxList';
import { useTypedSelector } from '#store/helpers';
import { apiTaskDependencies } from '#utils/apiUrls';
import { request } from '#utils/request';
import { getStageTaskOptions } from '#utils/stringUtils';
import { Search } from '@material-ui/icons';
import { debounce, isEqual } from 'lodash';
import React, { FC, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

export interface TaskDependencyModalProps {
  taskId: string;
  taskName: string;
  hasPrerequisites: boolean;
}

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
      gap: 16px;

      p {
        margin: 0;
      }

      span {
        font-weight: 700;
      }

      .container {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-top: 8px;
      }

      .selected-task {
        display: flex;
        gap: 8px;
        padding: 0px 4px;
        background-color: #e0e0e0;
        font-size: 12px;
        cursor: pointer;
      }
    }

    .modal-footer {
      flex-direction: row-reverse !important;
    }
  }
`;

const TaskDependency: FC<CommonOverlayProps<TaskDependencyModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { taskId, taskName, hasPrerequisites },
}) => {
  const dispatch = useDispatch();
  const {
    stages: { listById: stagesListById },
    tasks: { listById: tasksListById, tasksOrderInStage },
  } = useTypedSelector((state) => state.prototypeComposer);

  const task = tasksListById[taskId];

  const taskOptions = useMemo(() => {
    return getStageTaskOptions(stagesListById, tasksListById, tasksOrderInStage, taskId);
  }, [stagesListById, tasksListById, tasksOrderInStage, taskId]);

  const [filteredOptions, setFilteredOptions] = useState(taskOptions);
  const [checkedItems, setCheckedItems] = useState({});
  const [selectedTasks, setSelectedTasks] = useState<Record<string, boolean>>({});
  const preSelectedTasks = useRef<{ [key: string]: boolean }>({});

  const handleSearch = (term: string) => {
    const filtered = taskOptions.flatMap((option) => {
      const stageMatch = option.label.toLowerCase().includes(term.toLowerCase());
      const taskMatches = option.options.filter((task) =>
        task.label.toLowerCase().includes(term.toLowerCase()),
      );

      if (stageMatch) {
        return [
          {
            ...option,
            options: option.options,
          },
        ];
      }

      if (taskMatches.length > 0) {
        return [
          {
            ...option,
            label: option.label,
            options: taskMatches,
          },
        ];
      }

      return [];
    });

    setFilteredOptions(filtered);
  };

  const handleSelectedTasks = (selectedTasksData: any[]) => {
    const updatedCheckedItems: Record<string, boolean> = {};
    const updatedSelectedTasks: Record<string, boolean> = {};

    selectedTasksData.forEach((stage) => {
      stage.tasks.forEach((task) => {
        updatedSelectedTasks[task.id] = true;
      });
    });

    taskOptions.forEach((stage) => {
      const allStageTasksSelected = stage.options.every((task) => updatedSelectedTasks[task.value]);

      updatedCheckedItems[stage.value] = allStageTasksSelected;

      stage.options.forEach((task) => {
        const isChecked = updatedSelectedTasks[task.value];
        updatedCheckedItems[task.value] = isChecked;
      });
    });
    preSelectedTasks.current = { ...updatedSelectedTasks };
    setCheckedItems(updatedCheckedItems);
    setSelectedTasks(updatedSelectedTasks);
  };

  const handleStageCheckboxChange = (stageValue, checked) => {
    const updatedCheckedItems: Record<string, boolean> = { ...checkedItems };
    const updatedSelectedTasks: Record<string, boolean> = { ...selectedTasks };

    filteredOptions.forEach((stage) => {
      if (stage.value === stageValue) {
        stage.options.forEach((task) => {
          if (checked && !updatedSelectedTasks[task.value]) {
            updatedCheckedItems[task.value] = true;
            updatedSelectedTasks[task.value] = true;
          } else if (!checked) {
            updatedCheckedItems[task.value] = false;
            delete updatedSelectedTasks[task.value];
          }
        });
      }
    });

    updatedCheckedItems[stageValue] = checked;
    setCheckedItems(updatedCheckedItems);
    setSelectedTasks(updatedSelectedTasks);
  };

  const handleTaskCheckboxChange = (taskValue, checked) => {
    const updatedCheckedItems: Record<string, boolean> = { ...checkedItems };
    const updatedSelectedTasks: Record<string, boolean> = { ...selectedTasks };

    filteredOptions.forEach((stage) => {
      stage.options.forEach((task) => {
        if (task.value === taskValue) {
          updatedCheckedItems[taskValue] = checked;
          const allTasksChecked = stage.options.every((t) => updatedCheckedItems[t.value]);
          updatedCheckedItems[stage.value] = allTasksChecked;
          if (checked) {
            updatedSelectedTasks[task.value] = true;
          } else {
            delete updatedSelectedTasks[task.value];
          }
        }
      });
    });

    setCheckedItems(updatedCheckedItems);
    setSelectedTasks(updatedSelectedTasks);
  };
  const handleRemoveTask = (taskId: string) => {
    const updatedSelectedTasks: Record<string, boolean> = { ...selectedTasks };
    delete updatedSelectedTasks[taskId];
    setSelectedTasks(updatedSelectedTasks);
    const updatedCheckedItems: Record<string, boolean> = { ...checkedItems };

    updatedCheckedItems[taskId] = false;

    taskOptions.forEach((stage) => {
      const tasksFromSameStage = stage.options;
      const tasksStillSelected = Object.keys(updatedSelectedTasks)?.filter((task) =>
        tasksFromSameStage.some((t) => t.value === task),
      );

      if (tasksStillSelected.length === tasksFromSameStage.length) {
        updatedCheckedItems[stage.value] = true;
      } else if (tasksStillSelected.length === 0) {
        updatedCheckedItems[stage.value] = false;
      } else {
        updatedCheckedItems[stage.value] = false;
        tasksStillSelected.forEach((taskId) => {
          updatedCheckedItems[taskId] = true;
        });
      }
    });

    setCheckedItems(updatedCheckedItems);
  };

  const AddDependencies = async (selectedTasks: Record<string, boolean>) => {
    const { data, errors } = await request('PATCH', apiTaskDependencies(taskId), {
      data: {
        prerequisiteTaskIds: Object.keys(selectedTasks),
      },
    });
    if (data) {
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Task dependencies successfully updated',
        }),
      );
      dispatch(updateTask({ ...task, hasPrerequisites: data?.length > 0 ? true : false }));
    }

    if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: errors[0].message,
        }),
      );
    }
  };

  const fetchDependencies = async () => {
    const { data } = await request('GET', apiTaskDependencies(taskId, 'details'));
    if (data) {
      handleSelectedTasks(data.stages);
    }
  };

  useEffect(() => {
    fetchDependencies();
  }, []);

  const isPrimaryButtonDisabled = useMemo(() => {
    return isEqual(selectedTasks, preSelectedTasks.current);
  }, [selectedTasks, preSelectedTasks.current]);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title="Add Dependency"
        closeModal={closeOverlay}
        primaryText={hasPrerequisites ? 'Update' : 'Add'}
        disabledPrimary={isPrimaryButtonDisabled}
        secondaryText="Cancel"
        onPrimary={() => AddDependencies(selectedTasks)}
        onSecondary={closeOverlay}
      >
        <p>
          Specify what tasks need to be performed before the
          <br /> execution of <span>{taskName}</span>
        </p>
        <TextInput
          afterElementWithoutError
          AfterElement={Search}
          afterElementClass=""
          placeholder="Search for stages/tasks"
          onChange={debounce(({ value }) => handleSearch(value), 500)}
        />
        <NestedCheckboxList
          options={filteredOptions}
          checkedOptions={checkedItems}
          onStageCheckboxChange={handleStageCheckboxChange}
          onTaskCheckboxChange={handleTaskCheckboxChange}
        />
        {Object.keys(selectedTasks).length > 0 ? (
          <div>
            <span>Dependencies added</span>
            <div className="container">
              {Object.keys(selectedTasks).map((taskId) => (
                <div
                  key={taskId}
                  className="selected-task"
                  onClick={() => handleRemoveTask(taskId)}
                >
                  <p>
                    {
                      taskOptions
                        .flatMap((stage) => stage.options)
                        .find((task) => task.value === taskId)?.label
                    }
                  </p>
                  <span>X</span>
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </BaseModal>
    </Wrapper>
  );
};

export default TaskDependency;
