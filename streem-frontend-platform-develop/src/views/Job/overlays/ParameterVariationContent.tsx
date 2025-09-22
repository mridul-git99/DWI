import {
  BaseModal,
  Button,
  CustomTag,
  DataTable,
  Link,
  LoadingContainer,
  Pagination,
  TextInput,
} from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { CommonOverlayProps, OverlayNames } from '#components/OverlayContainer/types';
import { createFetchList } from '#hooks/useFetchData';
import checkPermission from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { Job, MandatoryParameter, Parameter, ParameterVariationType } from '#types';
import { labelByConstraint, openLinkInNewTab } from '#utils';
import {
  apiArchiveParameterVariation,
  apiGetObjectTypes,
  apiGetVariationsList,
  apiGetVariationsListByParameterId,
} from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import { generateShouldBeCriteria } from '#utils/stringUtils';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { Search } from '@material-ui/icons';
import KeyboardArrowLeftIcon from '@material-ui/icons/KeyboardArrowLeft';
import { debounce } from 'lodash';
import React, { FC, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import ParameterVariationDrawer, {
  getLabelByVariationType,
} from '../components/ParameterVariationDrawer';
import { getParametersInfo } from '../utils';

const Wrapper = styled.div`
  .modal {
    min-height: 100dvh;
    min-width: 100dvw !important;
  }

  .modal-body {
    height: 100%;
  }
`;

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
};

const getAllParameterIds = (data: any) => {
  const parameterIds: string[] = [];

  data.forEach((item: any) => {
    if (item?.parameterId) {
      parameterIds.push(item.parameterId);
    }

    if (item?.oldVariation) {
      item.oldVariation.forEach((variation: any) => {
        if (variation?.parameterId) {
          parameterIds.push(variation.parameterId);
        }
      });
    }

    if (item?.newVariation) {
      item.newVariation.forEach((variation: any) => {
        if (variation?.parameterId) {
          parameterIds.push(variation.parameterId);
        }
      });
    }
  });

  return [...new Set(parameterIds)];
};

export const objectTypesUrlParams = {
  ...urlParams,
  usageStatus: 1,
  filters: {
    op: FilterOperators.AND,
    fields: [],
  },
};

const ParameterVariationContent: FC<
  CommonOverlayProps<{
    jobId?: Job['id'];
    isReadOnly?: boolean;
    parameterId?: string;
  }>
> = ({ closeAllOverlays, closeOverlay, props: { jobId, isReadOnly = false, parameterId } }) => {
  const dispatch = useDispatch();

  const { list, reset, pagination, status } = createFetchList(
    parameterId ? apiGetVariationsListByParameterId(parameterId) : apiGetVariationsList(jobId!),
    { ...urlParams, ...(parameterId && { jobId }) },
    false,
  );
  const objectTypeIds = useRef<string[]>([]);
  const { id } = useTypedSelector((state) => state.job);
  const [filters, setFilters] = useState<Record<string, any>>(urlParams);
  const [parameterVariationDrawer, setParameterVariationDrawer] = useState<string | boolean>('');
  const [shouldToggle, setShouldToggle] = useState<boolean>(false);
  const [referenceParameters, setReferenceParameters] = useState<any>(null);
  const [refLoading, setRefLoading] = useState<boolean>(false);
  const isCreateEditAllowed = useMemo(() => {
    return !checkPermission(['plannedVariation', 'create']) || isReadOnly;
  }, [isReadOnly]);
  const [state, setState] = useState({
    objectTypesList: [],
    loading: false,
  });
  const { objectTypesList, loading } = state;

  const archiveVariation = async (
    parameterId: string,
    variationId: string,
    type: string,
    reason: string,
    setFormErrors: (errors?: Error[]) => void,
  ) => {
    try {
      const { data, errors } = await request('DELETE', apiArchiveParameterVariation(), {
        data: { reason, jobId, parameterId, variationId, type },
      });
      if (data) {
        setFormErrors(errors);
        setShouldToggle((prev) => !prev);
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: 'Variation Deleted Successfully',
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

  const generateVariationDetailText = (details: any[], type: string, parameterId: string) => {
    const parameterData = referenceParameters[parameterId];
    switch (type) {
      case ParameterVariationType.FILTER:
        return getContentString(details, parameterData);
      case ParameterVariationType.VALIDATION:
        return getContentString(details, parameterData, true);
      case ParameterVariationType.SHOULD_BE:
        const detail = Array.isArray(details) ? details[0] : details;
        const uom = detail?.uom || '';
        const value =
          detail.operator === 'BETWEEN'
            ? `${detail.lowerValue} ${uom} and ${detail.upperValue} ${uom}`
            : `${detail.value} ${uom}`;
        return `Check if entered value is ${generateShouldBeCriteria(detail)} ${value}`;
    }
  };

  const getContentString = (
    details: any[],
    parameter: Parameter,
    isValidation: boolean = false,
  ) => {
    switch (parameter.type) {
      case MandatoryParameter.NUMBER:
        return details
          ?.map((currDetail: any) => {
            const dependentParameter = referenceParameters[currDetail.parameterId];
            return `Check if entered value ${
              labelByConstraint(parameter.type)[currDetail.constraint]
            } ${currDetail.propertyDisplayName} of selected ${dependentParameter?.label} value`;
          })
          .join(',');

      case MandatoryParameter.RESOURCE:
        return isValidation
          ? details
              ?.map((currDetail: any) => {
                const value = currDetail?.value
                  ? currDetail.value
                  : currDetail.options.map((currOption) => currOption.displayName).join(',');
                return `Check if ${currDetail.propertyDisplayName} of ${
                  parameter.data.objectTypeDisplayName
                } ${labelByConstraint(parameter.type)[currDetail.constraint]} ${value}`;
              })
              .join(',')
          : details
              ?.map((currDetail: any) => {
                const dependentParameter = referenceParameters[currDetail.referencedParameterId];
                const parameterObjectType = objectTypesList.find(
                  (currObjectType) => currObjectType.id === parameter?.data.objectTypeId,
                );
                const parameterObjectTypeProperty = [
                  ...(parameterObjectType?.properties || []),
                  ...(parameterObjectType?.relations || []),
                ].find((currProperty) => currProperty.id === currDetail?.field?.split('.')[1]);
                const value =
                  currDetail.selector === 'PARAMETER'
                    ? `the selected ${dependentParameter.label} value`
                    : currDetail?.displayName
                    ? `${currDetail.displayName} ${
                        currDetail?.externalId ? currDetail.externalId : ''
                      }`
                    : ` ${currDetail.values[0]}`;
                return `Check if ${parameter.data.objectTypeDisplayName} where ${
                  parameterObjectTypeProperty?.displayName
                } ${labelByConstraint(parameter.type)[currDetail.op]} ${value}`;
              })
              .join(',');

      default:
        return '';
    }
  };

  const getObjectTypeIdsForResourceParameters = () => {
    list.forEach((item) => {
      if (item.parameterType === MandatoryParameter.RESOURCE) {
        const parameter = referenceParameters[item.parameterId];
        if (parameter) {
          objectTypeIds.current.push(parameter.data.objectTypeId);
        }
      }
    });
    if (objectTypeIds.current.length) {
      fetchObjectTypesList();
    }
  };

  const fetchObjectTypesList = async () => {
    setState((prev) => ({ ...prev, loading: true }));
    const { data = [] } = await request('GET', apiGetObjectTypes(), {
      params: {
        ...objectTypesUrlParams,
        size: objectTypeIds.current.length || objectTypesUrlParams.size,
        filters: {
          op: FilterOperators.AND,
          fields: [{ field: 'id', op: FilterOperators.ANY, values: objectTypeIds.current }],
        },
      },
    });
    setState((prev) => ({ ...prev, objectTypesList: data, loading: false }));
  };

  const fetchReferenceParameters = async (list: any[]) => {
    setRefLoading(true);
    const referencedParameterIds = getAllParameterIds(list);
    const data = await getParametersInfo(id, referencedParameterIds);

    if (data) {
      setReferenceParameters(data);
      setRefLoading(false);
    }
  };

  useEffect(() => {
    reset({ params: { ...filters } });
  }, [filters, shouldToggle]);

  useEffect(() => {
    if (list.length) {
      fetchReferenceParameters(list);
    }
  }, [list]);

  useEffect(() => {
    if (referenceParameters && list.length && !refLoading) {
      getObjectTypeIdsForResourceParameters();
    }
  }, [referenceParameters, list]);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showHeader={true}
        title={
          <Link
            label="Parameter Variation"
            backIcon={KeyboardArrowLeftIcon}
            onClick={closeOverlay}
            iconColor="#000000"
            labelColor="#000000"
            addMargin={false}
          />
        }
        showFooter={false}
        showCloseIcon={true}
      >
        <TabContentWrapper>
          <div className="before-table-wrapper">
            <div className="filters">
              <TextInput
                afterElementWithoutError
                AfterElement={Search}
                afterElementClass=""
                placeholder={`Search by Parameter name`}
                onChange={debounce((option) => {
                  setFilters((prev) => ({
                    ...prev,
                    parameterName: option?.value,
                  }));
                }, 500)}
              />
            </div>
            <div className="actions">
              <Button
                onClick={() => setParameterVariationDrawer(true)}
                disabled={isCreateEditAllowed}
              >
                Add Variation
              </Button>
            </div>
          </div>
          <LoadingContainer
            loading={loading || status === 'loading' || refLoading}
            component={
              <>
                <DataTable
                  columns={[
                    {
                      id: 'parameterName',
                      label: 'Parameter Name',
                      minWidth: 100,
                      format: (item) => {
                        return item.parameterName;
                      },
                    },
                    {
                      id: 'variationName',
                      label: 'Variation Name',
                      minWidth: 100,
                      format: (item) => {
                        return item.name;
                      },
                    },
                    {
                      id: 'variationNumber',
                      label: 'Variation Number',
                      minWidth: 100,
                      format: (item) => {
                        const media = item?.medias?.[0] || {};
                        const customStyle = !item.medias
                          ? {}
                          : { color: '#1d84ff', cursor: 'pointer' };
                        return (
                          <CustomTag
                            as={'div'}
                            onClick={
                              !item.medias
                                ? undefined
                                : () => {
                                    openLinkInNewTab(`/media?link=${media.link}`);
                                  }
                            }
                          >
                            <span style={customStyle}>{item.variationNumber}</span>
                          </CustomTag>
                        );
                      },
                    },
                    {
                      id: 'location',
                      label: 'Location',
                      minWidth: 100,
                      format: (item) =>
                        `Task ${item.stageOrderTree}.${item.taskOrderTree}${
                          item?.taskExecutionOrderTree - 1 > 0
                            ? `.${item.taskExecutionOrderTree - 1}`
                            : ''
                        }`,
                    },
                    {
                      id: 'variationType',
                      label: 'Variation Type',
                      minWidth: 100,
                      format: (item) => {
                        return getLabelByVariationType(item.type);
                      },
                    },
                    {
                      id: 'valueBefore',
                      label: 'Value Before',
                      minWidth: 100,
                      format: (item) => {
                        return generateVariationDetailText(
                          item.oldVariation,
                          item.type,
                          item.parameterId,
                        );
                      },
                    },
                    {
                      id: 'valueAfter',
                      label: 'Value After',
                      minWidth: 100,
                      format: (item) => {
                        return generateVariationDetailText(
                          item.newVariation,
                          item.type,
                          item.parameterId,
                        );
                      },
                    },
                    {
                      id: 'action',
                      label: 'Actions',
                      minWidth: 100,
                      format: (item) => {
                        return (
                          <div
                            style={{
                              display: 'flex',
                              gap: 20,
                              alignItems: 'flex-start',
                            }}
                          >
                            {/* <div className="primary">
                              <span>Edit</span>
                            </div> */}
                            <div
                              className="secondary"
                              onClick={() => {
                                if (!isCreateEditAllowed) {
                                  dispatch(
                                    openOverlayAction({
                                      type: OverlayNames.REASON_MODAL,
                                      props: {
                                        modalTitle: 'Delete Variation',
                                        modalDesc: `Provide reason for deleting ${item?.name}`,
                                        onSubmitHandler: (
                                          reason: string,
                                          setFormErrors: (errors?: Error[]) => void,
                                        ) => {
                                          archiveVariation(
                                            item.parameterId,
                                            item.id,
                                            item.type,
                                            reason,
                                            setFormErrors,
                                          );
                                        },
                                        onSubmitModalText: 'Confirm',
                                      },
                                    }),
                                  );
                                }
                              }}
                            >
                              <span>{isCreateEditAllowed ? '-' : 'Delete'}</span>
                            </div>
                          </div>
                        );
                      },
                    },
                  ]}
                  rows={list}
                  emptyTitle="No Variations Found"
                />
                <Pagination
                  pageable={pagination}
                  fetchData={(p) => reset({ params: { page: p.page, size: p.size } })}
                />
                {parameterVariationDrawer && (
                  <ParameterVariationDrawer
                    onCloseDrawer={setParameterVariationDrawer}
                    label={parameterVariationDrawer}
                    setShouldToggle={setShouldToggle}
                  />
                )}
              </>
            }
          />
        </TabContentWrapper>
      </BaseModal>
    </Wrapper>
  );
};

export default ParameterVariationContent;
