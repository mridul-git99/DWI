import {
  addTaskAction,
  archiveTaskAction,
  updateTaskAction,
} from '#PrototypeComposer/Tasks/actions';
import { Task } from '#PrototypeComposer/Tasks/types';
import { updatedActions } from '#PrototypeComposer/Tasks/utils';
import {
  AutomationActionActionType,
  AutomationActionConfigurationDataType,
  AutomationActionDetails,
  AutomationActionTriggerType,
  AutomationActionType,
  AutomationTargetEntityType,
} from '#PrototypeComposer/checklist.types';
import { parameterLabelMap } from '#PrototypeComposer/constants';
import { isArraySubset } from '#PrototypeComposer/utils';
import RightArrowIcon from '#assets/svg/Arrows.svg';
import backIcon from '#assets/svg/back-icon.svg';
import InfoIcon from '#assets/svg/info.svg';
import { BaseModal, Button, FormGroup, LoadingContainer, ToggleSwitch } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { CommonOverlayProps, OverlayNames } from '#components/OverlayContainer/types';
import Tooltip from '#components/shared/Tooltip';
import { createFetchList } from '#hooks/useFetchData';
import { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { MandatoryParameter, Parameter } from '#types';
import { apiGetObjectTypes, apiGetParameters, baseUrl } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, SELECTOR_OPTIONS } from '#utils/constants';
import {
  CapturePropertySelectorOptionsEnum,
  ConfigureAutomationSelect,
  FilterOperators,
  InputTypes,
  OffsetSelectorOptionsEnum,
  SelectorOptionsEnum,
  configuredAutomationsOptions,
  fetchDataParams,
} from '#utils/globalTypes';
import { request } from '#utils/request';
import { FlagPositions, getBooleanFromDecimal } from '#views/Ontology/ObjectTypes';
import { fetchObjectTypes } from '#views/Ontology/actions';
import {
  Cardinality,
  ObjectType,
  ObjectTypeProperty,
  ObjectTypeRelation,
} from '#views/Ontology/types';
import { Close } from '@material-ui/icons';
import DeleteOutlineOutlinedIcon from '@material-ui/icons/DeleteOutlineOutlined';
import { debounce, isEqual, keyBy, startCase, toLower, isArray } from 'lodash';
import React, { FC, memo, useEffect, useRef, useState, useMemo } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    padding: 0;

    &-body {
      padding: 0 !important;

      .body {
        height: 70dvh;
        width: 50dvw;
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

          .relation-value {
            display: flex;
            align-items: center;
            gap: 6px;
          }

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
            }
          }

          .toggle-switch {
            margin: 12px 0px 18px 0px;
          }
          .action-create-object-fields {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            gap: 16px;
            .form-group {
              width: 240px;
              padding: 14px 0px;
            }

            .select-parameter {
              display: flex;
              flex-direction: column;
              margin-top: 12px;

              .toggle-switch {
                margin: 0px;

                .label {
                  font-size: 12px;
                  color: #525252;
                }
              }
            }
          }
          .action-info-icon {
            margin: 22px 8px 0px 0px;
          }

          .action-buttons-container {
            padding-bottom: 10px;
          }
        }
      }
    }
  }

  .field-error {
    font-size: 14px;
    margin-top: 4px !important;
  }
`;

type Props = {
  task: Task;
  checklistId?: string;
  isReadOnly?: boolean;
  state: Record<string, any>;
  setState: React.Dispatch<React.SetStateAction<any>>;
  activeTaskId: string;
};

type ActionFormCardProps = Props & {
  parameterInfoMap: Record<string, Parameter>;
  selectedRelation: any;
  selectedProperty: any;
  setSelectedProperty: any;
  setSelectedRelation: any;
};

const getDateUnits = (inputType: InputTypes) => {
  switch (inputType) {
    case InputTypes.DATE:
      return {
        DAYS: 'Days',
        MONTHS: 'Months',
        YEARS: 'Years',
      };
    default:
      return {
        HOURS: 'Hours',
        MINUTES: 'Minutes',
        SECONDS: 'Seconds',
      };
  }
};

const customizer = (objValue: any, srcValue: any) => {
  if (objValue?.name === srcValue?.displayName) {
    return true;
  }
  return false;
};

const ConfigureActions: FC<CommonOverlayProps<Pick<Props, 'checklistId' | 'isReadOnly'>>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { checklistId, isReadOnly = false },
}) => {
  const [state, setState] = useState<any>({
    selectedAction: {},
    editActionFlag: false,
    addNewAction: false,
  });

  const {
    tasks: { listById, activeTaskId },
  } = useTypedSelector((state) => state.prototypeComposer);

  const task = listById[activeTaskId];

  const dispatch = useDispatch();

  const { addNewAction, editActionFlag } = state;

  const archiveAction = (taskId: string, actionId: string, setFormErrors: any) => {
    dispatch(
      archiveTaskAction({
        taskId: taskId,
        actionId: actionId,
        setFormErrors,
      }),
    );
  };

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
          {!addNewAction ? (
            <div className="header">
              <Close className="close-icon" onClick={closeOverlay} />
              <div>
                <h2 className="heading">Task Automations</h2>
              </div>
            </div>
          ) : (
            <div className="header">
              <Close className="close-icon" onClick={closeOverlay} />
              <div className="header-title">
                <img
                  src={backIcon}
                  alt="Back Icon"
                  style={{ cursor: 'pointer' }}
                  onClick={() => {
                    setState((prev) => ({
                      ...prev,
                      addNewAction: false,
                      selectedAction: {},
                      editActionFlag: false,
                    }));
                  }}
                />
                <h2 className="heading">
                  {editActionFlag ? 'Edit Configured Action' : 'Configure New Action'}
                </h2>
              </div>
            </div>
          )}
          <div className="content">
            {addNewAction ? (
              <ActionFormCardContainer
                isReadOnly={isReadOnly}
                state={state}
                setState={setState}
                task={task}
                activeTaskId={activeTaskId!}
                checklistId={checklistId}
              />
            ) : task.automations.length > 0 ? (
              <div className="actions-card-container">
                {!isReadOnly && (
                  <Button
                    variant="primary"
                    onClick={() => {
                      setState((prev) => ({ ...prev, addNewAction: true }));
                    }}
                    className="add-action-button"
                  >
                    Add New Action
                  </Button>
                )}
                {task.automations.map((currAction, index) => {
                  return (
                    <div
                      className="actions-card"
                      key={index}
                      onClick={() => {
                        setState((prev) => ({
                          ...prev,
                          selectedAction: updatedActions(
                            currAction,
                            AutomationActionConfigurationDataType.ARRAY,
                          ),
                          editActionFlag: true,
                          addNewAction: true,
                        }));
                      }}
                    >
                      <div className="action-card-label">
                        <div className="action-card-label-top">
                          {currAction.triggerType === AutomationActionTriggerType.TASK_STARTED
                            ? 'When Task is Started'
                            : 'When Task is Completed'}
                        </div>
                        <div className="action-card-label-bottom">{currAction.displayName}</div>
                      </div>
                      {!isReadOnly && (
                        <DeleteOutlineOutlinedIcon
                          onClick={(e) => {
                            e.stopPropagation();
                            dispatch(
                              openOverlayAction({
                                type: OverlayNames.REASON_MODAL,
                                props: {
                                  modalTitle: 'Delete Action',
                                  modalDesc: `Are you sure you want to Delete this action ?`,
                                  shouldAskForReason: false,
                                  onSubmitHandler: (
                                    _: string,
                                    setFormErrors: (errors?: Error[]) => void,
                                  ) => {
                                    archiveAction(task.id, currAction.id, setFormErrors);
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
                <div>No Actions Configured Yet</div>
                {!isReadOnly && (
                  <Button
                    variant="primary"
                    className="add-action-button"
                    onClick={() => {
                      setState((prev) => ({ ...prev, addNewAction: true }));
                    }}
                  >
                    Add New Action
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

const ActionFormCardContainer: FC<Props> = ({
  isReadOnly,
  state,
  setState,
  task,
  activeTaskId,
  checklistId,
}) => {
  const { selectedAction, editActionFlag } = state;
  const [parameterInfoDetails, setParameterInfoDetails] = useState({
    loading: editActionFlag,
    listMap: {},
  });
  const [selectedProperty, setSelectedProperty] = useState<any>(null);
  const [selectedRelation, setSelectedRelation] = useState<any>(null);
  const { loading, listMap } = parameterInfoDetails;

  const fetchAllMappedUnmappedParameterById = async () => {
    setParameterInfoDetails((prev) => ({ ...prev, loading: true }));
    const { actionDetails } = selectedAction;

    if (editActionFlag && (actionDetails?.referencedParameterId || actionDetails?.parameterId)) {
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
                values: [
                  ...(actionDetails?.referencedParameterId
                    ? [
                        actionDetails.referencedParameterId,
                        ...(actionDetails?.parameterId ? [actionDetails.parameterId] : []),
                        ...(actionDetails?.offsetParameterId
                          ? [actionDetails.offsetParameterId]
                          : []),
                      ]
                    : []),
                ],
              },
            ],
          },
        },
      });
      if (data) {
        const dataMap = keyBy(data || [], 'id');
        const selectedResourceParameter = dataMap[actionDetails.referencedParameterId];
        if (actionDetails?.referencedParameterId) {
          fetchResourcePropertiesRelations(
            selectedResourceParameter.data.objectTypeId,
            actionDetails,
          );
        }
        setParameterInfoDetails(() => ({ listMap: dataMap, loading: false }));
      }
    } else {
      setParameterInfoDetails((prev) => ({ ...prev, loading: false }));
    }
  };

  const fetchResourcePropertiesRelations = async (id: string, actionDetails: any) => {
    const res = await request('GET', apiGetObjectTypes(id));
    if (res) {
      const _selectedProperty = (res?.data?.properties || []).find(
        (property: any) => actionDetails?.propertyId === property?.id,
      );
      const _selectedRelation = (res?.data?.relations || []).find(
        (relation: any) => actionDetails?.relationId === relation?.id,
      );
      if (_selectedProperty) {
        setSelectedProperty({
          ..._selectedProperty,
          _options: _selectedProperty.options,
        });
      } else if (_selectedRelation) {
        setSelectedRelation(_selectedRelation);
      }
    }
  };

  useEffect(() => {
    if (editActionFlag && !!selectedAction) {
      fetchAllMappedUnmappedParameterById();
    }
  }, []);

  return (
    <LoadingContainer
      style={{ flex: '1' }}
      loading={loading}
      component={
        <ActionFormCard
          isReadOnly={isReadOnly}
          state={state}
          setState={setState}
          task={task}
          activeTaskId={activeTaskId}
          parameterInfoMap={listMap}
          selectedProperty={selectedProperty}
          setSelectedProperty={setSelectedProperty}
          selectedRelation={selectedRelation}
          setSelectedRelation={setSelectedRelation}
        />
      }
    />
  );
};

const ActionFormCard: FC<ActionFormCardProps> = ({
  isReadOnly,
  state,
  setState,
  task,
  activeTaskId,
  parameterInfoMap,
  selectedProperty,
  selectedRelation,
  setSelectedProperty,
  setSelectedRelation,
}) => {
  const {
    ontology: {
      objectTypes: { list, listLoading, pageable: objectTypePagination },
    },
    prototypeComposer: {
      data: checklistData,
      parameters: { listById: taskParametersById },
    },
  } = useTypedSelector((state) => state);
  const dispatch = useDispatch();
  const [isOpenObjectTypes, setIsOpenObjectTypes] = useState(false);
  const [isLoadingParameters, setIsLoadingParameters] = useState(false);
  const [resourceParameters, setResourceParameters] = useState<any>([]);
  const [numberParameters, setNumberParameters] = useState<any>([]);
  const [dateParameters, setDateParameters] = useState<any>([]);
  const [allParameters, setAllParameters] = useState<Parameter[]>([]);
  const [allowParameterMapping, setAllowParameterMapping] = useState<boolean>(false);
  const [selectedObjectType, setSelectedObjectType] = useState<ObjectType | undefined>(undefined);
  const [isLoadingObjectType, setIsLoadingObjectType] = useState(false);
  const [isReferenceParameterInvalid, setIsReferenceParameterInvalidError] = useState(false);
  const [isParameterInvalid, setIsParameterInvalidError] = useState(false);
  const [isSelectParameterOptionsMismatched, setIsSelectParameterOptionsMismatched] =
    useState(false);
  const { selectedAction, editActionFlag } = state;
  const [objectUrlPath, setObjectUrlPath] = useState<string>('');
  const searchedValue = useRef<string>('');
  const { reset: resetObjects } = createFetchList(objectUrlPath, {}, false);

  useEffect(() => {
    resetObjects({ url: objectUrlPath });
  }, [objectUrlPath]);

  const handleForm = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      displayName: '',
      actionType: '',
      actionDetails: null,
      triggerType: '',
    },
  });

  const {
    handleSubmit,
    formState: { isDirty, isValid },
    register,
    watch,
    reset,
    control,
    getValues,
    errors,
    setError,
    clearErrors,
    setValue,
  } = handleForm;

  const { actionType, actionDetails, displayName, triggerType } = watch([
    'actionType',
    'actionDetails',
    'displayName',
    'triggerType',
  ]);

  const getSetAsOptions = (triggerType: string) => {
    return [
      triggerType === AutomationActionTriggerType.TASK_STARTED
        ? {
            label: 'Task Start time',
            value: {
              entityType: 'TASK',
              entityId: activeTaskId,
              captureProperty: CapturePropertySelectorOptionsEnum.START_TIME,
            },
          }
        : {
            label: 'Task End time',
            value: {
              entityType: 'TASK',
              entityId: activeTaskId,
              captureProperty: CapturePropertySelectorOptionsEnum.END_TIME,
            },
          },
      ...([InputTypes.DATE, InputTypes.DATE_TIME].includes(actionDetails?.propertyInputType)
        ? [
            {
              label: 'Constant',
              value: {
                entityType: 'TASK',
                entityId: activeTaskId,
                captureProperty: CapturePropertySelectorOptionsEnum.CONSTANT,
              },
            },
            {
              label: 'Parameter',
              value: {
                entityType: 'TASK',
                entityId: activeTaskId,
                captureProperty: CapturePropertySelectorOptionsEnum.PARAMETER,
              },
            },
          ]
        : []),
    ];
  };

  const customActionDetailValidation = (value: any) => {
    if (actionType) {
      if (actionType === AutomationActionActionType.BULK_CREATE_OBJECT) {
        // Require selector to be set
        if (!value?.selector) {
          return false;
        }

        // Check required fields based on selector
        if (value.selector === ConfigureAutomationSelect.CONSTANT && !value.bulkCount) {
          return false;
        } else if (
          value.selector === ConfigureAutomationSelect.PARAMETER &&
          !value.referencedParameterId
        ) {
          return false;
        }
      }

      if (actionType === AutomationActionActionType.ARCHIVE_OBJECT) {
        return true;
      }

      if (
        ![
          AutomationActionActionType.CREATE_OBJECT,
          AutomationActionActionType.BULK_CREATE_OBJECT,
        ].includes(actionType)
      ) {
        let keysToValidate: string[] = ['parameterId', 'value'];
        let commonKeys = [
          'propertyId',
          'propertyInputType',
          'propertyExternalId',
          'propertyDisplayName',
          'referencedParameterId',
          'selector',
        ];
        if (actionType === AutomationActionActionType.SET_PROPERTY) {
          if (value?.selector === SelectorOptionsEnum.CONSTANT) {
            if (value?.propertyInputType) {
              if ([InputTypes.DATE, InputTypes.DATE_TIME].includes(value.propertyInputType)) {
                if (value.captureProperty === CapturePropertySelectorOptionsEnum.CONSTANT) {
                  keysToValidate = [
                    'entityType',
                    'entityId',
                    'captureProperty',
                    'value',
                    'offsetSelector',
                  ];
                  if (value.offsetSelector === OffsetSelectorOptionsEnum.CONSTANT) {
                    keysToValidate.push('offsetValue');
                    keysToValidate.push('offsetDateUnit');
                  } else {
                    keysToValidate.push('offsetParameterId');
                    keysToValidate.push('offsetDateUnit');
                  }
                } else if (value.captureProperty === CapturePropertySelectorOptionsEnum.PARAMETER) {
                  keysToValidate = [
                    'entityType',
                    'entityId',
                    'captureProperty',
                    'parameterId',
                    'offsetSelector',
                  ];
                  if (value.offsetSelector === OffsetSelectorOptionsEnum.CONSTANT) {
                    keysToValidate.push('offsetValue');
                    keysToValidate.push('offsetDateUnit');
                  } else {
                    keysToValidate.push('offsetParameterId');
                    keysToValidate.push('offsetDateUnit');
                  }
                } else if (
                  value.captureProperty === CapturePropertySelectorOptionsEnum.END_TIME ||
                  value.captureProperty === CapturePropertySelectorOptionsEnum.START_TIME
                ) {
                  keysToValidate = ['entityType', 'entityId', 'captureProperty'];
                  if (value.offsetSelector === OffsetSelectorOptionsEnum.CONSTANT) {
                    keysToValidate.push('offsetValue');
                    keysToValidate.push('offsetDateUnit');
                  } else {
                    keysToValidate.push('offsetParameterId');
                    keysToValidate.push('offsetDateUnit');
                  }
                }
              } else if (
                value.propertyInputType === InputTypes.SINGLE_SELECT &&
                value.selector === SelectorOptionsEnum.CONSTANT
              ) {
                keysToValidate = ['choices'];
              }
            }
          } else if (value?.selector === SelectorOptionsEnum.PARAMETER) {
            keysToValidate = ['parameterId'];
          }
        } else if (actionType === AutomationActionActionType.SET_RELATION) {
          commonKeys = [
            'referencedParameterId',
            'relationId',
            'relationExternalId',
            'relationDisplayName',
            'relationObjectTypeId',
            'selector',
          ];
        }
        if (isEqual(keysToValidate, ['parameterId', 'value'])) {
          const presentKeysCount = keysToValidate.filter((key) => !!value?.[key]).length;
          return presentKeysCount === 1 && commonKeys.every((key) => !!value?.[key]);
        }
        return [...commonKeys, ...keysToValidate].every((key) => !!value?.[key]);
      }
      return true;
    }
    return false;
  };

  const checkUnMapValidation = (eligibleParameters: Record<string, Parameter>) => {
    if (editActionFlag) {
      setIsReferenceParameterInvalidError(
        !eligibleParameters[selectedAction.actionDetails?.referencedParameterId],
      );
      setIsParameterInvalidError(!eligibleParameters[selectedAction.actionDetails?.parameterId]);
      if (
        selectedProperty &&
        selectedAction &&
        selectedAction.actionType === AutomationActionActionType.SET_PROPERTY &&
        selectedAction.actionDetails?.propertyInputType === InputTypes.SINGLE_SELECT &&
        selectedAction.actionDetails?.selector === SelectorOptionsEnum.PARAMETER
      ) {
        const selectedParameter = eligibleParameters[selectedAction.actionDetails?.parameterId];
        const areValuesEqual = isArraySubset(
          selectedParameter.data,
          selectedProperty._options,
          customizer,
        );
        if (!areValuesEqual) {
          setIsSelectParameterOptionsMismatched(true);
        } else {
          setIsSelectParameterOptionsMismatched(false);
        }
      }
    }
  };

  const fetchParameters = async () => {
    if (!resourceParameters.length && !numberParameters.length) {
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

      setNumberParameters(
        filterParametersByType(allParameters, [
          MandatoryParameter.NUMBER,
          MandatoryParameter.CALCULATION,
          MandatoryParameter.SHOULD_BE,
        ]),
      );
      setDateParameters(
        filterParametersByType(allParameters, [
          MandatoryParameter.DATE,
          MandatoryParameter.DATE_TIME,
        ]),
      );
      setAllParameters(
        filterParametersByType(allParameters, [
          MandatoryParameter.NUMBER,
          MandatoryParameter.CALCULATION,
          MandatoryParameter.SHOULD_BE,
          MandatoryParameter.DATE,
          MandatoryParameter.DATE_TIME,
          MandatoryParameter.MULTI_RESOURCE,
          MandatoryParameter.RESOURCE,
          MandatoryParameter.SINGLE_LINE,
          MandatoryParameter.SINGLE_SELECT,
          MandatoryParameter.MULTISELECT,
          MandatoryParameter.MULTI_LINE,
        ]),
      );

      if (task.automations.length) {
        const _selectedResource =
          parameterInfoMap?.[selectedAction?.actionDetails?.referencedParameterId] ||
          allParameters[selectedAction?.actionDetails?.referencedParameterId];

        if (_selectedResource && _selectedResource.data.objectTypeId) {
          fetchObjectType(_selectedResource.data.objectTypeId);
        } else if (
          selectedAction.actionType === AutomationActionActionType.CREATE_OBJECT ||
          selectedAction.actionType === AutomationActionActionType.BULK_CREATE_OBJECT
        ) {
          fetchObjectType(selectedAction.actionDetails.objectTypeId);
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
    const { actionType: _actionType, actionDetails: _actionDetails } = getValues();
    if (
      ([
        AutomationActionActionType.SET_PROPERTY,
        AutomationActionActionType.INCREASE_PROPERTY,
        AutomationActionActionType.DECREASE_PROPERTY,
      ].includes(_actionType) &&
        _actionDetails?.propertyId) ||
      (_actionType === AutomationActionActionType.SET_RELATION && _actionDetails?.relationId)
    ) {
      const _selectedProperty = (res?.data?.properties || []).find(
        (property: any) => _actionDetails?.propertyId === property.id,
      );
      const _selectedRelation = (res?.data?.relations || []).find(
        (property: any) => _actionDetails?.relationId === property.id,
      );
      if (_selectedProperty) {
        setSelectedProperty({
          ..._selectedProperty,
          _options: _selectedProperty.options,
        });
      } else if (_selectedRelation) {
        setSelectedRelation(_selectedRelation);
      }
    }
    setIsLoadingObjectType(false);
  };

  const fetchObjectTypesData = (params: fetchDataParams = {}, appendData: boolean = false) => {
    const { page = DEFAULT_PAGE_NUMBER, query = searchedValue.current } = params;
    dispatch(
      fetchObjectTypes(
        {
          page,
          size: DEFAULT_PAGE_SIZE,
          usageStatus: 1,
          ...(query && { displayName: query }),
        },
        appendData,
      ),
    );
  };

  const handleDisabledSubmit = () => {
    if (
      [
        AutomationActionActionType.CREATE_OBJECT,
        AutomationActionActionType.BULK_CREATE_OBJECT,
      ].includes(actionType as AutomationActionActionType)
    ) {
      if (actionType === AutomationActionActionType.BULK_CREATE_OBJECT) {
        if (!actionDetails?.selector) {
          return true;
        }

        if (
          actionDetails.selector === ConfigureAutomationSelect.CONSTANT &&
          !actionDetails.bulkCount
        ) {
          return true;
        } else if (
          actionDetails.selector === ConfigureAutomationSelect.PARAMETER &&
          !actionDetails.referencedParameterId
        ) {
          return true;
        }
      }

      return editActionFlag
        ? selectedAction.actionDetails?.hasOwnProperty('configuration')
          ? false
          : !isDirty || !isValid
        : !isDirty || !isValid;
    } else if (actionDetails?.selector === SelectorOptionsEnum.PARAMETER) {
      return (
        !isDirty ||
        !isValid ||
        isParameterInvalid ||
        isReferenceParameterInvalid ||
        isSelectParameterOptionsMismatched
      );
    } else {
      return !isDirty || !isValid || isReferenceParameterInvalid;
    }
  };

  const onSubmit = (data: {
    actionType: AutomationActionActionType;
    actionDetails: AutomationActionDetails;
    displayName: string;
    triggerType: AutomationActionTriggerType;
  }) => {
    const commonData = {
      type: AutomationActionType.PROCESS_BASED,
      orderTree: task.automations?.length + 1,
      targetEntityType:
        data.actionType === AutomationActionActionType.CREATE_OBJECT
          ? AutomationTargetEntityType.OBJECT
          : AutomationTargetEntityType.RESOURCE_PARAMETER,
      triggerDetails: {},
    };
    if (editActionFlag && selectedAction?.id) {
      dispatch(
        updateTaskAction({
          taskId: task.id,
          action: { ...commonData, ...selectedAction, ...data },
          actionId: selectedAction.id,
        }),
      );
    } else {
      dispatch(
        addTaskAction({
          taskId: task.id,
          action: { ...commonData, ...data },
        }),
      );
    }
    setState({
      addNewAction: false,
      selectedAction: {},
      editActionFlag: false,
    });
  };

  useEffect(() => {
    reset({
      displayName: selectedAction.displayName,
      actionType: selectedAction.actionType,
      actionDetails: selectedAction.actionDetails,
      triggerType: selectedAction.triggerType,
    });
    setAllowParameterMapping(
      !!selectedAction.actionDetails?.configuration &&
        Object.keys(selectedAction.actionDetails?.configuration).length > 0,
    );
  }, [editActionFlag, selectedAction]);

  useEffect(() => {
    if (task.automations.length) {
      fetchParameters();
    }
  }, []);

  useEffect(() => {
    if (selectedProperty && editActionFlag) {
      checkUnMapValidation({ ...keyBy(allParameters, 'id') });
    }
  }, [selectedProperty, editActionFlag]);

  const calculateSelectorShowAndOptions = (actionType: string) => {
    switch (actionType) {
      case AutomationActionActionType.INCREASE_PROPERTY:
      case AutomationActionActionType.DECREASE_PROPERTY:
        return { shouldShow: !!selectedProperty, options: SELECTOR_OPTIONS };
      case AutomationActionActionType.SET_PROPERTY:
        return {
          shouldShow: [
            InputTypes.NUMBER,
            InputTypes.SINGLE_SELECT,
            InputTypes.DATE,
            InputTypes.DATE_TIME,
          ].includes(selectedProperty?.inputType),
          options: SELECTOR_OPTIONS,
        };
      default:
        return { shouldShow: false, options: [] };
    }
  };

  const renderSelectorComponent = (value: any, onChange: any) => {
    const { shouldShow = false, options = [] } = calculateSelectorShowAndOptions(actionType);
    return shouldShow
      ? [
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'selectOne',
              label: 'Select One',
              options: options,
              isDisabled: isReadOnly,
              value: value?.selector
                ? [
                    {
                      label: options.find((option: any) => option.value === value.selector)?.label,
                      value: value.selector,
                    },
                  ]
                : null,
              placeholder: 'Select Any One',
              onChange: (_option: any) => {
                onChange({
                  referencedParameterId: value?.referencedParameterId,
                  propertyId: value?.propertyId,
                  propertyInputType: value?.propertyInputType,
                  propertyExternalId: value?.propertyExternalId,
                  propertyDisplayName: value?.propertyDisplayName,
                  selector: _option.value,
                });
              },
            },
          },
        ]
      : [];
  };

  const fetchRefParamsValue = (id, options) => {
    if (id && parameterInfoMap && parameterInfoMap?.[id]) {
      return parameterInfoMap?.[id];
    } else if (id && options?.length > 0) {
      var optionsObject = keyBy(options, 'id');
      return optionsObject?.[id] || null;
    }
    return null;
  };

  const actionTypeChangeHandler = (_option) => {
    if (
      !resourceParameters.length ||
      _option.value === AutomationActionActionType.CREATE_OBJECT ||
      _option.value === AutomationActionActionType.BULK_CREATE_OBJECT
    ) {
      fetchParameters();
    }
    reset({
      displayName: displayName,
      triggerType: triggerType,
      actionType: _option.value,
      actionDetails: null,
    });
    setSelectedObjectType(undefined);
    setAllowParameterMapping(false);
  };

  const getSelectParameterOptions = (selectedProperty: any) => {
    const propertyType = selectedProperty?.inputType;
    switch (propertyType) {
      case MandatoryParameter.NUMBER:
        return numberParameters;
      case MandatoryParameter.SINGLE_SELECT:
        const selectParameters = allParameters.filter(
          (parameter) =>
            parameter.type === MandatoryParameter.SINGLE_SELECT &&
            parameter.metadata !== null &&
            (parameter.metadata.propertyId === selectedProperty.value ||
              parameter.metadata.propertyId === selectedProperty.id),
        );

        return selectParameters;
      default:
        return [];
    }
  };

  function generateDynamicInputs({
    selectedProperty,
    value,
    isReadOnly,
    triggerType,
    dateParameters,
    numberParameters,
    isLoadingParameters,
    onChange,
    setIsParameterInvalidError,
    fetchRefParamsValue,
  }: {
    selectedProperty: Record<string, any>;
    value: any;
    isReadOnly: any;
    triggerType: any;
    dateParameters: any[];
    numberParameters: any[];
    isLoadingParameters: boolean;
    onChange: any;
    setIsParameterInvalidError: any;
    fetchRefParamsValue: any;
  }) {
    const createDynamicInput = ({
      id,
      label,
      value,
      placeholder,
      onChange,
      type,
      options,
      error,
      isLoading,
    }: {
      id: string;
      label: string;
      value: any;
      placeholder: string;
      onChange: (option: any) => void;
      type: InputTypes;
      options?: any[];
      error?: string;
      isLoading?: boolean;
    }) => {
      const props: any = {
        id,
        label,
        value,
        isDisabled: isReadOnly,
        disabled: isReadOnly,
        placeholder,
        onChange,
        error,
      };

      if (type === InputTypes.SINGLE_SELECT) {
        props.options = options || [];
        props.isLoading = isLoading;
      }

      return {
        type,
        props,
      };
    };

    let inputs = [];

    if (selectedProperty) {
      const isConstantSelector = value?.selector === SelectorOptionsEnum.CONSTANT;
      const isParameterSelector = value?.selector === SelectorOptionsEnum.PARAMETER;

      if (selectedProperty.inputType === InputTypes.SINGLE_SELECT && isConstantSelector) {
        inputs.push(
          createDynamicInput({
            id: 'value',
            label: 'Select Value',
            options: (selectedProperty?._options ?? selectedProperty?.options).map((o: any) => ({
              label: o?.displayName,
              value: o?.id,
            })),
            value:
              value?.choices?.map((c: any) => ({
                label: c.displayName,
                value: c.id,
              })) || null,
            placeholder: 'Select Value',
            type: InputTypes.SINGLE_SELECT,
            onChange: (_option: any) => {
              onChange({
                ...value,
                choices: [{ id: _option.value, displayName: _option.label }],
              });
            },
          }),
        );
      }

      if ([InputTypes.DATE, InputTypes.DATE_TIME].includes(selectedProperty.inputType)) {
        if (isParameterSelector) {
          inputs.push(
            createDynamicInput({
              id: 'dateParameter',
              label: `${parameterLabelMap[selectedProperty.inputType]} Parameter`,
              options: dateParameters
                .filter((params: Parameter) => value?.propertyInputType === params?.type)
                .map((date: any) => ({
                  label: date.label,
                  value: date.id,
                })),
              value: fetchRefParamsValue(value?.parameterId, dateParameters),
              placeholder: 'Select Date Parameter',
              type: InputTypes.SINGLE_SELECT,
              onChange: (_option: any) => {
                onChange({ ...value, parameterId: _option.value });
                setIsParameterInvalidError(false);
              },
              error:
                value?.parameterId && !keyBy(allParameters, 'id')?.[value?.parameterId]
                  ? 'Unmapped Parameter'
                  : undefined,
              isLoading: isLoadingParameters,
            }),
          );
        } else if (isConstantSelector) {
          inputs.push(
            createDynamicInput({
              id: 'setAs',
              label: 'Set As',
              options: getSetAsOptions(triggerType),
              value: value?.entityId
                ? getSetAsOptions(triggerType).filter(
                    (o) =>
                      o.value.captureProperty === value.captureProperty &&
                      o.value.entityType === value.entityType,
                  )
                : null,
              placeholder: 'Select Set As',
              type: InputTypes.SINGLE_SELECT,
              onChange: (_option: any) => {
                onChange({
                  ...value,
                  ..._option.value,
                  offsetDateUnit: null,
                  value: '',
                  parameterId: null,
                  offsetSelector: null,
                  offsetValue: '',
                  offsetParameterId: null,
                });
                clearErrors('offsetValue');
              },
            }),
          );

          if (value.captureProperty === CapturePropertySelectorOptionsEnum.CONSTANT) {
            inputs.push(
              createDynamicInput({
                id: 'value',
                label: 'Value',
                value: value?.value,
                placeholder: 'Enter Value',
                onChange: (_option: any) => onChange({ ...value, value: _option.value }),
                type: InputTypes[value.propertyInputType] || InputTypes.NUMBER,
              }),
            );
          } else if (value.captureProperty === CapturePropertySelectorOptionsEnum.PARAMETER) {
            inputs.push(
              createDynamicInput({
                id: 'dateParameter',
                label: `${parameterLabelMap[selectedProperty.inputType]} Parameter`,
                options: dateParameters
                  .filter((params: Parameter) => value?.propertyInputType === params?.type)
                  .map((date: any) => ({
                    label: date.label,
                    value: date.id,
                  })),
                value: fetchRefParamsValue(value?.parameterId, dateParameters),
                placeholder: 'Select Parameter',
                onChange: (_option: any) => {
                  onChange({ ...value, parameterId: _option.value });
                  setIsParameterInvalidError(false);
                },
                type: InputTypes.SINGLE_SELECT,
                isLoading: isLoadingParameters,
                error:
                  value?.parameterId && !keyBy(allParameters, 'id')?.[value?.parameterId]
                    ? 'Unmapped Parameter'
                    : undefined,
              }),
            );
          }

          inputs.push(
            createDynamicInput({
              id: 'offsetSelector',
              label: 'Offset Type',
              options: SELECTOR_OPTIONS,
              value: SELECTOR_OPTIONS.find((o) => o.value === value?.offsetSelector) || null,
              placeholder: 'Select Offset Type',
              type: InputTypes.SINGLE_SELECT,
              onChange: (_option: any) => {
                onChange({
                  ...value,
                  offsetSelector: _option.value,
                  offsetParameterId: null,
                  offsetValue: '',
                  offsetDateUnit: null,
                });
                clearErrors('offsetValue');
              },
            }),
          );

          if (value?.offsetSelector === OffsetSelectorOptionsEnum.CONSTANT) {
            inputs.push(
              createDynamicInput({
                id: 'offsetValue',
                label: 'Value',
                value: value?.offsetValue,
                placeholder: 'Enter Value',
                type: InputTypes.NUMBER,
                error: !!errors?.offsetValue?.message,
                onChange: (_option: any) => {
                  if (value?.propertyInputType === InputTypes.DATE) {
                    if (_option?.value % 1 !== 0) {
                      setError('offsetValue', {
                        message: 'Decimal Value Invalid',
                      });
                    } else {
                      clearErrors('offsetValue');
                    }
                  }
                  onChange({ ...value, offsetValue: _option.value });
                },
              }),
              createDynamicInput({
                id: 'objectPropertyUnit',
                label: 'Unit',
                options: Object.entries(getDateUnits(value?.propertyInputType)).map(
                  ([unitValue, label]) => ({
                    label,
                    value: unitValue,
                  }),
                ),
                value: value?.offsetDateUnit
                  ? [
                      {
                        label: getDateUnits(value.propertyInputType)[value.offsetDateUnit],
                        value: value.offsetDateUnit,
                      },
                    ]
                  : null,
                placeholder: 'Select Unit',
                type: InputTypes.SINGLE_SELECT,
                onChange: (_option: any) => onChange({ ...value, offsetDateUnit: _option.value }),
              }),
            );
          } else if (value?.offsetSelector === OffsetSelectorOptionsEnum.PARAMETER) {
            inputs.push(
              createDynamicInput({
                id: 'numberParameter',
                label: 'Select Parameter',
                options: numberParameters
                  .filter((param) => param.type === MandatoryParameter.NUMBER)
                  .map((date: any) => ({
                    label: date.label,
                    value: date.id,
                  })),
                value: fetchRefParamsValue(value?.offsetParameterId, numberParameters),
                placeholder: 'Select Parameter',
                type: InputTypes.SINGLE_SELECT,
                onChange: (_option: any) => {
                  onChange({ ...value, offsetParameterId: _option.value });
                },
                error:
                  value?.offsetParameterId &&
                  !keyBy(allParameters, 'id')?.[value?.offsetParameterId]
                    ? 'Unmapped Parameter'
                    : undefined,
                isLoading: isLoadingParameters,
              }),
              createDynamicInput({
                id: 'objectPropertyUnit',
                label: 'Unit',
                options: Object.entries(getDateUnits(value?.propertyInputType)).map(
                  ([unitValue, label]) => ({
                    label,
                    value: unitValue,
                  }),
                ),
                value: value?.offsetDateUnit
                  ? [
                      {
                        label: getDateUnits(value.propertyInputType)[value.offsetDateUnit],
                        value: value.offsetDateUnit,
                      },
                    ]
                  : null,
                placeholder: 'Select Unit',
                type: InputTypes.SINGLE_SELECT,
                onChange: (_option: any) => onChange({ ...value, offsetDateUnit: _option.value }),
              }),
            );
          }
        }
      }
    }
    return inputs;
  }

  const actionTypeOptions = useMemo(() => {
    return Object.keys(AutomationActionActionType)
      .filter(
        (actionType) =>
          actionType !== AutomationActionActionType.BULK_CREATE_OBJECT ||
          isFeatureAllowed('bulkCreateObjectAction'),
      )
      .map((actionType) => ({
        label: startCase(toLower(actionType)),
        value: actionType,
      }));
  }, []);

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="action-card-form">
      <div className="fields">
        <FormGroup
          inputs={[
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                id: 'displayName',
                name: 'displayName',
                label: 'Action Label',
                placeholder: 'Enter Label',
                value: displayName,
                defaultValue: displayName || null,
                ref: register({ required: true }),
                disabled: isReadOnly,
              },
            },
          ]}
        />
        <Controller
          control={control}
          name="triggerType"
          key="triggerType"
          shouldUnregister={false}
          defaultValue={triggerType || null}
          rules={{
            required: true,
          }}
          render={({ onChange, value }) => {
            return (
              <FormGroup
                inputs={[
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
                      value: value
                        ? [
                            {
                              label:
                                value === AutomationActionTriggerType.TASK_STARTED
                                  ? 'When task is started'
                                  : 'When task is completed',
                              value: value,
                            },
                          ]
                        : null,
                      onChange: (_option: { value: string }) => {
                        onChange(_option.value);
                      },
                    },
                  },
                ]}
              />
            );
          }}
        />
        <Controller
          control={control}
          name="actionType"
          key="actionType"
          shouldUnregister={false}
          defaultValue={actionType || null}
          rules={{
            required: true,
          }}
          render={({ value }) => {
            return (
              <FormGroup
                inputs={[
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'actionType',
                      label: 'Action Type',
                      options: actionTypeOptions,
                      placeholder: 'Select Action Type',
                      isDisabled: isReadOnly,
                      value: value
                        ? [
                            {
                              label: startCase(toLower(value)),
                              value: value,
                            },
                          ]
                        : null,
                      onChange: (_option: { value: string }) => {
                        actionTypeChangeHandler(_option);
                      },
                    },
                  },
                ]}
              />
            );
          }}
        />
        {actionType ? (
          <Controller
            control={control}
            name={`actionDetails`}
            key={`actionDetails`}
            defaultValue={actionDetails || null}
            shouldUnregister={false}
            rules={{
              required: true,
              validate: customActionDetailValidation,
            }}
            render={({ onChange, value }) => {
              return (
                <FormGroup
                  inputs={[
                    ...([
                      AutomationActionActionType.CREATE_OBJECT,
                      AutomationActionActionType.BULK_CREATE_OBJECT,
                    ].includes(actionType)
                      ? [
                          {
                            type: InputTypes.SINGLE_SELECT,
                            props: {
                              id: 'actionDetails',
                              label: 'Object Type',
                              isLoading: isReadOnly
                                ? false
                                : isOpenObjectTypes
                                ? listLoading
                                : false,
                              options: list.map((objectType) => ({
                                ...objectType,
                                label: objectType.displayName,
                                value: objectType.id,
                              })),
                              isDisabled: isReadOnly,
                              placeholder: 'Select Object Type',
                              value: value
                                ? [
                                    {
                                      label: value?.objectTypeDisplayName,
                                      value: value?.objectTypeId,
                                    },
                                  ]
                                : null,
                              onMenuOpen: () => {
                                fetchObjectTypesData();
                                setIsOpenObjectTypes(true);
                              },
                              onMenuClose: () => {
                                setIsOpenObjectTypes(false);
                              },
                              onInputChange: debounce((searchedString: string, actionMeta) => {
                                if (searchedString !== actionMeta.prevInputValue) {
                                  searchedValue.current = searchedString;
                                  fetchObjectTypesData({ query: searchedString });
                                }
                              }, 500),
                              onMenuScrollToBottom: () => {
                                if (!listLoading && !objectTypePagination.last) {
                                  fetchObjectTypesData(
                                    { page: objectTypePagination.page + 1 },
                                    true,
                                  );
                                }
                              },
                              onChange: (_option: any) => {
                                const actionDetails = {
                                  urlPath: `/objects/partial?collection=${_option.externalId}`,
                                  collection: _option.externalId,
                                  objectTypeId: _option.id,
                                  objectTypeExternalId: _option.externalId,
                                  objectTypeDisplayName: _option.displayName,
                                  ...(actionType === AutomationActionActionType.CREATE_OBJECT && {
                                    selector: 'NONE',
                                  }),
                                };

                                if (allowParameterMapping) {
                                  setAllowParameterMapping(false);
                                  reset({ ...getValues(), actionDetails }, { isDirty: true });
                                } else {
                                  onChange(actionDetails);
                                }

                                setSelectedObjectType(_option);
                              },
                            },
                          },
                          ...(actionType === AutomationActionActionType.BULK_CREATE_OBJECT
                            ? [
                                {
                                  type: InputTypes.SINGLE_SELECT,
                                  props: {
                                    id: 'selectOne',
                                    label: 'Select One',
                                    options: configuredAutomationsOptions,
                                    isDisabled: isReadOnly,
                                    value: value?.selector
                                      ? [
                                          {
                                            label: configuredAutomationsOptions.find(
                                              (option: any) => option.value === value.selector,
                                            )?.label,
                                            value: value.selector,
                                          },
                                        ]
                                      : null,
                                    placeholder: 'Select Any One',
                                    onChange: (option: any) => {
                                      onChange({
                                        ...value,
                                        selector: option.value,
                                      });
                                    },
                                  },
                                },
                                ...(value?.selector
                                  ? value.selector === ConfigureAutomationSelect.PARAMETER
                                    ? [
                                        {
                                          type: InputTypes.SINGLE_SELECT,
                                          props: {
                                            id: 'selectParameter',
                                            label: 'Select Parameter',
                                            isLoading: isLoadingParameters,
                                            options: getSelectParameterOptions({
                                              inputType: MandatoryParameter.NUMBER,
                                            }).map((parameter: any) => ({
                                              ...parameter,
                                              label: parameter.label,
                                              value: parameter.id,
                                            })),
                                            isDisabled: isReadOnly,
                                            value: fetchRefParamsValue(
                                              value?.referencedParameterId,
                                              getSelectParameterOptions({
                                                inputType: MandatoryParameter.NUMBER,
                                              }),
                                            ),
                                            placeholder: 'Select Parameter',
                                            onChange: (option: any) => {
                                              onChange({
                                                ...value,
                                                referencedParameterId: option?.value,
                                              });
                                            },
                                          },
                                        },
                                      ]
                                    : [
                                        {
                                          type: InputTypes.NUMBER,
                                          props: {
                                            id: 'bulkCount',
                                            name: 'bulkCount',
                                            label: 'Enter the number of objects for bulk creation',
                                            placeholder: 'Enter a number',
                                            value: value.bulkCount,
                                            defaultValue: value.bulkCount,
                                            onChange: (_option: any) => {
                                              onChange({
                                                ...value,
                                                bulkCount: _option.value,
                                              });
                                            },
                                            disabled: isReadOnly,
                                          },
                                        },
                                      ]
                                  : []),
                              ]
                            : []),
                        ]
                      : [
                          {
                            type: InputTypes.SINGLE_SELECT,
                            props: {
                              id: 'objectType',
                              label: 'Resource Parameter',
                              isLoading: isLoadingParameters,
                              options: resourceParameters
                                .filter((parameter: Parameter) => {
                                  if (actionType === AutomationActionActionType.SET_RELATION) {
                                    return (
                                      parameter.type === MandatoryParameter.RESOURCE ||
                                      parameter.type === MandatoryParameter.MULTI_RESOURCE
                                    );
                                  } else {
                                    return true;
                                  }
                                })
                                .map((resource: Parameter) => ({
                                  ...resource.data,
                                  label: resource.label,
                                  value: resource.id,
                                })),
                              placeholder: 'Select Resource Parameter',
                              error: isReferenceParameterInvalid ? 'Unmapped Parameter' : undefined,
                              isDisabled: isReadOnly,
                              value: fetchRefParamsValue(
                                value?.referencedParameterId,
                                resourceParameters,
                              ),
                              onChange: (_option: any) => {
                                if (actionType !== AutomationActionActionType.ARCHIVE_OBJECT) {
                                  onChange({
                                    referencedParameterId: _option.value,
                                    parameterId: value?.parameterId,
                                  });
                                  fetchObjectType(_option.objectTypeId);
                                } else {
                                  onChange({
                                    referencedParameterId: _option.value,
                                    parameterId: value?.parameterId,
                                    selector: 'NONE',
                                  });
                                }
                                setSelectedProperty(null);
                                setIsReferenceParameterInvalidError(false);
                                setIsParameterInvalidError(false);
                                setIsSelectParameterOptionsMismatched(false);
                              },
                            },
                          },
                          ...(selectedObjectType &&
                          actionType !== AutomationActionActionType.ARCHIVE_OBJECT &&
                          actionType !== AutomationActionActionType.SET_RELATION
                            ? [
                                {
                                  type: InputTypes.SINGLE_SELECT,
                                  props: {
                                    id: 'objectProperty',
                                    label: 'Object Property',
                                    isLoading: isLoadingObjectType,
                                    isDisabled: isReadOnly,
                                    options: selectedObjectType?.properties?.reduce<
                                      Array<Record<string, any>>
                                    >((acc, objectTypeProperty) => {
                                      if (
                                        (actionType === AutomationActionActionType.SET_PROPERTY
                                          ? [
                                              InputTypes.SINGLE_SELECT,
                                              InputTypes.DATE,
                                              InputTypes.DATE_TIME,
                                              InputTypes.NUMBER,
                                            ]
                                          : [InputTypes.NUMBER]
                                        ).includes(objectTypeProperty.inputType)
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
                                    }, []),
                                    value: value?.propertyId
                                      ? [
                                          {
                                            label: value.propertyDisplayName,
                                            value: value.propertyId,
                                          },
                                        ]
                                      : null,
                                    placeholder: 'Select Object Property',
                                    onChange: (_option: any) => {
                                      let data = {};
                                      if (
                                        ![
                                          InputTypes.NUMBER,
                                          InputTypes.SINGLE_SELECT,
                                          InputTypes.DATE,
                                          InputTypes.DATE_TIME,
                                        ].includes(_option.inputType)
                                      ) {
                                        data = {
                                          referencedParameterId: value?.referencedParameterId,
                                          parameterId: value?.parameterId,
                                          propertyId: _option.value,
                                          propertyInputType: _option.inputType,
                                          propertyExternalId: _option.externalId,
                                          propertyDisplayName: _option.label,
                                          selector: 'CONSTANT',
                                        };
                                      } else {
                                        data = {
                                          referencedParameterId: value?.referencedParameterId,
                                          parameterId: value?.parameterId,
                                          propertyId: _option.value,
                                          propertyInputType: _option.inputType,
                                          propertyExternalId: _option.externalId,
                                          propertyDisplayName: _option.label,
                                        };
                                      }
                                      onChange(data);
                                      setSelectedProperty(_option);
                                      setIsSelectParameterOptionsMismatched(false);
                                    },
                                  },
                                },
                              ]
                            : []),
                          ...renderSelectorComponent(value, onChange),
                          ...([
                            AutomationActionActionType.ARCHIVE_OBJECT,
                            AutomationActionActionType.SET_RELATION,
                          ].includes(actionType)
                            ? []
                            : value?.selector === SelectorOptionsEnum.PARAMETER &&
                              [InputTypes.NUMBER, InputTypes.SINGLE_SELECT].includes(
                                selectedProperty?.inputType,
                              )
                            ? [
                                {
                                  type: InputTypes.SINGLE_SELECT,
                                  props: {
                                    id: 'selectParameter',
                                    label: 'Select Parameter',
                                    isLoading: isLoadingParameters,
                                    options: getSelectParameterOptions(selectedProperty).map(
                                      (parameter: any) => ({
                                        ...parameter,
                                        label: parameter.label,
                                        value: parameter.id,
                                      }),
                                    ),
                                    isDisabled: isReadOnly,
                                    error:
                                      (isParameterInvalid && 'Unmapped Parameter') ||
                                      (isSelectParameterOptionsMismatched &&
                                        value?.parameterId &&
                                        'Dropdown options mismatched'),
                                    value: fetchRefParamsValue(
                                      value?.parameterId,
                                      getSelectParameterOptions(selectedProperty),
                                    ),
                                    placeholder: 'Select Parameter',
                                    onChange: (_option: any) => {
                                      onChange({
                                        ...value,
                                        parameterId: _option.value,
                                      });
                                      setIsParameterInvalidError(false);
                                      const areValuesEqual = isArraySubset(
                                        _option.data,
                                        selectedProperty._options,
                                        customizer,
                                      );
                                      if (
                                        !areValuesEqual &&
                                        selectedProperty?.inputType === InputTypes.SINGLE_SELECT
                                      ) {
                                        setIsSelectParameterOptionsMismatched(true);
                                      } else {
                                        setIsSelectParameterOptionsMismatched(false);
                                      }
                                    },
                                  },
                                },
                              ]
                            : [
                                AutomationActionActionType.INCREASE_PROPERTY,
                                AutomationActionActionType.DECREASE_PROPERTY,
                              ].includes(actionType) &&
                              value?.selector === SelectorOptionsEnum.CONSTANT
                            ? [
                                {
                                  type: InputTypes.NUMBER,
                                  props: {
                                    id: 'value',
                                    name: 'value',
                                    label: 'Set Value',
                                    placeholder: 'Enter a number',
                                    value: value.value,
                                    defaultValue: value.value,
                                    onChange: (_option: any) => {
                                      onChange({
                                        ...value,
                                        value: _option.value,
                                      });
                                    },
                                    disabled: isReadOnly,
                                  },
                                },
                              ]
                            : actionType === AutomationActionActionType.SET_PROPERTY &&
                              value?.selector === SelectorOptionsEnum.CONSTANT &&
                              selectedProperty?.inputType === InputTypes.NUMBER
                            ? [
                                {
                                  type: InputTypes.NUMBER,
                                  props: {
                                    id: 'value',
                                    name: 'value',
                                    label: 'Set Value',
                                    placeholder: 'Enter a number',
                                    value: value.value,
                                    defaultValue: value.value,
                                    onChange: (_option: any) => {
                                      onChange({
                                        ...value,
                                        value: _option.value,
                                      });
                                    },
                                    disabled: isReadOnly,
                                  },
                                },
                              ]
                            : []),
                          ...(actionType === AutomationActionActionType.SET_PROPERTY
                            ? generateDynamicInputs({
                                selectedProperty,
                                value,
                                isReadOnly,
                                triggerType,
                                dateParameters,
                                numberParameters,
                                isLoadingParameters,
                                onChange,
                                setIsParameterInvalidError,
                                fetchRefParamsValue,
                              })
                            : []),
                          ...(selectedObjectType &&
                          actionType === AutomationActionActionType.SET_RELATION
                            ? [
                                {
                                  type: InputTypes.SINGLE_SELECT,
                                  props: {
                                    id: 'objectRelation',
                                    label: 'Object Relation',
                                    isLoading: isLoadingObjectType,
                                    isDisabled: isReadOnly,
                                    options: selectedObjectType?.relations?.map((relation) => ({
                                      value: relation.id,
                                      label: relation?.displayName,
                                      externalId: relation?.externalId,
                                      target: relation?.target,
                                      objectTypeId: relation?.objectTypeId,
                                      flags: relation?.flags,
                                    })),
                                    value: actionDetails?.relationId
                                      ? [
                                          {
                                            label: actionDetails.relationDisplayName,
                                            value: actionDetails.relationId,
                                          },
                                        ]
                                      : null,
                                    placeholder: 'Select Object Relation',
                                    onChange: (option: any) => {
                                      setSelectedRelation(option);
                                      setObjectUrlPath(`${baseUrl}${option?.target?.urlPath}`);
                                      onChange({
                                        ...value,
                                        relationId: option.value,
                                        relationExternalId: option.externalId,
                                        relationDisplayName: option.label,
                                        relationObjectTypeId: option.objectTypeId,
                                        flags: option.flags,
                                        selector: 'PARAMETER',
                                        parameterId: undefined,
                                      });
                                    },
                                  },
                                },
                              ]
                            : []),
                          ...(selectedRelation &&
                          actionType === AutomationActionActionType.SET_RELATION
                            ? [
                                {
                                  type: InputTypes.SINGLE_SELECT,
                                  props: {
                                    id: 'value',
                                    label: (
                                      <div className="relation-value">
                                        Set Value
                                        {selectedRelation.target.cardinality ===
                                          Cardinality.ONE_TO_ONE && (
                                          <Tooltip
                                            title={
                                              'Only Single Resource Selectors are applicable for ONE-TO-ONE cardinality of selected relation.'
                                            }
                                            arrow
                                            placement="right"
                                          >
                                            <img src={InfoIcon}></img>
                                          </Tooltip>
                                        )}
                                      </div>
                                    ),
                                    options: resourceParameters
                                      ?.filter(
                                        (param: Parameter) =>
                                          param?.data?.objectTypeId ===
                                            selectedRelation.objectTypeId &&
                                          (selectedRelation.target.cardinality ===
                                          Cardinality.ONE_TO_ONE
                                            ? param.type === MandatoryParameter.RESOURCE
                                            : [
                                                param.type === MandatoryParameter.RESOURCE,
                                                param.type === MandatoryParameter.MULTI_RESOURCE,
                                              ]),
                                      )
                                      .map((parameter: Parameter) => ({
                                        value: parameter.id,
                                        label: parameter?.label,
                                      })),
                                    isDisabled: isReadOnly,
                                    placeholder: 'Set Value',
                                    error: isParameterInvalid ? 'Unmapped Parameter' : undefined,
                                    value: fetchRefParamsValue(
                                      value?.parameterId,
                                      resourceParameters,
                                    ),
                                    onChange: (option: any) => {
                                      onChange({
                                        ...value,
                                        parameterId: option.value,
                                      });
                                      setIsParameterInvalidError(false);
                                    },
                                  },
                                },
                              ]
                            : []),
                        ]),
                  ]}
                />
              );
            }}
          />
        ) : null}
        {[
          AutomationActionActionType.CREATE_OBJECT,
          AutomationActionActionType.BULK_CREATE_OBJECT,
        ].includes(actionType) && (
          <ToggleSwitch
            height={24}
            width={48}
            offLabel="Map Object Properties with Process Properties"
            onColor="#24a148"
            onChange={(isChecked) => {
              setAllowParameterMapping(isChecked);
              if (!isChecked && actionDetails) {
                const { configuration, ...updatedActionDetails } = actionDetails;
                reset(
                  { ...getValues(), actionDetails: { ...updatedActionDetails } },
                  { isDirty: true },
                );
              }
            }}
            onLabel="Map Object Properties with Process Properties"
            checked={allowParameterMapping}
            disabled={isReadOnly}
          />
        )}
        {[
          AutomationActionActionType.CREATE_OBJECT,
          AutomationActionActionType.BULK_CREATE_OBJECT,
        ].includes(actionType) &&
          allowParameterMapping &&
          selectedObjectType &&
          [...selectedObjectType?.properties, ...selectedObjectType?.relations]
            ?.filter(
              (property: ObjectTypeProperty | ObjectTypeRelation) =>
                property.flags !== 1 && property.usageStatus === 1,
            )
            ?.map((property: ObjectTypeProperty | ObjectTypeRelation) => {
              return (
                <CreateObjectPropertyField
                  key={property.id}
                  property={property}
                  isReadOnly={isReadOnly}
                  control={control}
                  uniqueKey={property.id}
                  parametersList={allParameters}
                  isRelation={property?.objectTypeId ? true : false}
                  handleForm={handleForm}
                  setValue={setValue}
                />
              );
            })}
      </div>
      {!isReadOnly && (
        <div className="action-buttons-container">
          <Button variant="primary" type="submit" disabled={handleDisabledSubmit()}>
            {editActionFlag ? 'Update' : 'Save'}
          </Button>
        </div>
      )}
    </form>
  );
};

const parameterListAsPerInputType = (
  type: MandatoryParameter | InputTypes,
  parametersList: Parameter[],
  relationObjectTypeId: string | null,
  property: any,
) => {
  switch (type) {
    case MandatoryParameter.CALCULATION:
    case MandatoryParameter.NUMBER:
    case MandatoryParameter.SHOULD_BE:
      return parametersList.filter((currParameter) =>
        [
          MandatoryParameter.CALCULATION,
          MandatoryParameter.NUMBER,
          MandatoryParameter.SHOULD_BE,
        ].includes(currParameter.type),
      );
    case MandatoryParameter.SINGLE_LINE:
      return parametersList.filter(
        (currParameter) => currParameter.type === MandatoryParameter.SINGLE_LINE,
      );
    case MandatoryParameter.MULTI_LINE:
      return parametersList.filter(
        (currParameter) => currParameter.type === MandatoryParameter.MULTI_LINE,
      );
    case MandatoryParameter.SINGLE_SELECT:
      return parametersList.filter(
        (currParameter) =>
          currParameter.type === MandatoryParameter.SINGLE_SELECT &&
          currParameter.metadata &&
          currParameter.metadata.propertyId === property.id,
      );
    case InputTypes.MULTI_SELECT:
      return parametersList.filter(
        (currParameter) =>
          currParameter.type === MandatoryParameter.MULTISELECT &&
          currParameter.metadata &&
          currParameter.metadata.propertyId === property.id,
      );
    case InputTypes.ONE_TO_ONE:
      return parametersList.filter(
        (currParameter) =>
          currParameter.type === MandatoryParameter.RESOURCE &&
          currParameter.data.objectTypeId === relationObjectTypeId,
      );
    case InputTypes.ONE_TO_MANY:
      return parametersList.filter(
        (currParameter) =>
          [MandatoryParameter.RESOURCE, MandatoryParameter.MULTI_RESOURCE].includes(
            currParameter.type,
          ) && currParameter.data.objectTypeId === relationObjectTypeId,
      );
    case MandatoryParameter.DATE:
      return parametersList.filter(
        (currParameter) => currParameter.type === MandatoryParameter.DATE,
      );
    case MandatoryParameter.DATE_TIME:
      return parametersList.filter(
        (currParameter) => currParameter.type === MandatoryParameter.DATE_TIME,
      );
    default:
      return [];
  }
};

const CreateObjectPropertyField = memo<{
  property: ObjectTypeProperty | ObjectTypeRelation;
  isReadOnly: boolean;
  control: any;
  uniqueKey: string;
  parametersList: Parameter[];
  isRelation: boolean;
  handleForm: any;
  setValue: any;
}>(
  ({
    property,
    isReadOnly,
    control,
    parametersList,
    isRelation,
    handleForm,
    uniqueKey,
    setValue,
  }) => {
    //In Case is relations property key has relation in it and we use isRelation to distinguish
    const { flags } = property;
    const [showError, setShowError] = useState({});
    const [useDefaultValueForProperty, setUseDefaultValueForProperty] = useState<boolean>(false);
    const [state, setState] = useState<any>({
      isLoading: false,
      options: [],
      isOpen: false,
      searchValue: '',
    });
    const { options, isLoading, isOpen, searchValue } = state;

    const pagination = useRef({
      current: -1,
      isLast: false,
    });

    const getOptions = async (url?: string) => {
      if (url) {
        setState((prev) => ({ ...prev, isLoading: true }));
        try {
          const response = await request('GET', url);
          if (response.pageable) {
            pagination.current = {
              current: response.pageable?.page,
              isLast: response.pageable?.last,
            };
          }
          setState((prev) => ({
            ...prev,
            options:
              pagination.current.current === 0
                ? response.data
                : [...prev.options, ...response.data],
            isLoading: false,
          }));
        } catch (e) {
          setState((prev) => ({ ...prev, isLoading: false }));
        }
      }
    };

    const getUrl = (page: number, searchValue?: string) => {
      let urlString = `${baseUrl}${property?.target?.urlPath}&page=${page}`;
      return urlString + (searchValue ? `&query=${searchValue}` : '');
    };

    const isRequired = !isRelation
      ? getBooleanFromDecimal(flags, FlagPositions.MANDATORY)
      : flags === 16;
    const isAutoGenerated = getBooleanFromDecimal(flags, FlagPositions.AUTOGENERATE);

    const { register, watch } = handleForm;
    const { actionDetails } = watch(['actionDetails']);
    const { configuration = {} } = actionDetails;
    const activeConfiguration = configuration?.[uniqueKey];
    const isMapped =
      parametersList?.filter((el) => el?.id === activeConfiguration?.parameterId)?.length > 0;

    const defaultPropertyValue = useMemo(() => {
      if (property?.inputType === InputTypes.SINGLE_SELECT) {
        const selectedOption = property?.options?.find(
          (option: any) => option.id === activeConfiguration?.defaultValue,
        );
        if (selectedOption) {
          return {
            ...selectedOption,
            label: selectedOption?.displayName,
            value: selectedOption?.id,
          };
        }
      } else if (property?.inputType === InputTypes.MULTI_SELECT) {
        const selectedOptions = property?.options
          ?.filter((option: any) => activeConfiguration?.defaultValue?.includes(option.id))
          .map((option: any) => ({
            ...option,
            label: option.displayName,
            value: option.id,
          }));

        if (selectedOptions && selectedOptions.length > 0) {
          return selectedOptions;
        }
      } else {
        return activeConfiguration?.defaultValue;
      }
    }, [property?.inputType]);

    let defaultValue = activeConfiguration?.parameterId
      ? {
          label: parametersList.find(
            (currParameter: Parameter) => currParameter.id === activeConfiguration?.parameterId,
          )?.label,
          value: activeConfiguration?.parameterId,
        }
      : null;

    const defaultPropertyValueOptions = useMemo(() => {
      if ([InputTypes.SINGLE_SELECT, InputTypes.MULTI_SELECT].includes(property?.inputType)) {
        return (property?.options || []).map((option: any) => ({
          ...option,
          label: option.displayName,
          value: option.id,
        }));
      }
      return [];
    }, [property?.inputType]);

    const defaultInputType = useMemo(() => {
      if (isRelation) {
        if (property?.target?.cardinality === InputTypes.ONE_TO_MANY) {
          return InputTypes.MULTI_SELECT;
        } else {
          return InputTypes.SINGLE_SELECT;
        }
      } else {
        return property?.inputType;
      }
    }, [property?.inputType]);

    const setOptionsError = (oldOptions: any[], newOptions: any[]) => {
      const areValuesEqual = isArraySubset(oldOptions, newOptions, customizer);
      if (!areValuesEqual) {
        setShowError({ status: true, errorMesssage: 'Dropdown options mismatched' });
      } else {
        setShowError({ status: false, errorMesssage: '' });
      }
    };

    useEffect(() => {
      const propertyType = property.inputType;

      const checkOptions = (parameterType) => {
        if (defaultValue?.value) {
          const data = parametersList.filter(
            (currParameter) =>
              currParameter.type === parameterType && currParameter.id === defaultValue?.value,
          );
          setOptionsError(data?.[0]?.data || [], property.options);
        }
      };

      if (propertyType === InputTypes.SINGLE_SELECT) {
        checkOptions(MandatoryParameter.SINGLE_SELECT);
      } else if (propertyType === InputTypes.MULTI_SELECT) {
        checkOptions(MandatoryParameter.MULTISELECT);
      }
    }, [parametersList.length, defaultValue?.value]);

    useEffect(() => {
      if (isOpen) getOptions(getUrl(0, searchValue));
    }, [isOpen, searchValue]);

    useEffect(() => {
      if (defaultPropertyValue) {
        setUseDefaultValueForProperty(true);
      }
    }, [defaultPropertyValue]);

    const controllerKey = `actionDetails.configuration.${uniqueKey}`;

    return (
      <div className="action-create-object-fields">
        <FormGroup
          inputs={[
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'objectPropertyName',
                label: property.displayName,
                isDisabled: true,
                optional: !isRequired,
                value: property.id
                  ? [
                      {
                        label: property.displayName,
                        value: property.id,
                      },
                    ]
                  : null,
                isSearchable: false,
                placeholder: 'Select Property',
              },
            },
          ]}
        />
        <input
          name={isRelation ? `${controllerKey}.relationId` : `${controllerKey}.propertyId`}
          ref={register()}
          defaultValue={property?.id}
          type="hidden"
        />
        <input
          name={
            isRelation
              ? `${controllerKey}.relationCardinality`
              : `${controllerKey}.propertyInputType`
          }
          ref={register()}
          defaultValue={isRelation ? property?.target?.cardinality : property?.inputType}
          type="hidden"
        />
        <input
          name={
            isRelation
              ? `${controllerKey}.relationExternalId`
              : `${controllerKey}.propertyExternalId`
          }
          ref={register()}
          defaultValue={property?.externalId}
          type="hidden"
        />
        <input
          name={
            isRelation
              ? `${controllerKey}.relationDisplayName`
              : `${controllerKey}.propertyDisplayName`
          }
          ref={register()}
          defaultValue={property?.displayName}
          type="hidden"
        />
        {isAutoGenerated ? (
          <Tooltip
            title={isAutoGenerated && <>Mapping is disabled for autogenerated properties</>}
            arrow
            placement="right"
          >
            <img src={InfoIcon} alt="action-info" className="action-info-icon" />
          </Tooltip>
        ) : (
          <img src={RightArrowIcon} alt="action-arrow" className="action-info-icon" />
        )}

        <Controller
          name={`actionDetails.configuration.${uniqueKey}.parameterId`}
          control={control}
          key={`actionDetails.configuration.${uniqueKey}.parameterId`}
          defaultValue={defaultValue}
          render={({ onChange, value, name }) => {
            return (
              <FormGroup
                inputs={[
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: name,
                      name,
                      label: 'Select Parameter',
                      options: parameterListAsPerInputType(
                        isRelation ? property?.target?.cardinality : property.inputType,
                        parametersList,
                        isRelation ? property?.objectTypeId : null,
                        property,
                      ),
                      isSearchable: false,
                      placeholder: 'Select Parameter',
                      isClearable: true,
                      isDisabled: isReadOnly || isAutoGenerated,
                      defaultValue: value,
                      onChange: (_option: any) => {
                        if (_option) {
                          if (
                            [InputTypes.SINGLE_SELECT, InputTypes.MULTI_SELECT].includes(
                              property.inputType,
                            )
                          ) {
                            setOptionsError(_option.data, property.options);
                          }
                          onChange(_option.id);
                        } else {
                          setShowError({ status: false, errorMesssage: '' });
                          onChange(null);
                        }
                      },
                      ...((showError?.status || !isMapped) && {
                        error:
                          !isMapped && activeConfiguration?.parameterId
                            ? 'Unmapped Parameter'
                            : showError?.errorMesssage,
                      }),
                    },
                  },
                ]}
              />
            );
          }}
        />
        <img src={RightArrowIcon} alt="action-arrow" className="action-info-icon" />

        <div className="select-parameter">
          <Controller
            name={`actionDetails.configuration.${uniqueKey}.defaultValue`}
            control={control}
            key={`actionDetails.configuration.${uniqueKey}.defaultValue`}
            defaultValue={defaultPropertyValue}
            render={({ onChange, value, name }) => {
              return (
                <FormGroup
                  inputs={[
                    {
                      type: defaultInputType,
                      props: {
                        id: name,
                        name,
                        label: 'Default Value',
                        options: defaultPropertyValueOptions,
                        placeholder: 'Default Value',
                        isClearable: true,
                        ...([InputTypes.SINGLE_SELECT, InputTypes.MULTI_SELECT].includes(
                          defaultInputType,
                        )
                          ? {
                              isDisabled:
                                isReadOnly || isAutoGenerated || !useDefaultValueForProperty,
                            }
                          : {
                              disabled:
                                isReadOnly || isAutoGenerated || !useDefaultValueForProperty,
                            }),
                        isDisabled: isReadOnly || isAutoGenerated || !useDefaultValueForProperty,
                        disabled: isReadOnly || isAutoGenerated || !useDefaultValueForProperty,
                        defaultValue: value,
                        onChange: (value: any) => {
                          let newValue;
                          if (value) {
                            if (isArray(value)) {
                              if (isRelation) {
                                newValue = value.length
                                  ? value.map((val) => {
                                      return {
                                        value: val?.value,
                                        label: val?.label,
                                        ...val?.option,
                                      };
                                    })
                                  : null;
                              } else {
                                newValue = value.length ? value.map((val) => val.value) : null;
                              }
                            } else {
                              if (isRelation) {
                                newValue = {
                                  value: value?.value,
                                  label: value?.label,
                                  ...value?.option,
                                };
                              } else {
                                newValue = value?.value || null;
                              }
                            }
                          }
                          onChange(newValue);
                        },
                        ...(isRelation && {
                          onMenuScrollToBottom: () => {
                            if (!isLoading && !pagination.current.isLast) {
                              getOptions(getUrl(pagination.current.current + 1, searchValue));
                            }
                          },
                          onMenuOpen: () => {
                            setState((prev) => ({ ...prev, isOpen: true }));
                          },
                          onMenuClose: () => {
                            setState((prev) => ({ ...prev, isOpen: false }));
                          },
                          onInputChange: debounce((value, actionMeta) => {
                            if (value !== actionMeta.prevInputValue) {
                              setState((prev) => ({ ...prev, searchValue: value }));
                            }
                          }, 500),
                          options: options?.map((option) => ({
                            value: option.id,
                            label: option.displayName,
                            externalId: <div>&nbsp;(ID: {option.externalId})</div>,
                            option,
                          })),
                          isSearchable: true,
                        }),
                      },
                    },
                  ]}
                />
              );
            }}
          />
          <ToggleSwitch
            id={`${controllerKey}.useDefaultValue`}
            checkedIcon={false}
            uncheckedIcon={false}
            offLabel="Default Value"
            onColor="#24a148"
            onChange={(isChecked) => {
              if (!isChecked) {
                setValue(`actionDetails.configuration.${uniqueKey}.defaultValue`, null);
              }
              setUseDefaultValueForProperty(isChecked);
            }}
            onLabel="Default Value"
            checked={useDefaultValueForProperty}
            disabled={isReadOnly}
          />
        </div>
      </div>
    );
  },
);

export default ConfigureActions;
