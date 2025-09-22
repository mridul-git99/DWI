import { ChecklistStatesContent } from '#PrototypeComposer/checklist.types';
import { CollaboratorType } from '#PrototypeComposer/reviewer.types';
import { Button, FormGroup, LoadingContainer, useDrawer } from '#components';
import { FilterOperators, InputTypes } from '#utils/globalTypes';
import {
  FilterCardWrapper,
  FiltersWrapper,
} from '#views/Checklists/JobLogs/Overlays/FiltersDrawer';
import { AddCircleOutline, Close } from '@material-ui/icons';
import React, { FC, useEffect, useMemo } from 'react';
import { Controller, FormProvider, useFieldArray, useForm, useFormContext } from 'react-hook-form';
import { v4 as uuidv4 } from 'uuid';

const metaFilters = [
  {
    label: 'Prototype State',
    value: 'state',
    field: 'state',
  },
  {
    label: 'I am Involved as',
    value: 'collaborator',
    field: 'collaborator',
  },
];

const metaFiltersValues = (filterType: string) => {
  switch (filterType) {
    case 'state':
      return [
        ...Object.keys(ChecklistStatesContent)
          .filter((key) => key !== 'PUBLISHED')
          .map((key) => ({
            label: ChecklistStatesContent[key as keyof typeof ChecklistStatesContent],
            value: key,
            field: 'state',
          })),
      ];
    case 'collaborator':
      return [
        { label: 'As Author', value: CollaboratorType.AUTHOR },
        { label: 'As Collaborator', value: CollaboratorType.REVIEWER },
        { label: 'Not Involved', value: 'NOT' },
      ];
    default:
      return;
  }
};

const FilterCard: FC<any> = ({ item, index, remove }) => {
  const { control, register, setValue, watch } = useFormContext();

  const formValues = watch('filters');

  const prototypeFilterOptions = useMemo(() => {
    return metaFilters.filter(
      (filter) =>
        !formValues.some((formValueFilter: any) => formValueFilter?.field === filter?.field),
    );
  }, [formValues]);

  const prototypeFilterValue = useMemo(() => {
    if (!item?.field) return null;
    return metaFilters.find((currFilter) => {
      if (item.field === 'state') {
        return currFilter.value === item.field;
      } else {
        return currFilter.value === 'collaborator';
      }
    });
  }, [item?.field]);

  const filterValueOptions = useMemo(() => {
    if (!item?.field) return [];
    return item.field === 'state'
      ? metaFiltersValues(item.field)
      : metaFiltersValues('collaborator');
  }, [item?.field]);

  const filterValue = useMemo(() => {
    if (!item?.values?.length) return null;
    if (item?.field === 'state') {
      return item.values.map((val) => ({
        label: ChecklistStatesContent[val],
        value: val,
      }));
    } else {
      const value = item.values[0];
      if (value === CollaboratorType.AUTHOR) {
        return [{ label: 'As Author', value: CollaboratorType.AUTHOR }];
      } else if (value === CollaboratorType.REVIEWER) {
        return [{ label: 'As Collaborator', value: CollaboratorType.REVIEWER }];
      } else {
        return [{ label: 'Not Involved', value: 'NOT' }];
      }
    }
  }, [item?.field, item?.values]);

  return (
    <FilterCardWrapper>
      <div className="upper-row">
        <input
          name={`filters.${index}.id`}
          ref={register()}
          defaultValue={item?.id}
          type="hidden"
        />
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
                      id: 'prototypeFilter',
                      label: 'Where',
                      placeholder: 'Select',
                      options: prototypeFilterOptions,
                      value: prototypeFilterValue,
                      onChange: (value: any) => {
                        onChange(value.field);
                        setValue(`filters.${index}.values`, null);
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
                      id: 'prototypeCondition',
                      label: 'Condition',
                      placeholder: 'Select Condition',
                      options: [{ label: 'Is', value: FilterOperators.EQ }],
                      value: value ? [{ label: 'Is', value: FilterOperators.EQ }] : null,
                      onChange: (value: any) => {
                        onChange(value.value);
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
          name={`filters.${index}.values`}
          rules={{
            required: true,
          }}
          defaultValue={item?.values || null}
          render={({ onChange }) => {
            return (
              <FormGroup
                inputs={[
                  {
                    type:
                      item?.field === 'state' ? InputTypes.MULTI_SELECT : InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'value',
                      label: 'Value',
                      placeholder: 'Enter Value',
                      options: filterValueOptions,
                      onChange: (options: any) => {
                        const filterOptions = Array.isArray(options) ? options : [options];
                        onChange(
                          filterOptions?.length
                            ? filterOptions?.map((option: any) => option.value)
                            : null,
                        );
                      },
                      value: filterValue,
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

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      _setState(false);
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
            <FormProvider {...form}>
              <div className="filter-cards">
                {controlledFields.map((item, index) => {
                  return (
                    <FilterCard key={item.id} item={item} index={index} remove={onRemoveFilter} />
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
            </FormProvider>
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
