import React, { FC } from 'react';
import ListView from './ListView';
import { Router } from '@reach/router';
import { ReportsViewProps } from './types';
import ReportView from './ListView/ReportView';

const ReportsView: FC<ReportsViewProps> = () => (
  <Router>
    <ListView path="/" />
    <ReportView path="/:id" />
  </Router>
);

export default ReportsView;
