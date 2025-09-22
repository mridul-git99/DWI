import { ParameterVerificationTypeEnum } from '#PrototypeComposer/checklist.types';
import rightArrow from '#assets/svg/right-arrow.svg';
import {
  Avatar,
  DataTable,
  LoadingContainer,
  Pagination,
  ResourceFilter,
  TextInput,
} from '#components';
import { closeOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import Option from '#components/shared/DropdownCheckboxOption';
import { Select } from '#components/shared/Select';
import { createFetchList } from '#hooks/useFetchData';
import { User } from '#services/users';
import { useTypedSelector } from '#store';
import { ParameterVerificationStatus } from '#types';
import { apiGetUsers } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, InputTypes, fetchDataParams } from '#utils/globalTypes';
import { generateUserSearchFilters } from '#utils/smartFilterUtils';
import { formatDateTime } from '#utils/timeUtils';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { FiberManualRecord, Search } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { InputActionMeta } from 'react-select';
import { InboxWrapper } from '../styles';
import { fetchVerifications, fetchVerificationsSuccess } from './actions';
import { navigateToTaskExecution } from '#views/Job/utils';
import { getFormattedUserOptions, urlParams } from '../utils';

const options = [
  {
    label: 'Pending',
    value: ParameterVerificationStatus.PENDING,
  },
  {
    label: 'Approved',
    value: ParameterVerificationStatus.ACCEPTED,
  },
  {
    label: 'Rejected',
    value: ParameterVerificationStatus.REJECTED,
  },
  {
    label: 'Recalled',
    value: ParameterVerificationStatus.RECALLED,
  },
];

const renderVerificationStatus = (status: string) => {
  let statusText = 'Pending';
  let indicatorColor = '#F1C21B';

  if (status === ParameterVerificationStatus.ACCEPTED) {
    statusText = 'Approved';
    indicatorColor = '#24A148';
  } else if (status === ParameterVerificationStatus.REJECTED) {
    statusText = 'Rejected';
    indicatorColor = '#DA1E28';
  } else if (status === ParameterVerificationStatus.RECALLED) {
    statusText = 'Recalled';
    indicatorColor = '#C2C2C2';
  } else {
    statusText = 'Pending';
    indicatorColor = '#F1C21B';
  }

  return (
    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
      <FiberManualRecord
        style={{
          fontSize: '8px',
          color: indicatorColor,
        }}
      />
      <div>{statusText}</div>
    </div>
  );
};

const VerificationsContent: FC<{
  values?: { jobId?: string; isJobOpen?: boolean; redirectedFromBanner?: boolean };
}> = ({ values = {} }) => {
  const { jobId, isJobOpen, redirectedFromBanner } = values;

  const {
    list: usersList,
    reset,
    fetchNext,
    status,
  } = createFetchList<User>(apiGetUsers(), urlParams, false);

  const dispatch = useDispatch();
  const { loading, list, pageable } = useTypedSelector(
    (state) => state.inboxListView.verifications,
  );
  const userId = useTypedSelector((state) => state.auth.userId);
  const useCaseId = useTypedSelector((state) => state.auth.selectedUseCase?.id);

  const [filters, setFilters] = useState<any>({
    requestedTo: !isJobOpen || redirectedFromBanner ? userId : undefined,
    jobId,
    status: ParameterVerificationStatus.PENDING,
  });
  const [resourceFilter, setResourceFilter] = useState<string>('');
  const [dropdownOpen, setDropdownOpen] = useState<string>('');

  useEffect(() => {
    fetchData();

    return () => {
      dispatch(fetchVerificationsSuccess());
    };
  }, [filters, resourceFilter]);

  const fetchData = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE } = params;
    dispatch(
      fetchVerifications({
        params: {
          page,
          size,
          useCaseId,
          ...(resourceFilter && { objectId: resourceFilter }),
          ...filters,
        },
      }),
    );
  };

  const searchUser = (value: string) => {
    reset({
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

  return (
    <InboxWrapper>
      <TabContentWrapper>
        <div className="before-table-wrapper">
          <div className="filters" style={{ alignItems: 'flex-end' }}>
            <TextInput
              afterElementWithoutError
              AfterElement={Search}
              afterElementClass=""
              placeholder={`Search by Process Name & Parameter Name`}
              onChange={debounce((option) => {
                setFilters((prev) => ({
                  ...prev,
                  parameterName: option?.value,
                  processName: option?.value,
                }));
              }, 500)}
            />
            <Select
              placeholder="Select"
              label="Requested By"
              options={getFormattedUserOptions(usersList)}
              isLoading={status === 'loading' && dropdownOpen === 'requestedBy'}
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
                reset({ params: urlParams });
                setDropdownOpen('requestedBy');
              }}
              onMenuScrollToBottom={fetchNext}
              isClearable={true}
              filterOption={() => true}
            />
            {isJobOpen && !redirectedFromBanner && (
              <Select
                placeholder="Select"
                label="Requested To"
                isLoading={status === 'loading' && dropdownOpen === 'requestedTo'}
                options={getFormattedUserOptions(usersList)}
                onChange={(data) => {
                  setFilters((prev) => ({
                    ...prev,
                    requestedTo: data?.value,
                  }));
                }}
                onInputChange={debounce((newValue: string, actionMeta: InputActionMeta) => {
                  if (newValue !== actionMeta.prevInputValue) {
                    searchUser(newValue);
                  }
                }, 500)}
                onMenuOpen={() => {
                  reset({ params: urlParams });
                  setDropdownOpen('requestedTo');
                }}
                onMenuScrollToBottom={fetchNext}
                isClearable={true}
                filterOption={() => true}
              />
            )}
            <Select
              placeholder="Select"
              options={options}
              components={{ Option }}
              hideSelectedOptions={false}
              label="Status"
              defaultValue={options.filter((el) => el.value === filters.status)}
              onChange={(option) => {
                setFilters((prev) => ({ ...prev, status: option?.value }));
              }}
              isClearable={true}
            />
            {!jobId && (
              <ResourceFilter onChange={onChildChange} onClear={() => setResourceFilter('')} />
            )}
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
                      return item.code;
                    },
                  },
                  {
                    id: 'taskName',
                    label: 'Task Name',
                    minWidth: 100,
                    format: (item) => {
                      return item.taskName;
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
                    id: 'requestedByAndTo',
                    label: 'Requested By- Requested To',
                    minWidth: 100,
                    format: (item) => {
                      return (
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <Avatar user={item?.createdBy} />
                          <img src={rightArrow} alt="arrow" />
                          <Avatar
                            user={
                              item.verificationType === ParameterVerificationTypeEnum.SELF
                                ? item?.createdBy
                                : item?.requestedTo
                            }
                          />
                        </div>
                      );
                    },
                  },
                  {
                    id: 'verificationStatus',
                    label: 'Verification Status',
                    minWidth: 100,
                    format: (item) => renderVerificationStatus(item?.verificationStatus),
                  },
                  {
                    id: 'verificationType',
                    label: 'Verification Type',
                    minWidth: 100,
                    format: (item) => {
                      return item.verificationType === ParameterVerificationTypeEnum.PEER ? (
                        <div
                          style={{
                            backgroundColor: '#BAE6FF',
                            color: '#00539A',
                            padding: '4px 8px',
                            fontSize: '12px',
                            maxWidth: '48px',
                          }}
                        >
                          PEER
                        </div>
                      ) : (
                        <div
                          style={{
                            backgroundColor: '#A7F0BA',
                            color: '#0E6027',
                            padding: '4px 8px',
                            fontSize: '12px',
                            maxWidth: '48px',
                          }}
                        >
                          SELF
                        </div>
                      );
                    },
                  },
                  {
                    id: 'requestedAt',
                    label: 'Requested At',
                    minWidth: 100,
                    format: (item) => {
                      return formatDateTime({ value: item?.createdAt, type: InputTypes.DATE_TIME });
                    },
                  },
                  {
                    id: 'action',
                    label: 'Action',
                    minWidth: 100,
                    format: (item) => {
                      const taskExecutionId = item.taskExecutionId;
                      return (
                        <span
                          className="primary"
                          onClick={() => {
                            dispatch(closeOverlayAction(OverlayNames.JOB_VERIFICATION));
                            navigateToTaskExecution(
                              item.jobId,
                              taskExecutionId,
                              item.parameterValueId,
                              !isJobOpen,
                            );
                          }}
                        >
                          View
                        </span>
                      );
                    },
                  },
                ]}
                rows={list}
                emptyTitle="No Parameters Found"
              />
              <Pagination pageable={pageable} fetchData={fetchData} />
            </>
          }
        />
      </TabContentWrapper>
    </InboxWrapper>
  );
};

export default VerificationsContent;
