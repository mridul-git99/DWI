import { Checkbox } from '#components';
import { ParameterVerificationTypeEnum } from '#types';
import { getParameterContent } from '#utils/parameterUtils';
import React, { useMemo } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  .selected-parameter {
    background-color: #f9f9f9;
    border: 1px solid #f4f4f4;
  }

  .item-content {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 8px;
    border-radius: 4px;

    .parameter-label {
      font-size: 16px;
      color: #161616;
    }

    .parameter-consent-container {
      color: #161616;
      background-color: #f9f9f9;
      border: 1px solid #f4f4f4;
      padding: 8px;
    }
  }
`;

export const ParameterVerificationDetails = ({
  selected,
  handleParameterSelection,
  parameter,
  verificationType,
}: any) => {
  const parameterValue = useMemo(() => {
    return getParameterContent(parameter);
  }, [parameter]);

  return (
    <Wrapper>
      <div className={`item-content ${selected ? 'selected-parameter' : ''}`}>
        <Checkbox
          checked={selected}
          onClick={() => {
            handleParameterSelection(parameter);
          }}
          label={
            <div>
              <div className="parameter-label">{parameter.label}</div>
              {verificationType === ParameterVerificationTypeEnum.SELF && (
                <div className="parameter-consent-container">{parameterValue}</div>
              )}
            </div>
          }
        />
      </div>
    </Wrapper>
  );
};
