import ResourceFilter from '#PrototypeComposer/Parameters/FilterViews/Resource';
import NumberValidation from '#PrototypeComposer/Parameters/ValidationViews/Number';
import ResourceValidation from '#PrototypeComposer/Parameters/ValidationViews/Resource';
import { Media } from '#PrototypeComposer/checklist.types';
import { ParameterTypeMap } from '#PrototypeComposer/constants';
import closeIcon from '#assets/svg/close.svg';
import { Button, FormGroup, ImageUploadButton, useDrawer } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { MandatoryParameter, Parameter, ParameterType, ParameterVariationType } from '#types';
import { apiCreateEditParameterVariation, apiGetParameterVariations } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FileUploadData, InputTypes, ResponseError } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import AttachFileIcon from '@material-ui/icons/AttachFile';
import { debounce } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { FormProvider, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

type Props = {
  showBasic?: number;
};
const DeviateParameterDrawerWrapper = styled.form.attrs({})<Pick<Props, 'showBasic'>>`
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
  .form-group {
    padding: 0;
    margin-bottom: 16px;
    :last-of-type {
      margin-bottom: 0;
    }
  }

  .form-basic-info-section {
    display: ${({ showBasic }) => (showBasic === 0 ? 'unset' : 'none')};
    padding-block: 16px;
    .form-group {
      margin-bottom: unset;
    }
  }
  .form-setup-section {
    display: ${({ showBasic }) => (showBasic !== 0 ? 'unset' : 'none')};
    padding-block: 16px;
  }

  .media {
    display: flex;
    gap: 16px;
    align-items: center;
  }

  .media-name {
    font-size: 14px;
    color: #525252;
  }

  .media-name-remove-icon {
    cursor: pointer;
  }

  .attach-button {
    .icon {
      cursor: not-allowed;
    }

    span {
      color: #1d84ff;
      font-size: 14px;
    }

    .upload-image {
      > div {
        display: flex;
        align-items: center;
      }

      .icon {
        cursor: pointer;
        color: #1d84ff;
        font-size: 16px;
      }
    }
  }

  .form-group {
    > div:last-child {
      margin-bottom: 16px;
    }
  }
`;

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
};

const variationOptions = (inputType: string | null, parameter: Parameter) => {
  switch (inputType) {
    case MandatoryParameter.SHOULD_BE:
      return [
        {
          label: 'Should Be',
          value: ParameterVariationType.SHOULD_BE,
        },
      ];
    case MandatoryParameter.NUMBER:
      return [
        {
          label: 'Validation',
          value: ParameterVariationType.VALIDATION,
        },
      ];
    case MandatoryParameter.RESOURCE:
      const variationType = parameter.data?.propertyFilters
        ? parameter.data.propertyValidations.length > 0
          ? [
              {
                label: 'Validation',
                value: ParameterVariationType.VALIDATION,
              },
              {
                label: 'Filter',
                value: ParameterVariationType.FILTER,
              },
            ]
          : [
              {
                label: 'Filter',
                value: ParameterVariationType.FILTER,
              },
            ]
        : [
            {
              label: 'Validation',
              value: ParameterVariationType.VALIDATION,
            },
          ];
      return variationType;

    default:
      return [];
  }
};

export const getLabelByVariationType = (variationType: string) => {
  switch (variationType) {
    case ParameterVariationType.FILTER:
      return 'Filter';
    case ParameterVariationType.VALIDATION:
      return 'Validation';
    case ParameterVariationType.SHOULD_BE:
      return 'Should Be';
    default:
      return '-';
  }
};

const ParameterVariationDrawer: FC<{
  onCloseDrawer: React.Dispatch<React.SetStateAction<boolean | string>>;
  label: string | boolean;
  setShouldToggle: React.Dispatch<React.SetStateAction<boolean>>;
}> = ({ onCloseDrawer, label, setShouldToggle }) => {
  const dispatch = useDispatch();
  const [activeStep, setActiveStep] = useState(0);

  const {
    processId,
    id: jobId,
    tasks,
    stages,
    taskExecutions,
  } = useTypedSelector((state) => state.job);
  const [filters, setFilters] = useState<Record<string, any>>(urlParams);
  const [selectedParameter, setSelectedParameter] = useState<Parameter | null>(null);

  const {
    list: parameters,
    reset: resetParametersList,
    fetchNext: fetchNextParameters,
  } = createFetchList<Parameter>(apiGetParameterVariations(jobId!), urlParams, false);

  const form = useForm<{
    name: string;
    description: string;
    parameterType: ParameterType | null;
    data: any;
    type: ParameterVariationType;
    variationNumber: string;
    media?: Media;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: { parameterType: null },
  });

  const {
    register,
    handleSubmit,
    formState: { isDirty, isValid },
    setValue,
    watch,
    getValues,
    reset,
  } = form;

  register('type', { required: true });
  register('parameterType', {
    required: true,
  });
  register('data');
  register('validations');
  register('media');

  const { parameterType, type, media } = watch(['parameterType', 'type', 'media']);
  const onUploadError = (error: ResponseError[]) => {
    dispatch(
      showNotification({
        type: NotificationType.ERROR,
        msg: getErrorMsg(error) as string,
      }),
    );
  };
  const onUploaded = (fileData: FileUploadData) => {
    setValue(
      'media',
      {
        ...fileData,
        name: fileData.originalFilename,
        description: '',
      },
      { shouldDirty: true, shouldValidate: true },
    );
    dispatch(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'File Uploaded Successfully',
      }),
    );
  };

  const basicInformation = () => {
    return (
      <div
        style={{
          height: '100%',
        }}
        key="basic-info-section"
        className="form-basic-info-section"
      >
        <FormGroup
          inputs={[
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'parameterType',
                label: 'Select Parameter',
                placeholder: 'Select Parameter',
                options: parameters.map((parameter) => {
                  const taskExecution = taskExecutions.get(
                    parameter?.response?.[0]?.taskExecutionId,
                  );
                  const task = tasks?.get(taskExecution?.taskId);
                  const stage = stages?.get(task?.stageId);
                  return {
                    ...parameter,
                    externalId: `(Task ${stage?.orderTree}.${task?.orderTree})`,
                    value: parameter.id,
                  };
                }),
                onMenuScrollToBottom: () => fetchNextParameters(),
                onInputChange: debounce((searchedValue: string, actionMeta) => {
                  if (searchedValue !== actionMeta.prevInputValue)
                    setFilters({ ...filters, parameterName: searchedValue });
                }, 500),
                onChange: (option: Parameter) => {
                  reset({
                    parameterType: option.type,
                    validations:
                      option.type === MandatoryParameter.NUMBER ? option.validations : undefined,
                    data: option.type !== MandatoryParameter.NUMBER ? option.data : undefined,
                  });

                  setSelectedParameter(option);
                },
              },
            },
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'type',
                label: 'Parameter Type',
                placeholder: 'Select Parameter Type',
                isDisabled: true,
                value: parameterType
                  ? [{ label: ParameterTypeMap[parameterType], value: parameterType }]
                  : null,
              },
            },
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'variationOn',
                label: 'Variation Type',
                options: variationOptions(parameterType, selectedParameter),
                placeholder: 'Select Variation Type',
                isDisabled: label === 'Edit',
                value: type ? [{ label: getLabelByVariationType(type), value: type }] : null,
                onChange: (option: { value: string }) => {
                  setValue('type', option.value, {
                    shouldDirty: true,
                    shouldValidate: true,
                  });
                },
              },
            },
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Write here',
                label: 'Variation Name',
                id: 'name',
                name: 'name',
                ref: register({ required: true }),
              },
            },
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Write here',
                label: 'Variation Number',
                id: 'variationNumber',
                name: 'variationNumber',
                ref: register({ required: true }),
              },
            },
            {
              type: InputTypes.MULTI_LINE,
              props: {
                id: 'description',
                label: 'Description',
                name: 'description',
                optional: true,
                placeholder: 'Write Here',
                rows: 3,
                ref: register,
              },
            },
          ]}
        />
        {!!media ? (
          <div className="media">
            <div className="media-name">{media.name}</div>
            <img
              src={closeIcon}
              className="media-name-remove-icon"
              onClick={() => {
                setValue('media', undefined, { shouldDirty: true, shouldValidate: true });
              }}
            />
          </div>
        ) : (
          <div className="attach-button">
            <ImageUploadButton
              label="Attach File"
              onUploadSuccess={(fileData) => {
                onUploaded(fileData);
              }}
              icon={AttachFileIcon}
              onUploadError={onUploadError}
              disabled={!!media}
              acceptedTypes={[
                'image/*',
                '.pdf',
                '.doc',
                '.docx',
                '.png',
                '.jpg',
                '.jpeg',
                '.xlsx',
                '.xls',
                '.ppt',
                '.pptx',
              ]}
            />
          </div>
        )}
      </div>
    );
  };

  const setupSection = () => {
    return (
      <div
        style={{
          height: '100%',
        }}
        className="form-setup-section"
        key="setup-section"
      >
        {renderSetupViewsByType()}
      </div>
    );
  };

  const renderSetupViewsByType = () => {
    switch (parameterType) {
      case MandatoryParameter.RESOURCE:
        return type === ParameterVariationType.FILTER ? (
          <ResourceFilter
            isReadOnly={false}
            parameter={selectedParameter}
            checklistId={processId}
            isVariationView={true}
          />
        ) : (
          <ResourceValidation
            isReadOnly={false}
            isVariationView={true}
            currentParameter={selectedParameter}
          />
        );
      case MandatoryParameter.NUMBER:
        return (
          <NumberValidation
            isReadOnly={false}
            checklistId={processId}
            isVariationView={true}
            currentParameter={selectedParameter}
          />
        );
      default:
        return null;
    }
  };

  const sections = [
    {
      label: 'Basic Information',
      value: '0',
      panelContent: <div />,
      renderFn: basicInformation,
    },
    {
      label: 'Setup',
      value: '1',
      panelContent: <div />,
      renderFn: setupSection,
    },
  ];

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  const handleNext = () => {
    setActiveStep((prevActiveStep) => prevActiveStep + 1);
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
    }, 200);
  };

  const createEditParameterVariation = async (variationData: any) => {
    try {
      const { data, errors } = await await request('POST', apiCreateEditParameterVariation(), {
        data: variationData,
      });
      if (data) {
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: 'Variation Created Successfully',
          }),
        );
      } else {
        throw getErrorMsg(errors);
      }
    } catch (error) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: typeof error !== 'string' ? 'Oops! Please Try Again.' : error,
        }),
      );
    }
  };

  useEffect(() => {
    resetParametersList({ params: { ...filters } });
  }, [filters]);

  const onSubmit = () => {
    const _data = getValues();

    const newData = {
      name: _data.name,
      parameterId: selectedParameter?.id,
      type: _data.type,
      variationNumber: _data.variationNumber,
      description: _data.description,
      media: _data.media,
      details:
        _data.parameterType === MandatoryParameter.SHOULD_BE
          ? _data.data
          : _data.parameterType === MandatoryParameter.NUMBER
          ? _data.validations?.resourceParameterValidations
          : type === ParameterVariationType.FILTER
          ? _data.data.propertyFilters
          : _data.data.propertyValidations,
      jobId: jobId,
    };

    createEditParameterVariation(newData);
    setTimeout(() => setShouldToggle((prev) => !prev), 300);

    handleCloseDrawer();
  };

  const handleBack = () => {
    setActiveStep((prevActiveStep) => prevActiveStep - 1);
  };

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: label === 'Edit' ? 'Edit Parameter Variation' : 'Parameter Variation',
    hideCloseIcon: true,
    bodyContent: (
      <FormProvider {...form}>
        <DeviateParameterDrawerWrapper onSubmit={handleSubmit(onSubmit)} showBasic={activeStep}>
          {sections.map((section) => {
            return section.renderFn();
          })}
        </DeviateParameterDrawerWrapper>
      </FormProvider>
    ),
    footerContent: (
      <>
        <Button variant="secondary" style={{ marginRight: 'auto' }} onClick={handleCloseDrawer}>
          Cancel
        </Button>
        {activeStep !== 0 && (
          <Button variant="secondary" onClick={handleBack}>
            Back
          </Button>
        )}
        {activeStep === sections.length - 1 ? (
          <Button type="submit" onClick={onSubmit} disabled={!isDirty || !isValid}>
            {label === 'Edit' ? 'Update' : 'Create'}
          </Button>
        ) : (
          <Button onClick={handleNext} disabled={!isDirty || !isValid}>
            Next
          </Button>
        )}
      </>
    ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return <DeviateParameterDrawerWrapper>{StyledDrawer}</DeviateParameterDrawerWrapper>;
};

export default ParameterVariationDrawer;
