import { Button, FormGroup, useDrawer } from '#components';
import { MandatoryParameter } from '#types';
import { labelByConstraint, validateNumber } from '#utils';
import { InputTypes } from '#utils/globalTypes';
import { JobStateEnum } from '#views/Jobs/ListView/types';
import { AddCircleOutline, Close } from '@material-ui/icons';
import { camelCase, startCase } from 'lodash';
import React, { FC, useEffect, useMemo, useRef, useState } from 'react';
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

const metaFilters = [
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

const getParameterType = (type: any) => {
  if (
    type === MandatoryParameter.RESOURCE ||
    type === MandatoryParameter.SINGLE_SELECT ||
    type === MandatoryParameter.YES_NO
  ) {
    return InputTypes.SINGLE_SELECT;
  } else if (type === MandatoryParameter.CALCULATION || type === MandatoryParameter.SHOULD_BE) {
    return InputTypes.NUMBER;
  }
  return type || InputTypes.SINGLE_LINE;
};

export const FilterCard: FC<any> = ({ item, index, remove, styles = {}, control }) => {
  const { setValue, watch } = useFormContext();
  const formValues = watch('filters', []);
  const filter = formValues?.[index];
  const [state, setState] = useState<{
    valueOptions: any[];
    selectedParameter?: any;
    selectedValue?: any;
  }>({
    valueOptions: [],
    selectedParameter: undefined,
  });
  const pagination = useRef({
    current: -1,
    isLast: false,
  });

  const { selectedParameter, valueOptions, selectedValue } = state;

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
          }));
        }
      }
    }
  }, []);

  useEffect(() => {
    if (selectedParameter) {
      const _valueOptions = selectedParameter?.data?.map((i: any) => ({
        label: i.name,
        value: i.id,
      }));
      const _selectedValue = _valueOptions?.find((o: any) => o.value === filter?.value);
      setState((prev) => ({
        ...prev,
        valueOptions: _valueOptions,
        selectedValue: _selectedValue ? [_selectedValue] : prev?.selectedValue,
      }));
    }
  }, [selectedParameter]);

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
                      options: metaFilters.map((parameter: any) => ({
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
                      options: selectedParameter
                        ? Object.entries(
                            labelByConstraint(getParameterType(selectedParameter?.type)),
                          ).map(([value, label]) => ({ label, value }))
                        : [],
                      placeholder: 'Select Condition',
                      style: {
                        flex: 1,
                      },
                      onChange: (value: any) => {
                        onChange(value.value);
                      },
                      value: value
                        ? [
                            {
                              label: (labelByConstraint(selectedParameter?.type) as any)[value],
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
          defaultValue={item?.value || null}
          render={({ onChange, value }: any) => {
            if (selectedParameter?.type === MandatoryParameter.SINGLE_SELECT) {
              return (
                <FormGroup
                  inputs={[
                    {
                      type: getParameterType(selectedParameter?.type),
                      props: {
                        id: 'value',
                        label: 'Value',
                        options: valueOptions,
                        value: selectedValue,
                        isSearchable: false,
                        placeholder: 'Select Value',
                        onChange: (value: any) => {
                          onChange(value.value);
                          setState((prev) => ({ ...prev, selectedValue: [value] }));
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
                    type: getParameterType(selectedParameter?.type),
                    props: {
                      id: 'value',
                      label: 'Value',
                      defaultValue: value,
                      placeholder: 'Enter Value',
                      onChange: ({ value }: any) => {
                        let newValue = value;
                        if (
                          [InputTypes.DATE, InputTypes.DATE_TIME, InputTypes.NUMBER].includes(
                            selectedParameter?.type,
                          )
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

const JobLogsFilterDrawer: FC<any> = ({ setShowDrawer, filters, onSubmit }: any) => {
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
      setShowDrawer(false);
    }, 200);
  };

  useEffect(() => {
    setDrawerOpen(true);
  }, []);

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

export default JobLogsFilterDrawer;
