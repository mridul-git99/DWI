import { ComposerEntity } from '#PrototypeComposer/types';
import { Header, NavigationMenu, WorkArea } from '#components';
import { DashboardLayout } from '#components/Layouts';
import { useTypedSelector } from '#store';
import { toggleIsDrawerOpen } from '#store/extras/action';
import { fetch } from '#store/properties/actions';
import {
  ChecklistView,
  InboxView,
  JobsView,
  OntologyView,
  QuicksightView,
  ReportsView,
  UserAccessView,
  ScheduleCalendar,
} from '#views';
import { RouteComponentProps, Router } from '@reach/router';
import React, { FC, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { ErrorBoundary } from '../../components/ErrorBoundary';
import NotificationBanner from '../../components/NotificationBanner';

const NavigationDrawer = styled.div<{ open: boolean }>`
  flex: unset;
  overflow: hidden;
  grid-area: 'nav-menu';
  display: flex;
  flex: 1;
  flex-direction: column;
  background: #fff;
  z-index: 12;
  box-shadow: 1px 0px 4px rgba(0, 0, 0, 0.08);
  transition: ${({ open }) =>
    open
      ? 'width 225ms cubic-bezier(0.0, 0, 0.2, 1) 0ms'
      : 'width 195ms cubic-bezier(0.4, 0, 0.6, 1) 0ms'};
  @media (max-width: 900px) {
    width: ${({ open }) => (open ? '240px' : '0px')};
    position: absolute;
    left: 0;
    bottom: 0;
    top: 48px;
  }
  width: ${({ open }) => (open ? '240px' : '48px')};
  .navigation-content {
    flex: 1;
    display: flex;
    overflow-y: auto;
  }
`;

const ClickAwayWrapper = styled.div<{ open: boolean }>`
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 11;
  display: none;
  width: ${({ open }) => (open ? '100dvw' : '0')};
  transition: ${({ open }) =>
    open
      ? 'width 90ms cubic-bezier(0.0, 0, 0.2, 1) 0ms'
      : 'width 90ms cubic-bezier(0.4, 0, 0.6, 1) 0ms'};
  @media (max-width: 900px) {
    display: block;
    margin-top: 48px;
  }
`;

const UseCaseSelectionView: FC<RouteComponentProps> = () => {
  const dispatch = useDispatch();
  const {
    auth: { selectedUseCase },
    extras: { isDrawerOpen },
  } = useTypedSelector((state) => state);

  useEffect(() => {
    if (selectedUseCase) {
      dispatch(fetch([ComposerEntity.CHECKLIST], selectedUseCase.id));
    }
  }, [selectedUseCase]);

  return (
    <DashboardLayout>
      <Header />
      <ClickAwayWrapper open={isDrawerOpen} onClick={() => dispatch(toggleIsDrawerOpen())} />
      <NavigationDrawer open={isDrawerOpen}>
        <div className="navigation-content">
          <NavigationMenu />
        </div>
      </NavigationDrawer>
      <WorkArea>
        <ErrorBoundary>
          <NotificationBanner />
          <Router>
            <ChecklistView path="checklists/*" />
            <InboxView path="inbox/*" />
            <JobsView path="jobs/*" />
            <OntologyView path="ontology/*" />
            <UserAccessView path="users/*" />
            <ReportsView path="reports/*" />
            <QuicksightView path="quicksight/*" />
            <ScheduleCalendar path="schedule/*" />
          </Router>
        </ErrorBoundary>
      </WorkArea>
    </DashboardLayout>
  );
};

export default UseCaseSelectionView;
