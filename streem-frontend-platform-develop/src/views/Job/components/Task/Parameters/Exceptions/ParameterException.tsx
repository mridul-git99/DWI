import { AssigneeList, FormGroup } from '#components';
import { InputTypes } from '#utils/globalTypes';
import React, { FC, useMemo } from 'react';
import styled from 'styled-components';
import SelectApproversField from './SelectApproversField';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import { getCurrentValidation } from '#utils/parameterUtils';
import ValidationError from './ValidationError';

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 24px;

  .parameter-exception {
    display: flex;
    gap: 24px;
  }

  .form-group {
    padding: 0px;
  }

  .read-only-group {
    font-size: 14px;
    padding: 0px 0px 24px 0px;
    border-bottom: 1px solid #d0d5dd;
    color: #000000;
    width: 100%;

    .assignments {
      margin: 0px;
    }
  }
`;

type TParameterExceptionProps = {
  step: number;
  showApprovers?: boolean;
  parameterLabel: string;
  approverList: any[];
  approverListStatus: string;
  fetchApprovers: (params?: Record<string, any>) => void;
  fetchNextApprovers: () => void;
  reason: string;
  approvers: any[];
  onReasonChange: (value: string) => void;
  onApproverChange: (value: any[]) => void;
  isReadOnly: boolean;
  validations: any;
};

const ParameterException: FC<TParameterExceptionProps> = ({
  step,
  showApprovers = false,
  parameterLabel,
  approverList,
  approverListStatus,
  fetchApprovers,
  fetchNextApprovers,
  reason,
  approvers,
  onReasonChange,
  onApproverChange,
  isReadOnly,
  validations,
}) => {
  const validation = useMemo(() => getCurrentValidation(validations), [validations]);

  return (
    <Wrapper>
      {step === 1 ? (
        <>
          <ValidationError error={validation?.errorMessage} />
          <div className="parameter-exception">
            <FormGroup
              style={{ height: '65px' }}
              inputs={[
                {
                  type: InputTypes.SINGLE_LINE,
                  props: {
                    id: 'parameter',
                    label: 'Parameter',
                    placeholder: 'Parameter',
                    disabled: true,
                    defaultValue: parameterLabel,
                    onChange: () => {},
                    ...(isReadOnly && {
                      tooltipLabel: showApprovers
                        ? 'Exception request initiated successfully'
                        : 'Exception request approved successfully',
                    }),
                  },
                },
              ]}
            />
            <FormGroup
              inputs={[
                {
                  type: InputTypes.MULTI_LINE,
                  props: {
                    id: 'reason',
                    label: 'Reason',
                    placeholder: 'Enter your reason here',
                    onChange: (value: any) => {
                      const reason = value.value.trim();
                      onReasonChange(reason);
                    },
                    defaultValue: reason,
                    disabled: isReadOnly,
                    rows: 2,
                    maxRows: 3,
                  },
                },
              ]}
            />
            {showApprovers && (
              <SelectApproversField
                list={approverList}
                loading={approverListStatus === 'loading'}
                fetchData={fetchApprovers}
                fetchNext={fetchNextApprovers}
                onChange={onApproverChange}
                selectedApprovers={approvers}
                isDisabled={isReadOnly}
              />
            )}
          </div>
        </>
      ) : (
        <ReadOnlyGroup
          className="read-only-group"
          items={[
            {
              label: 'Parameter',
              value: parameterLabel,
            },
            ...(showApprovers
              ? [
                  {
                    label: 'Approver',
                    value: <AssigneeList users={approvers} />,
                  },
                ]
              : []),
            {
              label: 'Reason',
              value: reason,
            },
          ]}
        />
      )}
    </Wrapper>
  );
};

export default ParameterException;
