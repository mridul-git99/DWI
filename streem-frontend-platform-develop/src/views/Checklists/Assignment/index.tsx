import { fetchComposerData, resetComposer } from '#PrototypeComposer/actions';
import { Checklist } from '#PrototypeComposer/checklist.types';
import { Button, Checkbox, LoadingContainer } from '#components';
import checkPermission from '#services/uiPermissions';
import { User } from '#services/users';
import { useTypedSelector } from '#store';
import { apiGetAssignedUsersToChecklist } from '#utils/apiUrls';
import { ResponseObj } from '#utils/globalTypes';
import { request } from '#utils/request';
import { Wrapper } from '#views/Jobs/Assignment';
import { RouteComponentProps } from '@reach/router';
import { isEmpty, keyBy } from 'lodash';
import React, { FC, useEffect, useReducer, useState } from 'react';
import { useDispatch } from 'react-redux';
import { AssignTask } from '../UserGroups/AssignTask';
import Section from './Section';

type Props = RouteComponentProps<{ id: string }>;
type ReducerState = {
  selection: Record<string, Record<string, boolean>>;
  isAllTaskSelected: boolean;
  isNoTaskSelected: boolean;
  selectedTasks: string[];
  totalTasks: number;
  isAllTaskSelectedByStageId: Record<string, boolean>;
  isNoTaskSelectedByStageId: Record<string, boolean>;
};

type AllowedUser = Pick<User, 'id' | 'lastName' | 'employeeId' | 'firstName'> & {
  taskIds: string[];
};

const reducer = (state: ReducerState, action: any): ReducerState => {
  switch (action.type) {
    case 'SET_INITIAL_STATE':
      state = {
        totalTasks: 0,
        selection: {},
        isAllTaskSelected: false,
        isNoTaskSelected: true,
        selectedTasks: [],
        isAllTaskSelectedByStageId: {},
        isNoTaskSelectedByStageId: {},
      };
      (action.payload as Checklist).stages.forEach((stage) => {
        stage.tasks.forEach((task) => {
          state.totalTasks++;
          state.selection[stage.id] = {
            ...state.selection[stage.id],
            [task.id]: false,
          };
        });
        state.isAllTaskSelectedByStageId = {
          ...state.isAllTaskSelectedByStageId,
          [stage.id]: false,
        };
        state.isNoTaskSelectedByStageId = {
          ...state.isNoTaskSelectedByStageId,
          [stage.id]: true,
        };
      });
      return { ...state };

    case 'SET_TASK_SELECTED_STATE':
      const { taskIds, stageId, states } = action.payload;
      const triggeredTasksLength = taskIds.length;
      const currentStageLength = Object.keys(state.selection[stageId]).length;
      taskIds.forEach((taskId: string, index: number) => {
        state.selection = {
          ...state.selection,
          [stageId]: {
            ...state.selection[stageId],
            [taskId]: states[index],
          },
        };
        if (states[index]) {
          state.selectedTasks.push(taskId);
          if (currentStageLength === triggeredTasksLength) {
            state.isNoTaskSelectedByStageId[stageId] = false;
            state.isAllTaskSelectedByStageId[stageId] = true;
          }
        } else {
          state.selectedTasks = state.selectedTasks.filter((id) => id !== taskId);
          if (currentStageLength === triggeredTasksLength) {
            state.isAllTaskSelectedByStageId[stageId] = false;
            state.isNoTaskSelectedByStageId[stageId] = true;
          }
        }
      });
      state.totalTasks === state.selectedTasks.length
        ? (state.isAllTaskSelected = true)
        : (state.isAllTaskSelected = false);
      if (!state.selectedTasks.length) {
        state.isNoTaskSelected = true;
      } else {
        state.isNoTaskSelected = false;
      }
      if (triggeredTasksLength !== currentStageLength) {
        state.isAllTaskSelectedByStageId[stageId] = false;
        state.isNoTaskSelectedByStageId[stageId] = false;
        let allSelected = true;
        let noneSelected = true;
        Object.values(state.selection[stageId]).forEach((isSelected) =>
          isSelected ? (noneSelected = false) : (allSelected = false),
        );
        if (allSelected) {
          state.isAllTaskSelectedByStageId[stageId] = true;
        } else if (noneSelected) {
          state.isNoTaskSelectedByStageId[stageId] = true;
        }
      }
      return { ...state };

    case 'SET_ALL_TASK_STATE':
      state.selectedTasks = [];
      (action.payload.data as Checklist).stages.forEach((stage) => {
        stage.tasks.forEach((task) => {
          state.selection[stage.id] = {
            ...state.selection[stage.id],
            [task.id]: action.payload.state,
          };
          state.isAllTaskSelectedByStageId = {
            ...state.isAllTaskSelectedByStageId,
            [stage.id]: action.payload.state,
          };
          state.isNoTaskSelectedByStageId = {
            ...state.isNoTaskSelectedByStageId,
            [stage.id]: !action.payload.state,
          };
          if (action.payload.state) {
            state.selectedTasks.push(task.id);
          }
        });
      });
      state.isAllTaskSelected = action.payload.state;
      state.isNoTaskSelected = !action.payload.state;
      return { ...state };

    default:
      return state;
  }
};

const ChecklistTaskAssignment: FC<Props> = ({ values }: any) => {
  const { id } = values;
  const dispatch = useDispatch();
  const [createJobDrawerVisible, setCreateJobDrawerVisible] = useState(false);
  const {
    prototypeComposer: {
      loading,
      data,
      stages: { listById, listOrder },
    },
  } = useTypedSelector((state) => state);
  const [
    {
      selection,
      totalTasks,
      isAllTaskSelected,
      isNoTaskSelected,
      selectedTasks,
      isAllTaskSelectedByStageId,
      isNoTaskSelectedByStageId,
    },
    localDispatch,
  ] = useReducer(reducer, {
    totalTasks: 0,
    selection: {},
    isAllTaskSelected: false,
    isNoTaskSelected: true,
    selectedTasks: [],
    isAllTaskSelectedByStageId: {},
    isNoTaskSelectedByStageId: {},
  });
  const [refresh, setRefresh] = useState(false);
  const [assignedData, setAssignedData] = useState<any>({});

  useEffect(() => {
    if (data && !isEmpty(data)) {
      localDispatch({ type: 'SET_INITIAL_STATE', payload: data });
    }
  }, [data, refresh]);

  useEffect(() => {
    if (id && totalTasks) {
      (async () => {
        const { data: assignedUsersData }: ResponseObj<AllowedUser[]> = await request(
          'GET',
          apiGetAssignedUsersToChecklist(id),
        );
        setAssignedData(keyBy(assignedUsersData, 'taskId'));
      })();
    }
  }, [totalTasks, refresh]);

  useEffect(() => {
    if (id) {
      dispatch(fetchComposerData({ id }));
    }
    return () => {
      dispatch(resetComposer());
    };
  }, [id]);

  return (
    <LoadingContainer
      loading={loading}
      component={
        <Wrapper>
          <div className="header">
            {checkPermission(['trainedUsers', 'edit']) && (
              <>
                <Checkbox
                  {...(isAllTaskSelected
                    ? { checked: true, partial: false }
                    : isNoTaskSelected
                    ? { checked: false, partial: false }
                    : { checked: false, partial: true })}
                  label="Select All Tasks And Stages"
                  onClick={() => {
                    localDispatch({
                      type: 'SET_ALL_TASK_STATE',
                      payload: {
                        data,
                        state: isAllTaskSelected ? false : isNoTaskSelected,
                      },
                    });
                  }}
                />
                <Button
                  onClick={() => {
                    setCreateJobDrawerVisible(true);
                  }}
                  disabled={isNoTaskSelected}
                >
                  {selectedTasks.length ? `Assign ${selectedTasks.length} Tasks` : 'Assign Tasks'}
                </Button>
              </>
            )}
          </div>

          {listOrder.map((stageId, index) => (
            <Section
              stage={listById[stageId]}
              key={stageId}
              sectionState={selection[stageId]}
              localDispatch={localDispatch}
              isFirst={index === 0}
              isAllTaskSelected={isAllTaskSelectedByStageId[stageId]}
              isNoTaskSelected={isNoTaskSelectedByStageId[stageId]}
              assignedData={assignedData}
            />
          ))}

          {createJobDrawerVisible && (
            <AssignTask
              onCloseDrawer={setCreateJobDrawerVisible}
              checklistId={id}
              selectedTasks={selectedTasks}
              source="checklist"
              renderAssignedUsers={() => setRefresh(!refresh)}
            />
          )}
        </Wrapper>
      }
    />
  );
};

export default ChecklistTaskAssignment;
