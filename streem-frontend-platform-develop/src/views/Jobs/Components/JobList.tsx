import React, { FC } from 'react';
import styled from 'styled-components';
import JobCard from './JobCard';
import { Job } from '../ListView/types';

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: auto;
  gap: 16px;

  .job-list-empty {
    margin: auto;
    color: #bbbbbb;
    line-height: 1.5;
  }
`;

const JobList: FC<{
  jobs: Job[];
  view: string;
  label?: string;
  onSetDate?: (jobId: string) => void;
  setSelectedJob: React.Dispatch<React.SetStateAction<Job | undefined>>;
}> = ({ jobs, view, label, onSetDate, setSelectedJob }) => {
  return (
    <Wrapper className="job-list">
      {jobs.length > 0 ? (
        jobs.map((job) => (
          <JobCard
            job={job}
            view={view}
            label={label}
            onSetDate={onSetDate}
            setSelectedJob={setSelectedJob}
          />
        ))
      ) : (
        <div className="job-list-empty">No Jobs Found</div>
      )}
    </Wrapper>
  );
};

export default JobList;
