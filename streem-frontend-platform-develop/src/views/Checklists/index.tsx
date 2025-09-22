import Composer from '#PrototypeComposer';
import AuditLogs from '#PrototypeComposer/ChecklistAuditLogs';
import { ComposerEntity } from '#PrototypeComposer/types';
import { RouteComponentProps, Router } from '@reach/router';
import React, { FC } from 'react';
import ListView from './ListView';
import NewPrototype from './NewPrototype';
import Assignment from './Assignment';
import JobLogsContainer from './JobLogs';
import JobsListView from '#views/Jobs/ListView';
import Scheduler from './Schedular/index';
import { TrainedUser } from './TrainedUser';

const ChecklistView: FC<RouteComponentProps> = () => (
  <Router>
    <ListView path="/" />
    <JobsListView path="/jobs" />
    <Composer path="/:id" entity={ComposerEntity.CHECKLIST} />
    <AuditLogs path="/:id/activities" values={{ isTrainedUserView: false }} />
    <Assignment path="/:id/assignment" />
    <TrainedUser path="/:id/trained-user" />
    <JobLogsContainer path="/:id/logs" />
    <NewPrototype path="/prototype" />
    <Scheduler path="/:id/scheduler" />
  </Router>
);

export default ChecklistView;
