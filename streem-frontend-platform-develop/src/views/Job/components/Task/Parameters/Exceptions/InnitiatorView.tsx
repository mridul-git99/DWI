import { FormGroup, Select } from '#components';
import { uiPermissions } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, nonEmptyStringRegex } from '#utils/constants';
import { FilterOperators, InputTypes } from '#utils/globalTypes';
import { debounce } from 'lodash';
import React, { FC } from 'react';
import { Controller } from 'react-hook-form';

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  filters: {
    op: FilterOperators.AND,
    fields: [{ field: 'archived', op: FilterOperators.EQ, values: [false] }],
  },
};

export const ExceptionInitiatorView: FC<any> = ({
  form,
  approver,
  fetchNext,
  resetAssigneeList,
  assigneeList,
  assigneeListStatus,
  setShowPasswordField,
  acceptWithReasonView,
}) => {
  const { control, register } = form;
  const { id } = useTypedSelector((state) => state.auth);
  return (
    <>
      {!acceptWithReasonView && (
        <Controller
          control={control}
          name="approver"
          key="approver"
          defaultValue={approver || null}
          shouldUnregister={false}
          rules={{
            required: true,
          }}
          render={({ onChange, value }) => (
            <Select
              label="Approver"
              isLoading={assigneeListStatus === 'loading'}
              options={assigneeList.reduce((options, user) => {
                if (user.id !== id) {
                  options.push({
                    ...user,
                    value: user.id,
                    label: user?.firstName + ' ' + user?.lastName,
                    externalId: <div>&nbsp;(ID: {user?.employeeId})</div>,
                  });
                }
                return options;
              }, [])}
              onInputChange={debounce((searchedValue: string, actionMeta) => {
                if (searchedValue !== actionMeta.prevInputValue) {
                  const rolesParam = uiPermissions.exceptions.reviewers.join(',');
                  resetAssigneeList({
                    params: {
                      ...urlParams,
                      query: searchedValue,
                      roles: rolesParam,
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
                  externalId: <div>&nbsp;(ID: {currOption.employeeId})</div>,
                }));
                onChange(updatedValue?.length ? updatedValue : null);
                setShowPasswordField(false);
              }}
              value={value}
              onMenuScrollToBottom={fetchNext}
              isMulti={true}
              onMenuOpen={() => {
                const rolesParam = uiPermissions.exceptions.reviewers.join(',');
                resetAssigneeList({ params: { ...urlParams, roles: rolesParam } });
              }}
              placeholder="Select from list of approvers"
            />
          )}
        />
      )}
      <FormGroup
        key="basic-info-section"
        inputs={[
          {
            type: InputTypes.MULTI_LINE,
            props: {
              placeholder: 'Enter your reason here',
              label: 'Reason for exception',
              id: 'reason',
              name: 'reason',
              rows: 3,
              ref: register({ required: true, pattern: nonEmptyStringRegex }),
              onChange: () => setShowPasswordField(false),
            },
          },
        ]}
      />
    </>
  );
};
