import useTabs from '#components/shared/useTabs';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { apiGetObjectTypeJobLogsCustomViews } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, MAX_PAGE_SIZE } from '#utils/constants';
import { FilterOperators } from '#utils/globalTypes';
import React, { FC, useMemo } from 'react';
import JobLogsTabContent from './JobLogsTabContent';
import { TObject } from '#views/Ontology/types';
import { objectJobLogColumns } from '#views/Ontology/utils';
import { LoadingContainer } from '#components';

type TObjectJobLogsProps = {
  customViews: Record<string, any>;
  selectedObject: TObject;
};

const ObjectJobLogs: FC<TObjectJobLogsProps> = ({ customViews, selectedObject }) => {
  const { renderTabHeader, renderTabContent } = useTabs({
    tabs: [
      {
        id: 0,
        label: 'Default',
        tabContent: JobLogsTabContent,
        values: {
          selectedObject,
          columns: objectJobLogColumns,
          id: '0',
        },
        index: 0,
      },
      ...Object.values(customViews).map((view: any, i) => ({
        id: view.id,
        label: view.label,
        tabContent: JobLogsTabContent,
        values: {
          selectedObject,
          columns: view.columns,
          viewFilters: view.filters,
          id: view.id,
        },
        index: i + 1,
      })),
    ],
    indicatorForActiveTab: 'id',
    capitalizeHeader: false,
    showTooltip: true,
  });

  return (
    <div className="list-table">
      {renderTabHeader()}
      {renderTabContent()}
    </div>
  );
};

const ObjectJobLogsContainer: FC = () => {
  const {
    ontology: {
      objects: { active },
    },
    auth: {
      selectedFacility: { id: facilityId = '' },
    },
  } = useTypedSelector((state) => state);

  const urlParams = useMemo(
    () => ({
      page: DEFAULT_PAGE_NUMBER,
      size: MAX_PAGE_SIZE,
      sort: 'createdAt,desc',
      filters: {
        op: FilterOperators.AND,
        fields: [
          { field: 'facilityId', op: FilterOperators.EQ, values: [facilityId] },
          {
            field: 'objectTypeId',
            op: FilterOperators.EQ,
            values: [active?.objectType?.id],
          },
          {
            field: 'archived',
            op: FilterOperators.EQ,
            values: [false],
          },
        ],
      },
    }),
    [],
  );

  const { list, status } = createFetchList(apiGetObjectTypeJobLogsCustomViews(), urlParams);

  return (
    <LoadingContainer
      loading={status === 'loading'}
      component={<ObjectJobLogs customViews={list} selectedObject={active} />}
    />
  );
};

export default ObjectJobLogsContainer;
