import { LoadingContainer } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { getFullName } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import { jobActions } from '#views/Job/jobStore';
import { startJob, useJobStateToFlags } from '#views/Job/utils';
import { JobStateEnum } from '#views/Jobs/ListView/types';
import React, { FC, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import Footer from './Footer';
import Header from './Header';
import MediaCard from './MediaCard/MediaCard';
import ParameterList from './Parameters';
import { clearRetainedToastIds } from '#store/extras/action';

const TaskWrapper = styled.div.attrs({
  id: 'task-wrapper',
})<{ $isMobileDrawerOpen: boolean }>`
  width: ${({ $isMobileDrawerOpen }) => ($isMobileDrawerOpen ? '0%' : '100%')};
  @media (min-width: 900px) {
    width: 65%;
  }
  transition: all 0.3s;
  background-color: #ffffff;
  border: 1px solid #eeeeee;
  box-shadow: 0 1px 4px 0 rgba(18, 170, 179, 0.08);
  grid-area: task-card;
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-rows: auto auto 1fr auto auto;
  grid-template-areas:
    'task-header'
    'task-media-card'
    'task-body'
    'task-automation'
    'task-footer';

  :hover {
    box-shadow: 0 8px 8px 0 rgba(153, 153, 153, 0.16);
  }

  .task-body {
    grid-area: task-body;
    overflow: auto;
    background: hsl(0, 0%, 96%);
  }

  .task-pause-details {
    display: flex;
    flex-direction: column;
    padding: 14px 16px;
    background-color: #ffffff;
    gap: 8px;
    > div:first-child {
      background-color: #ffedd7;
      color: #ff541e;
      padding: 4px 8px;
      width: fit-content;
    }

    &-reason {
      background-color: #ffffff;
      padding: 4px 0px 4px 8px;
      font-size: 14px;
      color: #161616;
    }
  }

  .task-audit {
    color: #525252;
    font-size: 12px;
    line-height: 16px;
    padding: 14px 16px;
    background-color: white;
  }
`;

const Task: FC<{
  isUserAssignedToJob: boolean;
}> = ({ isUserAssignedToJob }) => {
  const dispatch = useDispatch();

  const taskNavState = useTypedSelector((state) => state.job.taskNavState);
  const jobState = useTypedSelector((state) => state.job.state);
  const isInboxView = useTypedSelector((state) => state.job.isInboxView);
  const id = useTypedSelector((state) => state.job.id);
  const jobExpectedStartDate = useTypedSelector((state) => state.job.expectedStartDate);
  const task = useTypedSelector((state) => state.job.activeTask);
  const forceCwe = useTypedSelector((state) => state.job.forceCwe);
  const code = useTypedSelector((state) => state.job.code);

  const { isTaskStarted, isTaskCompleted, isTaskPaused } = useJobStateToFlags();

  const {
    taskExecution: { correctionEnabled, audit, id: taskExecutionId },
    isTaskAssigned,
  } = task!;

  const handleForceCwe = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.COMPLETE_JOB_WITH_EXCEPTION,
        props: { jobId: id, code },
      }),
    );
  };

  const handleTaskBodyClick = (e: React.MouseEvent<HTMLDivElement, MouseEvent>) => {
    e.stopPropagation();
    // Commenting for the working of date, date-time and file upload parameters
    // e.preventDefault();

    if (forceCwe) {
      handleForceCwe();
    } else if (jobState === JobStateEnum.ASSIGNED && isInboxView && isTaskAssigned) {
      startJob(id!, jobExpectedStartDate!);
    }
    if (
      jobState === JobStateEnum.IN_PROGRESS &&
      (!isTaskStarted || isTaskPaused) &&
      isInboxView &&
      isTaskAssigned
    ) {
      dispatch(
        openOverlayAction({
          type: OverlayNames.START_TASK_ERROR_MODAL,
          props: {
            task,
          },
        }),
      );
    }
  };

  return (
    <TaskWrapper
      $isMobileDrawerOpen={taskNavState.isMobileDrawerOpen}
      data-testid="task-wrapper"
      key={taskExecutionId}
    >
      <Header isUserAssignedToJob={isUserAssignedToJob} />
      <MediaCard medias={task.medias} isTaskActive={true} />
      <div className="task-body" onClick={handleTaskBodyClick}>
        {isTaskStarted && (
          <div className="task-audit" style={{ paddingTop: isTaskPaused ? '0px' : '14px' }}>
            {audit
              ? audit.modifiedBy && (
                  <>
                    Last updated by {getFullName(audit.modifiedBy)}, ID:{' '}
                    {audit.modifiedBy.employeeId} on {formatDateTime({ value: audit.modifiedAt })}
                  </>
                )
              : 'Updating...'}
          </div>
        )}
        <ParameterList
          isTaskStarted={isTaskStarted}
          isTaskCompleted={isTaskCompleted}
          isTaskPaused={isTaskPaused}
          isCorrectingError={!!correctionEnabled}
        />
      </div>
      <Footer />
    </TaskWrapper>
  );
};

const TaskContainer: FC<{
  isUserAssignedToJob: boolean;
  taskExecutionId: string;
}> = ({ isUserAssignedToJob, taskExecutionId }) => {
  const dispatch = useDispatch();
  const loading = useTypedSelector((state) => state.job.activeTask.loading);

  useEffect(() => {
    dispatch(jobActions.stopPollActiveTaskExecution());
    dispatch(jobActions.pollActiveTaskExecution({ taskExecutionId }));

    return () => {
      dispatch(jobActions.stopPollActiveTaskExecution());
      dispatch(jobActions.resetActiveTask());
      dispatch(clearRetainedToastIds());
    };
  }, []);

  return (
    <LoadingContainer
      component={<Task key={taskExecutionId} isUserAssignedToJob={isUserAssignedToJob} />}
      loading={loading}
      style={{ flex: 1 }}
    />
  );
};

export default TaskContainer;
