import Job from '#views/Job';
import AuditLogs from '#views/Job/JobAuditLogs';
import { Router } from '@reach/router';
import React, { FC } from 'react';
import Assignments from './Assignment';
import ListView from './ListView';
import { JobsViewProps } from './types';

const JobsView: FC<JobsViewProps> = () => (
  <Router>
    <ListView path="/" />
    <Job path="/:id" />
    <Assignments path="/:jobId/assignments" />
    <AuditLogs path="/:jobId/activities" />
  </Router>
);

export default JobsView;
