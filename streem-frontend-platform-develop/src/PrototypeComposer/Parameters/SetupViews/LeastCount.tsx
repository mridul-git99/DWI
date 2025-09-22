import InfoIcon from '#assets/svg/info.svg';
import { FormGroup, ToggleSwitch } from '#components';
import Tooltip from '#components/shared/Tooltip';
import { useTypedSelector } from '#store';
import { MandatoryParameter, Parameter } from '#types';
import { apiGetParameters } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, SELECTOR_OPTIONS } from '#utils/constants';
import {
  FilterOperators,
  InputTypes,
  Pageable,
  ResponseObj,
  SelectorOptionsEnum,
  fetchDataParams,
} from '#utils/globalTypes';
import { request } from '#utils/request';
import React, { FC, useEffect, useState } from 'react';
import { useFormContext } from 'react-hook-form';
import styled from 'styled-components';

const LeastCountWrapper = styled.div`
  margin-bottom: 24px;

  .least-count-value-field {
    margin-top: 24px;
  }
`;

const LeastCount: FC<{ isReadOnly: boolean }> = ({ isReadOnly }) => {
  const { watch, setValue, register, trigger } = useFormContext();
  const formData = watch('data', {});
  const autoInitialized = watch('autoInitialized', false);
  const leastCountEnabled = watch('leastCountEnabled', !!formData?.leastCount);
  const leastCount = watch('data.leastCount');

  const customLeastCountValidation = (value: any) => {
    if (leastCountEnabled) {
      let commonKeys = ['selector'];
      let keysToValidate: string[] = [];
      if (leastCount?.selector === SelectorOptionsEnum.PARAMETER) {
        keysToValidate = ['referencedParameterId'];
      } else {
        keysToValidate = ['value'];
      }
      return [...commonKeys, ...keysToValidate].every((key) => !!value?.[key]);
    }
    return true;
  };

  register('leastCountEnabled');
  register('data.leastCount', {
    validate: customLeastCountValidation,
  });

  useEffect(() => {
    if (!leastCountEnabled) {
      setValue('data.leastCount', null, { shouldDirty: true, shouldValidate: true });
    } else {
      trigger('data.leastCount');
    }
  }, [leastCountEnabled]);

  return (
    <LeastCountWrapper>
      <div className="disabled-tooltip-info-icon">
        <ToggleSwitch
          height={24}
          width={48}
          offLabel="Enable least count validation"
          onColor="#24a148"
          onChange={(isChecked) => {
            setValue('leastCountEnabled', isChecked, { shouldDirty: true, shouldValidate: true });
          }}
          onLabel="Enable least count validation"
          checked={leastCountEnabled}
          disabled={isReadOnly || autoInitialized}
        />
        {!isReadOnly && autoInitialized && (
          <Tooltip
            title="Least count disabled for Linked parameters. Remove Linking to enable Least count."
            textAlignment="left"
            arrow
          >
            <img src={InfoIcon} alt="parameter-info" style={{ marginRight: 8 }} />
          </Tooltip>
        )}
      </div>
      {leastCountEnabled && <LeastCountValueField isReadOnly={isReadOnly} />}
    </LeastCountWrapper>
  );
};

const LeastCountValueField: FC<{
  isReadOnly: boolean;
}> = ({ isReadOnly }) => {
  const {
    data: checklistData,
    parameters: { addParameter },
  } = useTypedSelector((state) => state.prototypeComposer);

  const { watch, setValue } = useFormContext();
  const leastCount = watch('data.leastCount');
  const [parametersData, setParametersData] = useState<{
    parametersList: Parameter[];
    pageable: Pageable | null;
    selectedParameter: Parameter[];
  }>({ parametersList: [], pageable: null, selectedParameter: [] });
  const { parametersList, pageable, selectedParameter } = parametersData;

  const fetchParametersData = async (params: fetchDataParams = {}, readOnlyMode = false) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE } = params;
    if (checklistData?.id) {
      try {
        const response: ResponseObj<any> = await request(
          'GET',
          apiGetParameters(checklistData?.id),
          {
            params: {
              page,
              size,
              filters: {
                op: FilterOperators.AND,
                fields: [
                  { field: 'archived', op: FilterOperators.EQ, values: [false] },
                  {
                    field: 'type',
                    op: FilterOperators.ANY,
                    values: [MandatoryParameter.NUMBER],
                  },
                  ...(addParameter?.parameterId
                    ? [
                        {
                          field: 'id',
                          op: FilterOperators.NE,
                          values: [addParameter.parameterId],
                        },
                      ]
                    : []),
                  ...(readOnlyMode
                    ? [
                        {
                          field: 'id',
                          op: FilterOperators.EQ,
                          values: [leastCount?.referencedParameterId],
                        },
                      ]
                    : []),
                ],
              },
              sort: 'id,desc',
            },
          },
        );
        if (response.data) {
          setParametersData((prev) => ({
            ...prev,
            parametersList:
              response?.pageable?.page === 0
                ? response.data
                : [...(prev?.parametersList || []), ...response.data],
            pageable: response.pageable,
            selectedParameter: readOnlyMode ? response.data : prev.selectedParameter,
          }));
        }
      } catch (e) {
        console.error(e);
      }
    }
  };

  const handleMenuScrollToBottom = () => {
    if (pageable && !pageable.last) {
      fetchParametersData({
        page: pageable.page + 1,
      });
    }
  };

  useEffect(() => {
    if (leastCount?.selector === SelectorOptionsEnum.PARAMETER) {
      fetchParametersData({}, true);
    }
  }, []);

  return (
    <div className="least-count-value-field">
      <FormGroup
        inputs={[
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'leastCountSelector',
              label: 'Selector',
              isDisabled: isReadOnly,
              options: SELECTOR_OPTIONS,
              name: 'data.leastCount.selector',
              placeholder: 'Select One',
              value: leastCount?.selector
                ? [
                    {
                      label: SELECTOR_OPTIONS.find(
                        (option: any) => option.value === leastCount?.selector,
                      )?.label,
                      value: leastCount?.selector,
                    },
                  ]
                : null,
              onChange: (_option: any) => {
                setValue(
                  'data.leastCount',
                  { selector: _option.value },
                  {
                    shouldDirty: true,
                    shouldValidate: true,
                  },
                );
              },
            },
          },
        ]}
      />
      {leastCount?.selector === SelectorOptionsEnum.PARAMETER ? (
        <FormGroup
          inputs={[
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'leastCountReferencedParameterId',
                label: 'Select Parameter',
                isDisabled: isReadOnly,
                options: parametersList.map((parameter) => ({
                  ...parameter,
                  value: parameter.id,
                })),
                onMenuOpen: () => {
                  fetchParametersData();
                },
                placeholder: 'Select One',
                value: leastCount?.referencedParameterId
                  ? [
                      {
                        label: selectedParameter.find(
                          (option: any) => option.id === leastCount?.referencedParameterId,
                        )?.label,
                        value: leastCount?.referencedParameterId,
                      },
                    ]
                  : null,
                onChange: (_option: any) => {
                  setValue(
                    'data.leastCount',
                    { selector: leastCount?.selector, referencedParameterId: _option.value },
                    {
                      shouldDirty: true,
                      shouldValidate: true,
                    },
                  );
                  setParametersData((prev) => ({
                    ...prev,
                    selectedParameter: [_option],
                  }));
                },
                onMenuScrollToBottom: handleMenuScrollToBottom,
              },
            },
          ]}
        />
      ) : leastCount?.selector === SelectorOptionsEnum.CONSTANT ? (
        <FormGroup
          inputs={[
            {
              type: InputTypes.NUMBER,
              props: {
                style: { width: '100%' },
                id: 'leastCountValue',
                label: 'Enter Least Count Value',
                placeholder: '0',
                disabled: isReadOnly,
                name: 'data.leastCount.value',
                value: leastCount?.value || null,
                onChange: ({ value }: { value: string }) => {
                  setValue(
                    'data.leastCount',
                    { selector: leastCount?.selector, value },
                    {
                      shouldDirty: true,
                      shouldValidate: true,
                    },
                  );
                },
              },
            },
          ]}
        />
      ) : null}
    </div>
  );
};

export default LeastCount;
