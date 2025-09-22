import { Button, FormGroup } from '#components';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import { InputTypes } from '#utils/globalTypes';
import { debounce } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';

const YesNoTaskViewWrapper = styled.div<{ type: string }>`
  display: flex;
  flex-direction: column;

  .parameter-header {
    display: flex;

    button:first-child {
      background-color: ${(p) => (p.type === 'yes' ? '#5aa700' : '#ffffff')};
      color: ${(p) => (p.type === 'yes' ? '#ffffff' : '#5aa700')};
    }

    button:last-child {
      background-color: ${(p) => (p.type === 'no' ? '#ff6b6b' : '#ffffff')};
      color: ${(p) => (p.type === 'no' ? '#ffffff' : '#ff6b6b')};
    }
  }

  .parameter-textarea {
    margin-top: 16px;

    .input-label {
      font-size: 14px;
      color: rgb(22, 22, 22);
      letter-spacing: 0.16px;
      line-height: 1.29;
      margin-bottom: 8px;
    }
  }
`;

type InitialState = {
  id: string;
  name: string;
  type: string;
  reason?: string;
};

const YesNoTaskView: FC<Omit<ParameterProps, 'taskId'>> = ({ parameter, form }) => {
  const initialState: InitialState = useMemo(() => {
    const selectedOption = parameter?.data?.find((option: any) => option.state === 'SELECTED');
    return {
      id: selectedOption?.id || '',
      name: selectedOption?.name || '',
      type: selectedOption?.type || '',
      reason: selectedOption?.reason || '',
    };
  }, []);

  const [selection, setSelection] = useState(initialState);
  const { setValue, setError, clearErrors } = form;

  const selectedData = (selectedOptions: any, optionsList: any) => {
    return optionsList.map((currOption) => {
      const { reason, ...rest } = currOption;
      return currOption.id === selectedOptions.id
        ? {
            ...rest,
            state: 'SELECTED',
          }
        : { ...rest, state: 'NOT_SELECTED' };
    });
  };

  const setFormValues = (selectedOption: InitialState) => {
    setValue(
      parameter.id,
      {
        ...parameter,
        data: selectedData(selectedOption, parameter.data),
        response: {
          value: null,
          reason: selectedOption?.reason || '',
          state: 'EXECUTED',
          choices: {
            [parameter.data[0].id]:
              selectedOption.id === parameter.data[0].id ? 'SELECTED' : 'NOT_SELECTED',
            [parameter.data[1].id]:
              selectedOption.id === parameter.data[1].id ? 'SELECTED' : 'NOT_SELECTED',
          },
          medias: [],
          parameterValueApprovalDto: null,
        },
      },
      {
        shouldDirty: true,
        shouldValidate: true,
      },
    );
  };

  const updateValue = () => {
    let isValid = true;
    if (selection?.type === parameter.data[1].type) {
      if (!selection?.reason && parameter.mandatory) {
        isValid = false;
      } else {
        isValid = true;
      }
    }

    if (selection?.id) {
      if (selection?.type === parameter.data[1].type) {
        if (parameter.mandatory) {
          if (selection?.reason) {
            setFormValues(selection);
          }
        } else {
          setFormValues(selection);
        }
      } else if (selection?.type === parameter.data[0].type) {
        setFormValues(selection);
      }
    }

    if (!isValid) {
      setError(parameter.id, {
        message: 'Yes No Parameter Value Invalid',
      });
    } else {
      clearErrors(parameter.id);
    }
  };

  useEffect(() => {
    updateValue();
  }, [selection]);

  return (
    <YesNoTaskViewWrapper type={selection?.type} data-id={parameter.id} data-type={parameter.type}>
      <div className="parameter-header">
        <Button
          variant="secondary"
          color="green"
          onClick={() => {
            setSelection(parameter.data[0]);
          }}
        >
          {parameter.data[0].name}
        </Button>
        <Button
          variant="secondary"
          color="red"
          onClick={() => {
            setSelection(parameter.data[1]);
          }}
        >
          {parameter.data[1].name}
        </Button>
      </div>
      {selection?.type === parameter.data[1].type && (
        <div className="parameter-textarea">
          <div className="input-label">State your Reason</div>
          <FormGroup
            style={{ padding: 0 }}
            inputs={[
              {
                type: InputTypes.MULTI_LINE,
                props: {
                  placeholder: 'Write Here',
                  rows: '4',
                  defaultValue: selection?.reason,
                  onChange: debounce(({ value }) => {
                    setSelection((prev) => ({ ...prev, reason: value }));
                  }, 500),
                },
              },
            ]}
          />
        </div>
      )}
    </YesNoTaskViewWrapper>
  );
};

export default YesNoTaskView;
