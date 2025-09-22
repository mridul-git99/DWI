import rightArrow from '#assets/svg/right-arrow.svg';
import {
  AssigneeList,
  Avatar,
  DataTable,
  LoadingContainer,
  Pagination,
  ResourceFilter,
  Select,
  TextInput,
} from '#components';
import { closeOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import Option from '#components/shared/DropdownCheckboxOption';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { ParameterCorrectionStatus } from '#types';
import { apiGetAllCorrections, apiGetUsers } from '#utils/apiUrls';
import { STATUS_COLOR_MAP } from '#utils/jobMethods';
import { formatDateTime } from '#utils/timeUtils';
import { CorrectionInfoDrawer } from '#views/Job/components/Task/Parameters/Corrections';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { FiberManualRecord, Search } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { InboxWrapper } from '../styles';
import { navigateToTaskExecution } from '#views/Job/utils';
import { generateUserSearchFilters } from '#utils/smartFilterUtils';
import { FilterOperators } from '#utils/globalTypes';
import { User } from '#services/users';
import { InputActionMeta } from 'react-select';
import { getFormattedUserOptions, urlParams } from '../utils';

const options = [
  {
    label: 'Awaiting Correction',
    value: ParameterCorrectionStatus.INITIATED,
  },
  {
    label: 'Awaiting Review',
    value: ParameterCorrectionStatus.CORRECTED,
  },
  {
    label: 'Accepted',
    value: ParameterCorrectionStatus.ACCEPTED,
  },
  {
    label: 'Rejected',
    value: ParameterCorrectionStatus.REJECTED,
  },
];

const CorrectionsContent: FC<{
  values?: { jobId?: string; isJobOpen?: boolean };
}> = ({ values = {} }) => {
  const { jobId, isJobOpen } = values;
  const [selectedCorrection, setSelectedCorrection] = useState<any>(null);
  const userId = useTypedSelector((state) => state.auth.userId)!;
  const selectedUseCase = useTypedSelector((state) => state.auth.selectedUseCase)!;
  const dispatch = useDispatch();
  const [filters, setFilters] = useState<Record<string, any>>({
    ...urlParams,
    ...(jobId && { jobId }),
    useCaseId: selectedUseCase.id,
    userId,
    status: ParameterCorrectionStatus.INITIATED,
  });

  const {
    list: usersList,
    reset: resetUsersList,
    fetchNext: fetchNextUsers,
    status: usersStatus,
  } = createFetchList<User>(apiGetUsers(), urlParams, false);

  const { list, reset, pagination, status } = createFetchList(
    apiGetAllCorrections(),
    urlParams,
    false,
  );

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

  const columns = useMemo(
    () => [
      {
        id: 'processName',
        label: 'Process Name',
        minWidth: 100,
      },
      {
        id: 'jobCode',
        label: 'Job ID',
        minWidth: 100,
      },
      {
        id: 'taskName',
        label: 'Task Name',
        minWidth: 100,
      },
      {
        id: 'parameterName',
        label: 'Parameter Name',
        minWidth: 100,
      },
      {
        id: 'code',
        label: 'Correction ID',
        minWidth: 100,
      },
      {
        id: 'dateTime',
        label: 'Date Time',
        minWidth: 100,
        format: (item) => {
          return formatDateTime({
            value: item.createdAt,
          });
        },
      },
      {
        id: 'initiatorCorrectorReviewer',
        label: 'Initiator - Corrector - Reviewer',
        minWidth: 200,
        format: (item) => {
          const correctors = item.corrector.map((curr) => curr.user);
          const reviewers = item.reviewer.map((curr) => curr.user);
          return (
            <div style={{ display: 'flex', gap: '8px' }}>
              <Avatar
                user={item?.createdBy}
                color="blue"
                backgroundColor="#F4F4F4"
                borderColor="#FFFFFF"
              />
              <img src={rightArrow} alt="arrow" />
              <AssigneeList users={correctors} />
              <img src={rightArrow} alt="arrow" />
              <AssigneeList users={reviewers} />
            </div>
          );
        },
      },
      {
        id: 'correctionStatus',
        label: 'Correction Status',
        minWidth: 100,
        format: (item) => {
          const { statusText, color } = STATUS_COLOR_MAP[item.status];
          return (
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <FiberManualRecord
                className="icon"
                style={{
                  fontSize: '12px',
                  color: color,
                }}
              />
              <span title={[item.state]}>{statusText}</span>
            </div>
          );
        },
      },
      {
        id: 'action',
        label: 'Action',
        minWidth: 100,
        format: (item) => {
          const taskExecutionId = item.taskExecutionId;
          const showView = [
            ParameterCorrectionStatus.INITIATED,
            ParameterCorrectionStatus.CORRECTED,
          ].includes(item.status);
          return (
            <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
              {showView && (
                <span
                  className="primary"
                  onClick={() => {
                    dispatch(closeOverlayAction(OverlayNames.CORRECTION_LIST_MODAL));
                    navigateToTaskExecution(item.jobId, taskExecutionId, undefined, !isJobOpen);
                  }}
                >
                  View
                </span>
              )}
              <span
                className="primary"
                onClick={() => {
                  setSelectedCorrection(item);
                }}
              >
                Info
              </span>
            </div>
          );
        },
      },
    ],
    [],
  );

  useEffect(() => {
    reset({ params: { ...filters } });
  }, [filters]);

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
                ({ value }) => setFilters({ ...filters, parameterName: value, processName: value }),
                500,
              )}
            />
            <Select
              placeholder="Select"
              label="Initiated By"
              options={getFormattedUserOptions(usersList)}
              isLoading={usersStatus === 'loading'}
              onChange={(data) => {
                setFilters((prev) => ({
                  ...prev,
                  initiatedBy: data?.value,
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
            <Select
              label="Status"
              placeholder="Status"
              options={options}
              components={{ Option }}
              hideSelectedOptions={false}
              value={options.filter((el) => el.value === filters.status)?.[0]}
              onChange={(option) => {
                setFilters((prev) => ({ ...prev, status: option?.value }));
              }}
              isClearable={true}
            />
            <ResourceFilter
              onChange={(option: any) => {
                setFilters((prev) => ({ ...prev, objectId: option.id }));
              }}
              onClear={() => {
                setFilters((prev) => ({ ...prev, objectId: '' }));
              }}
            />
          </div>
        </div>
        <LoadingContainer
          loading={status === 'loading'}
          component={
            <>
              <DataTable columns={columns} rows={list} emptyTitle="No Corrections Found" />
              <Pagination
                pageable={pagination}
                fetchData={(p) => reset({ params: { page: p.page, size: p.size } })}
              />
            </>
          }
        />
        {selectedCorrection && (
          <CorrectionInfoDrawer
            correction={selectedCorrection}
            onCloseDrawer={setSelectedCorrection}
          />
        )}
      </TabContentWrapper>
    </InboxWrapper>
  );
};

export default CorrectionsContent;
