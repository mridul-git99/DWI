import { LoadingContainer } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { apiGetUserIsAsssignedToJob } from '#utils/apiUrls';
import { RouteComponentProps, useLocation } from '@reach/router';
import React, { FC, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import JobHeader from './components/Header';
import Task from './components/Task';
import TaskNavigation from './components/TaskNavigation';
import { jobActions } from './jobStore';

const JobWrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: hidden;

  .job-body {
    display: flex;
    flex: 1;
    overflow: hidden;
    flex-direction: row-reverse;
    background-color: #fff;
    @media (min-width: 900px) {
      flex-direction: row;
    }
  }
`;

const Job: FC<{
  isUserAssignedToJob: boolean;
}> = ({ isUserAssignedToJob }) => {
  const location = useLocation();
  const dispatch = useDispatch();
  const params = new URLSearchParams(location.search);
  const taskExecutionId = params.get('taskExecutionId');

  const checklistId = useTypedSelector((state) => state.job.processId);

  useEffect(() => {
    if (checklistId) {
      dispatch(
        jobActions.fetchActionsForProcess({
          checklistId,
        }),
      );
    }
  }, [checklistId]);

  return (
    <JobWrapper data-testid="job-wrapper">
      <JobHeader />
      <div className="job-body">
        <TaskNavigation />
        {taskExecutionId && (
          <Task
            isUserAssignedToJob={isUserAssignedToJob}
            taskExecutionId={taskExecutionId}
            key={taskExecutionId}
          />
        )}
      </div>
    </JobWrapper>
  );
};

const JobContainer: FC<
  RouteComponentProps<{
    id: string;
  }>
> = ({ id }) => {
  const dispatch = useDispatch();
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const initialExecutionId = params.get('taskExecutionId') || '';
  const parameterExecutionId = params.get('parameterExecutionId') || '';

  const loading = useTypedSelector((state) => state.job.loading);
  const isInboxView = useTypedSelector((state) => state.job.isInboxView);

  const { status, fetchedData, reset } = createFetchList(
    apiGetUserIsAsssignedToJob(id!),
    {},
    false,
  );

  useEffect(() => {
    if (isInboxView) {
      reset({
        url: apiGetUserIsAsssignedToJob(id!),
      });
    }
  }, [isInboxView, id]);

  useEffect(() => {
    dispatch(jobActions.pollJob({ id: id!, initialExecutionId, parameterExecutionId }));
    return () => {
      dispatch(jobActions.stopPollJob());
      dispatch(jobActions.reset());
    };
  }, []);

  return (
    <LoadingContainer
      loading={loading || ['loading', ...(isInboxView ? ['init'] : [])].includes(status)}
      component={<Job key={id} isUserAssignedToJob={!!fetchedData?.userAssigned} />}
    />
  );
};

export default JobContainer;
