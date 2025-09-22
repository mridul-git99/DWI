import { FormGroup } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { MandatoryParameter } from '#types';
import { labelByConstraint } from '#utils';
import { apiGetObjectTypes, apiGetParameters } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, InputTypes, ResponseObj } from '#utils/globalTypes';
import { request } from '#utils/request';
import { debounce, keyBy } from 'lodash';
import React, { FC, useEffect, useRef, useState } from 'react';
import { CommonWrapper } from '../SetupViews/styles';

type NumberValidationState = {
  isLoadingObjectType: Record<number, boolean>;
  isLoadingParameters: boolean;
  selectedObjectTypes: Record<number, any>;
};

export const NumberResourceValidationRow: FC<{
  updateValidation: any;
  resourceValidation: any;
  resourceValidations: any[];
  isReadOnly: boolean;
  checklistId: string;
  validationIndex: number;
  index: number;
}> = ({
  updateValidation,
  resourceValidation,
  resourceValidations,
  isReadOnly,
  checklistId,
  validationIndex,
  index,
}) => {
  const {
    list: resourceList,
    listById: resourceListById,
    reset: resourceReset,
    fetchNext: resourceFetchNext,
  } = createFetchList(apiGetParameters(checklistId!), {}, false);

  const selectedParametersMap = useRef<Record<string, any>>({});

  const {
    constraint,
    errorMessage,
    id,
    parameterId,
    propertyDisplayName,
    propertyId,
    propertyInputType,
  } = resourceValidation || {};

  const [state, setState] = useState<NumberValidationState>({
    isLoadingObjectType: {},
    isLoadingParameters: false,
    selectedObjectTypes: {},
  });

  const { isLoadingObjectType, isLoadingParameters, selectedObjectTypes } = state;

  const fetchObjectType = async (id: string, index: number) => {
    let result = Object.values(selectedObjectTypes).find((objectType) => {
      return id === objectType.id;
    });
    if (!result) {
      setState((prev) => ({
        ...prev,
        isLoadingObjectType: {
          ...prev.isLoadingObjectType,
          [index]: true,
        },
      }));
      const res: ResponseObj<any> = await request('GET', apiGetObjectTypes(id));
      result = res.data;
    }
    setState((prev) => ({
      ...prev,
      isLoadingObjectType: {
        ...prev.isLoadingObjectType,
        [index]: false,
      },
      selectedObjectTypes: {
        ...prev.selectedObjectTypes,
        [index]: result!,
      },
    }));
  };

  const fetchResourceParameter = async (query?: string) => {
    resourceReset({
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
              op: FilterOperators.EQ,
              values: [MandatoryParameter.RESOURCE],
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
          ],
        },
      },
    });
  };

  const fetchSelectedParameters = async () => {
    const selectedParameterList: string[] = [];
    parameterId && selectedParameterList.push(parameterId);

    if (selectedParameterList.length) {
      setState((prev) => ({ ...prev, isLoadingParameters: true }));

      const parametersData = await request('GET', apiGetParameters(checklistId!), {
        params: {
          page: DEFAULT_PAGE_NUMBER,
          size: selectedParameterList?.length ? selectedParameterList.length : DEFAULT_PAGE_SIZE,
          sort: 'createdAt,desc',
          filters: {
            op: FilterOperators.AND,
            fields: [
              { field: 'archived', op: FilterOperators.EQ, values: [false] },
              ...(selectedParameterList
                ? [
                    {
                      field: 'id',
                      op: FilterOperators.ANY,
                      values: selectedParameterList,
                    },
                  ]
                : []),
            ],
          },
        },
      });

      selectedParametersMap.current = keyBy(parametersData.data, 'id');
      setState((prev) => ({ ...prev, isLoadingParameters: false }));
    }

    const fetchedObjectTypeIds: Record<string, boolean> = {};
    const objectTypeId = { ...selectedParametersMap.current, ...resourceListById }?.[parameterId]
      ?.data?.objectTypeId;
    if (parameterId && objectTypeId) {
      if (!fetchedObjectTypeIds[objectTypeId]) {
        fetchObjectType(objectTypeId, validationIndex);
        fetchedObjectTypeIds[objectTypeId] = true;
      }
    }
  };

  const handleOperatorChange = (field: string, value: any) => {
    if (field === 'parameterId') {
      resourceValidations[index] = {
        id: resourceValidations[index]?.id,
        parameterId: value.value,
      };
    } else if (field === 'propertyId') {
      resourceValidations[index] = {
        ...resourceValidations[index],
        propertyId: value.value,
        propertyInputType: value.inputType,
        propertyExternalId: value.externalId,
        propertyDisplayName: value.label,
      };
    } else if (field === 'constraint' || field === 'errorMessage') {
      resourceValidations[index] = {
        ...resourceValidations[index],
        [field]: value.value,
      };
    }
    updateValidation(resourceValidations, validationIndex);
  };

  useEffect(() => {
    fetchSelectedParameters();
  }, []);

  return (
    <CommonWrapper>
      <div className="validation" key={id}>
        <div className="upper-row">
          <FormGroup
            inputs={[
              {
                type: InputTypes.SINGLE_SELECT,
                props: {
                  id: 'objectType',
                  label: 'Resource Parameter',
                  isLoading: isLoadingParameters,
                  options: resourceList.map((resource: any) => ({
                    ...resource.data,
                    label: resource.label,
                    value: resource.id,
                  })),
                  isDisabled: isReadOnly,
                  value: parameterId
                    ? [
                        {
                          label: {
                            ...selectedParametersMap.current,
                            ...resourceListById,
                          }?.[parameterId]?.label,
                          value: parameterId,
                        },
                      ]
                    : undefined,
                  placeholder: 'Select Resource Parameter',
                  onMenuOpen: () => {
                    fetchResourceParameter();
                  },
                  onChange: (value: any) => {
                    handleOperatorChange('parameterId', value);
                    fetchObjectType(value.objectTypeId, validationIndex);
                  },
                  onInputChange: debounce((value, actionMeta) => {
                    if (value !== actionMeta.prevInputValue) {
                      fetchResourceParameter(value);
                    }
                  }, 500),
                  onMenuScrollToBottom: resourceFetchNext,
                },
              },
              ...(parameterId
                ? [
                    {
                      type: InputTypes.SINGLE_SELECT,
                      props: {
                        id: 'objectProperty',
                        label: 'Object Property',
                        isLoading: isLoadingObjectType?.[validationIndex],
                        options: (selectedObjectTypes?.[validationIndex]?.properties || []).reduce<
                          Array<Record<string, string>>
                        >((acc, objectTypeProperty) => {
                          if (objectTypeProperty.inputType === InputTypes.NUMBER) {
                            acc.push({
                              inputType: objectTypeProperty.inputType,
                              externalId: objectTypeProperty.externalId,
                              label: objectTypeProperty.displayName,
                              value: objectTypeProperty.id,
                            });
                          }
                          return acc;
                        }, []),
                        isDisabled: isReadOnly,
                        placeholder: 'Select Object Property',
                        value: propertyId
                          ? [
                              {
                                label: propertyDisplayName,
                                value: propertyId,
                              },
                            ]
                          : null,
                        onChange: (value: any) => {
                          handleOperatorChange('propertyId', value);
                        },
                      },
                    },
                  ]
                : []),
              ...(propertyId
                ? [
                    {
                      type: InputTypes.SINGLE_SELECT,
                      props: {
                        id: 'objectPropertyCondition',
                        label: 'Condition',
                        options: Object.entries(labelByConstraint(propertyInputType)).map(
                          ([value, label]) => ({ label, value }),
                        ),
                        isDisabled: isReadOnly,
                        placeholder: 'Select Condition',
                        value: constraint
                          ? [
                              {
                                label: (labelByConstraint(propertyInputType) as any)[constraint],
                                value: constraint,
                              },
                            ]
                          : undefined,
                        onChange: (value: any) => {
                          handleOperatorChange('constraint', value);
                        },
                      },
                    },
                  ]
                : []),
            ]}
          />
        </div>
        {propertyInputType && (
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
                    defaultValue: errorMessage,
                    disabled: isReadOnly,
                    onBlur: (e: React.FocusEvent<HTMLInputElement>) => {
                      handleOperatorChange('errorMessage', e.target);
                    },
                  },
                },
              ]}
            />
          </>
        )}
      </div>
    </CommonWrapper>
  );
};
