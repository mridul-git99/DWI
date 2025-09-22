import { Button, Checkbox, Link as GoBack, LoadingContainer } from '#components';
import { COMPLETED_TASK_STATES, IN_PROGRESS_TASK_STATES } from '#types';
import { apiGetSelectedJob } from '#utils/apiUrls';
import { request } from '#utils/request';
import { AssignTask } from '#views/Checklists/UserGroups/AssignTask';
import React, { FC, useEffect, useMemo, useReducer, useState } from 'react';
import styled from 'styled-components';
import { Job } from '../ListView/types';
import Section from './Section';

export const Wrapper = styled.div`
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;

  .header {
    align-items: center;
    background-color: #ffffff;
    display: flex;
    justify-content: space-between;
    padding-bottom: 16px;
  }
`;

type Props = { jobId: Job['id'] };

type State = Record<string, Record<string, [boolean, string]>>;

const reducer = (state: State, action: any): State => {
  const temp: Record<string, any> = {};

  switch (action.type) {
    case 'SET_INITIAL_STATE':
      action.payload.forEach((task) => {
        temp[task.stageId] = {
          ...temp[task.stageId],
          [task.taskExecutions[0].id]: [false, task.id],
        };
      });
      return { ...state, ...temp };

    case 'SET_TASK_SELECTED_STATE':
      return {
        ...state,
        [action.payload.stageId]: {
          ...state[action.payload.stageId],
          ...action.payload.taskExecutionIds.reduce((acc: any, id: string, index: number) => {
            acc[id] = [action.payload.states[index], state[action.payload.stageId][id][1]];

            return acc;
          }, {}),
        },
      };

    case 'SET_ALL_TASK_STATE':
      action.payload.tasks.forEach((task) => {
        const taskExecution = task.taskExecutions[0];
        const isSoloTaskEnabled = task.soloTask;
        const isTaskCompletedOrInProgress =
          taskExecution.state in COMPLETED_TASK_STATES ||
          (taskExecution.state in IN_PROGRESS_TASK_STATES && isSoloTaskEnabled);

        temp[task.stageId] = {
          ...temp[task.stageId],
          [taskExecution.id]: [isTaskCompletedOrInProgress ? false : action.payload.state, task.id],
        };
      });
      return { ...state, ...temp };

    default:
      return { ...state };
  }
};

const getStageTaskMaps = (checklist: any) => {
  const stages = new Map();
  const tasks = new Map();

  checklist?.stages?.forEach((stage: any) => {
    const _stage = {
      ...stage,
      tasks: [],
    };

    stage?.tasks?.forEach((task: any) => {
      const _task = {
        ...task,
        stageId: stage.id,
      };

      _stage.tasks.push(_task.id);
      tasks.set(task.id, _task);
    });

    stages.set(stage.id, _stage);
  });

  return { stages, tasks };
};

const Assignments: FC<Props> = ({ jobId }) => {
  const [stages, setStages] = useState<any>(new Map());
  const [tasks, setTasks] = useState<any>(new Map());
  const [processId, setProcessId] = useState<string>();
  const [loading, setLoading] = useState<boolean>(true);
  const [createJobDrawerVisible, setCreateJobDrawerVisible] = useState(false);

  const [state, localDispatch] = useReducer(reducer, {});

  const getJobData = async () => {
    setLoading(true);
    const { data } = await request('GET', apiGetSelectedJob(jobId));

    if (data) {
      const { stages, tasks } = getStageTaskMaps(data?.checklist);
      setStages(stages);
      setTasks(tasks);
      setProcessId(data?.checklist?.id);
      localDispatch({ type: 'SET_INITIAL_STATE', payload: tasks });
    }
    setLoading(false);
  };

  useEffect(() => {
    if (jobId) {
      getJobData();
    }
  }, []);

  const { selectedTasks, isAllTaskSelected, isNoTaskSelected } = useMemo(
    () =>
      Object.keys(state).reduce<{
        selectedTasks: [string, string][];
        isAllTaskSelected: boolean;
        isNoTaskSelected: boolean;
      }>(
        (acc, stageId) => {
          Object.entries(state[stageId]).forEach(([taskExecutionId, val]) => {
            if (val[0] === true) {
              acc.selectedTasks.push([taskExecutionId, val[1]]);
              acc.isNoTaskSelected = false;
            } else {
              acc.isAllTaskSelected = false;
            }
          });
          return acc;
        },
        {
          selectedTasks: [],
          isAllTaskSelected: true,
          isNoTaskSelected: true,
        },
      ),
    [state],
  );

  const renderStages = () => {
    const _stages: JSX.Element[] = [];
    stages.forEach((stage, index) => {
      _stages.push(
        <Section
          stage={stage}
          tasks={tasks}
          key={stage.id}
          sectionState={state[stage.id]}
          localDispatch={localDispatch}
          isFirst={!!index}
        />,
      );
    });
    return _stages;
  };

  return (
    <LoadingContainer
      loading={loading}
      component={
        <div style={{ padding: '8px', height: '100%' }}>
          <GoBack label="Return to Job" />
          <Wrapper style={{ height: 'calc(100% - 32px)', padding: '16px 16px 0' }}>
            <div className="header">
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
                      tasks,
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
            </div>
            {renderStages()}
            {createJobDrawerVisible && processId && (
              <AssignTask
                onCloseDrawer={setCreateJobDrawerVisible}
                checklistId={processId}
                selectedTasks={selectedTasks}
                jobId={jobId}
                source="job"
                renderAssignedUsers={getJobData}
              />
            )}
          </Wrapper>
        </div>
      }
    />
  );
};

export default Assignments;
