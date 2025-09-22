import { LoadingContainer } from '#components';
import useRequest from '#hooks/useRequest';
import { useTypedSelector } from '#store/helpers';
import { MandatoryParameter, PartialParameter, StoreParameter } from '#types';
import { apiGetLatestParameterInfo } from '#utils/apiUrls';
import { SelectorOptionsEnum } from '#utils/globalTypes';
import { generateShouldBeCriteria } from '#utils/stringUtils';
import { navigateToTaskExecution } from '#views/Job/utils';
import React, { FC, memo, useMemo } from 'react';

const ClickableParameter: FC<{ parameterId: string }> = memo(({ parameterId }) => {
  const jobId = useTypedSelector((state) => state.job.id);

  const { dataById, status } = useRequest<PartialParameter>({
    url: apiGetLatestParameterInfo(jobId!),
    method: 'PATCH',
    body: {
      parameterIds: [parameterId],
    },
  });

  if (status !== 'success')
    return (
      <LoadingContainer linear loading={true} style={{ display: 'inline-flex', width: 200 }} />
    );

  const { hidden, parameterValueId, taskExecutionId, label } = dataById[parameterId] || {};

  return (
    <>
      {hidden || !taskExecutionId || !parameterValueId ? (
        label
      ) : (
        <span
          onClick={() => {
            navigateToTaskExecution(jobId, taskExecutionId, parameterValueId);
          }}
          style={{
            cursor: 'pointer',
            textDecoration: 'underline',
            color: '#1d84ff',
            pointerEvents: 'all',
          }}
        >
          {label}
        </span>
      )}
    </>
  );
});

const ParameterLabel: FC<{
  parameter: StoreParameter;
}> = ({ parameter }) => {
  const { type, label, validations } = parameter || {};

  const getCriteriaDetails = (criteriaValidations: any) => {
    const {
      uom,
      value,
      operator,
      lowerValue,
      upperValue,
      criteriaType,
      valueParameterId,
      lowerValueParameterId,
      upperValueParameterId,
    } = criteriaValidations[0];

    const unit = uom || '';

    if (criteriaType === SelectorOptionsEnum.CONSTANT) {
      const text =
        operator === 'BETWEEN'
          ? `${lowerValue} ${unit} and ${upperValue} ${unit}`
          : `${value} ${unit}`;
      return `${label} should be ${generateShouldBeCriteria(criteriaValidations[0])} ${text}`;
    } else if (criteriaType === SelectorOptionsEnum.PARAMETER) {
      return (
        <span>
          {label} should be {generateShouldBeCriteria(criteriaValidations[0])}{' '}
          {operator === 'BETWEEN' ? (
            <>
              <ClickableParameter parameterId={lowerValueParameterId} /> {unit} and{' '}
              <ClickableParameter parameterId={upperValueParameterId} /> {unit}
            </>
          ) : (
            <>
              <ClickableParameter parameterId={valueParameterId} /> {unit}
            </>
          )}
        </span>
      );
    }
  };

  const criteriaDetails = useMemo(() => {
    if (validations[0]?.criteriaValidations?.length) {
      return getCriteriaDetails(validations[0]?.criteriaValidations);
    }
    return null;
  }, [validations]);

  const getLabelForNumberParameter = () => {
    return criteriaDetails || label;
  };

  return <>{type === MandatoryParameter.NUMBER ? getLabelForNumberParameter() : label}</>;
};

export default ParameterLabel;
