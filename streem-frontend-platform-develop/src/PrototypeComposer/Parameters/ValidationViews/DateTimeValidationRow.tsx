import { FormGroup } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { filterPageTypeEnum, Parameter, TargetEntityType } from '#types';
import { labelByConstraint } from '#utils';
import { apiGetParameters, apiSingleParameter } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, SELECTOR_OPTIONS } from '#utils/constants';
import { FilterOperators, InputTypes, SelectorOptionsEnum } from '#utils/globalTypes';
import { request } from '#utils/request';
import { Close } from '@material-ui/icons';
import { capitalize, debounce } from 'lodash';
import { default as React, useEffect, useMemo, useState } from 'react';
import { getDateUnits } from './Resource';

export const DateTimeValidationRow = ({
  dateTimeValidation,
  parameterType,
  checklistId,
  isReadOnly,
  isVariationView,
  validations,
  index,
  updateValidations,
  currentParameter,
}: any) => {
  const [selectedParameter, setSelectedParameter] = useState<Parameter | null>(null);
  const { addParameter } = useTypedSelector((state) => state.prototypeComposer.parameters);
  const urlParams = useMemo(
    () => ({
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      filters: {
        op: FilterOperators.AND,
        fields: [
          { field: 'archived', op: FilterOperators.EQ, values: [false] },
          {
            field: 'type',
            op: FilterOperators.EQ,
            values: [parameterType],
          },
          ...(currentParameter?.targetEntityType === TargetEntityType.PROCESS
            ? [
                {
                  field: 'targetEntityType',
                  op: FilterOperators.EQ,
                  values: [TargetEntityType.PROCESS],
                },
              ]
            : currentParameter?.targetEntityType === TargetEntityType.UNMAPPED
            ? [
                {
                  field: 'targetEntityType',
                  op: FilterOperators.NE,
                  values: [TargetEntityType.PROCESS],
                },
                {
                  field: 'targetEntityType',
                  op: FilterOperators.NE,
                  values: [TargetEntityType.TASK],
                },
              ]
            : []),
          ...(addParameter?.parameterId
            ? [
                {
                  field: 'id',
                  op: FilterOperators.NE,
                  values: [addParameter.parameterId],
                },
              ]
            : []),
        ],
      },
    }),
    [parameterType, addParameter],
  );
  const {
    list: parametersList,
    reset: resetParameterList,
    fetchNext,
  } = createFetchList(apiGetParameters(checklistId), urlParams, false);

  const fetchReferenceParameter = async (referencedParameterId: string) => {
    const { data } = await request('GET', apiSingleParameter(referencedParameterId));
    if (data) {
      setSelectedParameter(data);
    }
  };

  const fetchParameterList = async (query?: string) => {
    resetParameterList({
      params: {
        ...urlParams,
        ...(query && {
          filters: {
            ...urlParams.filters,
            fields: [
              ...urlParams.filters.fields,
              { field: 'label', op: FilterOperators.LIKE, values: [query] },
            ],
          },
        }),
      },
    });
  };

  const unitOptions = useMemo(() => {
    return Object.entries(getDateUnits(parameterType)).map(([value, label]) => ({
      label,
      value,
    }));
  }, [parameterType]);

  useEffect(() => {
    if (dateTimeValidation?.referencedParameterId) {
      fetchReferenceParameter(dateTimeValidation?.referencedParameterId);
    }
  }, []);

  const handleValueChange = (value: any) => {
    validations[index] = {
      ...validations[index],
      dateTimeParameterValidations: [
        {
          ...dateTimeValidation,
          ...value,
        },
      ],
    };
    updateValidations(validations);
  };

  return (
    <>
      <div className="upper-row">
        <FormGroup
          inputs={[
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'validationCondition',
                label: 'Condition is',
                options: Object.entries(
                  labelByConstraint(parameterType, filterPageTypeEnum.RESOURCE_FILTERS),
                ).map(([value, label]) => ({
                  label,
                  value,
                })),
                placeholder: 'Select Condition',
                isDisabled: isReadOnly,
                value: dateTimeValidation?.constraint
                  ? [
                      {
                        label: (labelByConstraint(dateTimeValidation.propertyInputType) as any)[
                          dateTimeValidation.constraint
                        ],
                        value: dateTimeValidation.constraint,
                      },
                    ]
                  : null,
                onChange: ({ value }: { value: string }) => {
                  handleValueChange({ constraint: value });
                },
              },
            },
            ...(dateTimeValidation?.constraint
              ? [
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'selectorField',
                      label: 'Selector',
                      options: SELECTOR_OPTIONS,
                      value: dateTimeValidation?.selector
                        ? [
                            {
                              label: capitalize(dateTimeValidation.selector),
                              value: dateTimeValidation.selector,
                            },
                          ]
                        : null,
                      isDisabled: isReadOnly,
                      placeholder: 'Select One',
                      onChange: ({ value }: { value: string }) => {
                        handleValueChange({
                          selector: value,
                          referencedParameterId: null,
                          value: null,
                          dateUnit: null,
                        });
                      },
                    },
                  },
                ]
              : []),
            ...(dateTimeValidation?.selector
              ? dateTimeValidation.selector === SelectorOptionsEnum.CONSTANT
                ? [
                    {
                      type: InputTypes.NUMBER,
                      props: {
                        id: 'objectPropertyValue',
                        label: 'Value is',
                        disabled: isReadOnly,
                        placeholder: 'Enter Value',
                        value: dateTimeValidation?.value ? dateTimeValidation.value : '',
                        onChange: ({ value }: { value: string }) => {
                          const trimmedValue = value?.trim();
                          handleValueChange({ value: trimmedValue });
                        },
                      },
                    },
                    {
                      type: InputTypes.SINGLE_SELECT,
                      props: {
                        id: 'objectPropertyUnit',
                        label: 'Unit',
                        options: unitOptions,
                        isDisabled: isReadOnly,
                        placeholder: 'Select Unit',
                        defaultValue: dateTimeValidation?.dateUnit
                          ? [
                              {
                                label: unitOptions.find(
                                  (unit) => unit.value === dateTimeValidation.dateUnit,
                                )?.label,
                                value: dateTimeValidation.dateUnit,
                              },
                            ]
                          : undefined,
                        onChange: (value: any) => {
                          handleValueChange({ dateUnit: value.value });
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
                        options: parametersList,
                        value: dateTimeValidation?.referencedParameterId
                          ? [
                              {
                                label: selectedParameter?.label,
                                value: dateTimeValidation.referencedParameterId,
                              },
                            ]
                          : null,
                        isDisabled: isReadOnly,
                        placeholder: 'Select One',
                        onMenuScrollToBottom: fetchNext,
                        onMenuOpen: fetchParameterList,
                        onInputChange: debounce((value, actionMeta) => {
                          if (value !== actionMeta.prevInputValue) {
                            fetchParameterList(value);
                          }
                        }, 500),
                        onChange: (value: any) => {
                          handleValueChange({ referencedParameterId: value?.id });
                          setSelectedParameter(value);
                        },
                      },
                    },
                  ]
              : []),
          ]}
        />
        {!isReadOnly && !isVariationView && (
          <Close
            className="remove-icon"
            onClick={() => {
              validations?.splice(index, 1);
              updateValidations(validations);
            }}
          />
        )}
      </div>
      {dateTimeValidation?.selector && (
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
                  value: dateTimeValidation?.errorMessage || '',
                  disabled: isReadOnly,
                  onChange: ({ value }: { value: string }) => {
                    handleValueChange({ errorMessage: value });
                  },
                },
              },
            ]}
          />
        </>
      )}
    </>
  );
};
