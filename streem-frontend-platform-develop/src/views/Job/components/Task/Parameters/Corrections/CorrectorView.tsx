import { AssigneeList, Avatar, CustomTag, FormGroup, StatusTag } from '#components';
import { useTypedSelector } from '#store';
import { MandatoryParameter, Parameter as ParameterType } from '#types';
import { openLinkInNewTab } from '#utils';
import { InputTypes } from '#utils/globalTypes';
import { getParameterContent } from '#utils/parameterUtils';
import { formatDateTime } from '#utils/timeUtils';
import { useJobStateToFlags } from '#views/Job/utils';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import React, { FC } from 'react';
import styled from 'styled-components';
import Parameter from '../Parameter';
import { InitiatorInfo, InitiatorInfoWrapper } from './InitiatorView';

type CorrectorViewProps = {
  correction: any;
  parameter: ParameterType;
  form: any;
  setCorrectedParameterValues: React.Dispatch<React.SetStateAction<any>>;
  isLoggedInUserCorrector: boolean;
};

const CorrectorViewWrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;

  .correction-parameter-list {
    .parameter-audit,
    .correction-action-button,
    .correction-initiator-icon,
    .parameter-label {
      display: none;
    }

    .parameter {
      padding: 0px;

      :last-child {
        border-bottom: none;
      }
    }
  }

  .info-item {
    display: flex;
    flex-direction: column;
    gap: 2px;
    line-height: 16px;
    @media (max-width: 900px) {
      width: 100%;
      justify-content: space-between;
      padding: 1rem;
      margin-block: 0;
      border-left: 1px solid #e0e0e0;
    }
    .info-item-label {
      font-size: 14px;
    }
    .info-item-value {
      font-size: 14px;
      letter-spacing: 0.16px;
      color: #161616;
      overflow: hidden;
      text-overflow: ellipsis;
      padding: 8px;
    }
  }
  .info-item:nth-child(3n + 1) {
    @media (max-width: 900px) {
      padding: 1rem 0;
      border-left: none;
    }
  }

  .correction-parameter-label {
    font-size: 14px;
  }

  .file-links {
    display: flex;
    flex-direction: column;
    gap: 4px;
    a {
      margin-right: 8px;
    }
    div {
      color: #1d84ff;
      margin-right: 8px;
      cursor: pointer;
    }
    > div > span {
      color: #1d84ff;
    }
  }
`;

export const CorrectorView: FC<CorrectorViewProps> = ({
  correction,
  parameter,
  form,
  setCorrectedParameterValues,
  isLoggedInUserCorrector,
}) => {
  const { register } = form;

  return (
    <CorrectorViewWrapper>
      <InitiatorInfo correction={correction} />
      {isLoggedInUserCorrector && (
        <>
          <div className="info-item" key={parameter.label}>
            <label className="info-item-label">{parameter.label} - Old Value</label>
            {getParameterValueView(parameter)}
          </div>
          <CorrectionParameterList
            parameter={parameter}
            setCorrectedParameterValues={setCorrectedParameterValues}
          />
          <FormGroup
            key="basic-info-section"
            inputs={[
              {
                type: InputTypes.MULTI_LINE,
                props: {
                  placeholder: 'Write here',
                  label: 'Corrector Remark',
                  id: 'correctorReason',
                  name: 'correctorReason',
                  rows: 3,
                  maxRows: 8,
                  ref: register({ required: true }),
                },
              },
            ]}
          />
        </>
      )}
    </CorrectorViewWrapper>
  );
};

const CorrectionParameterList: FC<
  Pick<CorrectorViewProps, 'parameter' | 'setCorrectedParameterValues'>
> = ({ parameter, setCorrectedParameterValues }) => {
  const {
    activeTask: task,
    errors: { parametersErrors },
  } = useTypedSelector((state) => state.job);
  const { taskExecution, isTaskAssigned } = task!;

  const { correctionEnabled } = taskExecution!;

  if (!taskExecution) return null;

  const { isTaskCompleted, isBlocked, isTaskBlocked } = useJobStateToFlags();
  return (
    <div className="correction-parameter-list">
      <div className="correction-parameter-label">{parameter.label} - New Value</div>
      <Parameter
        key={parameter.response.id}
        parameter={parameter}
        isTaskCompleted={isTaskCompleted}
        isLoggedInUserAssigned={isTaskAssigned}
        isCorrectingError={!!correctionEnabled}
        errors={parametersErrors.get(parameter.response.id)}
        setCorrectedParameterValues={setCorrectedParameterValues}
        source="correction-modal"
        isTaskBlocked={isTaskBlocked}
        isJobBlocked={isBlocked}
      />
    </div>
  );
};

export const getParameterValueView = (parameter: ParameterType) => {
  switch (parameter.type) {
    case MandatoryParameter.FILE_UPLOAD:
    case MandatoryParameter.MEDIA:
    case MandatoryParameter.SIGNATURE:
      return (
        <div className="info-item-value file-links">
          {parameter?.response?.medias?.length > 0 ? (
            parameter.response.medias.map((media: any) => {
              return (
                <CustomTag as={'div'} onClick={() => openLinkInNewTab(`/media?link=${media.link}`)}>
                  <span>
                    {parameter.type === MandatoryParameter.SIGNATURE
                      ? media.filename
                      : `${media?.name}.${media?.filename?.split('.')?.[1]}`}
                  </span>
                </CustomTag>
              );
            })
          ) : (
            <span className="info-item-value">N/A</span>
          )}
        </div>
      );

    default:
      const parameterValue = getParameterContent(parameter);
      return (
        <span className="info-item-value" title={parameterValue}>
          {parameterValue}
        </span>
      );
  }
};

export const CorrectorInfo: FC<Pick<CorrectorViewProps, 'parameter' | 'correction'>> = ({
  correction,
  parameter,
}) => {
  const {
    corrector,
    status,
    initiatorsReason,
    correctorsReason,
    oldValue,
    newValue,
    oldChoices,
    newChoices,
    oldMedias,
    newMedias,
  } = correction;
  return (
    <InitiatorInfoWrapper>
      <ReadOnlyGroup
        className="read-only-group"
        items={[
          {
            label: 'Status',
            value: <StatusTag status={status} />,
          },
          {
            label: 'Date of Initiation',
            value: formatDateTime({
              value: correction.createdAt,
            }),
          },
          {
            label: 'Initiator',
            value: (
              <Avatar
                user={correction?.createdBy}
                color="blue"
                backgroundColor="#F4F4F4"
                borderColor="#FFFFFF"
              />
            ),
          },
          {
            label: 'Description',
            value: initiatorsReason,
          },
        ]}
      />
      <ReadOnlyGroup
        className="read-only-group"
        items={[
          {
            label: `${parameter.label} - Old Value`,
            value: getParameterValueView({
              ...parameter,
              response: {
                ...parameter.response,
                value: oldValue,
                choices: oldChoices,
                medias: oldMedias,
              },
            }),
          },
          {
            label: `${parameter.label} - New Value`,
            value: getParameterValueView({
              ...parameter,
              response: {
                ...parameter.response,
                value: newValue,
                choices: newChoices,
                medias: newMedias,
              },
            }),
          },
          {
            label: 'Correctors',
            value: (
              <AssigneeList
                users={(corrector || []).map((currCurrector) => ({ ...currCurrector.user }))}
              />
            ),
          },
          {
            label: 'Corrector Remarks',
            value: correctorsReason,
          },
        ]}
      />
    </InitiatorInfoWrapper>
  );
};
