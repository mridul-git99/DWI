import React, { FC, useEffect, useState } from 'react';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { debounce } from 'lodash';
import { Search } from '@material-ui/icons';
import {
  Avatar,
  DataTable,
  LoadingContainer,
  Pagination,
  ResourceFilter,
  Select,
  TextInput,
} from '#components';
import { formatDateTime } from '#utils/timeUtils';
import { apiGetUsers, getParameterExecutionInfo } from '#utils/apiUrls';
import { createFetchList } from '#hooks/useFetchData';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { InboxWrapper } from '../styles';
import { useDispatch } from 'react-redux';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { getErrorMsg, request } from '#utils/request';
import { fetchDataParams, FilterOperators } from '#utils/globalTypes';
import { NotificationType } from '#components/Notification/types';
import { showNotification } from '#components/Notification/actions';
import { User } from '#services/users';
import { generateUserSearchFilters } from '#utils/smartFilterUtils';
import { InputActionMeta } from 'react-select';
import { getExceptionParameter } from '#utils/parameterUtils';
import { useQueryParams } from '#hooks/useQueryParams';
import { fetchApprovals, fetchApprovalsSuccess } from './actions';
import { getFormattedUserOptions, urlParams } from '../utils';

const ApprovalsContent: FC<{
  values?: {
    jobId?: string;
    isJobOpen?: boolean;
    isInboxView?: boolean;
  };
}> = ({ values = {} }) => {
  const dispatch = useDispatch();
  const { jobId, isInboxView } = values;

  const { getQueryParam } = useQueryParams();

  const currentJobId = jobId || getQueryParam('jobId', '');

  const [filters, setFilters] = useState<Record<string, any>>({});
  const [resourceFilter, setResourceFilter] = useState<string>('');
  const { userId } = useTypedSelector((state) => state.auth);
  const selectedUseCase = useTypedSelector((state) => state.auth.selectedUseCase)!;

  const { list, pageable, loading } = useTypedSelector((state) => state.inboxListView.approvals);

  const fetchApprovalsList = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE } = params;
    dispatch(
      fetchApprovals({
        params: {
          page,
          size,
          userId,
          useCaseId: selectedUseCase.id,
          showAllException: !!currentJobId,
          ...(currentJobId && { jobId: currentJobId }),
          ...(isInboxView && { isInboxView }),
          ...(resourceFilter && { objectId: resourceFilter }),
          ...filters,
        },
      }),
    );
  };

  const {
    list: usersList,
    reset: resetUsersList,
    fetchNext: fetchNextUsers,
    status: usersStatus,
  } = createFetchList<User>(apiGetUsers(), urlParams, false);

  const searchUser = (value: string) => {
    resetUsersList({
      params: {
        ...urlParams,
        ...(value && {
          filters: generateUserSearchFilters(FilterOperators.LIKE, value),
        }),
      },
    });
  };

  const onChildChange = (option: any) => {
    setResourceFilter(option.id);
  };

  const handleCjfExceptionApproval = async (
    jobId: string,
    parameterId: string,
    rulesId: string,
  ) => {
    const { data, errors } = await request('GET', getParameterExecutionInfo(jobId!), {
      params: {
        filters: {
          op: FilterOperators.AND,
          fields: [
            {
              field: 'id',
              op: FilterOperators.EQ,
              values: [parameterId],
            },
          ],
        },
      },
    });

    if (data) {
      const parameter = getExceptionParameter(data[0], rulesId);
      dispatch(
        openOverlayAction({
          type: OverlayNames.PARAMETER_EXCEPTION_MODAL,
          props: {
            parameter,
            jobId,
            rulesId,
          },
        }),
      );
    }

    if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }
  };

  useEffect(() => {
    fetchApprovalsList();

    return () => {
      dispatch(fetchApprovalsSuccess());
    };
  }, [filters, resourceFilter]);

  return (
    <InboxWrapper>
      <TabContentWrapper>
        <div className="before-table-wrapper">
          <div className="filters" style={{ alignItems: 'flex-end' }}>
            <TextInput
              afterElementWithoutError
              AfterElement={Search}
              afterElementClass=""
              placeholder={`Search by Paramater or Process Name`}
              onChange={debounce(
                ({ value }) => setFilters({ ...filters, processName: value, parameterName: value }),
                500,
              )}
            />
            <Select
              placeholder="Select"
              label="Requested By"
              options={getFormattedUserOptions(usersList)}
              isLoading={usersStatus === 'loading'}
              onChange={(data) => {
                setFilters((prev) => ({
                  ...prev,
                  requestedBy: data?.value,
                }));
              }}
              onInputChange={debounce((newValue: string, actionMeta: InputActionMeta) => {
                if (newValue !== actionMeta.prevInputValue) {
                  searchUser(newValue);
                }
              }, 500)}
              onMenuOpen={() => {
                resetUsersList({ params: urlParams });
              }}
              onMenuScrollToBottom={fetchNextUsers}
              isClearable={true}
              filterOption={() => true}
            />
            <ResourceFilter onChange={onChildChange} onClear={() => setResourceFilter('')} />
          </div>
        </div>
        <LoadingContainer
          loading={loading}
          component={
            <>
              <DataTable
                columns={[
                  {
                    id: 'processName',
                    label: 'Process Name',
                    minWidth: 100,
                    format: (item) => {
                      return item.processName;
                    },
                  },
                  {
                    id: 'jobId',
                    label: 'Job ID',
                    minWidth: 100,
                    format: (item) => {
                      return item.jobCode;
                    },
                  },
                  {
                    id: 'taskName',
                    label: 'Task Name',
                    minWidth: 100,
                    format: (item) => {
                      return item.taskName || '-';
                    },
                  },
                  {
                    id: 'paramterName',
                    label: 'Parameter Name',
                    minWidth: 100,
                    format: (item) => {
                      return item.parameterName;
                    },
                  },
                  {
                    id: 'requestedBy',
                    label: 'Requested By',
                    minWidth: 100,
                    format: (item) => {
                      return <Avatar user={item.exceptionInitiatedBy} />;
                    },
                  },
                  {
                    id: 'modifiedAt',
                    label: 'Modified At',
                    minWidth: 100,
                    format: (item) => formatDateTime({ value: item.modifiedAt }),
                  },
                  {
                    id: 'action',
                    label: 'Action',
                    minWidth: 100,
                    format: (item) => {
                      const { parameterId, jobId, rulesId } = item || {};

                      return (
                        <span
                          className="primary"
                          onClick={() => {
                            handleCjfExceptionApproval(jobId, parameterId, rulesId);
                          }}
                        >
                          View
                        </span>
                      );
                    },
                  },
                ]}
                rows={list}
                emptyTitle="No Approvals Found"
              />
              <Pagination pageable={pageable} fetchData={fetchApprovalsList} />
            </>
          }
        />
      </TabContentWrapper>
    </InboxWrapper>
  );
};

export default ApprovalsContent;
