import useTabs, { Tab } from '#components/shared/useTabs';
import checkPermission from '#services/uiPermissions';
import { UsersListType } from '#store/users/types';
import { ViewWrapper } from '#views/Jobs/ListView/styles';
import React, { FC } from 'react';
import TabContent from './TabContent';
import { UserGroups } from './UserGroups/index';
import SessionActivity from './SessionActivity/index';
import { ListViewProps } from './types';
import { GeneralHeader } from '#components';

const ListView: FC<ListViewProps> = () => {
  const shownTabs: Tab[] = [];
  if (checkPermission(['usersAndAccess', 'activeUsers']))
    shownTabs.push({
      id: 0,
      label: `User`,
      tabContent: TabContent,
      values: [UsersListType.ACTIVE],
    });

  if (checkPermission(['usersAndAccess', 'activeUsers']))
    shownTabs.push({
      label: `User Groups`,
      tabContent: UserGroups,
      id: 1,
    });

  if (checkPermission(['usersAndAccess', 'sessionActivity']))
    shownTabs.push({
      label: 'Session Activity',
      tabContent: SessionActivity,
      id: 2,
    });

  const { renderTabContent, renderTabHeader } = useTabs({
    tabs: shownTabs,
    useTabIndexFromQuery: true,
  });

  return (
    <ViewWrapper>
      <GeneralHeader heading="Users" subHeading="Add, Remove or Edit Users" />

      <div className="list-table">
        {renderTabHeader()}
        {renderTabContent()}
      </div>
    </ViewWrapper>
  );
};

export default ListView;
