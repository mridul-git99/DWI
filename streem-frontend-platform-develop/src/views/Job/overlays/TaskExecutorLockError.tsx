import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { Block } from '@material-ui/icons';
import React, { FC } from 'react';
import styled from 'styled-components';
import { navigateToTaskExecution } from '../utils';

export interface TaskExecutorLockErrorProps {
  taskExecutorLockError: any;
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

      .executor-lock-rules {
        padding: 24px;
        display: flex;
        flex-direction: column;
        gap: 16px;

        span {
          cursor: pointer;
          color: #1d84ff;
        }
      }

      p {
        margin: 0;
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

const TaskLink = (props: any) => {
  const { taskName, taskExecutionId, handleClick, index, taskNames } = props;
  return (
    <span
      onClick={() => {
        handleClick(taskExecutionId);
      }}
      style={{ textDecoration: 'underline' }}
    >
      {taskName}
      {index !== undefined && index < taskNames.length - 1 && (
        <span style={{ color: '#525252' }}>, </span>
      )}
    </span>
  );
};

const taskName = (details: {
  stageOrderTree: string;
  taskOrderTree: string;
  taskName: string;
  taskExecutionOrderTree: string;
  taskExecutionId: string;
}) => {
  const { stageOrderTree, taskOrderTree, taskName, taskExecutionOrderTree, taskExecutionId } =
    details;

  const taskExecutionOrder =
    parseInt(taskExecutionOrderTree) > 1 ? `.${parseInt(taskExecutionOrderTree) - 1}` : '';

  const task = `Task ${stageOrderTree}.${taskOrderTree}${taskExecutionOrder} : ${taskName}`;

  return {
    task,
    taskExecutionId,
  };
};

const HasErrorBuilder = (props: any) => {
  const { hasToBeExecutorError, handleClick } = props;

  const isInValidStateError = hasToBeExecutorError['invalidTaskState'];
  const isValidStateError = hasToBeExecutorError['validTaskState'];

  if (isInValidStateError && isInValidStateError.length > 0) {
    const { task, taskExecutionId } = taskName(isInValidStateError[0]);
    return (
      <div>
        This task requires the initiation of{' '}
        <TaskLink taskName={task} taskExecutionId={taskExecutionId} handleClick={handleClick} /> to
        ensure that the executors of that task can be the same as the executor of this task.
      </div>
    );
  } else if (isValidStateError && isValidStateError.length > 0) {
    const { task, taskExecutionId } = taskName(isValidStateError[0]);
    return (
      <div>
        This task has been locked for execution by executors of{' '}
        <TaskLink taskName={task} taskExecutionId={taskExecutionId} handleClick={handleClick} />
      </div>
    );
  } else {
    return null;
  }
};

const CannotErrorBuilder = (props: any) => {
  const { cannotBeExecutorError, handleClick } = props;

  const isInValidStateError = cannotBeExecutorError['invalidTaskState'];
  const isValidStateError = cannotBeExecutorError['validTaskState'];

  if (isInValidStateError && isInValidStateError.length > 0) {
    const taskNames = isInValidStateError.map((err: any) => taskName(err));
    return (
      <div>
        This task requires the completion of Tasks -{' '}
        {taskNames.map((task: any, index: number) => (
          <TaskLink
            key={task.task}
            taskName={task.task}
            taskExecutionId={task.taskExecutionId}
            index={index}
            taskNames={taskNames}
            handleClick={handleClick}
          />
        ))}{' '}
        to ensure that the executors of the listed tasks cannot be the executor of this task.
      </div>
    );
  } else if (isValidStateError && isValidStateError.length > 0) {
    const taskNames = isValidStateError.map((err: any) => taskName(err));
    return (
      <div>
        This task cannot be executed by executors of Tasks -{' '}
        {taskNames.map((task: any, index: number) => (
          <TaskLink
            key={index}
            taskName={task.task}
            taskExecutionId={task.taskExecutionId}
            index={index}
            taskNames={taskNames}
            handleClick={handleClick}
          />
        ))}{' '}
      </div>
    );
  }

  return null;
};

const ErrorDetails = (props: { taskExecutorLockError: any; closeAllOverlays: () => void }) => {
  const { taskExecutorLockError, closeAllOverlays } = props;
  const jobId = useTypedSelector((state) => state.job.id);
  const hasToBeExecutorError = taskExecutorLockError['EQ'];
  const cannotBeExecutorError = taskExecutorLockError['NIN'];

  const handleClick = (taskExecutionId: string) => {
    navigateToTaskExecution(jobId, taskExecutionId);
    closeAllOverlays();
  };

  return (
    <>
      {hasToBeExecutorError && (
        <HasErrorBuilder hasToBeExecutorError={hasToBeExecutorError} handleClick={handleClick} />
      )}
      {cannotBeExecutorError && (
        <CannotErrorBuilder
          cannotBeExecutorError={cannotBeExecutorError}
          handleClick={handleClick}
        />
      )}
    </>
  );
};

const TaskExecutorLockError: FC<CommonOverlayProps<TaskExecutorLockErrorProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { taskExecutorLockError },
}) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title="Error"
        closeModal={closeOverlay}
        showFooter={false}
      >
        <div className="error">
          <Block style={{ color: '#DA1E28' }} fontSize="small" />
          <p>Task initiation blocked due to task executor lock</p>
        </div>
        <div className="executor-lock-rules">
          <ErrorDetails
            taskExecutorLockError={taskExecutorLockError}
            closeAllOverlays={closeAllOverlays}
          />
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default TaskExecutorLockError;
