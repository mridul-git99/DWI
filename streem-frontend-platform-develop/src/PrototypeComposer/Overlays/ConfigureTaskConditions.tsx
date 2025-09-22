import { getDateUnits } from '#PrototypeComposer/Parameters/ValidationViews/Resource';
import { updateTask } from '#PrototypeComposer/Tasks/actions';
import { resetChecklistValidationErrors } from '#PrototypeComposer/actions';
import { AutomationActionTriggerType } from '#PrototypeComposer/checklist.types';
import backIcon from '#assets/svg/back-icon.svg';
import { BaseModal, Button, FormGroup, LoadingContainer } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { CommonOverlayProps, OverlayNames } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { filterPageTypeEnum, MandatoryParameter, Parameter } from '#types';
import { labelByConstraint } from '#utils';
import {
  apiGetObjectTypes,
  apiGetParameters,
  apiTaskInterLocks,
  apiTaskInterLocksArchive,
} from '#utils/apiUrls';
import { FilterOperators, InputTypes } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import { ObjectType } from '#views/Ontology/types';
import { Close } from '@material-ui/icons';
import DeleteOutlineOutlinedIcon from '@material-ui/icons/DeleteOutlineOutlined';
import { keyBy } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { v4 as uuidv4 } from 'uuid';

const Wrapper = styled.div`
  .modal {
    padding: 0;

    &-body {
      padding: 0 !important;

      .body {
        height: 70dvh;
        min-width: 40dvw;
        .header {
          display: flex;
          align-items: center;
          padding: 16px;
          border-bottom: 1px solid #eeeeee;
        }

        .header-title {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 16px;
        }
        .heading {
          font-size: 16px !important;
        }

        .content {
          display: flex;
          height: 85%;
          overflow: auto;

          .empty-actions {
            display: flex;
            flex-direction: column;
            margin: auto;
            gap: 8px;
            > div {
              font-size: 14px;
            }
          }
          .actions-card-container {
            display: flex;
            padding: 16px;
            flex-direction: column;
            align-items: flex-start;
            gap: 16px;
            flex: 1 0 0;
            align-self: stretch;
          }
          .actions-card {
            display: flex;
            justify-content: space-between;
            padding: 16px;
            width: 100%;
            align-items: center;
            border: 1px solid #e0e0e0;
          }

          .action-card-label {
            display: flex;
            flex-direction: column;
            gap: 8px;

            &-top {
              font-size: 14px;
              font-weight: 700;
            }
            &-bottom {
              font-size: 14px;
            }
          }

          .actions-card:hover {
            background-color: #f4f4f4;
          }

          .add-action-button {
            padding: 8px 16px;
          }

          .action-card-form {
            padding: 24px;
            width: 100%;
            .fields {
              .form-group {
                padding: 12px 0px;
              }
              .inline-fields {
                .form-group {
                  flex: 1;
                  flex-direction: row;
                  padding-block: 0px;
                  gap: 12px;

                  > div {
                    flex: 1;
                    .input-wrapper {
                      flex: unset;
                    }
                  }
                }
              }
            }

            .info-message {
              color: #525252;
              font-size: 12px;
              margin-bottom: 16px;
            }
          }

          .action-buttons-container {
            padding-bottom: 10px;
          }
        }
      }
    }
  }
`;

type Props = {
  checklistId: string;
  isReadOnly?: boolean;
  state: Record<string, any>;
  setState: React.Dispatch<React.SetStateAction<any>>;
  activeTaskId: string;
};

type ConditionFormCardProps = Omit<Props, 'checklistId'> & {
  parameterInfoMap: Record<string, Parameter>;
};

type StateProps = {
  selectedCondition: Record<string, string>;
  editConditionFlag: boolean;
  addNewCondition: boolean;
  taskConditionsList: Record<string, string>[];
  shouldToggle: boolean;
};

const ConfigureTaskConditions: FC<CommonOverlayProps<Pick<Props, 'checklistId' | 'isReadOnly'>>> =
  ({ closeAllOverlays, closeOverlay, props: { checklistId, isReadOnly = false } }) => {
    const [state, setState] = useState<StateProps>({
      selectedCondition: {},
      editConditionFlag: false,
      addNewCondition: false,
      taskConditionsList: [],
      shouldToggle: false,
    });

    const {
      tasks: { activeTaskId, listById },
    } = useTypedSelector((state) => state.prototypeComposer);

    const dispatch = useDispatch();

    const { addNewCondition, editConditionFlag, taskConditionsList, shouldToggle } = state;

    const archiveCondition = (taskId: string, conditionId: string, setFormErrors: any) => {
      (async () => {
        try {
          const { data, errors } = await request(
            'DELETE',
            apiTaskInterLocksArchive(taskId, conditionId),
          );
          if (data) {
            setState((prev) => ({
              ...prev,
              shouldToggle: !prev.shouldToggle,
            }));
            dispatch(
              showNotification({
                type: NotificationType.SUCCESS,
                msg: 'Condition Deleted Successfully!',
              }),
            );
            dispatch(resetChecklistValidationErrors(taskId, 'E2303'));
          }
          setFormErrors(errors);
        } catch (error) {
          console.error(error);
        }
      })();
    };

    const getAllConditionsByTask = async () => {
      try {
        const { data, errors } = await request('GET', apiTaskInterLocks(activeTaskId!));
        if (data) {
          setState((prev) => ({
            ...prev,
            taskConditionsList: data?.validations?.resourceParameterValidations || [],
          }));
          dispatch(
            updateTask({
              ...listById[activeTaskId!],
              interlocks: data,
            }),
          );
        } else {
          throw getErrorMsg(errors);
        }
      } catch (error) {
        console.error(error);
      }
    };

    useEffect(() => {
      getAllConditionsByTask();
    }, [shouldToggle]);

    return (
      <Wrapper>
        <BaseModal
          closeAllModals={closeAllOverlays}
          closeModal={closeOverlay}
          showFooter={false}
          showHeader={false}
          showCloseIcon={false}
        >
          <div className="body">
            {!addNewCondition ? (
              <div className="header">
                <Close className="close-icon" onClick={closeOverlay} />
                <div>
                  <h2 className="heading">Parameter Conditions</h2>
                </div>
              </div>
            ) : (
              <div className="header">
                <Close className="close-icon" onClick={closeOverlay} />
                <div className="header-title">
                  <img
                    src={backIcon}
                    alt="Back Icon"
                    onClick={() => {
                      setState((prev) => ({
                        ...prev,
                        addNewCondition: false,
                        selectedCondition: {},
                        editConditionFlag: false,
                      }));
                    }}
                  />
                  <h2 className="heading">
                    {editConditionFlag ? 'Edit Condition' : 'Configure New Condition'}
                  </h2>
                </div>
              </div>
            )}
            <div className="content">
              {addNewCondition ? (
                <ConditionFormCardContainer
                  isReadOnly={isReadOnly}
                  state={state}
                  setState={setState}
                  checklistId={checklistId}
                  activeTaskId={activeTaskId!}
                />
              ) : taskConditionsList.length > 0 ? (
                <div className="actions-card-container">
                  {!isReadOnly && (
                    <Button
                      variant="primary"
                      onClick={() => {
                        setState((prev) => ({ ...prev, addNewCondition: true }));
                      }}
                      className="add-action-button"
                    >
                      Add New Condition
                    </Button>
                  )}
                  {taskConditionsList.map((currAction, index) => {
                    return (
                      <div
                        className="actions-card"
                        key={index}
                        onClick={() => {
                          setState((prev) => ({
                            ...prev,
                            selectedCondition: currAction,
                            editConditionFlag: true,
                            addNewCondition: true,
                          }));
                        }}
                      >
                        <div className="action-card-label">
                          <div className="action-card-label-top">
                            {currAction.triggerType === AutomationActionTriggerType.TASK_STARTED
                              ? 'When Task is Started'
                              : 'When Task is Completed'}
                          </div>
                          <div className="action-card-label-bottom">{currAction.label}</div>
                        </div>
                        {!isReadOnly && (
                          <DeleteOutlineOutlinedIcon
                            onClick={(e) => {
                              e.stopPropagation();
                              dispatch(
                                openOverlayAction({
                                  type: OverlayNames.REASON_MODAL,
                                  props: {
                                    modalTitle: 'Delete Condition',
                                    modalDesc: `Are you sure you want to Delete this condition ?`,
                                    shouldAskForReason: false,
                                    onSubmitHandler: (
                                      _: string,
                                      setFormErrors: (errors?: Error[]) => void,
                                    ) => {
                                      archiveCondition(activeTaskId!, currAction.id, setFormErrors);
                                    },
                                  },
                                }),
                              );
                            }}
                          />
                        )}
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="empty-actions">
                  <div>No conditions configured yet</div>
                  {!isReadOnly && (
                    <Button
                      variant="primary"
                      className="add-action-button"
                      onClick={() => {
                        setState((prev) => ({ ...prev, addNewCondition: true }));
                      }}
                    >
                      Add New Condition
                    </Button>
                  )}
                </div>
              )}
            </div>
          </div>
        </BaseModal>
      </Wrapper>
    );
  };

const ConditionFormCardContainer: FC<Props> = ({
  isReadOnly,
  state,
  setState,
  checklistId,
  activeTaskId,
}) => {
  const { selectedCondition, editConditionFlag } = state;
  const [parameterInfoDetails, setParameterInfoDetails] = useState({
    loading: editConditionFlag,
    listMap: {},
  });
  const { loading, listMap } = parameterInfoDetails;

  const fetchAllMappedUnmappedParameterById = async () => {
    setParameterInfoDetails((prev) => ({ ...prev, loading: true }));
    const { parameterId } = selectedCondition;

    if (editConditionFlag && parameterId) {
      const { data } = await request('GET', apiGetParameters(checklistId!), {
        params: {
          sort: 'createdAt,desc',
          filters: {
            op: FilterOperators.AND,
            fields: [
              { field: 'archived', op: FilterOperators.EQ, values: [false] },
              {
                field: 'id',
                op: FilterOperators.ANY,
                values: [...(parameterId ? [parameterId] : [])],
              },
            ],
          },
        },
      });
      if (data) {
        const dataMap = keyBy(data || [], 'id');
        setParameterInfoDetails(() => ({ listMap: dataMap, loading: false }));
      }
    } else {
      setParameterInfoDetails((prev) => ({ ...prev, loading: false }));
    }
  };

  useEffect(() => {
    if (editConditionFlag && !!selectedCondition) {
      fetchAllMappedUnmappedParameterById();
    }
  }, []);

  return (
    <LoadingContainer
      style={{ flex: '1' }}
      loading={loading}
      component={
        <ConditionFormCard
          isReadOnly={isReadOnly}
          state={state}
          setState={setState}
          activeTaskId={activeTaskId!}
          parameterInfoMap={listMap}
        />
      }
    />
  );
};

const ConditionFormCard: FC<ConditionFormCardProps> = ({
  isReadOnly,
  state,
  setState,
  activeTaskId,
  parameterInfoMap,
}) => {
  const {
    prototypeComposer: {
      data: checklistData,
      parameters: { listById: taskParametersById },
    },
  } = useTypedSelector((state) => state);
  const dispatch = useDispatch();
  const [isLoadingParameters, setIsLoadingParameters] = useState<boolean>(false);
  const [resourceParameters, setResourceParameters] = useState<Parameter[]>([]);
  const [selectedObjectType, setSelectedObjectType] = useState<ObjectType | undefined>(undefined);
  const [isLoadingObjectType, setIsLoadingObjectType] = useState<boolean>(false);
  const [isParameterInvalid, setIsParameterInvalidError] = useState(false);
  const { selectedCondition, editConditionFlag, taskConditionsList } = state;

  const {
    handleSubmit,
    formState: { isDirty, isValid },
    watch,
    reset,
    control,
  } = useForm<Record<string, Record<string, string>>>({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      conditionDetails: {},
    },
  });

  const { conditionDetails } = watch(['conditionDetails']);

  const fetchParameters = async () => {
    if (!resourceParameters.length) {
      setIsLoadingParameters(true);
      const allParameters = {
        ...keyBy(checklistData?.parameters || {}, 'id'),
        ...taskParametersById,
      };
      const filterParametersByType = (
        objectHashMap: Record<string, Parameter>,
        parameterType: string[],
      ) => {
        const resultArray = [];

        for (const key in objectHashMap) {
          if (objectHashMap?.hasOwnProperty(key)) {
            const object = objectHashMap[key];
            if (parameterType.includes(object.type)) {
              resultArray.push(object);
            }
          }
        }

        return resultArray;
      };
      checkUnMapValidation(allParameters);
      setResourceParameters(
        filterParametersByType(allParameters, [
          MandatoryParameter.RESOURCE,
          MandatoryParameter.MULTI_RESOURCE,
        ]),
      );

      if (editConditionFlag) {
        const dependentParameter =
          parameterInfoMap?.[selectedCondition?.parameterId] ||
          allParameters[selectedCondition?.parameterId];
        if (dependentParameter) {
          fetchObjectType(dependentParameter.data.objectTypeId);
        }
      }

      setIsLoadingParameters(false);
    }
  };

  const fetchObjectType = async (id: string) => {
    setSelectedObjectType(undefined);
    setIsLoadingObjectType(true);
    const res = await request('GET', apiGetObjectTypes(id));
    setSelectedObjectType(res?.data);
    setIsLoadingObjectType(false);
  };

  const onSubmit = (data: any) => {
    let conditions = [];

    if (selectedCondition?.id) {
      conditions = taskConditionsList.map((condition: Record<string, string>) => {
        if (condition.id === selectedCondition.id) {
          return {
            ...condition,
            ...data.conditionDetails,
          };
        }
        return condition;
      });
    } else {
      conditions = [
        ...taskConditionsList,
        {
          ...data.conditionDetails,
          id: uuidv4(),
        },
      ];
    }
    createCondition(conditions);
  };

  const createCondition = async (conditions: Record<string, string>[]) => {
    try {
      const { data, errors } = await request(
        editConditionFlag ? 'PATCH' : 'POST',
        apiTaskInterLocks(activeTaskId),
        {
          data: {
            validations: {
              resourceParameterValidations: conditions,
            },
          },
        },
      );
      if (data) {
        setState((prev: StateProps) => ({
          ...prev,
          addNewCondition: false,
          selectedCondition: {},
          editConditionFlag: false,
          shouldToggle: !prev.shouldToggle,
        }));
        let successMessage = '';
        if (editConditionFlag) {
          dispatch(resetChecklistValidationErrors(activeTaskId, 'E2303'));
          successMessage = 'Condition updated successfully!';
        } else {
          successMessage = 'Condition added successfully!';
        }
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: successMessage,
          }),
        );
      } else {
        throw getErrorMsg(errors);
      }
    } catch (error) {
      console.error(error);
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: error as string,
        }),
      );
    }
  };

  const customConditionDetailValidation = (value: Record<string, string>) => {
    if (conditionDetails) {
      let keysToValidate = [
        'label',
        'triggerType',
        'constraint',
        'parameterId',
        'errorMessage',
        'propertyId',
      ];

      if (conditionDetails?.propertyInputType === InputTypes.SINGLE_SELECT) {
        keysToValidate = [...keysToValidate, 'choices'];
      } else {
        keysToValidate = [...keysToValidate, 'value'];
      }

      if (
        [InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(
          conditionDetails.propertyInputType,
        )
      ) {
        keysToValidate = [...keysToValidate, 'dateUnit'];
      }

      return keysToValidate.every((key) => {
        if (key === 'choices') {
          return conditionDetails?.propertyInputType === InputTypes.SINGLE_SELECT
            ? value?.[key]?.length
            : true;
        }
        return !!value?.[key];
      });
    }
    return false;
  };

  const checkUnMapValidation = (eligibleParameters: Record<string, Parameter>) => {
    if (editConditionFlag) {
      setIsParameterInvalidError(!eligibleParameters[selectedCondition.parameterId]);
    }
  };

  const fetchRefParamsValue = (id: string, options: Parameter[]) => {
    if (id && parameterInfoMap && parameterInfoMap?.[id]) {
      return parameterInfoMap?.[id];
    } else if (id && options?.length > 0) {
      let optionsObject = keyBy(options, 'id');
      return optionsObject?.[id] || null;
    }
    return null;
  };

  useEffect(() => {
    reset({
      conditionDetails: selectedCondition,
    });
  }, [editConditionFlag, selectedCondition]);

  useEffect(() => {
    fetchParameters();
  }, []);

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="action-card-form">
      <div className="fields">
        <Controller
          control={control}
          name={`conditionDetails`}
          key={`conditionDetails`}
          defaultValue={conditionDetails || null}
          shouldUnregister={false}
          rules={{
            required: true,
            validate: customConditionDetailValidation,
          }}
          render={({ onChange, value }) => {
            return (
              <FormGroup
                inputs={[
                  {
                    type: InputTypes.SINGLE_LINE,
                    props: {
                      id: 'label',
                      label: 'Condition Label',
                      placeholder: 'Enter Label',
                      disabled: isReadOnly,
                      value: value?.label || null,
                      onChange: (_option: Record<string, string>) => {
                        onChange({
                          ...conditionDetails,
                          label: _option.value,
                        });
                      },
                    },
                  },
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'triggerType',
                      label: 'Trigger',
                      options: Object.keys(AutomationActionTriggerType).map((triggerType) =>
                        triggerType === AutomationActionTriggerType.TASK_STARTED
                          ? {
                              label: 'When task is started',
                              value: triggerType,
                            }
                          : {
                              label: 'When task is completed',
                              value: triggerType,
                            },
                      ),
                      placeholder: 'Select Trigger Type',
                      isDisabled: isReadOnly,
                      value: value?.triggerType
                        ? [
                            {
                              label:
                                value.triggerType === AutomationActionTriggerType.TASK_STARTED
                                  ? 'When task is started'
                                  : 'When task is completed',
                              value: value.triggerType,
                            },
                          ]
                        : null,
                      onChange: (_option: { value: string }) => {
                        onChange({ ...conditionDetails, triggerType: _option.value });
                      },
                    },
                  },
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'objectType',
                      label: 'Object Type',
                      isLoading: isLoadingParameters,
                      options: resourceParameters.map((resource: Parameter) => ({
                        ...resource.data,
                        label: resource.label,
                        value: resource.id,
                        externalId: resource?.data?.objectTypeDisplayName,
                      })),
                      placeholder: 'Select Resource Parameter',
                      isDisabled: isReadOnly,
                      value: fetchRefParamsValue(value?.parameterId, resourceParameters),
                      error: isParameterInvalid ? 'Unmapped Parameter' : undefined,
                      onChange: (_option: any) => {
                        onChange({
                          label: conditionDetails.label,
                          triggerType: conditionDetails.triggerType,
                          parameterId: _option.value,
                        });
                        fetchObjectType(_option.objectTypeId);
                        setIsParameterInvalidError(false);
                      },
                    },
                  },
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'objectProperty',
                      label: 'Object Property',
                      isLoading: isLoadingObjectType,
                      isDisabled: isReadOnly,
                      hideSelectedOptions: true,
                      options: selectedObjectType?.properties?.reduce<Array<Record<string, any>>>(
                        (acc, objectTypeProperty) => {
                          if (
                            ![
                              InputTypes.SINGLE_LINE,
                              InputTypes.MULTI_LINE,
                              InputTypes.MULTI_SELECT,
                            ].includes(objectTypeProperty.inputType)
                          ) {
                            acc.push({
                              inputType: objectTypeProperty.inputType,
                              externalId: objectTypeProperty.externalId,
                              label: objectTypeProperty.displayName,
                              value: objectTypeProperty.id,
                              _options: objectTypeProperty.options,
                            });
                          }
                          return acc;
                        },
                        [],
                      ),
                      value: conditionDetails?.propertyId
                        ? [
                            {
                              label: conditionDetails.propertyDisplayName,
                              value: conditionDetails.propertyId,
                            },
                          ]
                        : null,
                      placeholder: 'Select Object Property',
                      onChange: (_option: any) => {
                        onChange({
                          label: conditionDetails.label,
                          triggerType: conditionDetails.triggerType,
                          parameterId: conditionDetails.parameterId,
                          propertyId: _option.value,
                          propertyInputType: _option.inputType,
                          propertyExternalId: _option.externalId,
                          propertyDisplayName: _option.label,
                        });
                      },
                    },
                  },
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'condition',
                      label: 'Condition',
                      isDisabled: isReadOnly,
                      options: Object.entries(
                        labelByConstraint(
                          conditionDetails?.propertyInputType,
                          filterPageTypeEnum.CONFIGURE_TASK_CONDITIONS,
                        ),
                      ).map(([value, label]) => ({
                        label,
                        value,
                      })),
                      value: value?.constraint
                        ? [
                            {
                              label: labelByConstraint(
                                conditionDetails?.propertyInputType,
                                filterPageTypeEnum.CONFIGURE_TASK_CONDITIONS,
                              )[value?.constraint],
                              value: value?.constraint,
                            },
                          ]
                        : null,
                      placeholder: 'Select Condition',
                      onChange: (_option: Record<string, string>) => {
                        onChange({
                          ...conditionDetails,
                          constraint: _option.value,
                        });
                      },
                    },
                  },

                  {
                    type: !conditionDetails?.propertyInputType
                      ? InputTypes.SINGLE_LINE
                      : [InputTypes.DATE, InputTypes.DATE_TIME].includes(
                          conditionDetails?.propertyInputType,
                        )
                      ? InputTypes.NUMBER
                      : conditionDetails?.propertyInputType,
                    props: {
                      id: 'value',
                      label: 'Value',
                      disabled: isReadOnly,
                      isDisabled: isReadOnly,
                      ...(conditionDetails.propertyInputType === InputTypes.SINGLE_SELECT && {
                        options: selectedObjectType?.properties
                          .find(
                            (objectTypeProperty) =>
                              objectTypeProperty.id === conditionDetails.propertyId,
                          )
                          ?.options?.map((option) => ({
                            label: option.displayName,
                            value: option.id,
                          })),
                      }),
                      placeholder: 'Enter Value',
                      value:
                        conditionDetails.propertyInputType === InputTypes.SINGLE_SELECT
                          ? value?.choices?.map((option: Record<string, string>) => ({
                              value: option.id,
                              label: option.displayName,
                            }))
                          : value?.value
                          ? value.value
                          : '',

                      onChange: (_option: Record<string, string>) => {
                        if (conditionDetails.propertyInputType === InputTypes.SINGLE_SELECT) {
                          let choices = [{ id: _option.value, displayName: _option.label }];
                          onChange({
                            ...conditionDetails,
                            choices,
                            value: null,
                          });
                        } else {
                          onChange({
                            ...conditionDetails,
                            value: _option.value,
                            choices: null,
                          });
                        }
                      },
                    },
                  },
                  ...([InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(
                    conditionDetails.propertyInputType,
                  )
                    ? [
                        {
                          type: InputTypes.SINGLE_SELECT,
                          props: {
                            id: 'objectPropertyUnit',
                            label: 'Unit',
                            options: Object.entries(
                              getDateUnits(conditionDetails.propertyInputType),
                            ).map(([value, label]) => ({
                              label,
                              value,
                            })),
                            isDisabled: isReadOnly,
                            placeholder: 'Select Unit',
                            defaultValue: conditionDetails?.dateUnit
                              ? [
                                  {
                                    label: getDateUnits(conditionDetails.propertyInputType)[
                                      conditionDetails.dateUnit as keyof typeof getDateUnits
                                    ],
                                    value: conditionDetails.dateUnit,
                                  },
                                ]
                              : undefined,
                            onChange: (_option: any) => {
                              onChange({
                                ...conditionDetails,
                                dateUnit: _option.value,
                              });
                            },
                          },
                        },
                      ]
                    : []),
                  {
                    type: InputTypes.SINGLE_LINE,
                    props: {
                      id: 'errorMessage',
                      label: 'Error Message',
                      disabled: isReadOnly,
                      placeholder: 'Enter Message',
                      value: conditionDetails?.errorMessage || '',
                      onChange: (_option: Record<string, string>) => {
                        onChange({
                          ...conditionDetails,
                          errorMessage: _option.value,
                        });
                      },
                    },
                  },
                ]}
              />
            );
          }}
        />
      </div>
      <div className="info-message">
        This Message will be shown to the user if the condition is not met
      </div>
      {!isReadOnly && (
        <div className="action-buttons-container">
          <Button
            variant="primary"
            type="submit"
            disabled={!isDirty || !isValid || isParameterInvalid}
          >
            {editConditionFlag ? 'Update' : 'Save'}
          </Button>
        </div>
      )}
    </form>
  );
};

export default ConfigureTaskConditions;
