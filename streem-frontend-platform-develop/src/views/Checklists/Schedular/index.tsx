import { GeneralHeader } from '#components';
import useTabs from '#components/shared/useTabs';
import { ViewWrapper } from '#views/Jobs/ListView/styles';
import React, { FC } from 'react';
import TabContent from './TabContent';

const SchedularListView: FC<any> = ({ location }) => {
  const processData = location?.state?.processFilter;
  const { renderTabHeader, renderTabContent } = useTabs({
    tabs: [
      { label: 'Active', tabContent: TabContent, values: processData },
      { label: 'Deprecated', tabContent: TabContent, values: processData },
    ],
  });
  return (
    <ViewWrapper>
      <GeneralHeader heading={`${processData?.processName} Schedule`} />
      <div className="list-table">
        {renderTabHeader()}
        {renderTabContent()}
      </div>
    </ViewWrapper>
  );
};
export default SchedularListView;
