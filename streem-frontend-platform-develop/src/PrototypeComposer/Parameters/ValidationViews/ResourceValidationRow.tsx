import { FormGroup } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { filterPageTypeEnum, MandatoryParameter, Parameter, TargetEntityType } from '#types';
import { labelByConstraint } from '#utils';
import { apiGetParameters, apiSingleParameter } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, SELECTOR_OPTIONS } from '#utils/constants';
import { FilterField, FilterOperators, InputTypes, SelectorOptionsEnum } from '#utils/globalTypes';
import { request } from '#utils/request';
import { ObjectType } from '#views/Ontology/types';
import { capitalize, debounce, isArray } from 'lodash';
import React, { Dispatch, FC, SetStateAction, useEffect, useMemo, useState } from 'react';
import { getDateUnits, ResourceValidationState } from './Resource';

type TResourceValidationRowProps = {
  index: number;
  item: Record<string, any>;
  isActiveLoading: boolean;
  selectedObjectType: ObjectType;
  isReadOnly: boolean;
  propertyValidations: any[];
  updatePropertyValidations: (data: any[], validationIndex: number) => void;
  setState: Dispatch<SetStateAction<ResourceValidationState>>;
  validationSelectOptions: Record<string, any>;
  updateValidationOptions: (data: any[]) => void;
  isVariationView: boolean;
  parameter: Parameter;
  validationIndex: number;
};

const ResourceValidationRow: FC<TResourceValidationRowProps> = ({
  index,
  item,
  isActiveLoading,
  selectedObjectType,
  isReadOnly,
  propertyValidations,
  updatePropertyValidations,
  setState,
  validationSelectOptions,
  parameter,
  validationIndex,
}) => {
  const [parametersFilterFields, setParametersFilterFields] = useState<FilterField[]>([]);
  const [selectedParameter, setSelectedParameter] = useState<Parameter>();

  const checklistId = useTypedSelector((state) => state.prototypeComposer?.data?.id)!;

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
    return item?.selector ? list : [];
  }, [list, item.selector]);

  const fetchReferenceParameter = async (referencedParameterId: string) => {
    const { data } = await request('GET', apiSingleParameter(referencedParameterId));
    if (data) {
      setSelectedParameter(data);
    }
  };

  const handleChangeObjectProperty = (item: any) => {
    const { propertyId, propertyInputType } = item;
    if (propertyInputType === MandatoryParameter.SINGLE_SELECT) {
      setParametersFilterFields([
        {
          field: 'type',
          op: FilterOperators.ANY,
          values: [MandatoryParameter.SINGLE_SELECT],
        },
        {
          field: 'metadata->propertyId',
          op: FilterOperators.EQ,
          values: [propertyId],
        },
      ]);
    } else if (propertyInputType === InputTypes.MULTI_SELECT) {
      setParametersFilterFields([
        {
          field: 'type',
          op: FilterOperators.ANY,
          values: [MandatoryParameter.MULTISELECT],
        },
        {
          field: 'metadata->propertyId',
          op: FilterOperators.EQ,
          values: [propertyId],
        },
      ]);
    } else if (
      [InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(propertyInputType)
    ) {
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
            propertyInputType === MandatoryParameter.NUMBER
              ? [propertyInputType, MandatoryParameter.CALCULATION]
              : [propertyInputType],
        },
      ]);
    }
  };

  useEffect(() => {
    if (item?.propertyId) {
      handleChangeObjectProperty(item);
    }
  }, [item?.propertyId]);

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

  return (
    <div key={item.id}>
      <FormGroup
        inputs={[
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'objectProperty',
              label: 'Object Property is',
              isLoading: isActiveLoading,
              options: selectedObjectType?.properties.map((objectTypeProperty: any) => ({
                _options: objectTypeProperty.options,
                externalId: objectTypeProperty.externalId,
                label: objectTypeProperty.displayName,
                value: objectTypeProperty.id,
                inputType: objectTypeProperty.inputType,
              })),
              value: item?.propertyId
                ? [
                    {
                      label: item.propertyDisplayName,
                      value: item.propertyId,
                    },
                  ]
                : undefined,
              isDisabled: isReadOnly,
              placeholder: 'Select Object Property',
              onChange: (value: any) => {
                propertyValidations[index] = {
                  id: propertyValidations[index]?.id,
                  propertyId: value.value,
                  propertyInputType: value.inputType,
                  propertyExternalId: value.externalId,
                  propertyDisplayName: value.label,
                };
                updatePropertyValidations(propertyValidations, validationIndex);
                if ([InputTypes.MULTI_SELECT, InputTypes.SINGLE_SELECT].includes(value.inputType)) {
                  setState((prev) => ({
                    ...prev,
                    validationSelectOptions: {
                      ...prev.validationSelectOptions,
                      [index]: value._options,
                    },
                  }));
                } else {
                  setState((prev) => ({
                    ...prev,
                    validationSelectOptions: {
                      ...prev.validationSelectOptions,
                      [index]: [],
                    },
                  }));
                }
              },
            },
          },
          ...(item?.propertyInputType
            ? [
                {
                  type: InputTypes.SINGLE_SELECT,
                  props: {
                    id: 'objectPropertyCondition',
                    label: 'Condition is',
                    options: Object.entries(
                      labelByConstraint(
                        item.propertyInputType,
                        filterPageTypeEnum.RESOURCE_FILTERS,
                      ),
                    ).map(([value, label]) => ({ label, value })),
                    placeholder: 'Select Condition',
                    isDisabled: isReadOnly,
                    value: item?.constraint
                      ? [
                          {
                            label: (
                              labelByConstraint(
                                item.propertyInputType,
                                filterPageTypeEnum.RESOURCE_FILTERS,
                              ) as any
                            )[item.constraint],
                            value: item.constraint,
                          },
                        ]
                      : null,
                    onChange: (value: any) => {
                      propertyValidations[index] = {
                        ...propertyValidations[index],
                        constraint: value.value,
                      };
                      updatePropertyValidations(propertyValidations, validationIndex);
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
                      propertyValidations[index] = {
                        ...propertyValidations[index],
                        selector: value.value,
                      };
                      updatePropertyValidations(propertyValidations, validationIndex);
                    },
                  },
                },
                ...(item.selector === SelectorOptionsEnum.CONSTANT
                  ? [
                      ...([InputTypes.MULTI_SELECT, InputTypes.SINGLE_SELECT].includes(
                        item.propertyInputType,
                      )
                        ? [
                            {
                              type: item.propertyInputType,
                              props: {
                                id: 'objectPropertyValue',
                                label: 'Value is',
                                placeholder: 'Select Property Option',
                                options: (validationSelectOptions?.[index] || []).map(
                                  (option: any) => ({
                                    label: option.displayName,
                                    value: option.id,
                                  }),
                                ),
                                isDisabled: isReadOnly,
                                value: (item?.options || []).map((option: any) => ({
                                  label: option.displayName,
                                  value: option.id,
                                })),
                                onChange: (value: any) => {
                                  propertyValidations[index] = {
                                    ...propertyValidations[index],
                                    options: isArray(value)
                                      ? value.map((v) => ({
                                          id: v.value,
                                          displayName: v.label,
                                        }))
                                      : [
                                          {
                                            id: value.value,
                                            displayName: value.label,
                                          },
                                        ],
                                  };
                                  updatePropertyValidations(propertyValidations, validationIndex);
                                },
                              },
                            },
                          ]
                        : [
                            {
                              type: [
                                InputTypes.DATE,
                                InputTypes.TIME,
                                InputTypes.DATE_TIME,
                              ].includes(item.propertyInputType)
                                ? InputTypes.NUMBER
                                : item.propertyInputType,
                              props: {
                                id: 'objectPropertyValue',
                                label: 'Value is',
                                disabled: isReadOnly,
                                placeholder: 'Enter Value',
                                value: item?.value ? item.value : '',
                                onChange: (e: { name: string; value: string }) => {
                                  const trimmedValue =
                                    item?.propertyInputType === MandatoryParameter.MULTI_LINE ||
                                    item?.propertyInputType === MandatoryParameter.SINGLE_LINE
                                      ? e.value
                                      : e?.value?.trim();
                                  propertyValidations[index] = {
                                    ...propertyValidations[index],
                                    value: trimmedValue,
                                  };
                                  updatePropertyValidations(propertyValidations, validationIndex);
                                },
                              },
                            },
                          ]),
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
                            propertyValidations[index] = {
                              ...propertyValidations[index],
                              referencedParameterId: value.id,
                            };
                            updatePropertyValidations(propertyValidations, validationIndex);
                          },
                        },
                      },
                    ]),
                ...([InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(
                  item.propertyInputType,
                )
                  ? [
                      {
                        type: InputTypes.SINGLE_SELECT,
                        props: {
                          id: 'objectPropertyUnit',
                          label: 'Unit',
                          options: Object.entries(getDateUnits(item.propertyInputType)).map(
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
                                  label: getDateUnits(item.propertyInputType)[
                                    item.dateUnit as keyof typeof getDateUnits
                                  ],
                                  value: item.dateUnit,
                                },
                              ]
                            : null,
                          onChange: (value: any) => {
                            propertyValidations[index] = {
                              ...propertyValidations[index],
                              dateUnit: value.value,
                            };
                            updatePropertyValidations(propertyValidations, validationIndex);
                          },
                        },
                      },
                    ]
                  : []),
              ]
            : []),
        ]}
      />
      {item?.propertyInputType && (
        <>
          <div className="validation-text">
            If the condition is breached display an error message
          </div>
          <FormGroup
            inputs={[
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  id: 'objectPropertyErrorMsg',
                  label: 'Error Message',
                  placeholder: 'Enter Error Message',
                  description:
                    'This message will be displayed when the validation rule is breached',
                  value: item?.errorMessage ? item.errorMessage : '',
                  disabled: isReadOnly,
                  onChange: (e: { name: string; value: string }) => {
                    propertyValidations[index] = {
                      ...propertyValidations[index],
                      errorMessage: e?.value,
                    };
                    updatePropertyValidations(propertyValidations, validationIndex);
                  },
                },
              },
            ]}
          />
        </>
      )}
    </div>
  );
};

export default ResourceValidationRow;
