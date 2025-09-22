import { MandatoryParameter, NonMandatoryParameter, TargetEntityType } from '#types';
import {
  CameraAltOutlined,
  DateRangeOutlined,
  DoneAllOutlined,
  EventNoteOutlined,
  ExposureOutlined,
  Filter1Outlined,
  FunctionsOutlined,
  GestureOutlined,
  LayersOutlined,
  LineStyle,
  PublishOutlined,
  ShortTextOutlined,
  SubjectOutlined,
  TonalityOutlined,
} from '@material-ui/icons';
import React from 'react';
import styled from 'styled-components';
import { ParameterExceptionTypeEnum } from './checklist.types';

export const ParameterIconByType = {
  [MandatoryParameter.CHECKLIST]: <LineStyle />,
  [NonMandatoryParameter.INSTRUCTION]: <LineStyle />,
  [NonMandatoryParameter.MATERIAL]: <LineStyle />,
  [MandatoryParameter.MEDIA]: <CameraAltOutlined />,
  [MandatoryParameter.MULTISELECT]: <DoneAllOutlined />,
  [MandatoryParameter.SHOULD_BE]: <ExposureOutlined />,
  [MandatoryParameter.SIGNATURE]: <GestureOutlined />,
  [MandatoryParameter.SINGLE_SELECT]: <DoneAllOutlined />,
  [MandatoryParameter.SINGLE_LINE]: <ShortTextOutlined />,
  [MandatoryParameter.MULTI_LINE]: <SubjectOutlined />,
  [MandatoryParameter.YES_NO]: <TonalityOutlined />,
  [MandatoryParameter.NUMBER]: <Filter1Outlined />,
  [MandatoryParameter.CALCULATION]: <FunctionsOutlined />,
  [MandatoryParameter.DATE]: <DateRangeOutlined />,
  [MandatoryParameter.DATE_TIME]: <EventNoteOutlined />,
  [MandatoryParameter.RESOURCE]: <LayersOutlined />,
  [MandatoryParameter.MULTI_RESOURCE]: <LayersOutlined />,
  [MandatoryParameter.FILE_UPLOAD]: <PublishOutlined />,
};

export const ParameterLabelByType = {
  [MandatoryParameter.CHECKLIST]: 'Checklist',
  [NonMandatoryParameter.INSTRUCTION]: 'Instruction',
  [NonMandatoryParameter.MATERIAL]: 'Material',
  [MandatoryParameter.MEDIA]: 'Image Capture',
  [MandatoryParameter.MULTISELECT]: 'Multi-select dropdown',
  [MandatoryParameter.SHOULD_BE]: 'Should Be',
  [MandatoryParameter.SIGNATURE]: 'Signature',
  [MandatoryParameter.SINGLE_SELECT]: 'Single-select dropdown',
  [MandatoryParameter.SINGLE_LINE]: 'Single-line text',
  [MandatoryParameter.MULTI_LINE]: 'Multi-line text',
  [MandatoryParameter.YES_NO]: 'Yes/No',
  [MandatoryParameter.NUMBER]: 'Number',
  [MandatoryParameter.CALCULATION]: 'Calculation',
  [MandatoryParameter.DATE]: 'Date',
  [MandatoryParameter.DATE_TIME]: 'Date-Time',
  [MandatoryParameter.RESOURCE]: 'Single Resource Selector',
  [MandatoryParameter.MULTI_RESOURCE]: 'Multiple Resource Selector',
  [MandatoryParameter.FILE_UPLOAD]: 'File Upload',
};

const ParameterLabelWrapper = styled.div`
  display: flex;
  align-items: center;
  color: #525252;
  font-size: 12px;
  line-height: 24px;
  letter-spacing: 0.32px;

  .parameter-icon {
    background-color: #4589ff;
    border-radius: 50%;
    margin-right: 8px;
    display: flex;
    padding: 4px;
    svg {
      color: #fff;
      height: 16px;
      width: 16px;
      margin: unset;
      &:hover {
        color: #fff;
      }
    }
  }
`;

const RenderParameterLabel: React.FC<{ label: string; icon: JSX.Element }> = ({ label, icon }) => {
  return (
    <ParameterLabelWrapper>
      <div className="parameter-icon">{icon}</div>
      {label}
    </ParameterLabelWrapper>
  );
};

export const ParameterTypeMap: Record<string, JSX.Element | string> = {
  [MandatoryParameter.CHECKLIST]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.CHECKLIST]}
      icon={ParameterIconByType[MandatoryParameter.CHECKLIST]}
    />
  ),
  [NonMandatoryParameter.INSTRUCTION]: (
    <RenderParameterLabel
      label={ParameterLabelByType[NonMandatoryParameter.INSTRUCTION]}
      icon={ParameterIconByType[NonMandatoryParameter.INSTRUCTION]}
    />
  ),
  [NonMandatoryParameter.MATERIAL]: (
    <RenderParameterLabel
      label={ParameterLabelByType[NonMandatoryParameter.MATERIAL]}
      icon={ParameterIconByType[NonMandatoryParameter.MATERIAL]}
    />
  ),
  [MandatoryParameter.MEDIA]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.MEDIA]}
      icon={ParameterIconByType[MandatoryParameter.MEDIA]}
    />
  ),
  [MandatoryParameter.MULTISELECT]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.MULTISELECT]}
      icon={ParameterIconByType[MandatoryParameter.MULTISELECT]}
    />
  ),
  [MandatoryParameter.SIGNATURE]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.SIGNATURE]}
      icon={ParameterIconByType[MandatoryParameter.SIGNATURE]}
    />
  ),
  [MandatoryParameter.SINGLE_SELECT]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.SINGLE_SELECT]}
      icon={ParameterIconByType[MandatoryParameter.SINGLE_SELECT]}
    />
  ),
  [MandatoryParameter.SINGLE_LINE]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.SINGLE_LINE]}
      icon={ParameterIconByType[MandatoryParameter.SINGLE_LINE]}
    />
  ),
  [MandatoryParameter.MULTI_LINE]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.MULTI_LINE]}
      icon={ParameterIconByType[MandatoryParameter.MULTI_LINE]}
    />
  ),
  [MandatoryParameter.YES_NO]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.YES_NO]}
      icon={ParameterIconByType[MandatoryParameter.YES_NO]}
    />
  ),
  [MandatoryParameter.NUMBER]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.NUMBER]}
      icon={ParameterIconByType[MandatoryParameter.NUMBER]}
    />
  ),
  [MandatoryParameter.CALCULATION]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.CALCULATION]}
      icon={ParameterIconByType[MandatoryParameter.CALCULATION]}
    />
  ),
  [MandatoryParameter.DATE]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.DATE]}
      icon={ParameterIconByType[MandatoryParameter.DATE]}
    />
  ),
  [MandatoryParameter.DATE_TIME]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.DATE_TIME]}
      icon={ParameterIconByType[MandatoryParameter.DATE_TIME]}
    />
  ),
  [MandatoryParameter.RESOURCE]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.RESOURCE]}
      icon={ParameterIconByType[MandatoryParameter.RESOURCE]}
    />
  ),
  [MandatoryParameter.MULTI_RESOURCE]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.MULTI_RESOURCE]}
      icon={ParameterIconByType[MandatoryParameter.MULTI_RESOURCE]}
    />
  ),
  [MandatoryParameter.FILE_UPLOAD]: (
    <RenderParameterLabel
      label={ParameterLabelByType[MandatoryParameter.FILE_UPLOAD]}
      icon={ParameterIconByType[MandatoryParameter.FILE_UPLOAD]}
    />
  ),
};

export const PARAMETER_OPERATORS = [
  { label: '( = ) Equal to', value: 'EQUAL_TO' },
  { label: '( < ) Less than', value: 'LESS_THAN' },
  { label: '( <= ) Less than equal to', value: 'LESS_THAN_EQUAL_TO' },
  { label: '( > ) More than', value: 'MORE_THAN' },
  { label: '( >= ) More than equal to', value: 'MORE_THAN_EQUAL_TO' },
  { label: '( <-> ) Between', value: 'BETWEEN' },
];

export const TargetEntityTypeVisual = {
  [TargetEntityType.TASK]: 'Task',
  [TargetEntityType.PROCESS]: 'Create Job Form',
  [TargetEntityType.UNMAPPED]: 'Unmapped',
};

export const validationTypeMap = [
  { label: 'Criteria', value: 'CRITERIA' },
  { label: 'Resource', value: 'RESOURCE' },
];

export enum ValidationTypeConstants {
  CRITERIA = 'CRITERIA',
  RESOURCE = 'RESOURCE',
}

export const exceptionTypeMap = [
  { label: 'Halt Parameter Exception', value: ParameterExceptionTypeEnum.DEFAULT_FLOW },
  { label: 'Enable Reasoned Entry', value: ParameterExceptionTypeEnum.ACCEPT_WITH_REASON_FLOW },
  { label: 'Enable Approval Workflow', value: ParameterExceptionTypeEnum.APPROVER_REVIEWER_FLOW },
];

export const parameterLabelMap = {
  [MandatoryParameter.DATE]: 'Date',
  [MandatoryParameter.DATE_TIME]: 'Date Time',
};
