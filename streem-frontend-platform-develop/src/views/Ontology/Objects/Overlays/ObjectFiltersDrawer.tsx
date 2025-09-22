import { Button, FormGroup, LoadingContainer, useDrawer } from '#components';
import { Constraint, filterPageTypeEnum } from '#types/common';
import { labelByConstraint, validateNumber } from '#utils';
import { baseUrl } from '#utils/apiUrls';
import { InputTypes } from '#utils/globalTypes';
import { request } from '#utils/request';
import {
  FilterCardWrapper,
  FiltersWrapper,
} from '#views/Checklists/JobLogs/Overlays/FiltersDrawer';
import { CommonFields } from '#views/Ontology/types';
import { AddCircleOutline, Close } from '@material-ui/icons';
import { isArray, debounce } from 'lodash';
import React, { FC, useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { Controller, FormProvider, useFieldArray, useForm, useFormContext } from 'react-hook-form';
import { v4 as uuidv4 } from 'uuid';

const FilterCard: FC<any> = ({ item, index, remove, activeObjectType }) => {
  const { control } = useFormContext();

  const [selectedObjectProperty, setSelectedObjectProperty] = useState<any>(null);

  const [relationOptions, setRelationOptions] = useState<{
    isFetching: boolean;
    options: CommonFields[];
    searchValue: string;
  }>({
    isFetching: false,
    options: [],
    searchValue: '',
  });

  const relationPagination = useRef({
    current: -1,
    isLast: true,
  });

  const { properties = [], relations = [] } = activeObjectType;

  const inputTypeForConstant = useMemo(() => {
    let type = selectedObjectProperty?.inputType || InputTypes.SINGLE_SELECT;

    if (
      selectedObjectProperty?.inputType === InputTypes.MULTI_SELECT ||
      selectedObjectProperty?.target?.cardinality === InputTypes.ONE_TO_MANY
    ) {
      type = InputTypes.MULTI_SELECT;
    } else if (
      selectedObjectProperty?.inputType === InputTypes.SINGLE_SELECT ||
      selectedObjectProperty?.target?.cardinality === InputTypes.ONE_TO_ONE
    ) {
      if (item?.op === Constraint.EQ || item?.op === Constraint.NE) {
        type = InputTypes.SINGLE_SELECT;
      } else {
        type = InputTypes.MULTI_SELECT;
      }
    }

    return type;
  }, [selectedObjectProperty, item?.op]);

  const getOptions = async (
    path: string,
    page: number = relationPagination.current.current + 1,
    searchValue: string = '',
  ) => {
    setRelationOptions((prev) => ({
      ...prev,
      isFetching: true,
    }));
    if (path) {
      const response: {
        data: CommonFields[];
        errors: { message: string }[];
        pageable: any;
      } = await request('GET', `${baseUrl}${path}`, {
        params: {
          page,
          ...(searchValue ? { query: searchValue } : {}),
        },
      });
      if (response.pageable) {
        relationPagination.current = {
          current: response.pageable?.page,
          isLast: response.pageable?.last,
        };
      }
      if (response.data) {
        setRelationOptions((prev) => ({
          isFetching: false,
          options: page === 0 ? response.data : [...prev.options, ...response.data],
          searchValue: prev.searchValue,
        }));
      }
    }
  };

  const handleMenuScrollToBottom = (path: string) => {
    if (!relationOptions.isFetching && !relationPagination.current.isLast) {
      getOptions(path, relationPagination.current.current + 1, relationOptions.searchValue);
    }
  };

  const handleInputChange = useCallback(
    debounce((searchedString: string, actionMeta: any) => {
      if (searchedString !== actionMeta.prevInputValue && selectedObjectProperty?.target) {
        setRelationOptions((prev) => ({
          ...prev,
          searchValue: searchedString,
        }));
      }
    }, 500),
    [selectedObjectProperty],
  );

  useEffect(() => {
    if (item) {
      const selectedProperty = [...properties, ...relations].find(
        (objectTypeProperty) => objectTypeProperty?.id === item?.field?.split('.')[1],
      );
      if (selectedProperty) {
        setSelectedObjectProperty({
          ...selectedProperty,
          label: selectedProperty?.displayName,
        });
      }
    }
  }, []);

  useEffect(() => {
    if (selectedObjectProperty?.target) {
      getOptions(selectedObjectProperty?.target?.urlPath, 0, '');
    }
  }, [selectedObjectProperty]);

  useEffect(() => {
    if (selectedObjectProperty?.target && relationOptions.searchValue !== undefined) {
      relationPagination.current = {
        current: -1,
        isLast: true,
      };
      getOptions(selectedObjectProperty?.target?.urlPath, 0, relationOptions.searchValue);
    }
  }, [relationOptions.searchValue]);

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
                      id: 'propertyId',
                      label: 'Where',
                      placeholder: 'Select',
                      value: selectedObjectProperty
                        ? [
                            {
                              label: selectedObjectProperty.label,
                              value: selectedObjectProperty.id,
                            },
                          ]
                        : null,
                      options: [...properties, ...relations].map((objectTypeProperty) => ({
                        externalId: objectTypeProperty?.externalId,
                        label: objectTypeProperty.displayName,
                        value: objectTypeProperty.id,
                        inputType: objectTypeProperty?.inputType,
                        _options: objectTypeProperty?.options,
                        target: objectTypeProperty?.target,
                      })),
                      style: {
                        flex: 1,
                      },
                      onChange: (value: any) => {
                        onChange(`searchable.${value.value}`);
                        control.setValue(`filters.${index}.op`, null);
                        control.setValue(`filters.${index}.value`, '', {
                          shouldDirty: true,
                          shouldValidate: true,
                        });
                        setSelectedObjectProperty({ ...value, options: value._options });
                        relationPagination.current = {
                          current: -1,
                          isLast: true,
                        };
                        setRelationOptions({
                          isFetching: false,
                          options: [],
                          searchValue: '',
                        });
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
                      value: value
                        ? [
                            {
                              label: (
                                labelByConstraint(
                                  selectedObjectProperty?.inputType
                                    ? selectedObjectProperty.inputType
                                    : selectedObjectProperty?.target?.cardinality,
                                  filterPageTypeEnum.OBJECTS_FILTERS_DRAWER,
                                ) as any
                              )[value],
                              value: value,
                            },
                          ]
                        : null,
                      options: Object.entries(
                        labelByConstraint(
                          selectedObjectProperty?.inputType
                            ? selectedObjectProperty.inputType
                            : selectedObjectProperty?.target?.cardinality,
                          filterPageTypeEnum.OBJECTS_FILTERS_DRAWER,
                        ),
                      ).map(([value, label]) => ({ label, value })),
                      onChange: (value: any) => {
                        onChange(value.value);
                        control.setValue(`filters.${index}.value`, '', {
                          shouldDirty: true,
                          shouldValidate: true,
                        });
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
          name={`filters.${index}.value`}
          rules={{
            required: true,
          }}
          defaultValue={validateNumber(item.value) ? item.value : null}
          render={({ onChange, value }) => {
            const isSelectable = [InputTypes.SINGLE_SELECT, InputTypes.MULTI_SELECT].includes(
              selectedObjectProperty?.inputType,
            );
            let selectOptions = [];
            if (isSelectable || selectedObjectProperty?.target) {
              selectOptions = selectedObjectProperty?.target
                ? relationOptions.options
                : selectedObjectProperty.options;
            }

            return (
              <FormGroup
                style={{ marginBottom: '24px' }}
                inputs={[
                  {
                    type: inputTypeForConstant,
                    props: {
                      id: 'value',
                      label: 'Value',
                      placeholder: 'Enter Value',
                      isSearchable: true,
                      value,
                      options: selectOptions.map((option: any) => ({
                        label: option.displayName,
                        value: option.id,
                        externalId: option.externalId,
                      })),
                      onChange: (value: any) => {
                        let newValue = value.value || null;
                        if (isSelectable || selectedObjectProperty?.target) {
                          if (value) {
                            if (isArray(value)) {
                              newValue = value.length ? value : null;
                            } else {
                              newValue = value.value ? value : null;
                            }
                          }
                        } else if (
                          [InputTypes.DATE, InputTypes.DATE_TIME, InputTypes.NUMBER].includes(
                            selectedObjectProperty?.inputType,
                          )
                        ) {
                          newValue = validateNumber(value.value) ? parseFloat(value.value) : null;
                        }
                        onChange(newValue);
                      },
                      onInputChange: handleInputChange,
                      onMenuScrollToBottom: () => {
                        if (selectedObjectProperty?.target) {
                          handleMenuScrollToBottom(selectedObjectProperty?.target?.urlPath);
                        }
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

const ObjectFiltersDrawer: FC<any> = ({
  setShowFiltersDrawer,
  activeObjectType,
  onSubmit,
  existingFilters,
}: any) => {
  const form = useForm<{
    filters: any[];
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      filters: existingFilters?.filters?.fields || [],
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
      setShowFiltersDrawer(false);
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
                    <FilterCard
                      key={item.id}
                      item={item}
                      index={index}
                      remove={onRemoveFilter}
                      activeObjectType={activeObjectType}
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

export default ObjectFiltersDrawer;
