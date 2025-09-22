import { LogType, TriggerTypeEnum } from '#PrototypeComposer/checklist.types';
import DownloadIcon from '#assets/svg/DownloadIcon.svg';
import FilterIcon from '#assets/svg/FilterIcon.svg';
import {
  CustomTag,
  DataTable,
  LoadingContainer,
  NestedSelect,
  Pagination,
  ResourceFilter,
  Select,
  TabContentProps,
} from '#components';
import Tooltip from '#components/shared/Tooltip';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { setAuditLogFilters, setPdfColumns, setPdfMetaData } from '#store/audit-log-filters/action';
import { openLinkInNewTab, transformDataToOptions } from '#utils';
import { apiGetChecklists, apiGetJobLogs, apiGetJobLogsExcel } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, EXPORT_LIMIT_LEVELS } from '#utils/constants';
import { filtersToQueryParams } from '#utils/filtersToQueryParams';
import { FilterField, FilterOperators, InputTypes } from '#utils/globalTypes';
import { logsParser, logsResourceChoicesMapper } from '#utils/parameterUtils';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { capitalize, debounce, filter } from 'lodash';
import React, { FC, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import JobLogsFilterDrawer from '../Overlays/JobLogsFilterDrawer';
import { DataTableColumn } from '#components/shared/DataTable';
import { navigate } from '@reach/router';
import { formatDateTime } from '#utils/timeUtils';
import { downloadPdf } from '#utils/downloadPdf';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';

const JobLogsTabWrapper = styled.div`
  display: flex;
  height: 100%;
  justify-content: center;
  .file-links {
    display: flex;
    flex-direction: column;
    a {
      margin-right: 8px;
    }

    div {
      color: #1d84ff;
      margin-right: 8px;
      cursor: pointer;
    }
  }
`;

interface ColumnsResult {
  columns: DataTableColumn[];
}

export const getFormattedJobLogs = (jobLogColumns: any) => {
  // Filtering out the self and peer verification timestamp columns, peer verification status as we don't want to show them.
  const updatedColumns = jobLogColumns.filter((item: any) => {
    return (
      item.triggerType !== TriggerTypeEnum.PARAMETER_SELF_VERIFIED_AT &&
      item.triggerType !== TriggerTypeEnum.PARAMETER_PEER_VERIFIED_AT &&
      item.triggerType !== TriggerTypeEnum.PARAMETER_PEER_STATUS
    );
  });

  const result = (updatedColumns || []).reduce(
    (acc: ColumnsResult, column: any) => {
      const _id = column.id + column.triggerType;
      const _column = {
        id: _id,
        label: column.displayName,
        pinned: column.pinned,
        minWidth: `${
          (column.displayName.length > 40
            ? column.displayName.length / 3
            : column.displayName.length + 10) + 5
        }ch`,
        format: (row: any) => {
          if (row[column.id + column.triggerType]) {
            if (column.triggerType === TriggerTypeEnum.RESOURCE) {
              const rowValue = row[column.id + column.triggerType];
              const cellValue = Object.values(rowValue.resourceParameters).reduce<any[]>(
                (acc, p: any) => {
                  acc.push(
                    `${p.displayName}: ${p.choices
                      .map((c: any) => `${c.objectDisplayName} (ID: ${c.objectExternalId})`)
                      .join(', ')}`,
                  );
                  return acc;
                },
                [],
              );
              return cellValue.join(',');
            }
            if (column.triggerType === TriggerTypeEnum.JOB_ID) {
              return (
                <span
                  title={row[column.id + column.triggerType].value}
                  className="primary"
                  onClick={() => {
                    navigate(`/jobs/${row[column.id + column.triggerType].jobId}`);
                  }}
                >
                  {row[column.id + column.triggerType].value}
                </span>
              );
            } else if (column.triggerType === TriggerTypeEnum.PARAMETER_SELF_VERIFIED_BY) {
              const selfVerifiedAt =
                row[column.id + TriggerTypeEnum.PARAMETER_SELF_VERIFIED_AT]?.value;
              return (
                <>
                  {row[column.id + column.triggerType].value ? (
                    <span title={row[column.id + column.triggerType].value}>
                      Performed at {formatDateTime({ value: selfVerifiedAt })}, by{' '}
                      {row[column.id + column.triggerType].value}
                    </span>
                  ) : (
                    '-'
                  )}
                </>
              );
            } else if (column.triggerType === TriggerTypeEnum.PARAMETER_PEER_VERIFIED_BY) {
              const peerVerifiedAt =
                row[column.id + TriggerTypeEnum.PARAMETER_PEER_VERIFIED_AT]?.value;
              const peerVerificationStatus =
                row[column.id + TriggerTypeEnum.PARAMETER_PEER_STATUS]?.value;
              return (
                <>
                  {row[column.id + column.triggerType].value ? (
                    <span title={row[column.id + column.triggerType].value}>
                      {capitalize(peerVerificationStatus.toLowerCase())} at{' '}
                      {formatDateTime({ value: peerVerifiedAt })}, by{' '}
                      {row[column.id + column.triggerType].value}
                    </span>
                  ) : (
                    '-'
                  )}
                </>
              );
            }

            if (column.type === LogType.DATE) {
              return formatDateTime({
                value: row[column.id + column.triggerType].value,
                type: InputTypes.DATE,
              });
            } else if (column.type === LogType.DATE_TIME) {
              return formatDateTime({
                value: row[column.id + column.triggerType].value,
                type: InputTypes.DATE_TIME,
              });
            } else if (column.type === LogType.TIME) {
              return formatDateTime({
                value: row[column.id + column.triggerType].value,
                type: InputTypes.TIME,
              });
            } else if (
              column.type === LogType.FILE &&
              row[column.id + column.triggerType]?.medias?.length
            ) {
              return (
                <div className="file-links">
                  {row[column.id + column.triggerType].medias.map(
                    (media: any, index: number, array: any[]) => {
                      return (
                        <CustomTag
                          as={'div'}
                          onClick={() => openLinkInNewTab(`/media?link=${media.link}`)}
                        >
                          <span>
                            {' '}
                            {media.name}
                            {index < array.length - 1 && (
                              <span style={{ color: '#333333' }}>,</span>
                            )}
                          </span>
                        </CustomTag>
                      );
                    },
                  )}
                </div>
              );
            }
            return (
              <span title={row[column.id + column.triggerType].value}>
                {row[column.id + column.triggerType].value || '-'}
              </span>
            );
          }
          return '-';
        },
      };
      acc.columns.push(_column);
      return acc;
    },
    { columns: [] },
  );

  return result;
};

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  sort: 'createdAt,desc',
};

const JobLogsTabContent: FC<TabContentProps> = ({ values }) => {
  const { selectedObject, columns, viewFilters = [], showFilters = true } = values;

  const dispatch = useDispatch();

  const facilityId = useTypedSelector((state) => state.auth.selectedFacility?.id || '');
  const useCaseId = useTypedSelector((state) => state.auth.selectedUseCase?.id || '');

  const assetLogViewFilters = useMemo(() => filtersToQueryParams(viewFilters), [viewFilters]);

  const { columns: jobLogColumns } = useMemo(() => getFormattedJobLogs(columns), [columns]);

  const [filterFields, setFilterFields] = useState<FilterField[]>([
    {
      field: 'logs.triggerType',
      op: FilterOperators.EQ,
      values: [TriggerTypeEnum.RESOURCE_PARAMETER],
    },
    {
      field: 'logs.identifierValue',
      op: FilterOperators.LIKE,
      values: [selectedObject?.id],
    },
    {
      field: 'facilityId',
      op: FilterOperators.EQ,
      values: [facilityId],
    },
  ]);

  const [drawerFilters, setDrawerFilters] = useState<FilterField[]>([]);
  const [showDrawer, setShowDrawer] = useState(false);

  const {
    list: jobLogsList,
    reset: resetJobLogs,
    pagination,
    status,
  } = createFetchList(apiGetJobLogs(), urlParams, false);

  const {
    list: processList,
    reset: resetProcessList,
    status: processlistStatus,
    fetchNext: fetchNextProcessList,
  } = createFetchList(apiGetChecklists(), urlParams, false);

  const onApplyFilters = (data) => {
    const filtersToRemove = ['startedAt', 'createdAt', 'endedAt', 'state'];
    const filters = filtersToQueryParams(data?.filters || []);
    setFilterFields((prev) => [
      ...prev.filter((field) => !filtersToRemove.includes(field.field)),
      ...filters,
    ]);
    setDrawerFilters(data?.filters || []);
    setShowDrawer(false);
  };

  const handleDownload = async () => {
    if (pagination.totalElements > EXPORT_LIMIT_LEVELS.MEDIUM) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: `Cannot export ${pagination.totalElements.toLocaleString()} asset logs. Maximum export limit is ${EXPORT_LIMIT_LEVELS.MEDIUM.toLocaleString()} records. Please apply filters to reduce the dataset.`,
        }),
      );
      return;
    }

    const getPropertyDisplayName = (id: string) =>
      (selectedObject?.properties || []).find((prop: any) => prop.externalId === id)?.displayName;

    const pdfMetaData = {
      objectTypeDisplayName: selectedObject?.objectType?.displayName!,
      objectDisplayName: selectedObject?.displayName!,
      objectExternalId: selectedObject?.externalId!,
      objectDisplayNameLabel: getPropertyDisplayName('displayName'),
      objectExternalIdLabel: getPropertyDisplayName('externalId'),
    };

    const params: Record<string, any> = {
      customViewId: values.id,
      jobLogType: 'ASSETS_LOGS',
      pdfMetaData: JSON.stringify(pdfMetaData),
    };

    if (filterFields.length || assetLogViewFilters.length) {
      params.filters = JSON.stringify({
        op: FilterOperators.AND,
        fields: [...filterFields, ...assetLogViewFilters],
      });
    }

    const timestamp = Date.now();
    await downloadPdf({
      url: apiGetJobLogsExcel('PDF', 'ASSETS_LOGS'),
      params,
      method: 'GET',
      filename: `ObjectJobLog_${selectedObject?.objectType?.id}_${selectedObject?.externalId}_${timestamp}`,
    });
  };

  const fetchProcessList = useCallback(
    (searchedValue?: string) => {
      resetProcessList({
        params: {
          ...urlParams,
          filters: {
            op: FilterOperators.AND,
            fields: [
              ...(searchedValue
                ? [{ field: 'name', op: FilterOperators.LIKE, values: [searchedValue] }]
                : []),
              { field: 'archived', op: 'EQ', values: [false] },
              { field: 'state', op: 'EQ', values: ['PUBLISHED'] },
              { field: 'useCaseId', op: 'EQ', values: [useCaseId] },
            ],
          },
        },
      });
    },
    [useCaseId],
  );

  useEffect(() => {
    resetJobLogs({
      params: {
        ...urlParams,
        filters: {
          op: FilterOperators.AND,
          fields: [...filterFields, ...assetLogViewFilters],
        },
      },
    });

    dispatch(
      setAuditLogFilters(
        JSON.stringify({
          op: FilterOperators.AND,
          fields: [...filterFields, ...assetLogViewFilters],
        }),
      ),
    );
    dispatch(setPdfColumns(columns));
  }, [filterFields]);

  const resourceParameterChoicesMap = useRef({});

  useEffect(() => {
    if (jobLogsList.length) {
      resourceParameterChoicesMap.current = logsResourceChoicesMapper(jobLogsList);
    }
  }, [jobLogsList]);

  return (
    <JobLogsTabWrapper>
      <TabContentWrapper>
        {showFilters && (
          <div className="before-table-wrapper">
            <div className="filters">
              <Select
                isLoading={processlistStatus === 'loading'}
                placeholder="Select Process"
                options={transformDataToOptions(processList, ['name'], ['id'])}
                onMenuScrollToBottom={fetchNextProcessList}
                onMenuOpen={() => {
                  fetchProcessList();
                }}
                isClearable
                onChange={(option) => {
                  if (option) {
                    setFilterFields((prev) => [
                      ...prev,
                      {
                        field: 'checklistId',
                        op: FilterOperators.EQ,
                        values: [option.value],
                      },
                    ]);
                  } else {
                    setFilterFields((prev) =>
                      filter(prev, (field) => field.field !== 'checklistId'),
                    );
                  }
                }}
                onInputChange={debounce((searchedValue: string, actionMeta) => {
                  if (searchedValue !== actionMeta.prevInputValue) {
                    fetchProcessList(searchedValue);
                  }
                }, 500)}
              />
              <ResourceFilter
                defaultValue={{
                  label: selectedObject?.displayName,
                  value: selectedObject?.id,
                }}
                disabled
              />
              <div
                className="icon-filter"
                onClick={() => {
                  setShowDrawer(!showDrawer);
                }}
              >
                <img src={FilterIcon} alt="filter icon" className="icon" />
                {drawerFilters?.length > 0 && <span>{`(${drawerFilters.length})`}</span>}
              </div>

              <NestedSelect
                id="download-logs"
                className={`dropdown-filter ${!jobLogsList.length ? 'disabled' : ''}`}
                label={() => (
                  <span className="icon-filter">
                    <Tooltip title={jobLogsList.length ? 'Export' : 'No Job Logs to export'} arrow>
                      <img src={DownloadIcon} alt="Download icon" className="icon" />
                    </Tooltip>
                  </span>
                )}
                items={{
                  excel: {
                    label: 'Excel',
                    disabled: true,
                  },
                  pdf: {
                    label: 'PDF',
                  },
                }}
                disableUnderline
                disabled={!jobLogsList.length}
                onChildChange={(option: any) => {
                  if (option.value === 'excel') {
                    // dispatch(
                    //   fetchJobLogsExcel({
                    //     customViewId: id,
                    //     timezoneOffset: getLocalTimeOffset(),
                    //     ...(filterFields.length || viewDetails?.filters?.length
                    //       ? {
                    //           filters: {
                    //             op: FilterOperators.AND,
                    //             fields: [
                    //               ...filterFields,
                    //               ...filtersToQueryParams(viewDetails?.filters || []),
                    //             ],
                    //           },
                    //         }
                    //       : {}),
                    //   }),
                    // );
                  } else {
                    // dispatch(
                    //   setPdfMetaData({
                    //     objectTypeDisplayName: selectedObject?.objectType?.displayName!,
                    //     objectDisplayName: selectedObject?.displayName!,
                    //     objectExternalId: selectedObject?.externalId!,
                    //     objectDisplayNameLabel: getPropertyDisplayName('displayName'),
                    //     objectExternalIdLabel: getPropertyDisplayName('externalId'),
                    //   }),
                    // );
                    // openLinkInNewTab(`/object-job-logs/${selectedObject?.id}/print`);
                    handleDownload();
                  }
                }}
              />
            </div>
          </div>
        )}
        <LoadingContainer
          loading={status === 'loading'}
          component={
            <>
              <DataTable
                columns={jobLogColumns}
                rows={jobLogsList.reduce((acc: any[], jobLog: any, index: number) => {
                  (jobLog.logs || []).forEach((log: any) => {
                    acc[index] = {
                      ...acc[index],
                      [log.entityId + log.triggerType]: logsParser(
                        { ...log, jobId: jobLog.id },
                        jobLog.id,
                        resourceParameterChoicesMap.current,
                      ),
                    };
                  });
                  return acc;
                }, [])}
                emptyTitle="No Job Logs Found"
              />
              <Pagination
                pageable={pagination}
                fetchData={(p) =>
                  resetJobLogs({
                    params: {
                      ...urlParams,
                      filters: {
                        op: FilterOperators.AND,
                        fields: filterFields,
                      },
                      page: p.page,
                      size: p.size,
                    },
                  })
                }
              />
              {showDrawer && (
                <JobLogsFilterDrawer
                  setShowDrawer={setShowDrawer}
                  onSubmit={onApplyFilters}
                  filters={drawerFilters}
                />
              )}
            </>
          }
        />
      </TabContentWrapper>
    </JobLogsTabWrapper>
  );
};

export default JobLogsTabContent;
