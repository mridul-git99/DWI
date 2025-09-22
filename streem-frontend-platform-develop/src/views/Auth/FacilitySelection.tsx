import { Header, WorkArea } from '#components';
import { DashboardLayout } from '#components/Layouts';
import { RouteComponentProps } from '@reach/router';
import React, { FC } from 'react';
import styled from 'styled-components';

import BaseView from './BaseView';
import { PAGE_NAMES } from './types';

const Wrapper = styled.div`
  display: flex;
  flex: 1;

  .main-layout-view {
    grid-template-areas:
      'header header'
      'workarea workarea';
  }
`;

const FacilitySelectionView: FC<RouteComponentProps> = () => {
  return (
    <Wrapper>
      <DashboardLayout>
        <Header />
        <WorkArea>
          <BaseView pageName={PAGE_NAMES.FACILITY_SELECTION} />
        </WorkArea>
      </DashboardLayout>
    </Wrapper>
  );
};

export default FacilitySelectionView;
