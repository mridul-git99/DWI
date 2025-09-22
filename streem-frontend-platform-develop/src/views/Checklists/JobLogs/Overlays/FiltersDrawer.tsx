import { Button, FormGroup, useDrawer } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { Constraint, filterPageTypeEnum, MandatoryParameter } from '#types';
import { labelByConstraint, validateNumber } from '#utils';
import { apiGetParameters, apiSingleParameter, baseUrl } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, InputTypes, ResponseObj } from '#utils/globalTypes';
import { request } from '#utils/request';
import { JobStateEnum } from '#views/Jobs/ListView/types';
import { getObjectData } from '#views/Ontology/utils';
import { AddCircleOutline, Close } from '@material-ui/icons';
import { camelCase, debounce, startCase } from 'lodash';
import React, { FC, useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { Controller, FormProvider, useFieldArray, useForm, useFormContext } from 'react-hook-form';
import styled from 'styled-components';
import { v4 as uuidv4 } from 'uuid';

export const FiltersWrapper = styled.form`
  display: flex;
  flex: 1;
  position: relative;
  flex-direction: column;
  padding-top: 16px;

  .form-group {
    padding: 0;
    margin-bottom: 24px;
  }

  .filter-cards {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 16px;
  }

  .footer-btn {
    display: flex;
    padding: 16px 0;
    border-top: 1px solid #ccc;
    position: sticky;
    bottom: 0;
    width: 100%;
    background: white;
  }
`;

export const FilterCardWrapper = styled.div`
  border: 1px solid #e0e0e0;
  padding-top: 16px;

  .upper-row {
    display: flex;
    align-items: center;
    padding-right: 16px;
    .remove-icon {
      cursor: pointer;
      margin-top: 6px;
      font-size: 16px;
      margin-bottom: 16px;
    }
  }

  .form-group {
    flex: 1;
    flex-direction: row;
    gap: 16px;
    padding: 0px 16px;

    > div {
      margin-bottom: 16px;
    }
  }
`;

export const metaFilters = [
  {
    label: 'Job Started At',
    value: 'startedAt',
    type: MandatoryParameter.DATE_TIME,
  },
  {
    label: 'Job Created At',
    value: 'createdAt',
    type: MandatoryParameter.DATE_TIME,
  },
  {
    label: 'Job Ended At',
    value: 'endedAt',
    type: MandatoryParameter.DATE_TIME,
  },
  {
    label: 'Job State',
    value: 'state',
    type: MandatoryParameter.SINGLE_SELECT,
    data: Object.keys(JobStateEnum).map((key) => ({
      name: startCase(camelCase(key)),
      id: key,
    })),
  },
];

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  filters: {
    op: FilterOperators.AND,
    fields: [
      { field: 'archived', op: FilterOperators.EQ, values: [false] },
      {
        field: 'type',
        op: FilterOperators.ANY,
        values: [
          MandatoryParameter.SHOULD_BE,
          MandatoryParameter.SINGLE_SELECT,
          MandatoryParameter.MULTI_LINE,
          MandatoryParameter.YES_NO,
          MandatoryParameter.NUMBER,
          MandatoryParameter.CALCULATION,
          MandatoryParameter.RESOURCE,
          MandatoryParameter.DATE,
          MandatoryParameter.DATE_TIME,
          MandatoryParameter.SINGLE_LINE,
          MandatoryParameter.MULTISELECT,
          MandatoryParameter.MULTI_RESOURCE,
        ],
      },
    ],
  },
};

const FilterCard: FC<any> = ({ item, index, remove, checklistId, styles = {}, control }) => {
  const { setValue, watch } = useFormContext();
  const formValues = watch('filters', []);
  const filter = formValues?.[index];
  const [state, setState] = useState<{
    valueOptionsLoading: boolean;
    parameterValueLoading: boolean;
    valueOptions: any[];
    selectedParameter?: any;
    selectedValue?: any;
    searchValue?: string;
  }>({
    valueOptionsLoading: false,
    parameterValueLoading: false,
    valueOptions: [],
    selectedParameter: undefined,
    searchValue: '',
  });
  const pagination = useRef({
    current: -1,
    isLast: false,
  });

  const {
    selectedParameter,
    valueOptions,
    valueOptionsLoading,
    selectedValue,
    parameterValueLoading,
  } = state;

  const inputTypeForParameter = useMemo(() => {
    if (
      [
        MandatoryParameter.RESOURCE,
        MandatoryParameter.SINGLE_SELECT,
        MandatoryParameter.YES_NO,
      ].includes(selectedParameter?.type)
    ) {
      if (
        [MandatoryParameter.RESOURCE, MandatoryParameter.SINGLE_SELECT].includes(
          selectedParameter?.type,
        ) &&
        (item?.constraint === Constraint.ANY || item?.constraint === Constraint.NIN)
      ) {
        return InputTypes.MULTI_SELECT;
      }

      return InputTypes.SINGLE_SELECT;
    } else if (
      [MandatoryParameter.MULTISELECT, MandatoryParameter.MULTI_RESOURCE].includes(
        selectedParameter?.type,
      )
    ) {
      return InputTypes.MULTI_SELECT;
    } else if (
      [MandatoryParameter.CALCULATION, MandatoryParameter.SHOULD_BE].includes(
        selectedParameter?.type,
      )
    ) {
      return InputTypes.NUMBER;
    }
    return selectedParameter?.type || InputTypes.SINGLE_LINE;
  }, [selectedParameter, item?.constraint]);

  const {
    list: parametersList,
    reset,
    fetchNext,
    status,
  } = createFetchList(apiGetParameters(checklistId), urlParams, false);

  const fetchReferenceParameter = async (parameterId: string) => {
    const { data } = await request('GET', apiSingleParameter(parameterId));
    if (data) {
      setState((prev) => ({
        ...prev,
        selectedParameter: data,
        parameterValueLoading: false,
      }));
    }
  };

  useEffect(() => {
    if (item?.key || item?.value) {
      const selectedParameterId = item.key.split('.')?.[1] || item.key.split('.')?.[0] || undefined;
      if (selectedParameterId) {
        if (
          ['checklistId', 'startedAt', 'createdAt', 'endedAt', 'state'].includes(
            selectedParameterId,
          )
        ) {
          const _selectedParameter = metaFilters.find(
            (p: any) => p.id === selectedParameterId || p.value === selectedParameterId,
          );
          setState((prev) => ({
            ...prev,
            selectedParameter: _selectedParameter,
            parameterValueLoading: false,
          }));
        } else {
          setState((prev) => ({ ...prev, parameterValueLoading: true }));
          fetchReferenceParameter(selectedParameterId);
        }
      }
    }
  }, []);

  const handleInputChange = useCallback(
    debounce((searchedString: string, actionMeta: any) => {
      if (searchedString !== actionMeta.prevInputValue && selectedParameter?.type) {
        setState((prev) => ({
          ...prev,
          searchValue: searchedString,
        }));
      }
    }, 500),
    [selectedParameter],
  );

  useEffect(() => {
    if (selectedParameter?.type && state.searchValue !== undefined) {
      if (
        selectedParameter.type === MandatoryParameter.RESOURCE ||
        selectedParameter.type === MandatoryParameter.MULTI_RESOURCE
      ) {
        pagination.current = {
          current: -1,
          isLast: true,
        };
        getOptions(0, state.searchValue);
      }
    }
  }, [state.searchValue]);

  useEffect(() => {
    if (selectedParameter) {
      if (
        selectedParameter.type === MandatoryParameter.RESOURCE ||
        selectedParameter.type === MandatoryParameter.MULTI_RESOURCE
      ) {
        getOptions(0, '');
      } else if (
        selectedParameter.type === MandatoryParameter.SINGLE_SELECT ||
        selectedParameter.type === MandatoryParameter.MULTISELECT
      ) {
        const _valueOptions = selectedParameter?.data?.map((i: any) => ({
          label: i.name,
          value: i.id,
        }));

        let _selectedValue = null;
        if (Array.isArray(filter?.value)) {
          _selectedValue = _valueOptions?.filter((o: any) => filter.value.includes(o.value));
        } else {
          _selectedValue = _valueOptions?.find((o: any) => o.value === filter?.value);
          _selectedValue = _selectedValue ? [_selectedValue] : null;
        }

        setState((prev) => ({
          ...prev,
          valueOptions: _valueOptions,
          selectedValue: _selectedValue || prev?.selectedValue,
        }));
      } else if (selectedParameter.type === MandatoryParameter.YES_NO) {
        const _valueOptions = selectedParameter?.data?.map((i: any) => ({
          label: i.name,
          value: i.id,
          type: i.type,
        }));
        const _selectedValue = _valueOptions?.find(
          (o: any) => o.value === filter?.value || o.value === filter?.value?.[0],
        );
        setState((prev) => ({
          ...prev,
          valueOptions: _valueOptions,
          selectedValue: _selectedValue ? [_selectedValue] : prev?.selectedValue,
        }));
      }
    }
  }, [selectedParameter]);

  const getOptions = async (
    page: number = pagination.current.current + 1,
    searchValue: string = '',
  ) => {
    if (
      selectedParameter.type === MandatoryParameter.RESOURCE ||
      selectedParameter.type === MandatoryParameter.MULTI_RESOURCE
    ) {
      setState((prev) => ({
        ...prev,
        valueOptionsLoading: true,
        valueOptions: page === 0 ? [] : prev?.valueOptions,
      }));
      try {
        const response: ResponseObj<any> = await request(
          'GET',
          `${baseUrl}${selectedParameter.data.urlPath}`,
          {
            params: {
              page,
              ...(searchValue ? { query: searchValue } : {}),
            },
          },
        );
        if (response.data) {
          if (response.pageable) {
            pagination.current = {
              current: response.pageable?.page,
              isLast: response.pageable?.last,
            };
          }
          const optionsToSet = response.data.map((o: any) => ({
            value: o.id,
            label: o.displayName,
            externalId: o.externalId,
          }));

          let _selectedValues = [];
          const filterValues = filter?.value
            ? Array.isArray(filter.value)
              ? filter.value
              : [filter?.value]
            : null;

          if (filterValues && filterValues.length) {
            const promises = filterValues?.map(async (value: string) => {
              let _selectedValue;
              if (value && selectedParameter.data.collection) {
                const object = await getObjectData({
                  id: value,
                  collection: selectedParameter.data.collection,
                });
                _selectedValue = {
                  value: object.id,
                  label: object.displayName,
                  externalId: object.externalId,
                };
              }
              return _selectedValue;
            });

            _selectedValues = await Promise.all(promises);
          }

          setState((prev) => ({
            ...prev,
            valueOptions: page === 0 ? optionsToSet : [...prev.valueOptions, ...optionsToSet],
            valueOptionsLoading: false,
            selectedValue: _selectedValues.length ? _selectedValues : prev.selectedValue,
            searchValue: prev.searchValue,
          }));
        }
      } catch (e) {
        setState((prev) => ({ ...prev, isLoading: false }));
      }
    }
  };

  return (
    <FilterCardWrapper style={styles}>
      <div className="upper-row">
        <Controller
          control={control}
          name={`filters.${index}.key`}
          rules={{
            required: true,
          }}
          defaultValue={item?.key || null}
          render={({ onChange }: any) => {
            return (
              <FormGroup
                inputs={[
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'field',
                      label: 'Where',
                      options: [...metaFilters, ...parametersList].map((parameter: any) => ({
                        label: parameter.label,
                        value: parameter.id,
                        ...parameter,
                      })),
                      value: selectedParameter
                        ? [
                            {
                              label: selectedParameter.label,
                              value: selectedParameter.id,
                            },
                          ]
                        : null,
                      placeholder: 'Select',
                      isLoading: status === 'loading' || parameterValueLoading,
                      onMenuOpen: () => {
                        reset({ params: urlParams });
                      },
                      onChange: (value: any) => {
                        pagination.current = {
                          current: -1,
                          isLast: false,
                        };
                        if (metaFilters.some((currFilter) => currFilter.value === value.value)) {
                          onChange(value.value);
                        } else {
                          onChange(`parameterValues.${value.value}`);
                        }
                        setValue(`filters.${index}.constraint`, null);
                        setValue(`filters.${index}.value`, null, {
                          shouldDirty: true,
                          shouldValidate: true,
                        });
                        setState((prev) => ({
                          ...prev,
                          selectedParameter: value,
                          selectedValue: undefined,
                        }));
                      },

                      onMenuScrollToBottom: () => {
                        fetchNext();
                      },
                      onInputChange: debounce((value: string, actionMeta) => {
                        if (value !== actionMeta.prevInputValue) {
                          reset({
                            params: {
                              ...urlParams,
                              filters: {
                                ...urlParams.filters,
                                fields: [
                                  ...urlParams.filters.fields,
                                  { field: 'label', op: FilterOperators.LIKE, values: [value] },
                                ],
                              },
                            },
                          });
                        }
                      }, 500),
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
          name={`filters.${index}.constraint`}
          rules={{
            required: true,
          }}
          defaultValue={item?.constraint || null}
          render={({ onChange, value }: any) => {
            return (
              <FormGroup
                inputs={[
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'constraint',
                      label: 'Condition',
                      options: Object.entries(
                        labelByConstraint(
                          selectedParameter?.type,
                          filterPageTypeEnum.JOB_LOGS_FILTERS_DRAWER,
                        ) as any,
                      ).map(([value, label]) => ({ label, value })),
                      placeholder: 'Select Condition',
                      style: {
                        flex: 1,
                      },
                      onChange: (value: any) => {
                        onChange(value.value);
                        setValue(`filters.${index}.value`, null, {
                          shouldDirty: true,
                          shouldValidate: true,
                        });
                        setState((prev) => ({ ...prev, selectedValue: undefined }));
                      },
                      value: value
                        ? [
                            {
                              label: (
                                labelByConstraint(
                                  selectedParameter?.type,
                                  filterPageTypeEnum.JOB_LOGS_FILTERS_DRAWER,
                                ) as any
                              )[value],
                              value: value,
                            },
                          ]
                        : null,
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
          defaultValue={item?.value ? item.value : validateNumber(item?.value) ? item.value : null}
          render={({ onChange, value }: any) => {
            if (
              [
                MandatoryParameter.SINGLE_SELECT,
                MandatoryParameter.RESOURCE,
                MandatoryParameter.YES_NO,
                MandatoryParameter.MULTISELECT,
                MandatoryParameter.MULTI_RESOURCE,
              ].includes(selectedParameter?.type)
            ) {
              return (
                <FormGroup
                  inputs={[
                    {
                      type: inputTypeForParameter,
                      props: {
                        id: 'value',
                        label: 'Value',
                        options: valueOptions,
                        value: selectedValue,
                        isSearchable: true,
                        placeholder: 'Select Value',
                        onChange: (value: any) => {
                          let newValue = null;

                          if (Array.isArray(value)) {
                            newValue = value?.length ? value.map((v: any) => v.value) : null;
                          } else {
                            newValue = value?.value ? [value.value] : null;
                          }

                          onChange(newValue);
                          setState((prev) => ({ ...prev, selectedValue: value }));
                        },
                        style: {
                          flex: 1,
                        },
                        isLoading: valueOptionsLoading,
                        onInputChange: handleInputChange,
                        onMenuScrollToBottom: () => {
                          if (!valueOptionsLoading && !pagination.current.isLast) {
                            getOptions(pagination.current.current + 1, state.searchValue);
                          }
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
                    type: inputTypeForParameter,
                    props: {
                      id: 'value',
                      label: 'Value',
                      value: validateNumber(value) ? value : '',
                      placeholder: 'Enter Value',
                      onChange: ({ value }: any) => {
                        let newValue = value;
                        if (
                          [
                            InputTypes.DATE,
                            InputTypes.DATE_TIME,
                            InputTypes.NUMBER,
                            MandatoryParameter.CALCULATION,
                          ].includes(selectedParameter?.type)
                        ) {
                          newValue = validateNumber(value) ? parseFloat(value) : null;
                        }
                        onChange(newValue);
                      },
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
        <Close className="remove-icon" onClick={() => remove(index)} />
      </div>
    </FilterCardWrapper>
  );
};

const FiltersDrawer: FC<any> = ({ setState: _setState, checklistId, filters, onSubmit }: any) => {
  const formMethods = useForm<{
    filters: any[];
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      filters: (filters || []).map((filter: any) => ({
        key: filter.key,
        constraint: filter.constraint,
        value: filter.value,
      })),
    },
  });

  const {
    handleSubmit,
    formState: { isDirty, isValid },
    control,
    watch,
  } = formMethods;

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

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      _setState((prev: any) => ({ ...prev, showDrawer: false }));
    }, 200);
  };

  useEffect(() => {
    setDrawerOpen(true);
  }, [checklistId]);

  const onAddNewFilter = () => {
    const id = uuidv4();
    append({
      id,
    });
  };

  const onRemoveFilter = (index: number) => {
    remove(index);
  };

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'More Filters',
    hideCloseIcon: true,
    bodyContent: (
      <FormProvider {...formMethods}>
        <FiltersWrapper onSubmit={handleSubmit((data) => onSubmit(data, handleCloseDrawer))}>
          <div className="filter-cards">
            {controlledFields.map((item, index) => {
              return (
                <FilterCard
                  key={item.id}
                  item={item}
                  index={index}
                  remove={onRemoveFilter}
                  checklistId={checklistId}
                  control={control}
                  styles={item?.key === 'checklistId' ? { display: 'none' } : {}}
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
          <div className="footer-btn">
            <Button variant="secondary" style={{ marginLeft: 'auto' }} onClick={handleCloseDrawer}>
              Cancel
            </Button>
            <Button type="submit" disabled={!isDirty || !isValid}>
              Apply
            </Button>
          </div>
        </FiltersWrapper>
      </FormProvider>
    ),
  });

  return StyledDrawer;
};

export default FiltersDrawer;
