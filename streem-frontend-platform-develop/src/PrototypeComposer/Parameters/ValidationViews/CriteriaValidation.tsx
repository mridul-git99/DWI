import { PARAMETER_OPERATORS } from '#PrototypeComposer/constants';
import { FormGroup } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { MandatoryParameter, Parameter, TargetEntityType } from '#types';
import { apiGetParameters } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, SELECTOR_OPTIONS } from '#utils/constants';
import { FilterOperators, InputTypes, SelectorOptionsEnum } from '#utils/globalTypes';
import { debounce } from 'lodash';
import React, { FC, useEffect, useMemo } from 'react';
import { CommonWrapper } from '../SetupViews/styles';

const CriteriaValidation: FC<{
  updateValidation: any;
  criteriaValidations: any[];
  isReadOnly: boolean;
  targetEntityType: string;
  checklistId: string;
  validationIndex: number;
}> = ({
  isReadOnly,
  targetEntityType,
  criteriaValidations,
  updateValidation,
  checklistId,
  validationIndex,
}) => {
  const {
    list: numberParameters,
    listById: numbersListById,
    reset: numbersReset,
    fetchNext: numbersFetchNext,
  } = createFetchList(apiGetParameters(checklistId!), {}, false);

  const { listById: referenceParameters, reset: fetchReferenceParameters } = createFetchList(
    apiGetParameters(checklistId!),
    {},
    false,
  );

  const { addParameter } = useTypedSelector((state) => state.prototypeComposer.parameters);

  const {
    uom,
    value,
    lowerValue,
    upperValue,
    operator,
    criteriaType,
    valueParameterId,
    lowerValueParameterId,
    upperValueParameterId,
    errorMessage,
  } = criteriaValidations?.[0] || {};

  const fetchNumberParameter = async (query?: string) => {
    numbersReset({
      params: {
        page: DEFAULT_PAGE_NUMBER,
        size: DEFAULT_PAGE_SIZE,
        sort: 'createdAt,desc',
        filters: {
          op: FilterOperators.AND,
          fields: [
            { field: 'archived', op: FilterOperators.EQ, values: [false] },
            {
              field: 'type',
              op: FilterOperators.ANY,
              values: [MandatoryParameter.NUMBER, MandatoryParameter.CALCULATION],
            },
            ...(query
              ? [
                  {
                    field: 'label',
                    op: FilterOperators.LIKE,
                    values: [query],
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
      },
    });
  };

  const handleOperatorChange = (field: string, value: any) => {
    if (value === SelectorOptionsEnum.CONSTANT && field === 'criteriaType') {
      criteriaValidations[0] = {
        ...criteriaValidations[0],
        [field]: value,
        valueParameterId: null,
        lowerValueParameterId: null,
        upperValueParameterId: null,
      };
    } else if (value === SelectorOptionsEnum.PARAMETER && field === 'criteriaType') {
      criteriaValidations[0] = {
        ...criteriaValidations[0],
        [field]: value,
        value: null,
        lowerValue: null,
        upperValue: null,
      };
    } else if (field === 'operator') {
      criteriaValidations[0] = {
        ...criteriaValidations[0],
        [field]: value,
        valueParameterId: null,
        lowerValueParameterId: null,
        upperValueParameterId: null,
        value: null,
        lowerValue: null,
        upperValue: null,
      };
    } else {
      criteriaValidations[0] = {
        ...criteriaValidations[0],
        [field]: value,
      };
    }
    updateValidation(criteriaValidations, validationIndex);
  };

  const parameterOptionsList = useMemo(() => {
    if (targetEntityType === TargetEntityType.PROCESS) {
      return numberParameters?.filter(
        (currParam: Parameter) => currParam.targetEntityType === TargetEntityType.PROCESS,
      );
    } else {
      return numberParameters;
    }
  }, [targetEntityType, numberParameters]);

  useEffect(() => {
    fetchNumberParameter();
  }, []);

  useEffect(() => {
    const referencedParameterIds = [
      valueParameterId,
      lowerValueParameterId,
      upperValueParameterId,
    ].filter(Boolean);
    if (referencedParameterIds?.length) {
      fetchReferenceParameters({
        params: {
          page: DEFAULT_PAGE_NUMBER,
          size: DEFAULT_PAGE_SIZE,
          sort: 'createdAt,desc',
          filters: {
            op: FilterOperators.AND,
            fields: [
              { field: 'archived', op: FilterOperators.EQ, values: [false] },
              {
                field: 'id',
                op: FilterOperators.ANY,
                values: referencedParameterIds,
              },
            ],
          },
        },
      });
    }
  }, []);

  return (
    <CommonWrapper>
      <FormGroup
        style={{ flexDirection: 'column', gap: '8px', marginBottom: '8px' }}
        inputs={[
          {
            type: InputTypes.SINGLE_LINE,
            props: {
              id: 'uom',
              label: 'Unit of Measurement',
              placeholder: 'Write here in smallcase characters',
              disabled: isReadOnly,
              optional: true,
              defaultValue: uom,
              onBlur: (e: React.FocusEvent<HTMLInputElement>) => {
                handleOperatorChange('uom', e.target.value);
              },
            },
          },
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'operator',
              label: 'Criteria',
              options: PARAMETER_OPERATORS,
              value: operator ? PARAMETER_OPERATORS.filter((o) => operator === o.value) : undefined,
              placeholder: 'Choose an option',
              isDisabled: isReadOnly,
              onChange: (value: any) => handleOperatorChange('operator', value.value),
            },
          },
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'criteriaType',
              label: 'Select Value Type',
              options: SELECTOR_OPTIONS,
              value: criteriaType
                ? SELECTOR_OPTIONS.filter((v) => criteriaType === v.value)
                : undefined,
              placeholder: 'Choose an option',
              isDisabled: isReadOnly,
              onChange: (value: any) => {
                handleOperatorChange('criteriaType', value.value);
              },
            },
          },
        ]}
      />

      {criteriaType === SelectorOptionsEnum.CONSTANT && (
        <FormGroup
          className="form-row"
          inputs={
            operator === 'BETWEEN'
              ? [
                  {
                    type: InputTypes.SINGLE_LINE,
                    props: {
                      id: 'lowerValue',
                      label: 'Lower Value',
                      placeholder: 'Write Here',
                      type: 'number',
                      disabled: isReadOnly,
                      defaultValue: lowerValue,
                      onBlur: (e: React.FocusEvent<HTMLInputElement>) => {
                        handleOperatorChange('lowerValue', e.target.value);
                      },
                    },
                  },
                  {
                    type: InputTypes.SINGLE_LINE,
                    props: {
                      id: 'upperValue',
                      label: 'Upper Value',
                      placeholder: 'Write Here',
                      type: 'number',
                      defaultValue: upperValue,
                      disabled: isReadOnly,
                      onBlur: (e: React.FocusEvent<HTMLInputElement>) => {
                        handleOperatorChange('upperValue', e.target.value);
                      },
                    },
                  },
                ]
              : [
                  {
                    type: InputTypes.SINGLE_LINE,
                    props: {
                      id: 'value',
                      label: 'Value',
                      placeholder: 'Write Here',
                      type: 'number',
                      disabled: isReadOnly,
                      defaultValue: value,
                      onBlur: (e: React.FocusEvent<HTMLInputElement>) => {
                        handleOperatorChange('value', e.target.value);
                      },
                    },
                  },
                ]
          }
        />
      )}
      {criteriaType === SelectorOptionsEnum.PARAMETER && (
        <div style={{ display: 'flex', gap: '16px' }}>
          {operator === 'BETWEEN' ? (
            <>
              <FormGroup
                className="form-row"
                inputs={[
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'lowerValueParameterId',
                      label: 'Lower Value',
                      placeholder: 'Write Here',
                      type: 'number',
                      isDisabled: isReadOnly,
                      options: parameterOptionsList.map((el: any) => ({
                        label: el.label,
                        value: el.id,
                      })),
                      value: lowerValueParameterId
                        ? [
                            {
                              value: lowerValueParameterId,
                              label: {
                                ...numbersListById,
                                ...referenceParameters,
                              }?.[lowerValueParameterId]?.label,
                            },
                          ]
                        : undefined,
                      onChange: (value: any) => {
                        handleOperatorChange('lowerValueParameterId', value.value);
                      },
                      onInputChange: debounce((value, actionMeta) => {
                        if (value !== actionMeta.prevInputValue) {
                          fetchNumberParameter(value);
                        }
                      }, 500),
                      onMenuScrollToBottom: numbersFetchNext,
                    },
                  },
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'upperValueParameterId',
                      label: 'Upper Value',
                      placeholder: 'Write Here',
                      type: 'number',
                      options: parameterOptionsList.map((el: any) => ({
                        label: el.label,
                        value: el.id,
                      })),
                      value: upperValueParameterId
                        ? [
                            {
                              value: upperValueParameterId,
                              label: {
                                ...numbersListById,
                                ...referenceParameters,
                              }?.[upperValueParameterId]?.label,
                            },
                          ]
                        : undefined,
                      isDisabled: isReadOnly,
                      onChange: (value: any) => {
                        handleOperatorChange('upperValueParameterId', value.value);
                      },
                      onInputChange: debounce((value, actionMeta) => {
                        if (value !== actionMeta.prevInputValue) {
                          fetchNumberParameter(value);
                        }
                      }, 500),
                      onMenuScrollToBottom: numbersFetchNext,
                    },
                  },
                ]}
              />
            </>
          ) : (
            <FormGroup
              className="form-row"
              inputs={[
                {
                  type: InputTypes.SINGLE_SELECT,
                  props: {
                    id: 'valueParameterId',
                    label: 'Value',
                    placeholder: 'Write Here',
                    type: 'number',
                    options: parameterOptionsList.map((el: any) => ({
                      label: el.label,
                      value: el.id,
                    })),
                    value: valueParameterId
                      ? [
                          {
                            value: valueParameterId,
                            label: {
                              ...numbersListById,
                              ...referenceParameters,
                            }?.[valueParameterId]?.label,
                          },
                        ]
                      : undefined,
                    isDisabled: isReadOnly,
                    onChange: (value: any) => {
                      handleOperatorChange('valueParameterId', value.value);
                    },
                    onInputChange: debounce((value, actionMeta) => {
                      if (value !== actionMeta.prevInputValue) {
                        fetchNumberParameter(value);
                      }
                    }, 500),
                    onMenuScrollToBottom: numbersFetchNext,
                  },
                },
              ]}
            />
          )}
        </div>
      )}

      <div className="validation-text">If the condition is breached display an error message</div>

      <FormGroup
        style={{ flexDirection: 'column', gap: '8px', marginBottom: '8px' }}
        inputs={[
          {
            type: InputTypes.SINGLE_LINE,
            props: {
              id: 'errorMessage',
              label: 'Error Message',
              placeholder: 'Enter the error message',
              description: 'This message will be displayed when the validation rule is breached',
              disabled: isReadOnly,
              defaultValue: errorMessage,
              onBlur: (e: React.FocusEvent<HTMLInputElement>) => {
                handleOperatorChange('errorMessage', e.target.value);
              },
            },
          },
        ]}
      />
    </CommonWrapper>
  );
};

export default CriteriaValidation;
