import MemoArchive from '#assets/svg/Archive';
import {
  Button,
  DataTable,
  GeneralHeader,
  ListActionMenu,
  LoadingContainer,
  Pagination,
  TabContentProps,
  TextInput,
  ToggleSwitch,
} from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import useTabs from '#components/shared/useTabs';
import { createFetchList } from '#hooks/useFetchData';
import checkPermission, { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { MandatoryParameter } from '#types';
import {
  apiArchiveObjectTypeQrCodeParser,
  apiGetObjectTypeRelations,
  apiQrCodeParsers,
  apiUnarchiveObjectTypeQrCodeParser,
  apigetObjectTypeProperties,
} from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import { formatDateTime } from '#utils/timeUtils';
import { TabContentWrapper, ViewWrapper } from '#views/Jobs/ListView/styles';
import { MenuItem } from '@material-ui/core';
import { ArrowDropDown, Search } from '@material-ui/icons';
import ArchiveOutlinedIcon from '@material-ui/icons/ArchiveOutlined';
import EditOutlinedIcon from '@material-ui/icons/EditOutlined';
import { RouteComponentProps, navigate } from '@reach/router';
import { debounce, startCase } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import ObjectList from '../Objects/ObjectList';
import {
  archiveObjectTypeProperty,
  archiveObjectTypeRelation,
  fetchObjectType,
  fetchObjectTypes,
  resetOntology,
} from '../actions';
import { PropertyFlags } from '../utils';
import AssetLogs from './Components/AssetLogs';
import AddPropertyDrawer from './Components/PropertyDrawer';
import AddRelationDrawer from './Components/RelationDrawer';

// TODO change this enum to Object and have positions defined explicity
export enum FlagPositions {
  SYSTEM,
  PRIMARY,
  TITLE,
  SEARCHABLE,
  MANDATORY,
  AUTOGENERATE,
}
// TODO move this to utils & expose methods for each flag specifically.
export const getBooleanFromDecimal = (flag: number, pos: number) => {
  return ((flag >> pos) & 1) === 1;
};

interface ReadOnlyGroupWrapperProps {
  minWidth?: string;
}

const ReadOnlyGroupWrapper = styled.div<ReadOnlyGroupWrapperProps>`
  display: flex;
  flex-direction: column;
  padding: 24px 16px;

  .read-only {
    display: flex;
    justify-content: space-between;
    margin-bottom: 24px;

    :last-child {
      margin-bottom: 0px;
    }

    .content {
      font-size: 14x;
      line-height: 1.14;
      color: rgba(51, 51, 51, 1);
      min-width: ${(props) => props.minWidth || '150px'};
      display: flex;
      align-items: flex-start;

      :last-child {
        flex: 1;
        padding-right: 20px;

        ::before {
          content: ':';
          margin-right: 24px;
        }
      }
    }
  }
`;

export const ReadOnlyGroup = ({
  items,
  ...rest
}: {
  items: { label: string; value: string | React.ReactNode }[];
} & React.HTMLProps<HTMLDivElement>) => {
  return (
    <ReadOnlyGroupWrapper {...rest}>
      {items.map((item, index) => (
        <div className="read-only" key={index}>
          <span className="content">{item.label}</span>
          <span className="content">{item.value}</span>
        </div>
      ))}
    </ReadOnlyGroupWrapper>
  );
};

const GeneralWrapper = styled.div`
  overflow: auto;

  h4 {
    padding: 16px;
    font-size: 20px;
    font-weight: 600;
    color: #333333;
    margin: unset;
    border-bottom: solid 1px #dadada;
  }

  .view {
    background: #fff;
    margin-bottom: 24px;
  }
`;

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  sort: 'createdAt,desc',
  usageStatus: 1,
};

const GeneralTabContent: FC<TabContentProps> = () => {
  const active = useTypedSelector((state) => state.ontology.objectTypes.active);

  if (!active) return null;
  return (
    <GeneralWrapper>
      <div className="view">
        <h4>Basic Information</h4>
        <ReadOnlyGroup
          items={[
            {
              label: 'ID',
              value: active.externalId,
            },
            {
              label: 'Display Name',
              value: active.displayName,
            },
            {
              label: 'Plural Name',
              value: active.pluralName,
            },
            {
              label: 'Description',
              value: active.description,
            },
            {
              label: 'Added On',
              value: formatDateTime({ value: active.createdAt }),
            },
          ]}
        />
      </div>
    </GeneralWrapper>
  );
};

const PropertiesTabContent: FC<TabContentProps> = ({ values }) => {
  const active = useTypedSelector((state) => state.ontology.objectTypes.active);

  const objectTypeId = active?.id;
  const { list, reset, pagination, status } = createFetchList(
    apigetObjectTypeProperties(objectTypeId),
    urlParams,
    false,
  );

  const [createPropertyDrawer, setCreatePropertyDrawer] = useState<string | boolean>('');
  const dispatch = useDispatch();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedProperty, setSelectedProperty] = useState(null);
  const [filters, setFilters] = useState<Record<string, any>>(urlParams);
  const { setShouldToggle } = values;

  const handleClose = () => {
    setAnchorEl(null);
    setTimeout(() => setSelectedProperty(null), 200);
  };

  useEffect(() => {
    reset({ params: { ...filters } });
  }, [filters]);

  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <TextInput
            afterElementWithoutError
            AfterElement={Search}
            afterElementClass=""
            placeholder={`Search with Property Name`}
            onChange={debounce(
              ({ value }) => setFilters({ ...filters, displayName: value ? value : undefined }),
              500,
            )}
          />
          <ToggleSwitch
            checkedIcon={false}
            uncheckedIcon={false}
            offLabel="Show Archived"
            onLabel="Showing Archived"
            checked={filters.usageStatus === 7}
            onChange={(value) => {
              setFilters({ ...filters, usageStatus: value ? 7 : 1 });
            }}
          />
        </div>
        {checkPermission(['ontology', 'createObjectType']) && (
          <div className="actions">
            <Button
              onClick={() => {
                setCreatePropertyDrawer(true);
              }}
            >
              Create New Property
            </Button>
          </div>
        )}
      </div>

      <LoadingContainer
        loading={status === 'loading' || status === 'loadingNext'}
        component={
          <DataTable
            columns={[
              {
                id: 'displayName',
                label: 'Property Name',
                minWidth: 100,
              },
              {
                id: 'inputType',
                label: 'Input Type',
                minWidth: 100,
                format: (item) => {
                  const contentString = (inputType: string) => {
                    switch (inputType) {
                      case MandatoryParameter.SINGLE_LINE:
                        return 'Single Line';
                      case MandatoryParameter.MULTI_LINE:
                        return 'Multi Line';
                      case MandatoryParameter.DATE:
                        return 'Date';
                      case MandatoryParameter.DATE_TIME:
                        return 'Date Time';
                      case MandatoryParameter.NUMBER:
                        return 'Number';
                      case MandatoryParameter.SINGLE_SELECT:
                        return 'Single Select';
                      case MandatoryParameter.MULTISELECT:
                      case 'MULTI_SELECT':
                        return 'Multi Select';
                      default:
                        return '';
                    }
                  };
                  return contentString(item.inputType);
                },
              },
              {
                id: 'mandatory',
                label: 'Is Mandatory?',
                minWidth: 100,
                format: (item) =>
                  getBooleanFromDecimal(item.flags, FlagPositions.MANDATORY) ? 'Yes' : 'No',
              },
              {
                id: 'status',
                label: 'Status',
                minWidth: 100,
                format: (item) => (item?.usageStatus === 1 ? 'Active' : 'Inactive'),
              },
              ...(checkPermission(['ontology', 'editObjectType'])
                ? [
                    {
                      id: 'action',
                      label: 'Action',
                      minWidth: 100,
                      format: function renderComp(item) {
                        return item.usageStatus === 1 &&
                          ![PropertyFlags.EXTERNAL_ID, PropertyFlags.SYSTEM].includes(
                            item.flags,
                          ) ? (
                          <div style={{ display: 'flex', gap: 16 }}>
                            <div
                              id="more-actions"
                              onClick={(event: any) => {
                                setAnchorEl(event.currentTarget);
                                setSelectedProperty(item);
                              }}
                            >
                              More <ArrowDropDown className="icon" fontSize="small" />
                            </div>

                            <ListActionMenu
                              id="row-more-actions"
                              anchorEl={anchorEl}
                              keepMounted
                              disableEnforceFocus
                              open={Boolean(anchorEl)}
                              onClose={handleClose}
                            >
                              {[PropertyFlags.MANDATORY, PropertyFlags.OPTIONAL]?.includes(
                                selectedProperty?.flags,
                              ) && (
                                <MenuItem
                                  onClick={() => {
                                    handleClose();
                                    dispatch(
                                      openOverlayAction({
                                        type: OverlayNames.REASON_MODAL,
                                        props: {
                                          modalTitle: 'Archive Property',
                                          modalDesc: `Are you sure you want to archive this property?`,
                                          onSubmitHandler: (
                                            reason: string,
                                            setFormErrors: (errors?: Error[]) => void,
                                          ) => {
                                            dispatch(
                                              archiveObjectTypeProperty({
                                                objectTypeId: active?.id,
                                                propertyId: selectedProperty?.id,
                                                reason,
                                                setFormErrors,
                                                fetchProperties: () =>
                                                  reset({ params: { ...filters } }),
                                              }),
                                            );
                                          },
                                          onSubmitModalText: 'Archive',
                                        },
                                      }),
                                    );
                                  }}
                                >
                                  <div className="list-item">
                                    <ArchiveOutlinedIcon />
                                    <span>Archive</span>
                                  </div>
                                </MenuItem>
                              )}
                              {[
                                PropertyFlags.MANDATORY,
                                PropertyFlags.DISPLAY_NAME,
                                PropertyFlags.OPTIONAL,
                              ]?.includes(selectedProperty?.flags) && (
                                <MenuItem
                                  onClick={() => {
                                    setAnchorEl(null);
                                    setCreatePropertyDrawer('Edit');
                                  }}
                                >
                                  <div className="list-item">
                                    <EditOutlinedIcon />
                                    <span>Edit</span>
                                  </div>
                                </MenuItem>
                              )}
                            </ListActionMenu>
                          </div>
                        ) : null;
                      },
                    },
                  ]
                : []),
            ]}
            emptyTitle="No Properties Found"
            rows={list}
          />
        }
      />
      <Pagination
        pageable={pagination}
        fetchData={(p) => reset({ params: { page: p.page, size: p.size } })}
      />
      {createPropertyDrawer && (
        <AddPropertyDrawer
          label={createPropertyDrawer}
          onCloseDrawer={setCreatePropertyDrawer}
          property={selectedProperty}
          setSelectedProperty={setSelectedProperty}
          setShouldToggle={setShouldToggle}
        />
      )}
    </TabContentWrapper>
  );
};

const RelationsTabContent: FC<TabContentProps> = ({ values }) => {
  const active = useTypedSelector((state) => state.ontology.objectTypes.active);

  const objectTypeId = active?.id;
  const { list, reset, pagination, status } = createFetchList(
    apiGetObjectTypeRelations(objectTypeId),
    urlParams,
    false,
  );

  const [createRelationDrawer, setRelationDrawer] = useState<string | boolean>('');
  const dispatch = useDispatch();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedRelation, setSelectedRelation] = useState(null);
  const [filters, setFilters] = useState<Record<string, any>>(urlParams);
  const { setShouldToggle } = values;

  const handleClose = () => {
    setAnchorEl(null);
    setTimeout(() => setSelectedRelation(null), 200);
  };

  useEffect(() => {
    reset({ params: { ...filters } });
  }, [filters]);

  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <TextInput
            afterElementWithoutError
            AfterElement={Search}
            afterElementClass=""
            placeholder={`Search with Relation Name`}
            onChange={debounce(
              ({ value }) => setFilters({ ...filters, displayName: value ? value : undefined }),
              500,
            )}
          />
          <ToggleSwitch
            checkedIcon={false}
            uncheckedIcon={false}
            offLabel="Show Archived"
            onLabel="Showing Archived"
            checked={filters.usageStatus === 7}
            onChange={(value) => {
              setFilters({ ...filters, usageStatus: value ? 7 : 1 });
            }}
          />
        </div>
        {checkPermission(['ontology', 'createObjectType']) && (
          <div className="actions">
            <Button
              onClick={() => {
                setRelationDrawer(true);
              }}
            >
              Create New Relation
            </Button>
          </div>
        )}
      </div>
      <LoadingContainer
        loading={status === 'loading' || status === 'loadingNext'}
        component={
          <DataTable
            columns={[
              {
                id: 'relatedTo',
                label: 'Related To',
                minWidth: 100,
                format: (item) => {
                  return startCase(item.externalId);
                },
              },
              {
                id: 'displayName',
                label: 'Relation Name',
                minWidth: 100,
              },
              {
                id: 'cardinality',
                label: 'Cardinality',
                minWidth: 100,
                format: (item) => {
                  const contentString = (cardinality: string) => {
                    switch (cardinality) {
                      case 'ONE_TO_ONE':
                        return 'One to One';
                      case 'ONE_TO_MANY':
                        return 'One to Many';
                      default:
                        return '';
                    }
                  };
                  return contentString(item?.target?.cardinality);
                },
              },

              {
                id: 'status',
                label: 'Status',
                minWidth: 100,
                format: (item) => (item?.usageStatus === 1 ? 'Active' : 'Inactive'),
              },
              ...(checkPermission(['ontology', 'editObjectType'])
                ? [
                    {
                      id: 'action',
                      label: 'Action',
                      minWidth: 100,
                      format: function renderComp(item) {
                        return item?.usageStatus === 1 ? (
                          <div style={{ display: 'flex', gap: 16 }}>
                            <div
                              id="more-actions"
                              onClick={(event: any) => {
                                setAnchorEl(event.currentTarget);
                                setSelectedRelation(item);
                              }}
                            >
                              More <ArrowDropDown className="icon" fontSize="small" />
                            </div>

                            <ListActionMenu
                              id="row-more-actions"
                              anchorEl={anchorEl}
                              keepMounted
                              disableEnforceFocus
                              open={Boolean(anchorEl)}
                              onClose={handleClose}
                            >
                              <MenuItem
                                onClick={() => {
                                  handleClose();
                                  dispatch(
                                    openOverlayAction({
                                      type: OverlayNames.REASON_MODAL,
                                      props: {
                                        modalTitle: 'Archive Relation',
                                        modalDesc: `Are you sure you want to archive this relation?`,
                                        onSubmitHandler: (
                                          reason: string,
                                          setFormErrors: (errors?: Error[]) => void,
                                        ) => {
                                          dispatch(
                                            archiveObjectTypeRelation({
                                              objectTypeId: active?.id,
                                              relationId: selectedRelation?.id,
                                              reason,
                                              setFormErrors,
                                              fetchRelations: () =>
                                                reset({ params: { ...filters } }),
                                            }),
                                          );
                                        },
                                        onSubmitModalText: 'Archive',
                                      },
                                    }),
                                  );
                                }}
                              >
                                <div className="list-item">
                                  <ArchiveOutlinedIcon />
                                  <span>Archive</span>
                                </div>
                              </MenuItem>
                              <MenuItem
                                onClick={() => {
                                  setAnchorEl(null);
                                  setRelationDrawer('Edit');
                                }}
                              >
                                <div className="list-item">
                                  <EditOutlinedIcon />
                                  <span>Edit</span>
                                </div>
                              </MenuItem>
                            </ListActionMenu>
                          </div>
                        ) : null;
                      },
                    },
                  ]
                : []),
            ]}
            emptyTitle="No Relations Found"
            rows={list}
          />
        }
      />
      <Pagination
        pageable={pagination}
        fetchData={(p) => reset({ params: { page: p.page, size: p.size } })}
      />
      {createRelationDrawer && (
        <AddRelationDrawer
          label={createRelationDrawer}
          onCloseDrawer={setRelationDrawer}
          relation={selectedRelation}
          setSelectedRelation={setSelectedRelation}
          setShouldToggle={setShouldToggle}
        />
      )}
    </TabContentWrapper>
  );
};

const QrParserTabContent: FC<TabContentProps> = ({ values }) => {
  const active = useTypedSelector((state) => state.ontology.objectTypes.active);

  const objectTypeId = active?.id;

  const qrParserFilter = {
    op: FilterOperators.AND,
    fields: [
      { field: 'objectTypeId', op: FilterOperators.EQ, values: [objectTypeId] },
      { field: 'usageStatus', op: FilterOperators.EQ, values: [1] },
      { field: 'displayName', op: FilterOperators.LIKE, values: [''] },
    ],
  };

  const dispatch = useDispatch();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedQrCodeParser, setSelectedQrCodeParser] = useState(null);
  const [filters, setFilters] = useState<Record<string, any>>({
    ...urlParams,
    filters: qrParserFilter,
  });
  const { shouldToggle, setShouldToggle } = values;

  const { list, reset, pagination } = createFetchList(apiQrCodeParsers(), filters, false);

  const handleClose = () => {
    setAnchorEl(null);
    setTimeout(() => setSelectedQrCodeParser(null), 200);
  };

  const fetchData = (page: number) => {
    dispatch(
      fetchObjectTypes({
        page,
        size: 256,
        usageStatus: 1,
      }),
    );
  };

  const toggleArchiveUnarchiveQrCodeParser = async (
    isQrCodeParserActive,
    qrCodeParserId,
    reason,
    setFormErrors,
  ) => {
    try {
      const { data, errors }: ResponseObj<any> = await request(
        'PATCH',
        isQrCodeParserActive
          ? apiArchiveObjectTypeQrCodeParser(qrCodeParserId)
          : apiUnarchiveObjectTypeQrCodeParser(qrCodeParserId),
        {
          data: { reason },
        },
      );
      if (data) {
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: isQrCodeParserActive
              ? 'QR Code Parser Archived successfully'
              : 'QR Code Parser Unarchived successfully',
          }),
        );
        setShouldToggle((prev) => !prev);
        setFormErrors(errors);
      } else if (errors) {
        throw getErrorMsg(errors);
      }
    } catch (error) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: typeof error === 'string' ? error : 'Oops! Please Try Again.',
        }),
      );
    }
  };

  useEffect(() => {
    reset({ params: { ...filters } });
  }, [filters, shouldToggle]);

  useEffect(() => {
    fetchData(0);
  }, []);

  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <TextInput
            afterElementWithoutError
            AfterElement={Search}
            afterElementClass=""
            placeholder={`Search by Name`}
            onChange={debounce(({ value }) => {
              setFilters((prevFilters) => ({
                ...prevFilters,
                filters: {
                  ...prevFilters.filters,
                  fields: prevFilters.filters.fields.map((field) =>
                    field.field === 'displayName'
                      ? { ...field, op: FilterOperators.LIKE, values: [value] }
                      : field,
                  ),
                },
              }));
            }, 500)}
          />
          <ToggleSwitch
            checkedIcon={false}
            uncheckedIcon={false}
            offLabel="Show Archived"
            onLabel="Showing Archived"
            checked={
              filters?.filters?.fields?.find((field) => field.field === 'usageStatus')
                ?.values[0] === 7
            }
            onChange={(value) => {
              setFilters((prevFilters) => ({
                ...prevFilters,
                filters: {
                  ...prevFilters.filters,
                  fields: prevFilters.filters.fields.map((field) =>
                    field.field === 'usageStatus' ? { ...field, values: [value ? 7 : 1] } : field,
                  ),
                },
              }));
            }}
          />
        </div>
        {checkPermission(['ontology', 'createObject']) && (
          <div className="actions">
            <Button
              onClick={() => {
                navigate(`/ontology/object-types/${objectTypeId}/parser/new`);
              }}
            >
              Create New QR Code Parser
            </Button>
          </div>
        )}
      </div>
      <DataTable
        columns={[
          {
            id: 'name',
            label: 'Name',
            minWidth: 100,
            format: (item) => item.displayName,
          },
          {
            id: 'createdAt',
            label: 'Created At',
            minWidth: 100,
            format: (item) => formatDateTime({ value: item.createdAt }),
          },
          {
            id: 'lastModified',
            label: 'Last Modified',
            minWidth: 100,
            format: (item) => formatDateTime({ value: item.modifiedAt }),
          },

          ...(checkPermission(['ontology', 'createObject'])
            ? [
                {
                  id: 'action',
                  label: 'Action',
                  minWidth: 100,
                  format: function renderComp(item) {
                    const isQrCodeParserActive = item.usageStatus === 1;
                    return (
                      <div style={{ display: 'flex', gap: 16 }}>
                        <div
                          id="more-actions"
                          onClick={(event: any) => {
                            setAnchorEl(event.currentTarget);
                            setSelectedQrCodeParser(item);
                          }}
                        >
                          More <ArrowDropDown className="icon" fontSize="small" />
                        </div>

                        <ListActionMenu
                          id="row-more-actions"
                          anchorEl={anchorEl}
                          keepMounted
                          disableEnforceFocus
                          open={Boolean(anchorEl)}
                          onClose={handleClose}
                        >
                          <MenuItem
                            onClick={() => {
                              handleClose();
                              dispatch(
                                openOverlayAction({
                                  type: OverlayNames.REASON_MODAL,
                                  props: {
                                    modalTitle: isQrCodeParserActive
                                      ? 'Archive Parser'
                                      : 'Unarchive Parser',
                                    modalDesc: `Are you sure you want to ${
                                      isQrCodeParserActive ? `archive` : `unarchive`
                                    } this parser?`,
                                    onSubmitHandler: (
                                      reason: string,
                                      setFormErrors: (errors?: Error[]) => void,
                                    ) => {
                                      toggleArchiveUnarchiveQrCodeParser(
                                        isQrCodeParserActive,
                                        selectedQrCodeParser?.id,
                                        reason,
                                        setFormErrors,
                                      );
                                    },
                                    onSubmitModalText: isQrCodeParserActive
                                      ? 'Archive'
                                      : 'Unarchive',
                                  },
                                }),
                              );
                            }}
                          >
                            <div className="list-item">
                              {isQrCodeParserActive ? <ArchiveOutlinedIcon /> : <MemoArchive />}
                              <span>{isQrCodeParserActive ? 'Archive' : 'Unarchive'}</span>
                            </div>
                          </MenuItem>
                          {isQrCodeParserActive ? (
                            <MenuItem
                              onClick={() => {
                                setAnchorEl(null);
                                navigate(
                                  `/ontology/object-types/${objectTypeId}/parser/${selectedQrCodeParser?.id}`,
                                );
                              }}
                            >
                              <div className="list-item">
                                <EditOutlinedIcon />
                                <span>Edit</span>
                              </div>
                            </MenuItem>
                          ) : null}
                        </ListActionMenu>
                      </div>
                    );
                  },
                },
              ]
            : []),
        ]}
        emptyTitle="No Qr Code Parsers Found"
        rows={list}
      />
      <Pagination
        pageable={pagination}
        fetchData={(p) => reset({ params: { page: p.page, size: p.size } })}
      />
    </TabContentWrapper>
  );
};

const ObjectTypesContent = ({ id }: RouteComponentProps<{ id: string }>) => {
  const dispatch = useDispatch();

  const active = useTypedSelector((state) => state.ontology.objectTypes.active);
  const activeLoading = useTypedSelector((state) => state.ontology.objectTypes.activeLoading);

  const [shouldToggle, setShouldToggle] = useState(false);

  useEffect(() => {
    if (id) {
      dispatch(fetchObjectType(id));
    }

    return () => {
      dispatch(resetOntology(['objectTypes', 'activeLoading']));
    };
  }, [shouldToggle]);

  const { renderTabHeader, renderTabContent } = useTabs({
    tabs: [
      {
        label: 'Objects',
        tabContent: ObjectList,
      },
      { label: 'Basic Information', tabContent: GeneralTabContent },
      {
        label: 'Properties',
        tabContent: PropertiesTabContent,
        values: { shouldToggle, setShouldToggle },
      },
      {
        label: 'Relations',
        tabContent: RelationsTabContent,
        values: { shouldToggle, setShouldToggle },
      },
      ...(isFeatureAllowed('createObjectFromQR')
        ? [
            {
              label: 'QR Code Parsers',
              tabContent: QrParserTabContent,
              values: { shouldToggle, setShouldToggle },
            },
          ]
        : []),
      {
        label: 'Assets Logs',
        tabContent: AssetLogs,
      },
    ],
  });

  return (
    <ViewWrapper>
      <GeneralHeader
        heading={`Object Types - ${activeLoading ? '...' : active?.pluralName}`}
        showBackButton={true}
        onBackButtonClick={() => {
          navigate('/ontology');
        }}
      />
      <div className="list-table">
        {renderTabHeader()}
        <LoadingContainer loading={activeLoading} component={<>{renderTabContent()}</>} />
      </div>
    </ViewWrapper>
  );
};

export default ObjectTypesContent;
