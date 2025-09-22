import { useTypedSelector } from '#store';
import { getParametersInfo } from '#views/Job/utils';
import React, { FC, useEffect, useState } from 'react';
import { ParameterProps } from './Parameter';

const VariableLabel: FC<{ parameterId: string; response: any; keyLabel: string }> = ({
  parameterId,
  response,
  keyLabel,
}) => {
  const { id } = useTypedSelector((state) => state.job);
  const [inputLabel, setInputlabel] = useState<string>('');

  const fetchReferenceParameter = async (referencedParameterId: string) => {
    const data = await getParametersInfo(id, [referencedParameterId]);
    if (data?.[referencedParameterId]) {
      setInputlabel(data[referencedParameterId].label);
    }
  };

  useEffect(() => {
    if (parameterId) {
      fetchReferenceParameter(parameterId);
    }
  }, []);

  return (
    <span className="variable">
      <span className="name">{keyLabel}:</span>
      <span className="value">
        {inputLabel} = {response?.value || ' -'}
      </span>
    </span>
  );
};

const CalculationParameter: FC<ParameterProps> = ({ parameter }) => {
  return (
    <div className="calculation-parameter" data-id={parameter.id} data-type={parameter.type}>
      <span className="head">Calculation</span>
      <span className="expression" data-for={parameter.id}>
        {parameter.label} = {parameter.data.expression}
      </span>
      <span className="head">Input(s)</span>
      {Object.entries(parameter.data.variables).map(([key, value]: any) => {
        const response = parameter?.response?.choices?.find((choice: any) => {
          return choice.parameterId === value.parameterId;
        });
        return (
          <VariableLabel
            key={key}
            parameterId={value.parameterId}
            response={response}
            keyLabel={key}
          />
        );
      })}
      {parameter?.response?.value && (
        <>
          <span className="head" style={{ marginTop: 24 }}>
            Result
          </span>
          <span className="result">
            {parameter.label} = {parameter.response.value} {parameter.data.uom || ''}
          </span>
        </>
      )}
    </div>
  );
};

export default CalculationParameter;
