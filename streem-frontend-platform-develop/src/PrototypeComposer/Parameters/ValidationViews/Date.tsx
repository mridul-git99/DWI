import { exceptionTypeMap } from '#PrototypeComposer/constants';
import InfoIcon from '#assets/svg/info.svg';
import { Button, FormGroup } from '#components';
import Tooltip from '#components/shared/Tooltip';
import { MandatoryParameter } from '#types';
import { InputTypes, SelectorOptionsEnum } from '#utils/globalTypes';
import { AddCircleOutline } from '@material-ui/icons';
import React, { FC, useEffect } from 'react';
import { useFormContext } from 'react-hook-form';
import { v4 as uuidv4 } from 'uuid';
import { DateTimeValidationRow } from './DateTimeValidationRow';
import { ValidationWrapper } from './styles';
import { Parameter } from '#PrototypeComposer/Activity/types';

const DateValidation: FC<{
  isReadOnly: boolean;
  isVariationView?: boolean;
  stepperCount?: number;
  checklistId: string;
  currentParameter?: Parameter;
}> = ({ isReadOnly, isVariationView = false, stepperCount, checklistId, currentParameter }) => {
  const { watch, setValue, setError, clearErrors, register } = useFormContext();

  const validations = watch('validations', []);
  const parameterType = watch('type');
  const autoInitialized = watch('autoInitialized', false);

  const updateValidations = (updatedValidations: any[]) => {
    let isValid = true;
    updatedValidations.every((validation: any) => {
      if (!validation) return true;
      const keyToValidate = ['exceptionApprovalType', 'dateTimeParameterValidations', 'ruleId'];
      const valuesToValidate = [
        'constraint',
        'selector',
        'errorMessage',
        ...(validation?.dateTimeParameterValidations[0]?.selector === SelectorOptionsEnum.CONSTANT
          ? ['value', 'dateUnit']
          : ['referencedParameterId']),
      ];
      keyToValidate.every((key) => {
        const checkSingleProperty = !!validation?.[key]?.length;

        if (!checkSingleProperty) {
          isValid = false;
          return isValid;
        }
        if (key === 'dateTimeParameterValidations') {
          valuesToValidate.every((value) => {
            const checkSingleValue =
              !!validation?.dateTimeParameterValidations?.[0]?.[value]?.length;
            if (!checkSingleValue) {
              isValid = false;
              return isValid;
            }
            return isValid;
          });
        }
        return isValid;
      });
      return isValid;
    });
    setValue('validations', [...updatedValidations], {
      shouldDirty: true,
    });
    if (!isValid) {
      setError('data', {
        stepperCount: stepperCount,
        message: 'All Validation Options Should be Filled.',
      });
    } else {
      clearErrors('data');
    }
  };

  useEffect(() => {
    if (Array.isArray(validations)) {
      updateValidations(validations);
    } else {
      updateValidations([]);
    }
    return () => {
      clearErrors('data');
    };
  }, []);

  register('validations');

  return (
    <ValidationWrapper>
      {Array.isArray(validations) &&
        validations.map((item: any, index: number) => {
          return (
            <ValidationItemRow
              item={item}
              index={index}
              validations={validations}
              updateValidations={updateValidations}
              isReadOnly={isReadOnly}
              isVariationView={isVariationView}
              parameterType={parameterType}
              checklistId={checklistId}
              currentParameter={currentParameter}
            />
          );
        })}
      <div className="validation-disabled">
        {!isReadOnly && !isVariationView && (
          <Button
            type="button"
            variant="secondary"
            style={{ marginBottom: 24, padding: '6px 8px' }}
            disabled={autoInitialized}
            onClick={() => {
              const ruleId = uuidv4();
              validations[validations.length] = {
                ruleId,
                dateTimeParameterValidations: [
                  {
                    id: uuidv4(),
                  },
                ],
              };
              updateValidations(validations);
            }}
          >
            <AddCircleOutline style={{ marginRight: 8 }} /> Add
          </Button>
        )}
        {!isReadOnly && autoInitialized && (
          <Tooltip
            title={
              'Validation disabled for Linked parameters. Remove Linking to enable Validation.'
            }
            arrow
            textAlignment="left"
          >
            <img src={InfoIcon} alt="parameter-info" style={{ marginRight: 8 }} />
          </Tooltip>
        )}
      </div>
    </ValidationWrapper>
  );
};

const ValidationItemRow = ({
  item,
  index,
  validations,
  updateValidations,
  isReadOnly,
  parameterType,
  isVariationView,
  checklistId,
  currentParameter,
}: {
  item: any;
  index: number;
  validations: any[];
  updateValidations: (validations: any[]) => void;
  isReadOnly: boolean;
  parameterType: MandatoryParameter;
  isVariationView: boolean;
  checklistId: string;
  currentParameter: Parameter;
}) => {
  const getExceptionType = (exceptionApprovalType: any) => {
    return exceptionApprovalType
      ? exceptionTypeMap.find((item) => item.value === exceptionApprovalType)
      : null;
  };

  return (
    <div className="validation" key={index}>
      <div className="validation-header">
        <div>Validation {index + 1}</div>
      </div>
      <div className="validation-text">Check if</div>
      <FormGroup
        inputs={[
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'exceptionApprovalType',
              label: 'Select Exception Type',
              options: exceptionTypeMap,
              placeholder: 'Choose from the options',
              value: getExceptionType(validations[index]?.exceptionApprovalType),
              isDisabled: isReadOnly || isVariationView,
              onChange: async (option: { value: any }) => {
                validations[index] = {
                  ...validations[index],
                  exceptionApprovalType: option.value,
                };
                updateValidations(validations);
              },
              style: {
                margin: 0,
              },
            },
          },
        ]}
      />
      {item.dateTimeParameterValidations?.map(
        (dateTimeValidation: any, dateTimeValidationIndex: number) => {
          return (
            <DateTimeValidationRow
              key={dateTimeValidationIndex}
              dateTimeValidation={dateTimeValidation}
              parameterType={parameterType}
              isReadOnly={isReadOnly}
              isVariationView={isVariationView}
              checklistId={checklistId}
              validations={validations}
              validation={item}
              index={index}
              updateValidations={updateValidations}
              currentParameter={currentParameter}
            />
          );
        },
      )}
    </div>
  );
};

export default DateValidation;
