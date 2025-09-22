import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { apiExecutorLock } from '#utils/apiUrls';
import { request } from '#utils/request';
import React, { FC, useEffect, useState } from 'react';
import styled from 'styled-components';

export interface TaskExecutorLockDetailsModalProps {
  taskId: string;
}

const Wrapper = styled.div.attrs({})`
  .modal {
    width: 502px !important;

    .modal-body {
      padding: 24px !important;
      overflow: auto;
      display: flex;
      flex-direction: column;
      gap: 16px;
      color: #525252;
      font-size: 14px;

      p {
        margin: 0;
      }

      .container {
        display: flex;
        gap: 6px;
        flex-wrap: wrap;
      }

      .executor-rules {
        display: flex;
        flex-direction: column;
        gap: 4px;
        font-size: 12px;
      }

      .task-badge {
        background-color: #dde1e6;
        color: #161616;
        padding: 2px 4px;
      }
    }

    .modal-footer {
      flex-direction: row;
      justify-content: flex-end;
    }
  }
`;

const formatTaskDetails = (taskDetails: any) => {
  const { stageOrderTree, taskOrderTree, taskName } = taskDetails;
  return `Task ${stageOrderTree}.${taskOrderTree} : ${taskName}`;
};

const TaskExecutorLockDetails: FC<CommonOverlayProps<TaskExecutorLockDetailsModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { taskId },
}) => {
  const [executorList, setExecutorList] = useState<any[]>([]);
  const [nonExecutorList, setNonExecutorList] = useState<any[]>([]);

  const getTaskExecutorLockDetails = async () => {
    const { data } = await request('GET', apiExecutorLock(taskId));

    if (data) {
      const executorList = data.filter((item: any) => item.condition === 'EQ');
      const nonExecutorList = data.filter((item: any) => item.condition === 'NIN');

      setExecutorList(executorList);
      setNonExecutorList(nonExecutorList);
    }
  };

  useEffect(() => {
    getTaskExecutorLockDetails();
  }, []);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title="Task Executor Lock Details"
        closeModal={closeOverlay}
        secondaryText="Close"
        onSecondary={closeOverlay}
        showPrimary={false}
      >
        <p>Execution rules for the task</p>
        <div className="executor-rules">
          <p>HAS to be executor of task</p>
          <div className="container">
            {executorList.length > 0
              ? executorList.map((task) => (
                  <div className="task-badge">{formatTaskDetails(task)}</div>
                ))
              : '--'}
          </div>
        </div>
        <div className="executor-rules">
          <p>CANNOT be executor of tasks</p>
          <div className="container">
            {nonExecutorList.length > 0
              ? nonExecutorList.map((task) => (
                  <div className="task-badge">{formatTaskDetails(task)}</div>
                ))
              : '--'}
          </div>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default TaskExecutorLockDetails;
