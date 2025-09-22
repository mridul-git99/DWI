import useTabs from '#components/shared/useTabs';
import { useTypedSelector } from '#store';
import { ViewWrapper } from '#views/Jobs/ListView/styles';
import React, { FC } from 'react';
import ObjectTypeList from './ObjectTypes/ObjectTypeList';
import { GeneralHeader } from '#components';

const OntologyContent: FC<{ path: string }> = () => {
  const { selectedUseCase } = useTypedSelector((state) => state.auth);
  const { renderTabHeader, renderTabContent } = useTabs({
    tabs: [
      {
        label: 'Object Types',
        values: {
          rootPath: 'object-types',
        },
        tabContent: ObjectTypeList,
      },
    ],
  });

  return (
    <ViewWrapper>
      <GeneralHeader heading={`${selectedUseCase?.label} - Ontology`} />
      <div className="list-table">
        {renderTabHeader()}
        {renderTabContent()}
      </div>
    </ViewWrapper>
  );
};

export default OntologyContent;
