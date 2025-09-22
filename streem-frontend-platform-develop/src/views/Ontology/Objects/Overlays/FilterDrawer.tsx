import { conditionByParameterType } from '#PrototypeComposer/BranchingRules/RuleConfiguration';
import { Button, FormGroup, LoadingContainer, useDrawer } from '#components';
import { useTypedSelector } from '#store';
import { fetchUsers } from '#store/users/actions';
import { UsersListType } from '#store/users/types';
import { MandatoryParameter } from '#types';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, InputTypes, fetchDataParams } from '#utils/globalTypes';
import { generateUserSearchFilters } from '#utils/smartFilterUtils';
import {
  FilterCardWrapper,
  FiltersWrapper,
} from '#views/Checklists/JobLogs/Overlays/FiltersDrawer';
import { AddCircleOutline, Close } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { Controller, useFieldArray, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import { InputActionMeta } from 'react-select';
import { v4 as uuidv4 } from 'uuid';

enum FilterType {
  ENTITY_ID = 'entityId',
  MODIFIED_BY_ID = 'modifiedBy.id',
}

const metaFilters = [
  {
    label: 'Change Done To',
    value: FilterType.ENTITY_ID,
  },
  {
    label: 'Change Done At',
    value: 'modifiedAt',
    type: MandatoryParameter.DATE_TIME,
  },
  {
    label: 'Change Done By',
    value: FilterType.MODIFIED_BY_ID,
  },
];

const FilterCard: FC<any> = ({ item, index, remove, fetchData, options, control }) => {
  const [selectedFilter, setSelectedFilter] = useState<any>(null);

  const {
    users: {
      all: { pageable },
    },
  } = useTypedSelector((state) => state);

  useEffect(() => {
    if (item) {
      setSelectedFilter(metaFilters.find((filter) => filter.value === item.field));
    }
  }, []);

  return (
    <FilterCardWrapper>
      <div className="upper-row">
        <Controller
          control={control}
          name={`filters.${index}.field`}
          rules={{
            required: true,
          }}
          defaultValue={item?.field || null}
          render={({ onChange }) => {
            return (
              <FormGroup
                inputs={[
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'parameterId',
                      label: 'Where',
                      placeholder: 'Select',
                      options: metaFilters,
                      value: selectedFilter
                        ? [
                            {
                              label: selectedFilter.label,
                              value: selectedFilter.id,
                            },
                          ]
                        : null,
                      style: {
                        flex: 1,
                      },
                      onChange: (value: any) => {
                        onChange(`${value.value}`);
                        control.setValue(`filters.${index}.op`, null);
                        control.setValue(`filters.${index}.value`, '', {
                          shouldDirty: true,
                          shouldValidate: true,
                        });
                        setSelectedFilter(value);
                      },
                    },
                  },
                ]}
              />
            );
          }}
        />

        <Controller
          control={control}
          name={`filters.${index}.op`}
          rules={{
            required: true,
          }}
          defaultValue={item?.op || null}
          render={({ onChange, value }) => {
            return (
              <FormGroup
                inputs={[
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'ruleCondition',
                      label: 'Condition',
                      placeholder: 'Select Condition',
                      options: Object.entries(conditionByParameterType(selectedFilter?.type)).map(
                        ([value, label]) => ({ label, value }),
                      ),
                      onChange: (value: any) => {
                        onChange(value.value);
                      },
                      value: value
                        ? [
                            {
                              label: (conditionByParameterType(selectedFilter?.type) as any)[value],
                              value: value,
                            },
                          ]
                        : null,
                      style: {
                        flex: 1,
                      },
                    },
                  },
                ]}
              />
            );
          }}
        />

        <Controller
          control={control}
          name={`filters.${index}.value`}
          rules={{
            required: true,
          }}
          defaultValue={item?.value || null}
          render={({ onChange, value }) => {
            if (selectedFilter?.type === MandatoryParameter.DATE_TIME) {
              return (
                <FormGroup
                  inputs={[
                    {
                      type: InputTypes.DATE_TIME,
                      props: {
                        id: 'value',
                        label: 'Value',
                        value: value || '',
                        placeholder: 'Enter Value',
                        onChange: (value: any) => {
                          onChange(parseInt(value.value));
                        },
                        style: {
                          flex: 1,
                        },
                      },
                    },
                  ]}
                />
              );
            }
            return (
              <FormGroup
                inputs={[
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'value',
                      label: 'Value',
                      placeholder: 'Enter Value',
                      options: options[selectedFilter?.value],
                      value: options[selectedFilter?.value]?.filter(
                        (el: any) => el.value === value?.value,
                      ),
                      onChange: (value: any) => {
                        onChange(value);
                      },
                      style: {
                        flex: 1,
                      },
                      ...(selectedFilter?.label === 'Change Done By' && {
                        onMenuOpen: () => {
                          fetchData();
                        },
                        onInputChange: debounce((query: string, actionMeta: InputActionMeta) => {
                          if (
                            actionMeta.action === 'input-change' &&
                            query !== actionMeta.prevInputValue
                          ) {
                            fetchData({
                              query,
                            });
                          }
                        }, 500),
                        onMenuScrollToBottom: () => {
                          if (!pageable.last) {
                            fetchData({ page: pageable.page + 1 });
                          }
                        },
                      }),
                    },
                  },
                ]}
              />
            );
          }}
        />
        <Close className="remove-icon" onClick={() => remove(index)} />
      </div>
    </FilterCardWrapper>
  );
};

const FiltersDrawer: FC<any> = ({ setState: _setState, onSubmit, filters }: any) => {
  const form = useForm<{
    filters: any[];
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      filters,
    },
  });

  const {
    users: {
      all: { list: allUsersList },
    },
    ontology: {
      objectTypes: {
        active: { properties = [], relations = [] },
      },
    },
  } = useTypedSelector((state) => state);

  const [options, setOptions] = useState({
    [FilterType.ENTITY_ID]: [
      ...properties,
      ...relations,
      { displayName: 'Usage Status', id: 'usageStatus.new' },
    ].map((currList) => {
      return {
        label: currList.displayName,
        value: currList.id,
      };
    }),
  });

  useEffect(() => {
    if (allUsersList) {
      const newList = allUsersList.map((currList) => ({
        label: `${currList?.firstName}`,
        value: currList?.id,
        externalId: currList?.employeeId,
      }));
      setOptions((prev) => ({
        ...prev,
        [FilterType.MODIFIED_BY_ID]: newList,
      }));
    }
  }, [allUsersList]);

  const {
    handleSubmit,
    formState: { isDirty, isValid },
    control,
    watch,
  } = form;

  const formValues = watch('filters');

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'filters',
  });

  const controlledFields = useMemo(() => {
    return fields.map((field, index) => ({
      ...field,
      ...formValues[index],
    }));
  }, [fields]);

  const dispatch = useDispatch();

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      _setState((prev: any) => ({ ...prev, showDrawer: false }));
    }, 200);
  };

  const onAddNewFilter = () => {
    const id = uuidv4();
    append({
      id,
    });
  };

  const onRemoveFilter = (index: number) => {
    remove(index);
  };

  const fetchUsersData = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE, query = '' } = params;
    dispatch(
      fetchUsers(
        {
          page,
          size,
          archived: false,
          sort: 'createdAt,desc',
          ...(query && {
            filters: generateUserSearchFilters(FilterOperators.LIKE, query),
          }),
        },
        UsersListType.ALL,
      ),
    );
  };

  useEffect(() => {
    setDrawerOpen(true);
  }, []);

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'Filters',
    hideCloseIcon: true,
    bodyContent: (
      <FiltersWrapper onSubmit={handleSubmit((data) => onSubmit(data, handleCloseDrawer))}>
        <LoadingContainer
          loading={false}
          component={
            <div className="filter-cards">
              {controlledFields.map((item, index) => {
                return (
                  <FilterCard
                    key={item.id}
                    item={item}
                    index={index}
                    form={form}
                    remove={onRemoveFilter}
                    fetchData={fetchUsersData}
                    options={options}
                    control={control}
                  />
                );
              })}
              <Button
                type="button"
                variant="secondary"
                style={{ marginBottom: 16, padding: '6px 8px' }}
                onClick={onAddNewFilter}
              >
                <AddCircleOutline style={{ marginRight: 8 }} /> Add Filter
              </Button>
            </div>
          }
        />
        <div className="footer-btn">
          <Button variant="secondary" style={{ marginLeft: 'auto' }} onClick={handleCloseDrawer}>
            Cancel
          </Button>
          <Button type="submit" disabled={!isDirty || !isValid}>
            Apply
          </Button>
        </div>
      </FiltersWrapper>
    ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return StyledDrawer;
};

export default FiltersDrawer;
