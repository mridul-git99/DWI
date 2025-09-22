import { AssigneeList, Avatar, FormGroup, Select, StatusTag } from '#components';
import { uiPermissions } from '#services/uiPermissions';
import { User } from '#store/users/types';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, InputTypes } from '#utils/globalTypes';
import { formatDateTime } from '#utils/timeUtils';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import { debounce } from 'lodash';
import React, { FC } from 'react';
import { Controller } from 'react-hook-form';
import styled from 'styled-components';

type InitiatorViewProps = {
  correctors: User[];
  reviewers: User[];
  form: any;
  fetchNext: () => void;
  assigneeList: User[];
  loggedInUserId: string;
  resetAssigneeList: (_params: { url?: string; params?: any }) => void;
  assigneeListStatus?: string;
};

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  filters: {
    op: FilterOperators.AND,
    fields: [{ field: 'archived', op: FilterOperators.EQ, values: [false] }],
  },
};

export const InitiatorInfoWrapper = styled.div`
  .read-only-group {
    padding: 8px 0 0 0;
    border-bottom: 1px solid #e0e0e0;
    .read-only {
      margin-bottom: 16px;
      flex-direction: column;
      .content {
        ::before {
          display: none;
        }
        font-size: 12px;
        line-height: 1.33;
        letter-spacing: 0.32px;
        color: #525252;

        :last-child {
          font-size: 14px;
          line-height: 1.14;
          letter-spacing: 0.16px;
          color: #161616;
          padding-top: 4px;
        }
      }
    }
  }

  .info-item-value {
    color: #161616;
  }

  .file-links {
    display: flex;
    flex-direction: column;
    gap: 4px;
    a {
      margin-right: 8px;
    }
    div {
      color: #1d84ff;
      margin-right: 8px;
      cursor: pointer;
    }
    > div > span {
      color: #1d84ff;
    }
  }

  .assignments {
    margin: 0;
  }
`;

export const InitiatorView: FC<InitiatorViewProps> = ({
  form,
  reviewers,
  correctors,
  fetchNext,
  resetAssigneeList,
  assigneeList,
  assigneeListStatus,
}) => {
  const { control, register } = form;
  return (
    <>
      <Controller
        control={control}
        name="correctors"
        key="correctors"
        defaultValue={correctors || null}
        shouldUnregister={false}
        rules={{
          required: true,
        }}
        render={({ onChange, value }) => (
          <Select
            label="Select Corrector"
            isLoading={assigneeListStatus === 'loading'}
            options={
              assigneeList.map((user) => ({
                ...user,
                value: user.id,
                label: user?.firstName + ' ' + user?.lastName,
                externalId: <div>&nbsp;(ID: {user?.employeeId})</div>,
              })) as any
            }
            onInputChange={debounce((searchedValue: string, actionMeta) => {
              if (searchedValue !== actionMeta.prevInputValue) {
                resetAssigneeList({
                  params: {
                    ...urlParams,
                    query: searchedValue,
                    roles: undefined,
                  },
                });
              }
            }, 500)}
            onChange={(_option: any) => {
              const updatedValue = _option.map((currOption) => ({
                id: currOption.id,
                firstName: currOption.firstName,
                lastName: currOption.lastName,
                employeeId: currOption.employeeId,
                label: currOption.label,
                value: currOption.value,
                externalId: currOption.employeeId,
              }));
              onChange(updatedValue?.length ? updatedValue : null);
            }}
            value={(value || []).map((currValue: any) => ({
              ...currValue,
              externalId:
                typeof currValue?.employeeId === 'string' ? (
                  <div>&nbsp;(ID: {currValue?.employeeId})</div>
                ) : (
                  currValue?.employeeId
                ),
            }))}
            onMenuScrollToBottom={fetchNext}
            isMulti={true}
            onMenuOpen={() =>
              resetAssigneeList({
                params: {
                  ...urlParams,
                  roles: undefined,
                },
              })
            }
            placeholder="Select one or more correctors"
          />
        )}
      />
      <Controller
        control={control}
        name="reviewers"
        key="reviewers"
        defaultValue={reviewers || null}
        shouldUnregister={false}
        rules={{
          required: true,
        }}
        render={({ onChange, value }) => (
          <Select
            label="Select Reviewer"
            isLoading={assigneeListStatus === 'loading'}
            options={
              assigneeList.map((user) => ({
                ...user,
                value: user.id,
                label: user?.firstName + ' ' + user?.lastName,
                externalId: <div>&nbsp;(ID: {user?.employeeId})</div>,
              })) as any
            }
            onInputChange={debounce((searchedValue: string, actionMeta) => {
              if (searchedValue !== actionMeta.prevInputValue) {
                const rolesParam = uiPermissions.corrections.reviewers.join(',');
                resetAssigneeList({
                  params: {
                    ...urlParams,
                    query: searchedValue,
                    roles: rolesParam,
                  },
                });
              }
            }, 500)}
            value={(value || []).map((currValue: any) => ({
              ...currValue,
              externalId:
                typeof currValue?.employeeId === 'string' ? (
                  <div>&nbsp;(ID: {currValue?.employeeId})</div>
                ) : (
                  currValue?.employeeId
                ),
            }))}
            onChange={(_option: any) => {
              const updatedValue = _option.map((currOption) => ({
                id: currOption.id,
                firstName: currOption.firstName,
                lastName: currOption.lastName,
                employeeId: currOption.employeeId,
                label: currOption.label,
                value: currOption.value,
                externalId: currOption?.employeeId,
              }));
              onChange(updatedValue?.length ? updatedValue : null);
            }}
            onMenuScrollToBottom={fetchNext}
            isMulti={true}
            onMenuOpen={() => {
              const rolesParam = uiPermissions.corrections.reviewers.join(',');

              resetAssigneeList({
                params: {
                  ...urlParams,
                  roles: rolesParam,
                },
              });
            }}
            placeholder="Select one or more reviewers"
          />
        )}
      />
      <FormGroup
        key="basic-info-section"
        inputs={[
          {
            type: InputTypes.MULTI_LINE,
            props: {
              placeholder: 'Write here',
              label: 'Reason',
              id: 'initiatorReason',
              name: 'initiatorReason',
              rows: 3,
              maxRows: 8,
              ref: register({ required: true, pattern: /^\s*\S+(?:\s+\S+)*\s*$/ }),
            },
          },
        ]}
      />
    </>
  );
};

export const InitiatorInfo: FC<any> = ({ correction }) => {
  const { corrector, reviewer, status, initiatorsReason, createdAt, createdBy } = correction;
  return (
    <InitiatorInfoWrapper>
      <ReadOnlyGroup
        className="read-only-group"
        items={[
          {
            label: 'Status',
            value: <StatusTag status={status} />,
          },
          {
            label: 'Date of Initiation',
            value: formatDateTime({
              value: createdAt,
            }),
          },
          {
            label: 'Initiator',
            value: (
              <Avatar
                user={createdBy}
                color="blue"
                backgroundColor="#F4F4F4"
                borderColor="#FFFFFF"
              />
            ),
          },
          {
            label: 'Reason',
            value: initiatorsReason,
          },
        ]}
      />
      <ReadOnlyGroup
        className="read-only-group"
        items={[
          {
            label: 'Correctors',
            value: (
              <AssigneeList
                users={(corrector || []).map((currCurrector) => ({ ...currCurrector.user }))}
              />
            ),
          },
          {
            label: 'Reviewers',
            value: (
              <AssigneeList
                users={(reviewer || []).map((currReviewer) => ({ ...currReviewer.user }))}
              />
            ),
          },
        ]}
      />
    </InitiatorInfoWrapper>
  );
};
