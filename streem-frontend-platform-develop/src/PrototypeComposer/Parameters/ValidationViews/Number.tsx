import {
  ValidationTypeConstants,
  exceptionTypeMap,
  validationTypeMap,
} from '#PrototypeComposer/constants';
import InfoIcon from '#assets/svg/info.svg';
import { Button, FormGroup } from '#components';
import Tooltip from '#components/shared/Tooltip';
import { InputTypes } from '#utils/globalTypes';
import { ObjectType } from '#views/Ontology/types';
import { AddCircleOutline, Close } from '@material-ui/icons';
import React, { FC, useEffect, useState } from 'react';
import { useFormContext } from 'react-hook-form';
import { v4 as uuidv4 } from 'uuid';
import CriteriaValidation from './CriteriaValidation';
import { NumberResourceValidationRow } from './NumberResourceValidationRow';
import { ValidationWrapper } from './styles';

type NumberValidationState = {
  isLoadingObjectType: Record<number, boolean>;
  isLoadingParameters: boolean;
  selectedObjectTypes: Record<number, ObjectType>;
  criteriaParameterValidations: any[];
  validationType?: ValidationTypeConstants;
};

const resourceKeyToValidate = [
  'parameterId',
  'propertyId',
  'propertyInputType',
  'propertyExternalId',
  'propertyDisplayName',
  'constraint',
  'errorMessage',
];

const criteriaKeyToValidate = ['errorMessage', 'operator', 'criteriaType'];

const NumberValidation: FC<{
  isReadOnly: boolean;
  checklistId: string;
  isVariationView?: boolean;
  stepperCount?: number;
  currentParameter?: any;
}> = ({ isReadOnly, checklistId, isVariationView = false, stepperCount, currentParameter }) => {
  const { watch, setValue, register, setError, clearErrors } = useFormContext();
  const validations = watch('validations', []);
  const autoInitialized = watch('autoInitialized', false);

  register('validations');

  const [state, setState] = useState<NumberValidationState>({
    isLoadingObjectType: {},
    isLoadingParameters: false,
    selectedObjectTypes: {},
    criteriaParameterValidations: [],
    validationType: validations?.[0]?.validationType,
  });
  const { validationType } = state;

  useEffect(() => {
    updateValidations(validations);
    return () => {
      clearErrors('data');
    };
  }, []);

  const updateValidation = (updatedValidation: any[], validationIndex: number) => {
    const _updated = validations.map((v: any, i: number) => {
      if (i === validationIndex) {
        if (v.validationType === ValidationTypeConstants.RESOURCE) {
          return {
            ...v,
            resourceParameterValidations: updatedValidation,
          };
        }
        return {
          ...v,
          criteriaValidations: updatedValidation,
        };
      }
      return v;
    });

    updateValidations(_updated);
  };

  const updateValidations = (validations: any[], type: string = '') => {
    let isValid = true;
    if (type && validations?.length === 0) {
      isValid = false;
    }
    validations.map((validation: any) => {
      if (!validation) return true;
      const _keyToValidate = ['exceptionApprovalType', 'validationType'];
      if (validationType === ValidationTypeConstants.RESOURCE) {
        _keyToValidate.push('resourceParameterValidations');
      } else if (validationType === ValidationTypeConstants.CRITERIA) {
        _keyToValidate.push('criteriaValidations');
      }

      _keyToValidate.every((key) => {
        const checkSingleProperty = !!validation?.[key]?.length;
        if (!checkSingleProperty) {
          isValid = false;
        }
        if (isValid) {
          if (validationType === ValidationTypeConstants.RESOURCE) {
            validation.resourceParameterValidations.every((propertyValidation: any) => {
              if (!propertyValidation) return true;
              resourceKeyToValidate.every((key) => {
                const checkSingleProperty = !!propertyValidation?.[key]?.length;
                if (!checkSingleProperty) {
                  isValid = false;
                }
                return isValid;
              });
              return isValid;
            });
          } else if (validationType === ValidationTypeConstants.CRITERIA) {
            validation.criteriaValidations.every((propertyValidation: any) => {
              if (!propertyValidation) return true;
              criteriaKeyToValidate.every((key) => {
                const checkSingleProperty = !!propertyValidation?.[key]?.length;
                if (!checkSingleProperty) {
                  isValid = false;
                }
                return isValid;
              });
              if (!isValid) {
                return isValid;
              }
              if (propertyValidation.operator === 'BETWEEN') {
                if (
                  (propertyValidation.lowerValueParameterId &&
                    propertyValidation.upperValueParameterId) ||
                  (propertyValidation.lowerValue && propertyValidation.upperValue)
                ) {
                  isValid = true;
                } else {
                  isValid = false;
                }
              } else {
                if (propertyValidation.valueParameterId || propertyValidation.value) {
                  isValid = true;
                } else {
                  isValid = false;
                }
              }
              return isValid;
            });
          }
        }
        return isValid;
      });
    });

    setValue('validations', validations, {
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

  const getValidationData = () => {
    return validationTypeMap.find((item) => item.value === validationType);
  };

  const getExceptionType = (exceptionApprovalType: any) => {
    return exceptionTypeMap.find((item) => item.value === exceptionApprovalType) || null;
  };

  return (
    <ValidationWrapper>
      <FormGroup
        style={{ marginBottom: 24 }}
        inputs={[
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'validations.validationType',
              label: 'Select Validation Type',
              options: autoInitialized
                ? [{ label: 'Criteria', value: 'CRITERIA' }]
                : validationTypeMap,
              placeholder: 'Choose from the options',
              value: validationType ? getValidationData() : undefined,
              isDisabled: isReadOnly || isVariationView || autoInitialized,
              onChange: (option: { value: any }) => {
                if (option?.value) {
                  if (option.value === ValidationTypeConstants.RESOURCE) {
                    updateValidations([], option?.value);
                  } else {
                    updateValidations(
                      [
                        {
                          ruleId: uuidv4(),
                          validationType: option.value,
                          criteriaValidations: [
                            {
                              id: uuidv4(),
                            },
                          ],
                          resourceParameterValidations: [],
                        },
                      ],
                      option?.value,
                    );
                  }
                } else {
                  updateValidations([]);
                }

                setState((p) => ({
                  ...p,
                  validationType: option?.value,
                }));
              },
              isClearable: true,
            },
          },
        ]}
      />
      {validations.map((validation: any, validationIndex: number) => {
        return (
          <div className="validation" key={validation.ruleId}>
            <div className="validation-header">
              <div>Validation {validationIndex + 1}</div>
            </div>
            <div className="upper-row">
              <FormGroup
                inputs={[
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'exceptionApprovalType',
                      label: 'Select Exception Type',
                      options: exceptionTypeMap,
                      placeholder: 'Choose from the options',
                      value: getExceptionType(validation.exceptionApprovalType),
                      isDisabled: isReadOnly || isVariationView,
                      onChange: async (option: { value: any }) => {
                        validations[validationIndex] = {
                          ...validations[validationIndex],
                          exceptionApprovalType: option.value,
                        };
                        updateValidations(validations);
                      },
                    },
                  },
                ]}
              />
              {validationType === ValidationTypeConstants.RESOURCE &&
                !isReadOnly &&
                !isVariationView && (
                  <Close
                    className="remove-icon"
                    onClick={() => {
                      validations?.splice(validationIndex, 1);
                      updateValidations(validations);
                    }}
                  />
                )}
            </div>
            {validationType === ValidationTypeConstants.CRITERIA && (
              <CriteriaValidation
                updateValidation={updateValidation}
                criteriaValidations={validation.criteriaValidations}
                isReadOnly={isReadOnly}
                targetEntityType={currentParameter?.targetEntityType}
                checklistId={checklistId}
                validationIndex={validationIndex}
              />
            )}
            {validationType === ValidationTypeConstants.RESOURCE && (
              <>
                {validation.resourceParameterValidations.map((item: any, index: number) => (
                  <NumberResourceValidationRow
                    key={item.id}
                    updateValidation={updateValidation}
                    resourceValidation={item}
                    isReadOnly={isReadOnly}
                    checklistId={checklistId}
                    validationIndex={validationIndex}
                    index={index}
                    resourceValidations={validation.resourceParameterValidations}
                  />
                ))}
              </>
            )}
          </div>
        );
      })}
      <div className="validation-disabled">
        {validationType === ValidationTypeConstants.RESOURCE && !isReadOnly && !isVariationView && (
          <Button
            type="button"
            variant="secondary"
            disabled={autoInitialized}
            style={{ marginBottom: 24, padding: '6px 8px' }}
            onClick={() => {
              const ruleId = uuidv4();
              validations[validations.length] = {
                ruleId,
                validationType: ValidationTypeConstants.RESOURCE,
                resourceParameterValidations: [
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

export default NumberValidation;
