import { ValidationTypeConstants } from '#PrototypeComposer/constants';
import { AssigneeList, FormGroup } from '#components';
import useParameterResponse from '#hooks/useParameterResponse';
import { useTypedSelector } from '#store';
import { MandatoryParameter } from '#types';
import { nonEmptyStringRegex } from '#utils/constants';
import { InputTypes } from '#utils/globalTypes';
import { formatDateTime } from '#utils/timeUtils';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import React, { FC, useCallback, useMemo } from 'react';
import styled from 'styled-components';
import { getCurrentValidation } from '#utils/parameterUtils';
import ValidationError from './ValidationError';

export const ReviewerViewWrapper = styled.div.attrs({
  className: 'reviewer-view-wrapper',
})`
  .read-only-group {
    padding: 8px 0 0 0;
    .read-only {
      margin-bottom: 16px;
      flex-direction: column;
      .content {
        ::before {
          display: none;
        }
        font-size: 12px;
        line-height: 1.33;
        letter-spacing: 0.32px;
        color: #525252;

        :last-child {
          font-size: 14px;
          line-height: 1.14;
          letter-spacing: 0.16px;
          color: #161616;
          padding-top: 4px;
        }
      }
    }
  }
  .grid-view {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0px;
  }
  .hr-tag {
    border: 1px solid #f4f4f4;
    margin: 0;
  }
  .badge {
    padding: 4px 8px;
    background: #d0e2ff;
    color: #0043ce;
    font-weight: 400;
    font-size: 12px;
    line-height: 16px;
  }
  .assignments {
    margin: 0;
  }
`;

export const ApproverView: FC<any> = ({
  parameter,
  form = {},
  setShowPasswordField = () => {},
  isReadOnly,
  jobId,
}) => {
  const { register } = form;

  const labelValueOrderTree = useMemo(
    () => ({
      operator: 1,
      criteriaType: 2,
      value: 3,
      lowerValue: 4,
      upperValue: 5,
      lowerValueParameterId: 6,
      upperValueParameterId: 7,
      valueParameterId: 8,
      uom: 9,
    }),
    [],
  );

  const labelValue = useMemo(
    () => ({
      uom: 'Unit of Measure',
      operator: 'Criteria',
      value: 'Value',
      criteriaType: 'Value Type',
      lowerValue: 'Lower Value',
      upperValue: 'Upper Value',
      lowerValueParameterId: 'Lower Value',
      upperValueParameterId: 'Upper Value',
      valueParameterId: 'Value',
    }),
    [],
  );

  const valueForLabel = useMemo(
    () => ({
      EQUAL_TO: '( = ) Equal to',
      LESS_THAN: '( < ) Less than',
      LESS_THAN_EQUAL_TO: '( <= ) Less than equal to',
      MORE_THAN: '( > ) More than',
      MORE_THAN_EQUAL_TO: '( >= ) More than equal to',
      BETWEEN: '( <-> ) Between',
      CONSTANT: 'Constant',
      PARAMETER: 'Parameter',
    }),
    [],
  );

  const initiator = useMemo(() => parameter.response?.exception?.createdBy, [parameter.response]);

  const approverDetails = useMemo(() => {
    return parameter.response?.exception?.reviewer?.reduce((acc, currReviewer) => {
      if (currReviewer?.user) {
        acc.push({
          id: currReviewer.user.id,
          firstName: currReviewer.user.firstName,
          lastName: currReviewer.user.lastName,
          employeeId: currReviewer.user.employeeId,
        });
      }
      return acc;
    }, []);
  }, [parameter.response]);

  const exceptionDetails = useMemo(() => parameter.response?.exception, [parameter.response]);
  const validationsCriteria = useMemo(() => parameter.validations, [parameter.validations]);
  const parameterType = useMemo(() => parameter.type, [parameter.response]);

  const linkedParameterIds = useMemo(() => {
    const ids = new Set<string>();
    if (validationsCriteria.validationType === ValidationTypeConstants.CRITERIA) {
      validationsCriteria?.criteriaValidations?.forEach((item) => {
        if (item?.valueParameterId) {
          ids.add(item.valueParameterId);
        }
        if (item?.lowerValueParameterId) {
          ids.add(item.lowerValueParameterId);
        }
        if (item?.upperValueParameterId) {
          ids.add(item.upperValueParameterId);
        }
      });
    }
    return ids;
  }, []);

  const dataById = useParameterResponse(jobId!, Array.from(linkedParameterIds));

  const getLatestValue = useCallback(
    (parameterId) => {
      return dataById?.[parameterId]?.value || null;
    },
    [dataById],
  );

  const getValue = useCallback(
    (key, value) => {
      if (
        key === 'lowerValueParameterId' ||
        key === 'upperValueParameterId' ||
        key === 'valueParameterId'
      ) {
        return getLatestValue(value) || '-';
      }
      return valueForLabel[value] || value;
    },
    [dataById],
  );

  const getValueDetails = useCallback(
    (key, value) => {
      if (
        key === 'lowerValueParameterId' ||
        key === 'upperValueParameterId' ||
        key === 'valueParameterId'
      ) {
        return dataById?.[value]?.label || '-';
      }
      return '';
    },
    [dataById],
  );

  const calculateCriteriaValues = useCallback(() => {
    if (validationsCriteria.validationType === ValidationTypeConstants.CRITERIA) {
      const keyValueArray: { label: string; value: any; orderTree: number }[] = [];
      validationsCriteria?.criteriaValidations?.forEach((item) => {
        for (const key in item) {
          if (item[key] !== null && item[key] !== '' && key !== 'id' && labelValue?.[key]) {
            const newObj: any = {};
            newObj['label'] = labelValue?.[key];
            newObj['value'] = getValue(key, item?.[key]);
            newObj['orderTree'] = labelValueOrderTree?.[key] || 11;
            if (
              key === 'lowerValueParameterId' ||
              key === 'upperValueParameterId' ||
              key === 'valueParameterId'
            ) {
              const parameterParameterDetails: any = {};
              parameterParameterDetails['label'] = labelValue[key] + ' Label';
              parameterParameterDetails['value'] = getValueDetails(key, item?.[key]);
              parameterParameterDetails['orderTree'] = labelValueOrderTree[key];
              keyValueArray.push(parameterParameterDetails);
            }
            keyValueArray.push(newObj);
          }
        }
      });
      return keyValueArray.sort((a, b) => a.orderTree - b.orderTree);
    }
    return [];
  }, [validationsCriteria, dataById]);

  const validation = useMemo(
    () => getCurrentValidation(parameter?.validations),
    [parameter?.validations],
  );

  return (
    <ReviewerViewWrapper>
      <ReadOnlyGroup
        className="read-only-group"
        items={[
          {
            label: 'Validation Rule',
            value: <ValidationError error={validation?.errorMessage} />,
          },
          {
            label: 'Status',
            value: <div className="badge">{exceptionDetails?.status}</div>,
          },
          {
            label: 'Initiator',
            value: <AssigneeList users={[initiator]} />,
          },
          {
            label: 'Date of Initiation',
            value: `${formatDateTime({ value: exceptionDetails?.createdAt })}`,
          },
          {
            label: 'Reason for exception',
            value: `${exceptionDetails.initiatorsReason || exceptionDetails.reason}`,
          },
          ...(exceptionDetails?.value
            ? [
                {
                  label: 'Value Entered',
                  value: [MandatoryParameter.DATE, MandatoryParameter.DATE_TIME].includes(
                    parameterType,
                  )
                    ? `${formatDateTime({ value: exceptionDetails?.value })}`
                    : `${exceptionDetails?.value}`,
                },
              ]
            : []),
          ...(exceptionDetails?.choices
            ? [
                {
                  label: 'Object Selected',
                  value: (
                    <span style={{ wordBreak: 'break-all', color: 'unset' }}>
                      {exceptionDetails?.choices
                        .map(
                          (choice) =>
                            `${choice?.objectDisplayName} (ID : ${choice?.objectExternalId})`,
                        )
                        .join(', ')}
                    </span>
                  ),
                },
              ]
            : []),
        ]}
      />
      {validationsCriteria.validationType === ValidationTypeConstants.CRITERIA && (
        <>
          <ReadOnlyGroup
            className="read-only-group grid-view"
            items={[...calculateCriteriaValues()]}
          />
          <hr className="hr-tag" />
        </>
      )}
      {approverDetails.length ? (
        <>
          <ReadOnlyGroup
            className="read-only-group"
            items={[
              {
                label: 'Approver',
                value: (
                  <AssigneeList
                    users={(approverDetails || []).map((currReviewer) => ({ ...currReviewer }))}
                  />
                ),
              },
            ]}
          />
          <hr className="hr-tag" />
        </>
      ) : null}
      {!isReadOnly && (
        <FormGroup
          key="basic-info-section"
          inputs={[
            {
              type: InputTypes.MULTI_LINE,
              props: {
                placeholder: 'Write here',
                label: 'Approver Remarks',
                id: 'reason',
                name: 'reason',
                rows: 2,
                ref: register({ required: true, pattern: nonEmptyStringRegex }),
                onChange: () => setShowPasswordField(false),
              },
            },
          ]}
        />
      )}
    </ReviewerViewWrapper>
  );
};
