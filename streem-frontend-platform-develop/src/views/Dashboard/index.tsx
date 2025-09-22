import React, { FC } from 'react';

import MemoUnderConstruction from '#assets/svg/UnderConstruction';
import { DashboardProps } from './types';

const Dashboard: FC<DashboardProps> = () => {
  return (
    <div>
      <MemoUnderConstruction />
    </div>
  );
};

export default Dashboard;
