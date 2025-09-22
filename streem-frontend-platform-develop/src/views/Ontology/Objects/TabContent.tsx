import DownloadIcon from '#assets/svg/DownloadIcon.svg';
import FilterIcon from '#assets/svg/FilterIcon.svg';
import { DataTable, LoadingContainer, Pagination } from '#components';
import { useTypedSelector } from '#store';
import {
  clearAuditLogFilters,
  setAuditLogFilters,
  setPdfMetaData,
} from '#store/audit-log-filters/action';
import { openLinkInNewTab } from '#utils';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, EXPORT_LIMIT_LEVELS } from '#utils/constants';
import { FilterField, FilterOperators, InputTypes, fetchDataParams } from '#utils/globalTypes';
import { formatDateTime } from '#utils/timeUtils';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { navigate } from '@reach/router';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { fetchObjectChangeLogs } from '../actions';
import { Constraint } from '../types';
import FiltersDrawer from './Overlays/FilterDrawer';
import { downloadPdf } from '#utils/downloadPdf';
import { apiPrintObjectChangeLogs } from '#utils/apiUrls';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';

const AuditLogsTabWrapper = styled.div`
  display: flex;
  height: 100%;
  justify-content: center;

  .file-links {
    display: flex;
    flex-direction: column;
    a {
      margin-right: 8px;
    }
  }
`;

const AuditLogTabContent: FC = () => {
  const dispatch = useDispatch();
  const [state, setState] = useState<{
    showDrawer: boolean;
    filterFields: FilterField[];
  }>({
    showDrawer: false,
    filterFields: [],
  });
  const { showDrawer, filterFields } = state;
  const {
    objectChangeLogs: { list, listLoading, pageable },
    objects: { active: activeObject },
  } = useTypedSelector((state) => state.ontology);
  const { selectedFacility } = useTypedSelector((state) => state.auth);
  const objectId = activeObject?.id;
  const fetchData = (params: fetchDataParams = {}) => {
    const {
      page = DEFAULT_PAGE_NUMBER,
      size = DEFAULT_PAGE_SIZE,
      filters = filterParser(filterFields),
    } = params;
    if (objectId) {
      dispatch(
        fetchObjectChangeLogs({
          page,
          size,
          filters: {
            op: FilterOperators.AND,
            fields: [
              ...filters,
              {
                field: 'objectId',
                op: FilterOperators.EQ,
                values: [objectId],
              },
              { field: 'facilityId', op: 'EQ', values: [selectedFacility?.id] },
            ],
          },
        }),
      );
    }
  };

  const dataParser = (type: string, data: Record<string, string>[]) => {
    switch (type) {
      case InputTypes.DATE_TIME:
      case InputTypes.DATE:
        return formatDateTime({ value: data?.[0]?.input, type });
      case InputTypes.MULTI_SELECT:
      case InputTypes.SINGLE_SELECT:
      case InputTypes.ONE_TO_MANY:
      case InputTypes.ONE_TO_ONE:
        const valueString = data
          .map((currData: Record<string, string>) => currData?.input)
          .join(', ');
        return valueString.length > 0 ? valueString : '-';
      default:
        return data?.[0]?.input ?? '-';
    }
  };

  const filterParser = (newFilters: any) => {
    const updatedFilters = newFilters.map((currFilter: any) => ({
      ...currFilter,
      values: [typeof currFilter.value === 'object' ? currFilter.value.value : currFilter.value],
    }));
    return updatedFilters.map((currFilter) => {
      if (currFilter?.values?.[0] === 'usageStatus.new') {
        return {
          values: currFilter.op === Constraint.EQ ? [1, 7] : [null],
          op: Constraint.ANY,
          field: currFilter.values?.[0],
        };
      } else {
        return {
          values:
            typeof currFilter.values?.[0] === 'number' ? currFilter.values : currFilter.values,
          op: currFilter.op,
          field: currFilter.field,
        };
      }
    });
  };
  const { filters: auditLogTabFilters } = useTypedSelector((state) => state.auditLogFilters);

  const hasAuditsToExport = (list?.length ?? 0) > 0;
  const exportTooltipText = hasAuditsToExport ? 'Export Audit Logs' : 'No Audit Logs Found';

  const handleDownload = async () => {
    if (pageable.totalElements > EXPORT_LIMIT_LEVELS.MEDIUM) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: `Cannot export ${pageable.totalElements.toLocaleString()} audit logs. Maximum export limit is ${EXPORT_LIMIT_LEVELS.MEDIUM.toLocaleString()} records. Please apply filters to reduce the dataset.`,
        }),
      );
      return;
    }

    const timestamp = Date.now();
    await downloadPdf({
      url: apiPrintObjectChangeLogs(),
      method: 'GET',
      filename: `ObjectAuditLog_${activeObject?.objectType?.id}_${activeObject?.externalId}_${timestamp}`,
      params: { filters: auditLogTabFilters },
    });
  };

  const onApplyFilters = (newFilters: any) => {
    const parsedFilters = filterParser(newFilters);
    setState((prev) => ({
      ...prev,
      filterFields: newFilters,
    }));
    fetchData({ filters: parsedFilters });
  };

  const onSubmit = (data: any, callback: any) => {
    onApplyFilters(data.filters || []);
    callback();
  };

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    dispatch(
      setAuditLogFilters(
        JSON.stringify({
          op: FilterOperators.AND,
          fields: [
            ...filterFields,
            {
              field: 'objectId',
              op: FilterOperators.EQ,
              values: [objectId],
            },
            {
              field: 'facilityId',
              op: FilterOperators.EQ,
              values: [selectedFacility?.id],
            },
          ],
        }),
      ),
    );

    return () => {
      dispatch(clearAuditLogFilters());
    };
  }, [filterFields]);

  return (
    <AuditLogsTabWrapper>
      <LoadingContainer
        loading={listLoading}
        component={
          <TabContentWrapper>
            <div className="before-table-wrapper">
              <div className="filters">
                <div
                  className="icon-filter"
                  onClick={() => {
                    setState((prev) => ({ ...prev, showDrawer: true }));
                  }}
                >
                  <img src={FilterIcon} alt="filter icon" className="icon" />
                  {filterFields?.length > 0 && <span>{`(${filterFields.length})`}</span>}
                </div>
                <div
                  className={`icon-filter${!hasAuditsToExport ? ' disabled' : ''}`}
                  onClick={() => {
                    if (!hasAuditsToExport) return;

                    const getPropertyDisplayName = (id: string) =>
                      (activeObject?.properties || []).find((prop) => prop.externalId === id)
                        ?.displayName;

                    dispatch(
                      setPdfMetaData({
                        objectTypeDisplayName: activeObject?.objectType?.displayName!,
                        objectDisplayName: activeObject?.displayName!,
                        objectExternalId: activeObject?.externalId!,
                        objectDisplayNameLabel: getPropertyDisplayName('displayName'),
                        objectExternalIdLabel: getPropertyDisplayName('externalId'),
                      }),
                    );
                    handleDownload();
                  }}
                  style={{
                    opacity: hasAuditsToExport ? 1 : 0.5,
                  }}
                  title={exportTooltipText}
                >
                  <img src={DownloadIcon} alt="Download icon" className="icon" />
                </div>
              </div>
            </div>
            <DataTable
              columns={[
                {
                  id: 'changedoneto',
                  label: 'Change Done To',
                  minWidth: 152,
                  format: function renderComp(item) {
                    const contentString = item?.entityDisplayName
                      ? item?.entityDisplayName
                      : item?.shortCode
                      ? 'QR Code'
                      : 'Usage Status';
                    return <div key={item?.id}>{contentString}</div>;
                  },
                },
                {
                  id: 'changeto',
                  label: 'Change To',
                  minWidth: 152,
                  format: function renderComp(item) {
                    let contentString;

                    if (item.new) {
                      contentString = dataParser(item?.entityInputType, item?.new);
                    } else if (item.shortCode) {
                      contentString = 'New QR Code';
                    } else {
                      item.usageStatus.new === 1
                        ? (contentString = 'Active')
                        : (contentString = 'Archived');
                    }
                    return <div key={item?.id}>{contentString}</div>;
                  },
                },
                {
                  id: 'changedoneat',
                  label: 'Change Done At',
                  minWidth: 152,
                  format: function renderComp(item) {
                    return (
                      <div key={item?.id}>
                        {item?.modifiedAt
                          ? formatDateTime({ value: item.modifiedAt, type: InputTypes.DATE_TIME })
                          : 'N/A'}
                      </div>
                    );
                  },
                },
                {
                  id: 'changedoneby',
                  label: 'Change Done By',
                  minWidth: 152,
                  format: function renderComp(item) {
                    return (
                      <div key={item?.id}>
                        {`${item?.modifiedBy?.firstName} ${item?.modifiedBy?.lastName} (ID: ${item?.modifiedBy?.employeeId})`}
                      </div>
                    );
                  },
                },
                {
                  id: 'reason',
                  label: 'Reason',
                  minWidth: 152,
                  format: function renderComp(item) {
                    const reason = item?.reason ? (
                      item?.info ? (
                        <div style={{ display: 'flex' }}>
                          <div>
                            {item.reason} {item?.info?.processName}
                            (ID:&nbsp;
                            <span
                              className="primary"
                              onClick={() => {
                                navigate(`/checklists/${item.info.processId}`);
                              }}
                            >
                              {item.info.processCode}
                            </span>
                            ) (JOB ID:&nbsp;
                            <span
                              className="primary"
                              onClick={() => {
                                navigate(`/jobs/${item.info.jobId}`);
                              }}
                            >
                              {item.info.jobCode}
                            </span>
                            )
                          </div>
                        </div>
                      ) : (
                        item.reason
                      )
                    ) : (
                      '-'
                    );

                    return <div key={item?.id}>{reason}</div>;
                  },
                },
              ]}
              rows={list}
              emptyTitle="No Audit Logs Found"
            />
            {showDrawer && (
              <FiltersDrawer setState={setState} filters={filterFields} onSubmit={onSubmit} />
            )}
            <Pagination pageable={pageable} fetchData={fetchData} />
          </TabContentWrapper>
        }
      />
    </AuditLogsTabWrapper>
  );
};

export default AuditLogTabContent;
