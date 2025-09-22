import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { Block } from '@material-ui/icons';
import React, { FC } from 'react';
import styled from 'styled-components';
import { navigateToTaskExecution } from '../utils';

export interface TaskDependencyErrorProps {
  taskDependencyError?: any;
}

const Wrapper = styled.div.attrs({})`
  .modal {
    width: 560px !important;

    .modal-body {
      padding: 0px !important;
      overflow: auto;
      font-size: 14px;
      color: #525252;
      display: flex;
      flex-direction: column;

      .dependencies {
        padding: 24px;
      }

      p {
        margin: 0;
      }

      .task {
        cursor: pointer;
        padding: 4px;
        color: #1d84ff;
      }

      .error {
        display: flex;
        gap: 16px;
        align-items: flex-start;
        background-color: #fff1f1;
        border-left: 4px solid #da1e28;
        padding: 16px;
      }
    }
  }
`;

const TaskDependencyError: FC<CommonOverlayProps<TaskDependencyErrorProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { taskDependencyError },
}) => {
  const jobId = useTypedSelector((state) => state.job.id);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title="Incomplete Dependencies"
        closeModal={closeOverlay}
        showFooter={false}
      >
        <div className="error">
          <Block style={{ color: '#DA1E28' }} />
          <p>
            We're sorry, but this task cannot be started because the following prerequisite task(s)
            have not been completed
          </p>
        </div>
        <div className="dependencies">
          <p style={{ fontWeight: 700 }}>Incomplete Prerequisite Task(s):</p>
          {taskDependencyError.map((error) => {
            return (
              <div key={error.id}>
                {error.tasks.map((task) => {
                  const taskExecutionOrderTree = task.taskExecutions[0].orderTree;
                  const taskExecutionId = task.taskExecutions[0].id;
                  return (
                    <p
                      key={task.id}
                      onClick={() => {
                        navigateToTaskExecution(jobId, taskExecutionId);
                        closeAllOverlays();
                      }}
                      className="task"
                    >
                      - Task {error.orderTree}.{task.orderTree}
                      {taskExecutionOrderTree > 1 ? `.${taskExecutionOrderTree - 1}` : ''} :{' '}
                      {task.name}
                    </p>
                  );
                })}
              </div>
            );
          })}
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default TaskDependencyError;
