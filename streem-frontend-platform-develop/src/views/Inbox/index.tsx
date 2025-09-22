import { Router } from '@reach/router';
import React, { FC } from 'react';
import Job from '#views/Job';
import ListView from './ListView';
import { InboxViewProps } from './types';
import VerificationsContent from './ListView/VerificationsContent';
import ApprovalsContent from './ListView/ApprovalsContent';
import CorrectionsContent from './ListView/CorrectionsContent';
import AuditLogs from '#views/Job/JobAuditLogs';

const InboxView: FC<InboxViewProps> = () => (
  <Router>
    <ListView path="/" />
    <ListView path="/jobs" />
    <VerificationsContent path="/verifications" />
    <ApprovalsContent path="/approvals" />
    <CorrectionsContent path="/corrections" />
    <Job path="/:id" />
    <AuditLogs path="/:jobId/activities" />
  </Router>
);

export default InboxView;
