import { FormGroup } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { filterPageTypeEnum, MandatoryParameter, Parameter, TargetEntityType } from '#types';
import { labelByConstraint } from '#utils';
import { apiGetParameters, apiGetPartialObject, apiSingleParameter } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, SELECTOR_OPTIONS } from '#utils/constants';
import { FilterField, FilterOperators, InputTypes, SelectorOptionsEnum } from '#utils/globalTypes';
import { request } from '#utils/request';
import { ObjectType } from '#views/Ontology/types';
import { getObjectData } from '#views/Ontology/utils';
import { Close } from '@material-ui/icons';
import { capitalize, debounce } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { getDateUnits } from '../ValidationViews/Resource';

type TResourceFilterRowProps = {
  index: number;
  item: Record<string, any>;
  isActiveLoading: boolean;
  isReadOnly: boolean;
  fields: any[];
  updateFilters: (data: any) => void;
  data: Record<string, any>;
  fieldKeysCorrection: (index: number) => void;
  isVariationView: boolean;
  handleConstantValue: (data: any) => void;
  selectedObjectType: ObjectType;
  propertiesMap: Record<string, any>;
  checklistId: string;
  parameter: Parameter;
};

const ResourceFilterRow: FC<TResourceFilterRowProps> = ({
  index,
  item,
  isActiveLoading,
  isReadOnly,
  fields,
  updateFilters,
  data,
  fieldKeysCorrection,
  isVariationView,
  handleConstantValue,
  selectedObjectType,
  propertiesMap,
  checklistId,
  parameter,
}) => {
  const [selectedParameter, setSelectedParameter] = useState<Parameter>();
  const [parametersFilterFields, setParametersFilterFields] = useState<FilterField[]>([]);
  const [selectedValues, setSelectedValues] = useState<any>([]);

  const selectedObjectProperty = useMemo(() => {
    return propertiesMap?.current[item?.field?.split('.')?.[1]];
  }, [propertiesMap?.current, item?.field]);

  const filterSelectOptions = useMemo(() => {
    return selectedObjectProperty?.options || [];
  }, [selectedObjectProperty]);

  const {
    list: relationList,
    reset: relationReset,
    pagination: relationPagination,
    fetchNext: relationFetchNext,
  } = createFetchList(
    apiGetPartialObject(),
    {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      collection: selectedObjectProperty?.externalId,
      usageStatus: 1,
    },
    false,
  );

  const handleRelationObjectFetch = async (externalId: string, searchQuery: string = '') => {
    relationReset({
      params: {
        page: DEFAULT_PAGE_NUMBER,
        size: DEFAULT_PAGE_SIZE,
        collection: externalId,
        query: searchQuery,
        usageStatus: 1,
      },
    });
  };

  const urlParams = useMemo(
    () => ({
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      filters: {
        op: FilterOperators.AND,
        fields: [
          { field: 'archived', op: FilterOperators.EQ, values: [false] },
          ...(parameter?.targetEntityType === TargetEntityType.PROCESS
            ? [
                {
                  field: 'targetEntityType',
                  op: FilterOperators.EQ,
                  values: [TargetEntityType.PROCESS],
                },
              ]
            : []),
        ],
      },
    }),
    [parameter?.targetEntityType],
  );

  const { list, reset, fetchNext } = createFetchList(
    apiGetParameters(checklistId),
    urlParams,
    false,
  );

  const parameterOptionsList = useMemo(() => {
    if (item.selector) {
      if (
        selectedObjectProperty?.target?.cardinality === InputTypes.ONE_TO_ONE &&
        [FilterOperators.EQ, FilterOperators.NE].includes(item?.op)
      ) {
        return list.filter((parameter) => parameter?.type === MandatoryParameter.RESOURCE);
      }
      return list;
    } else {
      return [];
    }
  }, [list, item.selector, item.op]);

  const inputTypeForConstant = useMemo(() => {
    let type = InputTypes.SINGLE_SELECT;
    if (
      selectedObjectProperty?.target?.cardinality === InputTypes.ONE_TO_MANY ||
      selectedObjectProperty?.inputType === InputTypes.MULTI_SELECT
    ) {
      type = InputTypes.MULTI_SELECT;
    } else if (
      selectedObjectProperty?.target?.cardinality === InputTypes.ONE_TO_ONE ||
      selectedObjectProperty?.inputType === InputTypes.SINGLE_SELECT
    ) {
      if (item?.op === FilterOperators.EQ || item?.op === FilterOperators.NE) {
        type = InputTypes.SINGLE_SELECT;
      } else {
        type = InputTypes.MULTI_SELECT;
      }
    }
    return type;
  }, [selectedObjectProperty, item.op]);

  const fetchReferenceParameter = async (referencedParameterId: string) => {
    const { data } = await request('GET', apiSingleParameter(referencedParameterId));
    if (data) {
      setSelectedParameter(data);
    }
  };

  const handleChangeObjectProperty = (value: any) => {
    if (value?.target?.cardinality && value?.target?.urlPath) {
      handleRelationObjectFetch(value?.externalId, '');
      setParametersFilterFields([
        {
          field: 'type',
          op: FilterOperators.ANY,
          values: [MandatoryParameter.RESOURCE, MandatoryParameter.MULTI_RESOURCE],
        },
        {
          field: 'data->objectTypeId',
          op: FilterOperators.EQ,
          values: [selectedObjectProperty.objectTypeId],
        },
      ]);
    } else if (value.inputType === MandatoryParameter.SINGLE_SELECT) {
      setParametersFilterFields([
        {
          field: 'type',
          op: FilterOperators.ANY,
          values: [MandatoryParameter.SINGLE_SELECT],
        },
        {
          field: 'metadata->propertyId',
          op: FilterOperators.EQ,
          values: [selectedObjectProperty.id],
        },
      ]);
    } else if (value.inputType === InputTypes.MULTI_SELECT) {
      setParametersFilterFields([
        {
          field: 'type',
          op: FilterOperators.ANY,
          values: [MandatoryParameter.MULTISELECT],
        },
        {
          field: 'metadata->propertyId',
          op: FilterOperators.EQ,
          values: [selectedObjectProperty.id],
        },
      ]);
    } else if ([InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(value.inputType)) {
      setParametersFilterFields([
        {
          field: 'type',
          op: FilterOperators.ANY,
          values: [MandatoryParameter.NUMBER, MandatoryParameter.CALCULATION],
        },
      ]);
    } else {
      setParametersFilterFields([
        {
          field: 'type',
          op: FilterOperators.ANY,
          values:
            value.inputType === MandatoryParameter.NUMBER
              ? [value.inputType, MandatoryParameter.CALCULATION]
              : [value.inputType],
        },
      ]);
    }
  };

  const fetchSelectedObjects = async () => {
    let selectedOptions = [];
    const promises = item?.values?.map(async (id: string) => {
      let selectedOption;
      if (id && selectedObjectProperty?.externalId) {
        const object = await getObjectData({
          id,
          collection: selectedObjectProperty.externalId,
        });
        selectedOption = {
          value: object.id,
          label: object.displayName,
          externalId: `(ID: ${object.externalId})`,
        };
      }
      return selectedOption;
    });

    selectedOptions = await Promise.all(promises);
    setSelectedValues(selectedOptions);
  };

  useEffect(() => {
    if (selectedObjectProperty && !isReadOnly) {
      handleChangeObjectProperty(selectedObjectProperty);
    }
  }, [selectedObjectProperty]);

  useEffect(() => {
    if (!isReadOnly && parametersFilterFields.length > 0) {
      reset({
        params: {
          ...urlParams,
          filters: {
            ...urlParams.filters,
            fields: [...urlParams.filters.fields, ...parametersFilterFields],
          },
        },
      });
    }
  }, [parametersFilterFields]);

  useEffect(() => {
    if (item?.referencedParameterId) {
      fetchReferenceParameter(item?.referencedParameterId);
    }
  }, []);

  useEffect(() => {
    if (item?.values?.length) {
      if (item?.fieldType === 'PROPERTY' && filterSelectOptions.length) {
        const selectedOptions = filterSelectOptions
          .filter((option) => item.values.includes(option.id))
          .map((option) => ({
            ...option,
            label: option.displayName,
            value: option.id,
          }));

        setSelectedValues(selectedOptions);
      } else if (item?.fieldType === 'RELATION' && selectedObjectProperty) {
        fetchSelectedObjects();
      }
    }
  }, [filterSelectOptions, selectedObjectProperty]);

  return (
    <div className="filter" key={item.id}>
      <div className="filter-header">
        <div>Filter {index + 1}</div>
      </div>
      <div className="filter-text">For {selectedObjectType?.displayName} filter by</div>
      <div className="upper-row">
        <FormGroup
          inputs={[
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'objectProperty',
                label: 'Object Property/Relations',
                isLoading: isActiveLoading,
                options: [
                  ...(selectedObjectType?.properties || []),
                  ...(selectedObjectType?.relations || []),
                ].map((objectTypeProperty) => ({
                  _options: objectTypeProperty?.options,
                  externalId: objectTypeProperty?.externalId,
                  label: objectTypeProperty.displayName,
                  value: objectTypeProperty.id,
                  inputType: objectTypeProperty?.inputType,
                  target: objectTypeProperty?.target,
                })),
                isDisabled: isReadOnly,
                value: item?.field
                  ? [
                      {
                        label: selectedObjectProperty?.displayName,
                        value: selectedObjectProperty?.id,
                      },
                    ]
                  : null,
                placeholder: 'Select Object Property',
                onChange: (value: any) => {
                  fields[index] = {
                    id: fields[index].id,
                    field: `searchable.${value.value}`,
                    fieldType: value?.target ? 'RELATION' : 'PROPERTY',
                    ...(value?.inputType && {
                      propertyType: value?.inputType,
                    }),
                  };
                  const _propertyFilters = {
                    ...data.propertyFilters,
                    fields,
                  };
                  updateFilters(_propertyFilters);
                },
              },
            },
            ...(selectedObjectProperty?.inputType || selectedObjectProperty?.target?.cardinality
              ? [
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'objectPropertyCondition',
                      label: 'Condition is',
                      options: Object.entries(
                        labelByConstraint(
                          selectedObjectProperty?.target?.cardinality ||
                            selectedObjectProperty?.inputType,
                          filterPageTypeEnum.RESOURCE_FILTERS,
                        ),
                      ).map(([value, label]) => ({ label, value })),
                      placeholder: 'Select One',
                      isDisabled: isReadOnly,
                      value: item?.op
                        ? [
                            {
                              label: (
                                labelByConstraint(
                                  selectedObjectProperty?.inputType ||
                                    selectedObjectProperty?.target?.cardinality,
                                  filterPageTypeEnum.RESOURCE_FILTERS,
                                ) as any
                              )[item.op],
                              value: item.op,
                            },
                          ]
                        : null,
                      onChange: (value: any) => {
                        fieldKeysCorrection(index);

                        fields[index] = {
                          ...fields[index],
                          op: value.value,
                        };

                        const _propertyFilters = {
                          ...data.propertyFilters,
                          fields,
                        };
                        updateFilters(_propertyFilters);
                      },
                    },
                  },
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'ValueField',
                      label: 'Select One',
                      options: SELECTOR_OPTIONS,
                      value: item?.selector
                        ? [
                            {
                              label: capitalize(item.selector),
                              value: item.selector,
                            },
                          ]
                        : null,
                      isDisabled: isReadOnly,
                      placeholder: 'Select One',
                      onChange: (value: any) => {
                        fieldKeysCorrection(index);
                        fields[index] = {
                          ...fields[index],
                          selector: value?.value,
                        };

                        const _propertyFilters = {
                          ...data.propertyFilters,
                          fields,
                        };
                        updateFilters(_propertyFilters);
                      },
                    },
                  },
                  ...(item?.selector === SelectorOptionsEnum.CONSTANT
                    ? selectedObjectProperty?.target?.cardinality ||
                      [InputTypes.MULTI_SELECT, InputTypes.SINGLE_SELECT].includes(
                        selectedObjectProperty?.inputType,
                      )
                      ? [
                          {
                            type: inputTypeForConstant,
                            props: {
                              id: 'objectPropertyValue',
                              label: 'Value',
                              placeholder: 'Enter Value',
                              options: (
                                (selectedObjectProperty?.target?.cardinality
                                  ? relationList
                                  : filterSelectOptions) || []
                              ).map((option) => ({
                                ...option,
                                label: option.displayName,
                                value: option.id,
                                ...(option?.externalId && {
                                  externalId: `(ID: ${option?.externalId})`,
                                }),
                              })),
                              isDisabled: isReadOnly,
                              onMenuScrollToBottom: () => {
                                if (
                                  !relationPagination?.last &&
                                  selectedObjectProperty?.target?.cardinality
                                ) {
                                  relationFetchNext();
                                }
                              },
                              onInputChange: debounce((searchedString: string, actionMeta) => {
                                if (searchedString !== actionMeta.prevInputValue) {
                                  handleRelationObjectFetch(
                                    selectedObjectProperty?.externalId,
                                    searchedString,
                                  );
                                }
                              }, 500),
                              value: item?.values ? selectedValues : null,
                              onChange: (value: any) => {
                                const newValues = Array.isArray(value) ? value : [value];

                                fields[index] = {
                                  ...fields[index],
                                  values: newValues.map((option) => option.value),
                                };

                                const _propertyFilters = {
                                  ...data.propertyFilters,
                                  fields,
                                };

                                setSelectedValues(newValues);
                                updateFilters(_propertyFilters);
                              },
                            },
                          },
                        ]
                      : [
                          {
                            type: [InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(
                              selectedObjectProperty?.inputType,
                            )
                              ? InputTypes.NUMBER
                              : selectedObjectProperty?.inputType,
                            props: {
                              id: 'objectPropertyValue',
                              label: 'Value',
                              placeholder: 'Enter Value',
                              defaultValue: item?.values?.[0],
                              disabled: isReadOnly,
                              style: { width: 'calc(25% - 16px)' },
                              onChange: (value: any) => {
                                const trimmedValue = value.value.trim();
                                fields[index] = {
                                  ...fields[index],
                                  values:
                                    value.value !== ''
                                      ? selectedObjectProperty?.inputType === InputTypes.NUMBER
                                        ? [parseInt(trimmedValue)]
                                        : [trimmedValue]
                                      : [],
                                };

                                const _propertyFilters = {
                                  ...data.propertyFilters,
                                  fields,
                                };

                                handleConstantValue(_propertyFilters);
                              },
                            },
                          },
                        ]
                    : [
                        {
                          type: InputTypes.SINGLE_SELECT,
                          props: {
                            id: 'parameter',
                            label: 'Select Parameter',
                            options: parameterOptionsList,
                            value: item?.referencedParameterId
                              ? [
                                  {
                                    label: selectedParameter?.label,
                                    value: item.referencedParameterId,
                                  },
                                ]
                              : null,
                            isDisabled: isReadOnly,
                            placeholder: 'Select One',
                            onMenuScrollToBottom: fetchNext,
                            onInputChange: debounce((searchedString: string, actionMeta) => {
                              if (searchedString !== actionMeta.prevInputValue) {
                                setParametersFilterFields((prev) => {
                                  const updatedFields = prev.filter(
                                    (field) => field.field !== 'label',
                                  );
                                  return [
                                    ...updatedFields,
                                    {
                                      field: 'label',
                                      op: FilterOperators.LIKE,
                                      values: [searchedString],
                                    },
                                  ];
                                });
                              }
                            }, 500),
                            onChange: (value: any) => {
                              setSelectedParameter(value);
                              fields[index] = {
                                ...fields[index],
                                referencedParameterId: value?.id,
                              };
                              const _propertyFilters = {
                                ...data.propertyFilters,
                                fields,
                              };
                              updateFilters(_propertyFilters);
                            },
                          },
                        },
                      ]),
                ]
              : []),
          ]}
        />
        {!isReadOnly && !isVariationView && (
          <Close
            className="remove-icon"
            onClick={() => {
              fields?.splice(index, 1);
              const _propertyFilters = {
                ...data.propertyFilters,
                fields,
              };
              updateFilters(_propertyFilters);
            }}
          />
        )}
      </div>
      {[InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(
        selectedObjectProperty?.inputType,
      ) && (
        <FormGroup
          inputs={[
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'objectPropertyUnit',
                label: 'Unit',
                options: Object.entries(getDateUnits(selectedObjectProperty?.inputType)).map(
                  ([value, label]) => ({
                    label,
                    value,
                  }),
                ),
                isDisabled: isReadOnly,
                placeholder: 'Select Unit',
                value: item?.dateUnit
                  ? [
                      {
                        label: getDateUnits(selectedObjectProperty?.inputType)[
                          item.dateUnit as keyof typeof getDateUnits
                        ],
                        value: item.dateUnit,
                      },
                    ]
                  : null,
                onChange: (value: any) => {
                  fields[index] = {
                    ...fields[index],
                    dateUnit: value.value,
                  };

                  const _propertyFilters = {
                    ...data.propertyFilters,
                    fields,
                  };

                  updateFilters(_propertyFilters);
                },
              },
            },
          ]}
        />
      )}
    </div>
  );
};
export default ResourceFilterRow;
