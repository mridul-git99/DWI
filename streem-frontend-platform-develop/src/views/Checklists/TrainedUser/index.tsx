import React, { FC, useMemo } from 'react';
import useTabs from '#components/shared/useTabs';
import { ViewWrapper } from '#views/Jobs/ListView/styles';
import { GeneralHeader } from '#components';
import { TrainedUsers } from '../AssignUsers';
import ChecklistTaskAssignment from '../Assignment';
import AuditLogs from '#PrototypeComposer/ChecklistAuditLogs';

export const TrainedUser: FC<any> = ({ id }: any) => {
  const shownTabs: any[] = useMemo(
    () => [
      {
        id: 1,
        label: `Users`,
        tabContent: TrainedUsers,
        values: { id: id, tab: 'users' },
      },
      {
        id: 2,
        label: `User Groups`,
        tabContent: TrainedUsers,
        values: { id: id, tab: 'groups' },
      },
      {
        id: 3,
        label: `Assign Task`,
        tabContent: ChecklistTaskAssignment,
        values: { id },
      },
      {
        id: 4,
        label: 'Audit Logs',
        tabContent: AuditLogs,
        values: { isTrainedUserView: true, checklistId: id },
      },
    ],
    [id],
  );

  const { renderTabContent, renderTabHeader } = useTabs({
    tabs: shownTabs,
    useTabIndexFromQuery: true,
  });

  return (
    <ViewWrapper>
      <GeneralHeader heading="Trained User" subHeading="Add, Remove or Edit Users" />

      <div className="list-table">
        {renderTabHeader()}
        {renderTabContent()}
      </div>
    </ViewWrapper>
  );
};
