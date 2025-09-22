import { Step, StepIconProps, StepLabel, Stepper } from '@material-ui/core';
import { CheckCircleOutline, RadioButtonChecked, RadioButtonUnchecked } from '@material-ui/icons';
import React from 'react';
import styled from 'styled-components';

type SectionData = {
  label: string;
  value: string;
  panelContent: React.JSX.Element;
  renderFn: () => React.JSX.Element;
  description?: undefined;
};

const StepperWrapper = styled(Stepper)`
  display: flex;
  padding: 24px 0 !important;
`;

const StepWrapper = styled(Step)<{ active?: boolean }>`
  border-top: 2px solid ${(p) => (p.active ? '#1d84ff' : '#e0e0e0')};
  font-weight: bold;
  padding: 8px 0 !important;
  flex: 1;

  .MuiStepLabel-root.MuiStepLabel-alternativeLabel {
    align-items: flex-start;
  }

  .MuiStepLabel-label.MuiStepLabel-alternativeLabel {
    margin-top: -20px;
    text-align: left;
    padding-left: 28px;
    font-weight: bold;
    font-family: inherit;
  }

  .label-title {
    font-weight: 700;
  }

  .label-description {
    font-size: 12px;
    line-height: 1.33;
    letter-spacing: 0.32px;
    color: #525252;
  }
`;

const StepIconWrapper = styled.div<{ active?: boolean }>`
  display: flex;
  align-items: center;

  svg {
    color: #1d84ff;
    font-size: 20px;
  }
`;

function CustomStepIcon(props: StepIconProps) {
  const { active, completed } = props;

  return (
    <StepIconWrapper active={active}>
      {completed ? (
        <CheckCircleOutline />
      ) : active ? (
        <RadioButtonChecked />
      ) : (
        <RadioButtonUnchecked />
      )}
    </StepIconWrapper>
  );
}

export const StepperContainer = (props: { activeStep: number; sections: SectionData[] }) => {
  return (
    <div>
      <StepperWrapper activeStep={props.activeStep} alternativeLabel connector={<div />}>
        {props.sections.map((step: SectionData, index: number) => (
          <StepWrapper key={step.label} active={index <= props.activeStep}>
            <StepLabel StepIconComponent={CustomStepIcon}>
              <div className="label-title">{step.label}</div>
              {step.description && <div className="label-description">{step.description}</div>}
            </StepLabel>
          </StepWrapper>
        ))}
      </StepperWrapper>
    </div>
  );
};
