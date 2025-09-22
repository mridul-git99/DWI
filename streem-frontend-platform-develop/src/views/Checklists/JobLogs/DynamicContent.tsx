import { JobLogColumnType, TriggerTypeEnum } from '#PrototypeComposer/checklist.types';
import DownloadIcon from '#assets/svg/DownloadIcon.svg';
import {
  Button,
  LoadingContainer,
  NestedSelect,
  Pagination,
  ResourceFilter,
  Select,
  TabContentProps,
  VirtualizationTable,
} from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import Tooltip from '#components/shared/Tooltip';
import { useQueryParams } from '#hooks/useQueryParams';
import checkPermission from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { setAuditLogFilters, setPdfColumns } from '#store/audit-log-filters/action';
import { openLinkInNewTab } from '#utils';
import { DEFAULT_PAGE_NUMBER, EXPORT_LIMIT_LEVELS } from '#utils/constants';
import {
  filtersToQueryParams,
  formatFilters,
  queryParamsToFilters,
} from '#utils/filtersToQueryParams';
import { FilterField, FilterOperators, fetchDataParams } from '#utils/globalTypes';
import { getLocalTimeOffset } from '#utils/timeUtils';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { Tune } from '@material-ui/icons';
import { navigate } from '@reach/router';
import { isEqual } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { fetchProcessLogs, saveCustomView } from '../ListView/actions';
import { CustomView } from '../ListView/types';
import { FilterDetails } from './FilterDetailsContainer';
import FiltersDrawer from './Overlays/FiltersDrawer';
import { fetchJobLogsExcel } from './actions';
import { downloadPdf } from '#utils/downloadPdf';
import { apiGetJobLogsExcel } from '#utils/apiUrls';
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
  .form-group {
    padding: 0 0;
  }
`;

const DynamicContent: FC<TabContentProps> = ({ values }) => {
  const { getQueryParam, updateQueryParams } = useQueryParams();
  const jobLogsFilters = getQueryParam('filters');
  const tabIndex = getQueryParam('tab', 0);

  const { id, checklistId } = values;
  const dispatch = useDispatch();

  const jobLogColumns = useTypedSelector((state) => state.prototypeComposer.data?.jobLogColumns);
  const processName = useTypedSelector((state) => state.prototypeComposer.data?.name);
  const parametersListById = useTypedSelector(
    (state) => state.prototypeComposer.parameters.listById,
  );
  const list = useTypedSelector((state) => state.checklistListView.jobLogs.list);
  const selectedFacility = useTypedSelector((state) => state.auth.selectedFacility);
  const logsLoading = useTypedSelector((state) => state.checklistListView.jobLogs.loading);
  const pageable = useTypedSelector((state) => state.checklistListView.jobLogs.pageable);
  const customViews = useTypedSelector((state) => state.checklistListView.customViews.views);
  const customViewLoading = useTypedSelector(
    (state) => state.checklistListView.customViews.loading,
  );

  const [filterFields, setFilterFields] = useState<FilterField[]>([]);
  // const { filters: jobLogFilters } = useTypedSelector((state) => state.auditLogFilters);
  const [state, setState] = useState<{
    viewDetails: CustomView;
    showDrawer: boolean;
    isChanged: boolean;
  }>({
    showDrawer: false,
    isChanged: false,
    viewDetails: {} as CustomView,
  });
  const { showDrawer, viewDetails, isChanged } = state;

  const compareKeys = (key: keyof CustomView, _viewDetails = viewDetails) => {
    let _isChanged = isChanged;
    if (!isChanged && _viewDetails) {
      _isChanged = !isEqual(_viewDetails?.[key], customViews?.[id]?.[key]);
    }
    return _isChanged;
  };

  const onColumnSelection = (selectedColumns: JobLogColumnType[]) => {
    setState((prev) => {
      const upDatedViewDetails = prev.viewDetails
        ? { ...prev.viewDetails, columns: selectedColumns }
        : prev.viewDetails;
      return {
        ...prev,
        viewDetails: upDatedViewDetails,
        isChanged: compareKeys('columns', upDatedViewDetails),
      };
    });
  };

  const navigateQueryParams = (newFilters: any[]) => {
    updateQueryParams({
      newParams: {
        filters: {
          op: FilterOperators.AND,
          fields: filtersToQueryParams(newFilters),
        },
      },
      navigateOptions: { replace: true },
    });
  };

  const onApplyMoreFilters = (newFilters: any[]) => {
    setState((prev) => {
      const upDatedViewDetails = prev.viewDetails
        ? { ...prev.viewDetails, filters: newFilters }
        : prev.viewDetails;

      navigateQueryParams(newFilters);
      return {
        ...prev,
        viewDetails: upDatedViewDetails,
        isChanged: compareKeys('filters', upDatedViewDetails),
      };
    });
  };

  const fetchData = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = 10, filters = filterFields } = params;
    dispatch(
      fetchProcessLogs({
        page,
        size,
        filters: {
          op: FilterOperators.AND,
          fields: [
            ...filters,
            ...filtersToQueryParams(viewDetails?.filters || []),
            {
              field: 'facilityId',
              op: FilterOperators.EQ,
              values: [selectedFacility!.id],
            },
            { field: 'checklistId', op: FilterOperators.EQ, values: [checklistId] },
          ],
        },
        sort: 'id,desc',
        ...(id ? { customViewId: id } : {}),
      }),
    );

    if (id)
      dispatch(
        setAuditLogFilters(
          JSON.stringify({
            op: FilterOperators.AND,
            fields: [
              ...filters,
              ...filtersToQueryParams(viewDetails?.filters || []),
              {
                field: 'facilityId',
                op: FilterOperators.EQ,
                values: [selectedFacility?.id],
              },
            ],
          }),
        ),
      );
  };

  useEffect(() => {
    if (viewDetails?.filters && filterFields) {
      fetchData();
    }
  }, [viewDetails?.filters, filterFields]);

  useEffect(() => {
    onResetToDefault();
  }, [id, customViews?.[id]]);

  useEffect(() => {
    if (jobLogsFilters) {
      const paramFilters = JSON.parse(jobLogsFilters)?.fields;
      const paramFiltersParsed = queryParamsToFilters(paramFilters, parametersListById);
      setState(
        (prev) =>
          ({
            ...prev,
            viewDetails: {
              ...prev.viewDetails,
              filters: paramFiltersParsed,
            },
            isChanged: !!paramFiltersParsed.length,
          } as any),
      );
    }
  }, []);

  const onResetToDefault = () => {
    setState((prev) => ({
      ...prev,
      viewDetails: id
        ? customViews?.[id]
        : {
            columns: jobLogColumns || [],
            filters: [],
            id: '0',
            processId: checklistId,
          },
      isChanged: false,
    }));
  };

  const onSaveView = () => {
    if (id && viewDetails) {
      dispatch(
        saveCustomView({
          data: {
            ...viewDetails,
            filters: formatFilters(viewDetails?.filters),
          },
          viewId: id,
        }),
      );
      navigate(`?tab=${tabIndex}`, { replace: true });
    }
  };

  const handleDownload = async () => {
    if (pageable.totalElements > EXPORT_LIMIT_LEVELS.MEDIUM) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: `Cannot export ${pageable.totalElements.toLocaleString()} job logs. Maximum export limit is ${EXPORT_LIMIT_LEVELS.MEDIUM.toLocaleString()} records. Please apply filters to reduce the dataset.`,
        }),
      );
      return;
    }

    const params: Record<string, any> = {
      customViewId: id, // <- file name + filter scope on the API
      jobLogType: 'PROCESS_LOGS',
    };
    if (filterFields.length || viewDetails?.filters?.length) {
      params.filters = {
        op: FilterOperators.AND,
        fields: [...filterFields, ...filtersToQueryParams(viewDetails?.filters || [])],
      };
    }
    const timestamp = Date.now();
    await downloadPdf({
      url: apiGetJobLogsExcel('PDF', 'PROCESS_LOGS'),
      params,
      method: 'GET',
      filename: `View_${id}_${checklistId}_${timestamp}`,
    });
  };

  const onChildChange = (option: any) => {
    setFilterFields([
      {
        field: 'logs.triggerType',
        op: FilterOperators.EQ,
        values: [TriggerTypeEnum.RESOURCE_PARAMETER],
      },
      {
        field: 'logs.identifierValue',
        op: FilterOperators.LIKE,
        values: [option.id],
      },
    ]);
  };

  const onSubmit = (_data: any, callback: any) => {
    callback && callback();
    onApplyMoreFilters(_data.filters || []);
  };

  return (
    <JobLogsTabWrapper key={`${checklistId}_${id}`}>
      <TabContentWrapper>
        <div className="before-table-wrapper">
          <div className="filters">
            <Select
              placeholder="Processes"
              isDisabled
              value={{ label: processName }}
              floatingLabel
              showTooltip={true}
            />
            <ResourceFilter onChange={onChildChange} onClear={() => setFilterFields([])} />
            <Button
              variant="textOnly"
              onClick={() => {
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.CONFIGURE_COLUMNS,
                    props: {
                      selectedColumns: viewDetails?.columns || [],
                      columns: jobLogColumns || [],
                      onColumnSelection,
                    },
                  }),
                );
              }}
            >
              <Tune />
              Configure Columns <span>{`(${viewDetails?.columns?.length || 0})`}</span>
            </Button>
            <Button
              variant="textOnly"
              onClick={() => {
                setState((prev) => ({ ...prev, showDrawer: true }));
              }}
            >
              <Tune />
              Filters{' '}
              <span>{`(${
                viewDetails?.filters?.length
                  ? id
                    ? viewDetails.filters.length - 1
                    : viewDetails.filters.length
                  : 0
              })`}</span>
            </Button>
            {id && (
              <NestedSelect
                id="download-logs"
                className={`dropdown-filter ${list.length < 2 ? 'disabled' : ''}`}
                label={() => (
                  <span className="icon-filter">
                    <Tooltip title={list.length > 1 ? 'Export' : 'No Job Logs to export'} arrow>
                      <img src={DownloadIcon} alt="Download icon" className="icon" />
                    </Tooltip>
                  </span>
                )}
                items={{
                  excel: {
                    label: 'Excel',
                  },
                  pdf: {
                    label: 'PDF',
                  },
                }}
                disableUnderline
                disabled={list.length < 2}
                onChildChange={(option: any) => {
                  if (pageable.totalElements > EXPORT_LIMIT_LEVELS.MEDIUM) {
                    dispatch(
                      showNotification({
                        type: NotificationType.ERROR,
                        msg: `Cannot export ${pageable.totalElements.toLocaleString()} job logs. Maximum export limit is ${EXPORT_LIMIT_LEVELS.MEDIUM.toLocaleString()} records. Please apply filters to reduce the dataset.`,
                      }),
                    );
                    return;
                  }

                  if (option.value === 'excel') {
                    dispatch(
                      fetchJobLogsExcel({
                        type: 'EXCEL',
                        customViewId: id,
                        checklistId: checklistId,
                        timezoneOffset: getLocalTimeOffset(),
                        ...(filterFields.length || viewDetails?.filters?.length
                          ? {
                              filters: {
                                op: FilterOperators.AND,
                                fields: [
                                  ...filterFields,
                                  ...filtersToQueryParams(viewDetails?.filters || []),
                                ],
                              },
                            }
                          : {}),
                      }),
                    );
                  } else {
                    // dispatch(setPdfColumns(viewDetails.columns));
                    // dispatch(
                    //   fetchJobLogsExcel({
                    //     type: 'PDF',
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
                    handleDownload();
                  }
                }}
              />
            )}
            <FilterDetails
              key="filterDetails"
              onSubmit={onSubmit}
              allFilters={viewDetails?.filters ?? []}
            />
          </div>
          {id && isChanged && checkPermission(['jobLogsViews', 'edit']) && (
            <div className="actions">
              {!customViewLoading && (
                <Button
                  style={{ marginBlock: '8px' }}
                  variant="textOnly"
                  onClick={() => {
                    navigate(`?tab=${tabIndex}`, { replace: true });
                    onResetToDefault();
                  }}
                >
                  Reset to Default
                </Button>
              )}
              <Button variant="secondary" onClick={onSaveView} disabled={customViewLoading}>
                Save View
              </Button>
            </div>
          )}
        </div>
        <LoadingContainer
          loading={logsLoading}
          component={
            <>
              <VirtualizationTable
                columns={viewDetails?.columns || []}
                data={list}
                emptyTitle="No Job Logs Found"
                isCompact
              />
              <Pagination pageable={pageable} fetchData={fetchData} />
              {showDrawer && (
                <FiltersDrawer
                  setState={setState}
                  onSubmit={onSubmit}
                  checklistId={checklistId}
                  filters={viewDetails?.filters || []}
                />
              )}
            </>
          }
        />
      </TabContentWrapper>
    </JobLogsTabWrapper>
  );
};

export default DynamicContent;
