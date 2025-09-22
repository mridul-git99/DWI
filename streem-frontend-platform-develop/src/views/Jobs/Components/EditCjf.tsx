import { updateHiddenParameterIds } from '#PrototypeComposer/Activity/utils';
import ParameterView from '#PrototypeComposer/Parameters/ExecutionViews';
import { Select } from '#components/shared/Select';
import { MandatoryParameter, Parameter } from '#types';
import { apiBranchingRuleExecute } from '#utils/apiUrls';
import { formatParameter } from '#utils/parameterUtils';
import { request } from '#utils/request';
import React, { FC, useEffect, useRef, useState } from 'react';
import styled from 'styled-components';

const EditCjfWrapper = styled.div.attrs({})`
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

type TEditCjfProps = {
  jobInfo: any;
  form: any;
};

const EditCjf: FC<TEditCjfProps> = ({ jobInfo, form }) => {
  const {
    parameterValues,
    checklist: { name, id },
  } = jobInfo || {};

  const { getValues } = form;

  const [isHiddenParamsUpdated, setIsHiddenParamsUpdated] = useState(false);
  const [hiddenParameterIds, setHiddenParameterIds] = useState<Record<string, boolean>>({});
  const initialHiddenParameterIds = useRef<Record<string, boolean>>({});

  const executeBranchingRulesParameter = async (
    parameterValues: Record<string, Parameter>,
    checklistId: string,
  ) => {
    const { data } = await request('PATCH', apiBranchingRuleExecute(), {
      data: { parameterValues, checklistId },
    });

    if (data) {
      setHiddenParameterIds(updateHiddenParameterIds(data));
    }
  };

  const onChangeHandler = (parameterData: Parameter) => {
    if (
      [MandatoryParameter.SINGLE_SELECT, MandatoryParameter.RESOURCE].includes(parameterData.type)
    ) {
      const data = getValues();
      let _parameterValues = parameterValues.reduce((acc, parameter: any) => {
        if (data[parameter.id]) {
          acc[parameter.id] = {
            parameter: data[parameter.id],
            reason: data[parameter?.id]?.response?.reason || '',
          };
        }
        return acc;
      }, {});
      executeBranchingRulesParameter(_parameterValues, id);
    }
  };

  useEffect(() => {
    const initialHiddenParameters: Record<string, string[]>[] = [];
    if (parameterValues?.length) {
      let updatedParam = {
        hide: [],
        show: [],
      };
      parameterValues.forEach((param) => {
        const hidden = param?.response?.[0]?.hidden;
        if (hidden) {
          updatedParam = { ...param, hide: [...updatedParam.hide, param.id], show: [] };
          initialHiddenParameters.push(updatedParam);
          initialHiddenParameterIds.current[param.id] = true;
          setHiddenParameterIds(updateHiddenParameterIds(initialHiddenParameters));
        }
      });
      setIsHiddenParamsUpdated(true);
    }

    return () => {
      setHiddenParameterIds(updateHiddenParameterIds([]));
    };
  }, [parameterValues]);

  return (
    <EditCjfWrapper>
      <Select
        label="Process"
        placeholder="Process"
        options={[]}
        isDisabled
        defaultValue={[
          {
            value: id,
            label: name,
          },
        ]}
      />
      {isHiddenParamsUpdated &&
        parameterValues?.map(
          (parameter: Parameter) =>
            !hiddenParameterIds[parameter.id] && (
              <ParameterView
                key={parameter.id}
                form={form}
                parameter={formatParameter(parameter)}
                onChangeHandler={onChangeHandler}
                parameterValues={parameterValues}
                initialHiddenParameterIds={initialHiddenParameterIds.current}
              />
            ),
        )}
    </EditCjfWrapper>
  );
};

export default EditCjf;
