import InfoIcon from '#assets/svg/info.svg';
import { Button, FormGroup } from '#components';
import Tooltip from '#components/shared/Tooltip';
import { exceptionTypeMap } from '#PrototypeComposer/constants';
import { apiGetObjectTypes } from '#utils/apiUrls';
import { InputTypes, ResponseObj, SelectorOptionsEnum } from '#utils/globalTypes';
import { request } from '#utils/request';
import { Choice, ObjectType } from '#views/Ontology/types';
import { AddCircleOutline, Close } from '@material-ui/icons';
import { keyBy } from 'lodash';
import React, { FC, useEffect, useRef, useState } from 'react';
import { useFormContext } from 'react-hook-form';
import { v4 as uuidv4 } from 'uuid';
import ResourceValidationRow from './ResourceValidationRow';
import { ValidationWrapper } from './styles';

export const getDateUnits = (inputType: InputTypes) => {
  switch (inputType) {
    case InputTypes.DATE:
      return {
        DAYS: 'Days from today',
      };
    default:
      return {
        HOURS: 'Hours from now',
        // DAYS: 'Days',
        // WEEKS: 'Weeks',
        // MONTHS: 'Months',
        // YEARS: 'Years',
      };
  }
};

export type ResourceValidationState = {
  isActiveLoading: boolean;
  validationSelectOptions: Record<number, Choice[]>;
  selectedObjectType?: ObjectType;
};

const ResourceValidation: FC<{
  isReadOnly: boolean;
  isVariationView?: boolean;
  stepperCount?: number;
  currentParameter?: Record<string, any>;
}> = ({ isReadOnly, isVariationView = false, stepperCount, currentParameter }) => {
  const { watch, setValue, setError, clearErrors, register } = useFormContext();
  const data = watch('data', {});
  const validations = watch('validations', []);

  register('validations');

  const autoInitialized = watch('autoInitialized', false);
  const [state, setState] = useState<ResourceValidationState>({
    isActiveLoading: true,
    validationSelectOptions: {},
  });
  const { isActiveLoading, selectedObjectType, validationSelectOptions } = state;
  const propertiesMap = useRef<Record<string, any>>({});

  const fetchObjectType = async (id: string) => {
    setState((prev) => ({ ...prev, isActiveLoading: true }));
    const res: ResponseObj<ObjectType> = await request('GET', apiGetObjectTypes(id));
    if (res.data) {
      propertiesMap.current = keyBy(res.data.properties || [], 'id');
      updateValidationOptions(validations);
    }
    setState((prev) => ({ ...prev, isActiveLoading: false, selectedObjectType: res.data }));
  };

  const updateValidationOptions = (validation: any[]) => {
    const updatedOptions: Record<number, Choice[]> = {};
    validation.forEach((validation: any, index: number) => {
      if (validation && validation.propertyValidations) {
        validation.propertyValidations.forEach((propertyValidation: any) => {
          updatedOptions[index] =
            propertiesMap.current?.[propertyValidation.propertyId]?.options || [];
        });
      }
    });
    setState((prev) => ({ ...prev, validationSelectOptions: updatedOptions }));
  };

  useEffect(() => {
    if (data?.objectTypeId) {
      fetchObjectType(data.objectTypeId);
    }
  }, [data?.objectTypeId]);

  const updatePropertyValidations = (updatedValidations: any[], validationIndex: number) => {
    const _updated = validations.map((v: any, i: number) => {
      if (i === validationIndex) {
        return {
          ...v,
          propertyValidations: updatedValidations,
        };
      }
      return v;
    });

    updateValidations(_updated);
  };

  const updateValidations = (validations: any[]) => {
    let isValid = true;

    validations.every((validation: any) => {
      if (!validation) return true;
      const _keyToValidate = ['exceptionApprovalType', 'propertyValidations'];

      _keyToValidate.every((key) => {
        const checkSingleProperty = !!validation?.[key]?.length;
        if (!checkSingleProperty) {
          isValid = false;
        }

        if (isValid) {
          validation.propertyValidations.every((propertyValidation: any) => {
            if (!propertyValidation) return true;
            const keyToValidate = [
              'propertyId',
              'propertyInputType',
              'propertyExternalId',
              'propertyDisplayName',
              'constraint',
              'selector',
              'errorMessage',
            ];

            if (
              [InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(
                propertyValidation.propertyInputType,
              )
            ) {
              keyToValidate.push('dateUnit');
            }

            if (propertyValidation?.selector === SelectorOptionsEnum.PARAMETER) {
              keyToValidate.push('referencedParameterId');
            } else {
              [InputTypes.SINGLE_SELECT, InputTypes.MULTI_SELECT].includes(
                propertyValidation.propertyInputType,
              )
                ? keyToValidate.push('options')
                : keyToValidate.push('value');
            }

            keyToValidate.every((key) => {
              const checkSingleProperty = !!propertyValidation?.[key]?.length;
              if (!checkSingleProperty) {
                isValid = false;
              }
              return isValid;
            });
            return isValid;
          });
        }
        return isValid;
      });
      return isValid;
    });

    setValue('validations', validations, {
      shouldDirty: true,
    });

    if (!isValid) {
      setError('data', {
        stepperCount: stepperCount,
        message: 'All Validation Options Should be Filled.',
      } as any);
    } else {
      clearErrors('data');
    }
  };

  const getExceptionType = (exceptionApprovalType: any) => {
    return exceptionTypeMap.find((item) => item.value === exceptionApprovalType) || null;
  };

  useEffect(() => {
    updateValidations(validations);
    return () => {
      clearErrors('data');
    };
  }, []);

  return (
    <ValidationWrapper>
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
                      isDisabled: isReadOnly || isVariationView || autoInitialized,
                      onChange: (option: { value: any }) => {
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
              {!isReadOnly && !isVariationView && (
                <Close
                  className="remove-icon"
                  onClick={() => {
                    validations?.splice(validationIndex, 1);
                    updateValidations(validations);
                    updateValidationOptions(validations);
                  }}
                />
              )}
            </div>
            {validation.exceptionApprovalType &&
              validation.propertyValidations.map((item: any, index: number) => {
                if (!item) return null;
                return (
                  <ResourceValidationRow
                    index={index}
                    item={item}
                    isActiveLoading={isActiveLoading}
                    isReadOnly={isReadOnly}
                    propertyValidations={validation.propertyValidations}
                    updatePropertyValidations={updatePropertyValidations}
                    setState={setState}
                    validationSelectOptions={validationSelectOptions}
                    isVariationView={isVariationView}
                    validationIndex={validationIndex}
                    exceptionApprovalType={validation.exceptionApprovalType}
                    parameter={currentParameter}
                    selectedObjectType={selectedObjectType}
                  />
                );
              })}
          </div>
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
                propertyValidations: [
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

export default ResourceValidation;
