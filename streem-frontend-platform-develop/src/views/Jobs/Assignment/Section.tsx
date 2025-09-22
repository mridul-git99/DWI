import { AssigneeList, Checkbox } from '#components';
import { COMPLETED_TASK_STATES, IN_PROGRESS_TASK_STATES, Stage, Task } from '#types';
import { ArrowDropDown, ArrowRight } from '@material-ui/icons';
import React, { Dispatch, FC, useState } from 'react';
import styled from 'styled-components';

type Props = {
  stage: Stage;
  tasks: Map<string, Task>;
  sectionState: Record<string, [boolean, string]>;
  localDispatch: Dispatch<any>;
  isFirst: boolean;
};

export const AssignmentSectionWrapper = styled.div.attrs({
  className: 'section',
})`
  display: flex;
  flex-direction: column;
  margin-bottom: 1px;

  .checkbox-input {
    label.container {
      color: #333333;
      font-weight: bold;
    }
  }

  .pill {
    border: 1px solid transparent;
    border-radius: 4px;
    font-size: 12px;
    margin-left: 16px;
    padding: 2px 4px;

    &.partial {
      background-color: rgba(247, 181, 0, 0.2);
      border-color: #f7b500;
      color: #dba613;
    }

    &.assigned,
    &.completed {
      background-color: #e1fec0;
      border-color: #5aa700;
      color: #5aa700;
    }

    &.unassigned {
      background-color: #ffebeb;
      border-color: #cc5656;
      color: #cc5656;
    }
  }

  .section {
    &-header {
      align-items: center;
      background-color: #eeeeee;
      display: flex;
      justify-content: space-between;
      padding: 10px 16px;

      .checkbox-input {
        margin-right: auto;
      }

      .icon-wrapper {
        align-items: center;
        background-color: #dadada;
        border-radius: 50%;
        display: flex;
        margin-right: 8px;
        padding: 2px;
      }

      .icon {
        color: #1d84ff;
        font-size: 20px;
        margin-left: 16px;
      }

      .toggle-section {
        color: #333333;
        font-size: 20px;
        margin-left: 0;
      }
    }

    &-body {
      display: flex;
      flex-direction: column;

      &-item {
        align-items: center;
        background-color: #ffffff;
        border-bottom: 1px solid #eeeeee;
        display: flex;
        padding: 10px 16px;

        > span {
          margin-left: 32px;
          margin-right: auto;
        }

        &.disabled {
          opacity: 0.5;
          pointer-events: none;
        }

        :last-child {
          border-bottom: none;
        }

        .checkbox-input {
          margin-left: 32px;
          margin-right: auto;

          label.container {
            font-weight: normal;
          }
        }

        .icon {
          color: #1d84ff;
          font-size: 20px;
          margin-left: 16px;
        }
      }
    }
  }
`;

const Section: FC<Props> = ({ stage, tasks, sectionState = {}, localDispatch, isFirst }) => {
  const [isOpen, toggleIsOpen] = useState(isFirst);

  const isAllTaskAssigned = stage.tasks
    .map((taskId: string) => {
      const task = tasks?.get(taskId);
      const taskExecution = task?.taskExecutions[0];
      return !!(taskExecution?.assignees?.length || taskExecution?.userGroupAssignees?.length);
    })
    .every(Boolean);

  const isNoTaskAssigned = stage.tasks
    .map((taskId) => {
      const task = tasks?.get(taskId);
      const taskExecution = task?.taskExecutions[0];
      return !!(taskExecution?.assignees?.length && taskExecution?.userGroupAssignees?.length);
    })
    .every((val) => val === false);

  const isAllTaskSelected = Object.values(sectionState).every((val) => val[0] === true);
  const isNoTaskSelected = Object.values(sectionState).every((val) => val[0] === false);

  return (
    <AssignmentSectionWrapper>
      <div className="section-header">
        <div className="icon-wrapper" onClick={() => toggleIsOpen((val) => !val)}>
          {isOpen ? (
            <ArrowDropDown className="icon toggle-section" />
          ) : (
            <ArrowRight className="icon toggle-section" />
          )}
        </div>

        <Checkbox
          {...(isAllTaskSelected
            ? { checked: true, partial: false }
            : isNoTaskSelected
            ? { checked: false, partial: false }
            : { checked: false, partial: true })}
          label={
            <div>
              <span style={{ fontWeight: 'bold' }}>Stage {stage.orderTree}</span> {stage.name}
            </div>
          }
          onClick={() => {
            localDispatch({
              type: 'SET_TASK_SELECTED_STATE',
              payload: {
                stageId: stage.id,
                taskExecutionIds: stage.tasks.reduce<string[]>((acc, taskId) => {
                  const task = tasks?.get(taskId);
                  const taskExecution = task?.taskExecutions[0];
                  const isSoloTaskEnabled = task?.soloTask;
                  const isTaskCompletedOrInProgressAndSoloEnabled =
                    taskExecution?.state in COMPLETED_TASK_STATES ||
                    (taskExecution?.state in IN_PROGRESS_TASK_STATES && isSoloTaskEnabled);

                  if (isTaskCompletedOrInProgressAndSoloEnabled) return acc;
                  return [...acc, taskExecution.id];
                }, []),

                states: stage.tasks.reduce<string[]>((acc, taskId) => {
                  const task = tasks?.get(taskId);
                  const taskExecution = task?.taskExecutions[0];
                  if (taskExecution?.state in COMPLETED_TASK_STATES) return acc;

                  return [...acc, isAllTaskSelected ? false : isNoTaskSelected ? true : false];
                }, []),
              },
            });
          }}
        />

        <div
          className={`pill ${
            isAllTaskAssigned ? 'assigned' : isNoTaskAssigned ? 'unassigned' : 'partial'
          }`}
        >
          {isAllTaskAssigned ? 'Assigned' : isNoTaskAssigned ? 'Unassigned' : 'Partial Assigned'}
        </div>
      </div>
      {isOpen ? (
        <div className="section-body">
          {stage.tasks.map((taskId) => {
            const task = tasks?.get(taskId);
            const taskExecution = task?.taskExecutions[0];
            const isTaskCompleted = taskExecution?.state in COMPLETED_TASK_STATES;
            const isSoloTaskEnabled = task?.soloTask;
            const isTaskCompletedOrInProgressAndSoloEnabled =
              taskExecution?.state in COMPLETED_TASK_STATES ||
              (taskExecution?.state in IN_PROGRESS_TASK_STATES && isSoloTaskEnabled);

            return (
              <div className="section-body-item" key={task?.id}>
                <Checkbox
                  disabled={isTaskCompletedOrInProgressAndSoloEnabled}
                  checked={(sectionState[taskExecution?.id] ?? [])[0] ?? false}
                  label={`Task ${stage.orderTree}.${task?.orderTree} : ${task?.name}`}
                  onClick={() => {
                    localDispatch({
                      type: 'SET_TASK_SELECTED_STATE',
                      payload: {
                        stageId: stage.id,
                        taskExecutionIds: [taskExecution?.id],
                        states: [!sectionState[taskExecution?.id][0]],
                      },
                    });
                  }}
                />

                {!!taskExecution?.assignees?.length && (
                  <AssigneeList users={taskExecution?.assignees} />
                )}

                {!!taskExecution?.userGroupAssignees?.length && (
                  <AssigneeList users={taskExecution?.userGroupAssignees} isGroup={true} />
                )}

                {taskExecution?.assignees?.length === 0 &&
                  taskExecution?.userGroupAssignees?.length === 0 && (
                    <div className="pill unassigned">Unassigned</div>
                  )}

                {isTaskCompleted ? <div className="pill completed">Task Complete</div> : null}
              </div>
            );
          })}
        </div>
      ) : null}
    </AssignmentSectionWrapper>
  );
};

export default Section;
