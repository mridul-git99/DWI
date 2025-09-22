import { useTypedSelector } from '#store';
import { ParameterVerificationTypeEnum } from '#types';
import React from 'react';
import styled from 'styled-components';
import { ParameterVerificationDetails } from './ParameterVerificationDetails';
import { Button } from '#components';

const Wrapper = styled.div`
  .peer-verification-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
    .selectall-container {
      display: flex;
      justify-content: flex-end;
    }

    .no-data-found {
      text-align: center;
      padding: 16px;
      font-size: 16px;
      color: #ccc;
      margin-top: 16px;
    }
  }
`;

export const BulkParametersSection = (props: any) => {
  const {
    parameterResponseFields = [],
    parameterResponseAppend,
    parameterResponseRemove,
    setValue,
  } = props;

  const executedParametersWithPeerVerification = useTypedSelector(
    (state) => state.job.activeTask.executedParametersWithPeerVerification,
  );

  const handleParameterSelection = (params) => {
    const index = parameterResponseFields?.findIndex((field) => field.id === params.id);
    if (index > -1) {
      parameterResponseRemove(index);
    } else {
      parameterResponseAppend({
        ...params,
        checkedAt: Math.floor(Date.now() / 1000),
      });
    }
  };

  const handleSelectAll = () => {
    const selectedParams = executedParametersWithPeerVerification.map((param) => ({
      ...param,
      checkedAt: Math.floor(Date.now() / 1000),
    }));
    setValue('parameterResponse', selectedParams);
  };

  return (
    <Wrapper>
      <div className="peer-verification-container">
        <div className="selectall-container">
          <Button
            color="blue"
            variant="textOnly"
            disabled={
              parameterResponseFields.length === executedParametersWithPeerVerification.length
            }
            onClick={() => handleSelectAll()}
          >
            Select All
          </Button>
        </div>
        {executedParametersWithPeerVerification.length > 0 ? (
          executedParametersWithPeerVerification?.map((parameter) => {
            return (
              <ParameterVerificationDetails
                selected={parameterResponseFields.some((field) => field.id === parameter.id)}
                parameter={parameter}
                handleParameterSelection={handleParameterSelection}
                verificationType={ParameterVerificationTypeEnum.PEER}
              />
            );
          })
        ) : (
          <div className="no-data-found">No parameters to verify</div>
        )}
      </div>
    </Wrapper>
  );
};
