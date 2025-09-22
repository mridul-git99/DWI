import AccordianIcon from '#assets/svg/accordianIcon.svg';
import Tooltip from '#components/shared/Tooltip';
import { useTypedSelector } from '#store';
import {
  COMPLETED_TASK_STATES,
  ScheduledTaskCondition,
  ScheduledTaskType,
  TaskExecutionType,
} from '#types';
import { apiTaskSchedule } from '#utils/apiUrls';
import { taskStateColor, taskStateText } from '#utils/jobMethods';
import { request } from '#utils/request';
import { getSummary } from '#utils/summaryUtils';
import { jobActions } from '#views/Job/jobStore';
import { navigateToTaskExecution } from '#views/Job/utils';
import { useLocation } from '@reach/router';
import React, { FC, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const TaskDetailCardWrapper = styled.div.attrs({
  className: 'task-detail-card-wrapper',
})<{ backgroundColor?: string; error: boolean }>`
  display: flex;
  align-items: center;
  gap: 50px;
  background-color: #ffffff;
  width: 100%;
  cursor: pointer;
  margin: 4px;
  background-color: ${({ backgroundColor }) => `${backgroundColor}`};

  :hover {
    background-color: #e7f1fd;
  }

  .title {
    white-space: nowrap;
    color: ${({ error }) => `${error ? '#DA1E28' : 'unset'}`};
  }
  .task-outer-container {
    display: flex;
    cursor: pointer;
    .task-accordian-container {
      width: 32px;
      .accordian-image {
        margin: 12px 8px;
        cursor: pointer;
        width: 8px;
      }
      .accordian-image-rotate {
        margin: 12px 8px;
        cursor: pointer;
        transform: rotate(-90deg);
        width: 8px;
      }
    }
  }

  .task-detail-container {
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    justify-content: space-between;
    width: 100%;
    .task-title-container {
      display: flex;
      justify-content: space-between;
      width: 100%;
      .task-top-container {
        display: flex;
        .left-tag {
          width: 8px;
          height: 100%;
        }
        .title {
          white-space: nowrap;
          color: #161616;
          display: flex;
        }
        .task-detail-name-container {
          margin: 8px 0;
        }
        .icon-container {
          margin-left: 10px;

          .nav-icon {
            margin-right: 4px;
          }
        }
      }
    }
  }
`;

const getTaskScheduleSummary = (data: any, tasks: any, stages: any) => {
  const { startDateDuration, condition, type, referencedTaskId } = data;
  const timeSummary = getSummary(startDateDuration);

  if (type === ScheduledTaskType.JOB) {
    return `Task Scheduled to be started ${timeSummary} after Job starts`;
  } else {
    const task = tasks.get(referencedTaskId);
    const stage = stages.get(task.stageId);
    return `Task Scheduled to be started ${timeSummary} after Task ${stage.orderTree}.${
      task.orderTree
    } ${condition === ScheduledTaskCondition.START ? 'starts' : 'completes'}.`;
  }
};

const TaskNavCard: FC<{ task: any; taskNo: number; stageNo: number }> = ({
  task,
  taskNo,
  stageNo,
}) => {
  const dispatch = useDispatch();
  const location = useLocation();
  const { taskExecutions, tasks, stages, id: jobId } = useTypedSelector((state) => state.job);
  const { enableScheduling } = task;
  const params = new URLSearchParams(location.search);
  const activeTaskExecutionId = params.get('taskExecutionId');

  const [accordianStatus, setAccordianStatus] = React.useState<boolean>(true);

  // The first task execution in the task is the master task execution and only master task can be scheduled.
  const masterTaskExecution = taskExecutions.get(task.taskExecutions[0]);

  const { id, type } = masterTaskExecution!;

  const getTaskSchedule = async (taskId: string) => {
    try {
      const { data } = await request('GET', apiTaskSchedule(taskId));
      if (data) {
        const summary = getTaskScheduleSummary(data, tasks, stages);
        dispatch(
          jobActions.updateTaskExecution({
            id: id,
            data: {
              ...masterTaskExecution,
              scheduleTaskSummary: summary,
            },
          }),
        );
      }
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    if (enableScheduling && type === TaskExecutionType.MASTER) {
      getTaskSchedule(task.id);
    }
  }, []);

  return task?.taskExecutions?.map((taskExecutionId: any, taskExecutionIndex: number) => {
    const taskExecution = taskExecutions.get(taskExecutionId);

    const { state, type, errors } = taskExecution || {};

    const isError = !!errors?.length;
    const backgroundColor =
      taskExecutionId === activeTaskExecutionId ? (isError ? '#FFF1F1' : '#e7f1fd') : '#ffffff';
    const nameColor = state in COMPLETED_TASK_STATES ? '#C2C2C2' : '#161616';

    return (
      <TaskDetailCardWrapper
        key={taskExecutionId}
        onClick={() => {
          navigateToTaskExecution(jobId, taskExecutionId);
          dispatch(jobActions.toggleMobileDrawer());
        }}
        backgroundColor={backgroundColor}
        error={isError && taskExecutionId === activeTaskExecutionId}
      >
        {((!accordianStatus && type === TaskExecutionType.MASTER) || accordianStatus) && (
          <div className="task-detail-container">
            <div className="task-title-container">
              <div className="task-top-container">
                <Tooltip title={taskStateText(state)} arrow placement="right">
                  <div
                    className="left-tag"
                    style={{ backgroundColor: taskStateColor(state) }}
                  ></div>
                </Tooltip>
                <div
                  className="task-outer-container"
                  onClick={() => setAccordianStatus(!accordianStatus)}
                >
                  <div className="task-accordian-container">
                    {type === TaskExecutionType.MASTER && task.visibleTaskExecutionsCount > 1 && (
                      <img
                        className={`${
                          accordianStatus ? 'accordian-image' : 'accordian-image-rotate'
                        }`}
                        src={AccordianIcon}
                        alt="accordian-icon"
                      />
                    )}
                  </div>
                </div>
                <div style={{ display: 'flex', flex: 1 }}>
                  <div
                    className="title"
                    style={{
                      margin:
                        type === TaskExecutionType.MASTER ? '8px 8px 8px 0px' : '8px 8px 8px 34px',
                    }}
                  >
                    <div>
                      {type === TaskExecutionType.MASTER
                        ? `${stageNo}.${taskNo}`
                        : `${stageNo}.${taskNo}.${taskExecutionIndex}`}
                    </div>
                  </div>
                  <div
                    className="task-detail-name-container"
                    style={{ marginLeft: type === TaskExecutionType.MASTER ? '0px' : '0px' }}
                  >
                    <div style={{ color: nameColor, fontWeight: 400 }}>{task?.name}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </TaskDetailCardWrapper>
    );
  });
};

export default TaskNavCard;
