import {
  addNewParameter,
  deleteParameterSuccess,
  toggleNewParameter,
  updateParameterApi,
} from '#PrototypeComposer/Activity/actions';
import { generateNewParameter } from '#PrototypeComposer/Activity/utils';
import ResourceFilter from '#PrototypeComposer/Parameters/FilterViews/Resource';
import CalculationParameter from '#PrototypeComposer/Parameters/SetupViews/Calculation';
import ChecklistParameter from '#PrototypeComposer/Parameters/SetupViews/Checklist';
import LeastCount from '#PrototypeComposer/Parameters/SetupViews/LeastCount';
import LinkParameter from '#PrototypeComposer/Parameters/SetupViews/LinkParameter';
import MaterialInstruction from '#PrototypeComposer/Parameters/SetupViews/MaterialInstruction';
import ResourceParameter from '#PrototypeComposer/Parameters/SetupViews/Resource';
import TextInstruction from '#PrototypeComposer/Parameters/SetupViews/TextInstruction';
import YesNoParameter from '#PrototypeComposer/Parameters/SetupViews/YesNo';
import DateValidation from '#PrototypeComposer/Parameters/ValidationViews/Date';
import NumberValidation from '#PrototypeComposer/Parameters/ValidationViews/Number';
import ResourceValidation from '#PrototypeComposer/Parameters/ValidationViews/Resource';
import { Checklist, ParameterVerificationTypeEnum } from '#PrototypeComposer/checklist.types';
import { ParameterTypeMap, TargetEntityTypeVisual } from '#PrototypeComposer/constants';
import InfoIcon from '#assets/svg/info.svg';
import {
  Button,
  Checkbox,
  FormGroup,
  LoadingContainer,
  StepperContainer,
  StyledTabs,
  ToggleSwitch,
  useDrawer,
} from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import Tooltip from '#components/shared/Tooltip';
import { useTypedSelector } from '#store';
import {
  MandatoryParameter,
  NonMandatoryParameter,
  Parameter,
  ParameterType,
  TargetEntityType,
} from '#types';
import { apiDeleteParameter, apiSingleParameter } from '#utils/apiUrls';
import { nonEmptyStringRegex } from '#utils/constants';
import { InputTypes } from '#utils/globalTypes';
import { filterByParameterType } from '#utils/parameterUtils';
import { getErrorMsg, request } from '#utils/request';
import { resetOntology } from '#views/Ontology/actions';
import { Divider } from '@material-ui/core';
import { DeleteOutlined } from '@material-ui/icons';
import { cloneDeep } from 'lodash';
import React, { FC, useCallback, useEffect, useState } from 'react';
import { FormProvider, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { resetTaskParameterError } from './actions';

export const AddParameterWrapper = styled.form`
  display: flex;
  flex-direction: column;
  flex: 1;

  .form-group {
    padding: 0;
    margin-bottom: 24px;

    :last-of-type {
      margin-bottom: 0;
    }
  }

  .parameters-tabs {
    flex: 0;
  }

  .parameters-tabs-panel {
    padding: 24px 0 0;
  }

  .disabled-tooltip-info-icon {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 10px;
  }
`;

const ParameterVerificationWrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 24px;
  color: #161616;

  .parameter-verification {
    display: flex;
    flex-direction: column;
    gap: 20px;
    width: 100%;
  }

  h4 {
    font-size: 14px;
    font-weight: 700;
    line-height: 1.14;
    letter-spacing: 0.16px;
    margin: 0px;
  }

  span {
    color: #c2c2c2;
  }

  .parameter-verification-disabled {
    display: flex;
    align-items: center;
    gap: 12px;
  }
`;

const isFiltersAllowed = (type: ParameterType) => {
  switch (type) {
    case MandatoryParameter.MULTI_RESOURCE:
    case MandatoryParameter.RESOURCE:
      return true;
    default:
      return false;
  }
};

const isValidationsAllowed = (type: ParameterType) => {
  switch (type) {
    case MandatoryParameter.RESOURCE:
    case MandatoryParameter.NUMBER:
    case MandatoryParameter.MULTI_RESOURCE:
    case MandatoryParameter.DATE_TIME:
    case MandatoryParameter.DATE:
      return true;
    default:
      return false;
  }
};

const defaultValues = {
  mandatory: true,
  label: '',
  description: '',
  type: undefined,
  validations: [],
  autoInitialize: undefined,
  autoInitialized: undefined,
  verificationType: ParameterVerificationTypeEnum.NONE,
  leastCountEnabled: false,
  metadata: null,
};

const AddParameter: FC<{ isReadOnly: boolean; id?: string }> = ({ isReadOnly }) => {
  const dispatch = useDispatch();
  const {
    data,
    parameters: { parameterOrderInTaskInStage, listById, addParameter },
    stages: { activeStageId },
  } = useTypedSelector((state) => state.prototypeComposer);
  const [isLoadingParameter, setIsLoadingParamter] = useState(false);
  const [activeStep, setActiveStep] = useState(0);
  const [currentParameter, setCurrentParameter] = useState<Parameter>();
  const [parameterOptions, setParameterOptions] = useState<
    { label: string | JSX.Element; value: string }[]
  >([]);

  const [parameterSubmitting, setParameterSubmitting] = useState(false);
  const [loadOptions, setLoadOptions] = useState(false);
  const form = useForm<{
    mandatory: boolean;
    label: string;
    description: string;
    type: ParameterType;
    data: any;
    validations: any;
    autoInitialize: Record<string, any>;
    autoInitialized?: boolean;
    verificationType: string;
    leastCountEnabled: boolean;
    metadata: any;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      ...defaultValues,
      ...(addParameter?.type && {
        type: addParameter.type,
        ...(addParameter.type in NonMandatoryParameter && {
          mandatory: false,
        }),
      }),
    },
  });

  const {
    register,
    handleSubmit,
    formState: { isDirty, isValid, errors },
    setValue,
    watch,
    getValues,
    reset,
  } = form;

  register('mandatory');
  register('data', {
    required: true,
  });
  register('type', {
    required: true,
  });
  register('verificationType');
  register('autoInitialized');
  register('metadata');

  const {
    mandatory,
    label,
    type,
    verificationType,
    autoInitialized,
    data: options,
  } = watch(['mandatory', 'label', 'type', 'verificationType', 'autoInitialized', 'data']);

  const isLabelReadOnly = isReadOnly;
  const showFiltersSection = isFiltersAllowed(type);
  const showValidationsSection = isValidationsAllowed(type);
  const disableParameterVerification =
    currentParameter?.targetEntityType === TargetEntityType.PROCESS ||
    autoInitialized ||
    type === MandatoryParameter.CALCULATION;
  const disableLoadOptions = options?.length > 0;

  const [formDescription, setFormDescription] = useState<string>('');

  const basicInfoSection = () => {
    return (
      <FormGroup
        key="basic-info-section"
        inputs={[
          {
            type: InputTypes.MULTI_LINE,
            props: {
              placeholder: 'Write here',
              label: 'Description',
              id: 'description',
              name: 'description',
              rows: 3,
              optional: true,
              disabled: isReadOnly,
              value: formDescription ? formDescription : '',
              onChange: (value: Record<string, string>) => {
                setFormDescription(value?.value);
              },
            },
          },
        ]}
      />
    );
  };

  const parameterTypeFilter = useCallback((option, input) => {
    return filterByParameterType(option, input);
  }, []);

  const setupSection = () => {
    return (
      <div
        style={{
          height: '100%',
        }}
        key="setup-section"
      >
        {(!type || (type in MandatoryParameter && type !== MandatoryParameter.CHECKLIST)) && (
          <FormGroup
            style={{ marginBottom: 24 }}
            inputs={[
              {
                type: InputTypes.SINGLE_SELECT,
                props: {
                  id: 'type',
                  label: 'Parameter Type',
                  options: parameterOptions,
                  filterOption: parameterTypeFilter,
                  placeholder: 'Select Parameter Type',
                  value: type ? [{ label: ParameterTypeMap[type], value: type }] : undefined,
                  isDisabled: isReadOnly || !!addParameter?.parameterId,
                  onChange: async (option: { value: ParameterType }) => {
                    reset({
                      ...defaultValues,
                      data: undefined,
                      label: label,
                      mandatory: mandatory,
                      autoInitialize: undefined,
                      autoInitialized: false,
                      type: option.value,
                      validations: [],
                    });
                  },
                },
              },
            ]}
          />
        )}
        {[MandatoryParameter.SHOULD_BE, MandatoryParameter.NUMBER].includes(type) && (
          <LeastCount isReadOnly={isReadOnly} />
        )}
        {!(type in NonMandatoryParameter) && (
          <ToggleSwitch
            height={24}
            width={48}
            offLabel="Optional"
            onColor="#24a148"
            onChange={(isChecked) => {
              setValue('mandatory', isChecked, {
                shouldDirty: true,
                shouldValidate: true,
              });
            }}
            onLabel="Required"
            checked={mandatory}
            disabled={isReadOnly}
          />
        )}
        {[MandatoryParameter.SINGLE_SELECT, MandatoryParameter.MULTISELECT].includes(type) && (
          <div style={{ display: 'flex', margin: '16px 0px', gap: '8px' }}>
            <ToggleSwitch
              height={24}
              width={48}
              offLabel="Load Options"
              onColor="#24a148"
              onChange={(isChecked) => {
                setLoadOptions(isChecked);
                setValue('data', []);
                if (!isChecked) {
                  setValue('metadata', null);
                }
              }}
              onLabel="Load Options"
              checked={loadOptions}
              disabled={disableLoadOptions}
            />
            {disableLoadOptions && !isReadOnly && (
              <Tooltip
                title={
                  loadOptions
                    ? 'Add options is disabled since load options is enabled, delete and re-configure the parameter to manually add the options'
                    : 'Load Options cannot be enabled. To Enable Loading options from object, click on ‘Remove all Options’.'
                }
                textAlignment="left"
                arrow
              >
                <img src={InfoIcon} alt="parameter-info" />
              </Tooltip>
            )}
          </div>
        )}
        {!(type in NonMandatoryParameter) && (
          <ParameterVerificationWrapper>
            <Divider />
            <h4>
              Parameter Verification <span>(Optional)</span>
            </h4>
            <div className="parameter-verification">
              <div className="parameter-verification-disabled">
                <Checkbox
                  onClick={(checked) => {
                    setValue(
                      'verificationType',
                      verificationType === ParameterVerificationTypeEnum.PEER
                        ? ParameterVerificationTypeEnum.BOTH
                        : verificationType === ParameterVerificationTypeEnum.BOTH
                        ? ParameterVerificationTypeEnum.PEER
                        : checked
                        ? ParameterVerificationTypeEnum.SELF
                        : ParameterVerificationTypeEnum.NONE,
                      {
                        shouldDirty: true,
                        shouldValidate: true,
                      },
                    );
                  }}
                  checked={
                    verificationType === ParameterVerificationTypeEnum.SELF ||
                    verificationType === ParameterVerificationTypeEnum.BOTH
                  }
                  label={'Self Verification'}
                  disabled={isReadOnly || disableParameterVerification}
                />
                {!isReadOnly && disableParameterVerification && (
                  <Tooltip
                    title={
                      currentParameter?.targetEntityType === TargetEntityType.PROCESS
                        ? 'Verification are not applicable for Parameters in the Create Job Form'
                        : type === MandatoryParameter.CALCULATION
                        ? 'Self-verification is disabled for Calculation parameter.'
                        : autoInitialized
                        ? 'Self-verification is disabled for Linked parameters. Remove Linking to enable Self-verification.'
                        : ''
                    }
                    arrow
                    textAlignment="left"
                  >
                    <img src={InfoIcon} alt="parameter-info" style={{ marginRight: 8 }} />
                  </Tooltip>
                )}
              </div>
              <div className="parameter-verification-disabled">
                <Checkbox
                  onClick={(checked) => {
                    setValue(
                      'verificationType',
                      verificationType === ParameterVerificationTypeEnum.SELF
                        ? ParameterVerificationTypeEnum.BOTH
                        : verificationType === ParameterVerificationTypeEnum.BOTH
                        ? ParameterVerificationTypeEnum.SELF
                        : checked
                        ? ParameterVerificationTypeEnum.PEER
                        : ParameterVerificationTypeEnum.NONE,
                      {
                        shouldDirty: true,
                        shouldValidate: true,
                      },
                    );
                  }}
                  checked={
                    verificationType === ParameterVerificationTypeEnum.PEER ||
                    verificationType === ParameterVerificationTypeEnum.BOTH
                  }
                  label="Peer Verification"
                  disabled={isReadOnly || disableParameterVerification}
                />
                {!isReadOnly && disableParameterVerification && (
                  <Tooltip
                    title={
                      currentParameter?.targetEntityType === TargetEntityType.PROCESS
                        ? 'Verification are not applicable for Parameters in the Create Job Form'
                        : type === MandatoryParameter.CALCULATION
                        ? 'Peer-verification is disabled for Calculation parameter.'
                        : autoInitialized
                        ? 'Peer-verification is disabled for Linked parameters. Remove Linking to enable Peer-verification.'
                        : ''
                    }
                    arrow
                    textAlignment="left"
                  >
                    <img src={InfoIcon} alt="parameter-info" style={{ marginRight: 8 }} />
                  </Tooltip>
                )}
              </div>
            </div>
          </ParameterVerificationWrapper>
        )}
        {renderSetupViewsByType()}
        {[
          MandatoryParameter.NUMBER,
          MandatoryParameter.SINGLE_LINE,
          MandatoryParameter.DATE,
          MandatoryParameter.DATE_TIME,
          MandatoryParameter.MULTI_LINE,
          MandatoryParameter.RESOURCE,
        ].includes(type) && <LinkParameter type={type} isReadOnly={isReadOnly} />}
        {type === MandatoryParameter.SINGLE_SELECT && loadOptions && (
          <LinkParameter type={type} isReadOnly={isReadOnly} loadOptions={loadOptions} />
        )}
      </div>
    );
  };

  const renderSetupViewsByType = () => {
    switch (type) {
      case MandatoryParameter.SINGLE_SELECT:
      case MandatoryParameter.MULTISELECT:
      case MandatoryParameter.CHECKLIST:
        return (
          <ChecklistParameter
            isReadOnly={isReadOnly}
            stepperCount={activeStep}
            loadOptions={loadOptions}
          />
        );
      case MandatoryParameter.YES_NO:
        return <YesNoParameter isReadOnly={isReadOnly} />;
      case MandatoryParameter.CALCULATION:
        return <CalculationParameter isReadOnly={isReadOnly} />;
      case MandatoryParameter.RESOURCE:
      case MandatoryParameter.MULTI_RESOURCE:
        return <ResourceParameter isReadOnly={isReadOnly} />;
      case NonMandatoryParameter.INSTRUCTION:
        return <TextInstruction isReadOnly={isReadOnly} />;
      case NonMandatoryParameter.MATERIAL:
        return <MaterialInstruction isReadOnly={isReadOnly} />;
      default:
        return null;
    }
  };

  const filtersSection = () => {
    return <div key="filters-section">{renderFiltersByType()}</div>;
  };

  const renderFiltersByType = () => {
    switch (type) {
      case MandatoryParameter.RESOURCE:
      case MandatoryParameter.MULTI_RESOURCE:
        return (
          <ResourceFilter
            isReadOnly={isReadOnly}
            parameter={currentParameter}
            stepperCount={activeStep}
          />
        );
      default:
        return null;
    }
  };

  const validationsSection = () => {
    return <div key="validations-section">{renderValidationsByType()}</div>;
  };

  const renderValidationsByType = () => {
    switch (type) {
      case MandatoryParameter.RESOURCE:
      case MandatoryParameter.MULTI_RESOURCE:
        return (
          <ResourceValidation
            isReadOnly={isReadOnly}
            stepperCount={activeStep}
            currentParameter={currentParameter}
          />
        );
      case MandatoryParameter.NUMBER:
        return (
          <NumberValidation
            isReadOnly={isReadOnly}
            stepperCount={activeStep}
            currentParameter={currentParameter}
            checklistId={data.id}
          />
        );
      case MandatoryParameter.DATE:
      case MandatoryParameter.DATE_TIME:
        return (
          <DateValidation
            isReadOnly={isReadOnly}
            stepperCount={activeStep}
            checklistId={data.id}
            currentParameter={currentParameter}
          />
        );
      default:
        return null;
    }
  };

  const locationSection = () => {
    return (
      <div key="location-section">
        {currentParameter?.targetEntityType
          ? TargetEntityTypeVisual[currentParameter.targetEntityType]
          : ''}
      </div>
    );
  };

  const sections =
    type in NonMandatoryParameter
      ? [
          {
            label: 'Setup',
            value: '0',
            panelContent: <div />,
            renderFn: setupSection,
          },
        ]
      : [
          {
            label: 'Basic Information',
            value: '0',
            panelContent: <div />,
            renderFn: basicInfoSection,
          },
          {
            label: 'Setup',
            value: '1',
            panelContent: <div />,
            renderFn: setupSection,
          },
          ...(showFiltersSection
            ? [
                {
                  label: 'Filters',
                  description: 'Optional',
                  value: '2',
                  panelContent: <div />,
                  renderFn: filtersSection,
                },
              ]
            : []),
          ...(showValidationsSection
            ? [
                {
                  label: 'Validations',
                  description: 'Optional',
                  value: showFiltersSection ? '3' : '2',
                  panelContent: <div />,
                  renderFn: validationsSection,
                },
              ]
            : []),
          ...(addParameter?.parameterId
            ? [
                {
                  label: 'Location',
                  value:
                    showFiltersSection && showValidationsSection
                      ? '4'
                      : showFiltersSection || showValidationsSection
                      ? '3'
                      : '2',
                  panelContent: <div />,
                  renderFn: locationSection,
                },
              ]
            : []),
        ];

  useEffect(() => {
    setParameterOptions(
      Object.entries(ParameterTypeMap).reduce<{ label: string | JSX.Element; value: string }[]>(
        (acc, [value, label]) => {
          if (!(value in NonMandatoryParameter) && value !== MandatoryParameter.CHECKLIST) {
            acc.push({
              label,
              value,
            });
          }
          return acc;
        },
        [],
      ),
    );
  }, []);

  useEffect(() => {
    setLoadOptions(false);
    if (setDrawerOpen && addParameter) {
      setDrawerOpen(!!addParameter);
      setActiveStep(0);
    }

    if (addParameter?.parameterId) {
      fetchParameter();
    } else if (addParameter?.type) {
      reset({
        ...defaultValues,
        type: addParameter.type,
        ...(addParameter.type in NonMandatoryParameter && { mandatory: false }),
      });
    } else {
      reset(defaultValues);
    }
    return () => {
      dispatch(resetOntology(['objectTypes']));
      if (addParameter?.parameterId) {
        setCurrentParameter(undefined);
      }
      setFormDescription('');
    };
  }, [addParameter]);

  useEffect(() => {
    const _setValue = (value: any) => {
      setValue('data', value, {
        shouldDirty: true,
        shouldValidate: true,
      });
    };
    const dataExists = !!getValues()?.data;
    if (!dataExists) {
      if (
        [
          MandatoryParameter.DATE,
          MandatoryParameter.DATE_TIME,
          MandatoryParameter.MEDIA,
          MandatoryParameter.MULTI_LINE,
          MandatoryParameter.NUMBER,
          MandatoryParameter.SIGNATURE,
          MandatoryParameter.SINGLE_LINE,
          MandatoryParameter.FILE_UPLOAD,
        ].includes(type)
      ) {
        _setValue({});
      }
    }
  }, [type]);

  const handleNext = () => {
    setActiveStep((prevActiveStep) => prevActiveStep + 1);
  };

  const handleBack = () => {
    setActiveStep((prevActiveStep) => prevActiveStep - 1);
  };

  const onTabChange = (value: string) => {
    setActiveStep(parseInt(value));
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      dispatch(toggleNewParameter());
    }, 200);
    setFormDescription('');
  };

  const onSubmit = () => {
    if (addParameter) {
      setParameterSubmitting(true);
      const _data = getValues();
      if (addParameter.parameterId && currentParameter) {
        dispatch(
          updateParameterApi({
            ...currentParameter,
            ..._data,
            setParameterSubmitting,
          }),
        );
      } else {
        let orderTree = 1;
        if ('data' in _data && !_data.data) delete _data.data;
        const generatedParameter = generateNewParameter({ orderTree, ..._data });
        if (addParameter.action === 'task' && addParameter.taskId && activeStageId) {
          const parametersInTask =
            parameterOrderInTaskInStage?.[activeStageId]?.[addParameter.taskId];
          const maxOrderTree =
            listById?.[parametersInTask?.[parametersInTask?.length - 1]]?.orderTree ?? 0;
          orderTree = maxOrderTree + 1;
          dispatch(resetTaskParameterError(addParameter.taskId));
          dispatch(
            addNewParameter({
              ...generatedParameter,
              orderTree,
              checklistId: (data as Checklist).id,
              taskId: addParameter.taskId,
              stageId: activeStageId,
              ..._data,
              ...(formDescription?.length > 0 && { description: formDescription }),
              setParameterSubmitting,
            }),
          );
        } else {
          dispatch(
            addNewParameter({
              ...generatedParameter,
              checklistId: (data as Checklist).id,
              orderTree,
              ..._data,
              ...(formDescription?.length > 0 && { description: formDescription }),
              setParameterSubmitting,
            }),
          );
        }
      }
    }
  };

  const fetchParameter = async () => {
    setIsLoadingParamter(true);
    const response = await request('GET', apiSingleParameter(addParameter!.parameterId!));
    if (response.data) {
      reset({
        mandatory: response.data.mandatory,
        label: response.data.label,
        description: response.data.description,
        type: response.data.type,
        data: response.data.data,
        validations: response.data.validations,
        autoInitialize: response.data?.autoInitialize,
        autoInitialized: response.data?.autoInitialized,
        verificationType: response.data?.verificationType,
        leastCountEnabled: !!response?.data?.data?.leastCount,
        metadata: response?.data?.metadata,
      });
      setFormDescription(response.data.description);
      setCurrentParameter(cloneDeep(response.data));
      setLoadOptions(!!response?.data?.metadata?.objectTypeId);
      setIsLoadingParamter(false);
    }
  };

  const onDelete = async () => {
    try {
      const { data, errors } = await request(
        'PATCH',
        apiDeleteParameter(addParameter!.parameterId!),
      );
      if (data) {
        dispatch(
          deleteParameterSuccess({
            taskId: data.taskId,
            stageId: data.stageId,
            parameterId: addParameter!.parameterId!,
            targetEntityType: currentParameter?.targetEntityType,
          }),
        );
      } else if (errors) {
        throw getErrorMsg(errors);
      }
      addParameter?.fetchData && addParameter.fetchData();
      handleCloseDrawer();
    } catch (error) {
      console.error('Error Deleting Parameter', error);
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: typeof error !== 'string' ? 'Oops! Please Try Again.' : error,
        }),
      );
    }
  };

  const handleDeleteParameter = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.CONFIRMATION_MODAL,
        props: {
          onPrimary: onDelete,
          primaryText: 'Yes',
          secondaryText: 'No',
          title: 'Delete Parameter',
          body: 'Are you sure you want to Delete this Parameter?',
        },
      }),
    );
  };

  const preventTabSwitching = (newValue: number) => {
    if (errors?.data?.stepperCount < newValue) {
      return false;
    }
    return true;
  };

  const handleDisable = () => {
    if (activeStep === 0) {
      return !label.trim();
    } else if (errors?.data?.stepperCount && activeStep >= errors.data.stepperCount) {
      return errors?.data?.stepperCount <= activeStep;
    } else {
      return !isValid;
    }
  };

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: addParameter?.title || '',
    bodyContent: (
      <LoadingContainer
        loading={isLoadingParameter}
        style={{ flex: 1 }}
        component={
          <FormProvider {...form}>
            <AddParameterWrapper onSubmit={handleSubmit(onSubmit)}>
              <FormGroup
                style={{ marginBlock: 24 }}
                inputs={[
                  {
                    type: InputTypes.SINGLE_LINE,
                    props: {
                      placeholder: 'Write here',
                      label: 'Label',
                      id: 'label',
                      name: 'label',
                      disabled: isLabelReadOnly,
                      ref: register({
                        required: true,
                        pattern: nonEmptyStringRegex,
                      }),
                    },
                  },
                ]}
              />
              {!(type in NonMandatoryParameter) && (
                <>
                  {addParameter?.parameterId ? (
                    <StyledTabs
                      containerProps={{
                        className: 'parameters-tabs',
                      }}
                      tabListProps={{
                        className: 'parameters-tabs-list',
                      }}
                      panelsProps={{
                        className: 'parameters-tabs-panel',
                      }}
                      activeTab={activeStep.toString()}
                      onChange={onTabChange}
                      tabs={sections}
                      preventTabSwitching={preventTabSwitching}
                    />
                  ) : (
                    <StepperContainer activeStep={activeStep} sections={sections} />
                  )}
                </>
              )}
              {sections.map((section) => {
                return activeStep === parseInt(section.value) ? section.renderFn() : null;
              })}
            </AddParameterWrapper>
          </FormProvider>
        }
      />
    ),
    footerContent: (
      <>
        {!isLabelReadOnly && !addParameter?.parameterId && activeStep !== 0 && (
          <Button variant="secondary" onClick={handleBack}>
            Back
          </Button>
        )}
        {!isReadOnly && addParameter?.parameterId && (
          <Button variant="textOnly" color="red" onClick={handleDeleteParameter}>
            <DeleteOutlined /> Delete Parameter
          </Button>
        )}
        <Button variant="secondary" style={{ marginLeft: 'auto' }} onClick={handleCloseDrawer}>
          {isLabelReadOnly ? 'Close' : 'Cancel'}
        </Button>
        {!isLabelReadOnly && (
          <>
            {activeStep === sections.length - 1 ? (
              <Button
                type="submit"
                disabled={!isDirty || !isValid || parameterSubmitting}
                loading={parameterSubmitting}
                onClick={onSubmit}
              >
                Save
              </Button>
            ) : (
              <Button onClick={handleNext} disabled={handleDisable()}>
                Next
              </Button>
            )}
          </>
        )}
      </>
    ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return !addParameter ? null : StyledDrawer;
};

export default AddParameter;
