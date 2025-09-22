import MemoArchive from '#assets/svg/Archive';
import filterIcon from '#assets/svg/FilterIcon.svg';
import qrParserScannerIcon from '#assets/svg/qr-parser-scanner.svg';
import {
  Button,
  DataTable,
  ImageUploadButton,
  ListActionMenu,
  LoadingContainer,
  Pagination,
  SearchFilter,
  TabContentProps,
  ToggleSwitch,
} from '#components';
import { NestedSelect } from '#components/shared/NestedSelect';
import styled from 'styled-components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { DataTableColumn } from '#components/shared/DataTable';
import Tooltip from '#components/shared/Tooltip';
import { useQueryParams } from '#hooks/useQueryParams';
import checkPermission, { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { ALL_FACILITY_ID, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, InputTypes, fetchDataParams } from '#utils/globalTypes';
import { formatDateTime } from '#utils/timeUtils';
import { getErrorMsg, request } from '#utils/request';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { MenuItem } from '@material-ui/core';
import { ArrowDropDown, CropFree, GetAppOutlined, PublishOutlined, Tune } from '@material-ui/icons';
import { navigate, useLocation } from '@reach/router';
import { getUnixTime } from 'date-fns';
import React, { FC, MouseEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import {
  archiveObject,
  fetchObjects,
  fetchQrShortCodeData,
  setActiveObject,
  unarchiveObject,
} from '../actions';
import { apiExportObjects, apiImportObjects } from '#utils/apiUrls';
import { Choice, ObjectTypeProperty, ObjectTypeRelation, TObject } from '../types';
import AddEditObjectDrawer from './components/AddEditObjectDrawer';
import ObjectFiltersDrawer from './Overlays/ObjectFiltersDrawer';
import { isArray } from 'lodash';

// Styled component for the primary button-like label
const PrimaryButtonLabel = styled.div`
  background-color: #1d84ff;
  color: #ffffff;
  border: 1px solid transparent;
  padding-block: 12px;
  padding-inline: 24px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:hover {
    background-color: #005dcc;
  }

  &:active {
    background-color: #00387a;
  }
`;

const ObjectList: FC<TabContentProps> = () => {
  const dispatch = useDispatch();
  const location = useLocation();
  const { getQueryParam, updateQueryParams } = useQueryParams();
  const page = getQueryParam('page');
  const objectFilters = getQueryParam('filters');
  const importButtonRef = useRef<HTMLDivElement>(null);
  const {
    objectTypes: { active },
    objects: { list, listLoading, pageable },
  } = useTypedSelector((state) => state.ontology);
  const selectedFacility = useTypedSelector((state) => state.auth.selectedFacility);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedObject, setSelectedObject] = useState<(TObject & Record<string, any>) | null>(
    null,
  );
  const [filters, setFilters] = useState<Record<string, string | number>>({
    usageStatus: 1,
  });

  const [showAddEditObjectDrawer, setShowAddEditObjectDrawer] = useState(false);
  const [showFiltersDrawer, setShowFiltersDrawer] = useState(false);
  const properties = useMemo(
    () => (active?.properties ? active.properties.sort((a, b) => a.sortOrder - b.sortOrder) : []),
    [active?.properties],
  );

  const relations = useMemo(
    () => (active?.relations ? active.relations.sort((a, b) => a.sortOrder - b.sortOrder) : []),
    [active?.relations],
  );

  const handleClose = () => {
    setAnchorEl(null);
  };

  const fetchData = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE } = params;
    const { usageStatus, filters: appliedFilters } = filters;
    const updatedFilters = appliedFilters
      ? {
          ...appliedFilters,
          fields: appliedFilters.fields.map(({ value, ...rest }) => rest),
        }
      : {};

    dispatch(
      fetchObjects({
        page,
        size,
        collection: active?.externalId,
        usageStatus,
        ...(appliedFilters && { filters: updatedFilters }),
      }),
    );
  };

  useEffect(() => {
    fetchData({ page });
  }, [filters, page]);

  useEffect(() => {
    if (objectFilters) {
      const filters = JSON.parse(objectFilters);
      setFilters(filters);
    }
  }, []);

  const formatPropertyValue = (
    property: ObjectTypeProperty | ObjectTypeRelation,
    value?: string | Choice[],
  ) => {
    let formattedValue = '-';
    if (value) {
      if (Array.isArray(value)) {
        formattedValue = value.map((option) => option.displayName).join(', ');
      } else if (
        'inputType' in property &&
        [InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(property.inputType)
      ) {
        formattedValue = formatDateTime({ value, type: property.inputType });
      } else {
        formattedValue = value;
      }
    }
    return formattedValue;
  };

  const createColumnValue = (label: string, item: any, primary = false) =>
    primary ? (
      <span
        className="primary"
        title={label}
        onClick={() => {
          dispatch(setActiveObject(item));
          navigate(`${location.pathname}/objects/${item.id}`);
        }}
      >
        {label}
      </span>
    ) : (
      label
    );

  const createPropertyColumn = (property: ObjectTypeProperty) => {
    const isPrimary = property.externalId === 'displayName';
    return {
      id: property.id,
      label: property.displayName,
      minWidth: 100,
      format: (item: Record<string, string | Choice[] | undefined>) => {
        return createColumnValue(
          formatPropertyValue(property, item?.[property.id]),
          item,
          isPrimary,
        );
      },
      sortOrder: property.sortOrder,
      type: 'PROPERTY',
      usageStatus: property?.usageStatus,
    };
  };

  const handlePrintQRCode = () => {
    const prevIframe = document.getElementById('qr-iframe');
    if (prevIframe) {
      document.body.removeChild(prevIframe);
    }
    const qrPdfPropertiesAllowed = isFeatureAllowed('qrPdfProperties');
    const iFrame = document.createElement('iframe');
    iFrame.setAttribute('id', 'qr-iframe');
    iFrame.setAttribute('style', 'height: 0px; width: 0px; position: absolute');
    document.body.appendChild(iFrame);
    const qrCode = document.getElementById('QRCode');
    const contentWindow = iFrame.contentWindow;
    if (qrCode && contentWindow) {
      const container = contentWindow.document.createElement('div');
      container.setAttribute(
        'style',
        `height: 100%; padding-block: 16px; width: 100%; gap: 16px; display: flex; align-items: center; flex-direction: column;  flex: 1; justify-content: ${
          qrPdfPropertiesAllowed ? 'flex-start' : 'center'
        };`,
      );
      container.innerHTML = `<span>Object Name : ${selectedObject?.displayName} (ID: ${selectedObject?.externalId})</span>`;
      const qrWrapper = contentWindow.document.createElement('div');
      qrWrapper.setAttribute(
        'style',
        'padding: 8px; display: flex; justify-content: center; align-items: center;',
      );
      qrWrapper.appendChild(qrCode); // move the SVG into the wrapper
      container.appendChild(qrWrapper);
      if (qrPdfPropertiesAllowed) {
        const propertiesTable = document.createElement('table');
        const propertiesTableCaption = propertiesTable.createCaption();
        propertiesTableCaption.innerHTML = `Properties (As of ${formatDateTime({
          value: getUnixTime(new Date()),
        })}`;
        propertiesTableCaption.setAttribute('style', 'padding: 8px;');
        propertiesTable.setAttribute(
          'style',
          'width: calc(100% - 32px); border: 0.5px solid #000; border-spacing: 0;',
        );
        const cellStyles = 'border: 0.5px solid #000; padding: 4px;';
        [...properties, ...relations].forEach((property) => {
          if (!['externalId', 'displayName'].includes(property.externalId)) {
            const row = propertiesTable.insertRow();
            const cell1 = row.insertCell(0);
            const cell2 = row.insertCell(1);
            cell1.setAttribute('style', cellStyles);
            cell2.setAttribute('style', cellStyles);
            cell1.innerHTML = property.displayName;
            cell2.innerHTML = formatPropertyValue(property, selectedObject?.[property.id]);
          }
        });
        container.appendChild(propertiesTable);
      }
      contentWindow.document.open();
      contentWindow.document.appendChild(container);
      contentWindow.document.close();
      contentWindow.focus();
      contentWindow.print();
    }
  };

  const handleQrScanParser = (qrCodeData, qrCodeParser) => {
    const rawData = qrCodeData.split(qrCodeParser.delimiter);
    if (rawData?.length === qrCodeParser?.rules?.length) {
      const properties: any[] = [];
      qrCodeParser.rules.forEach((rule, index) => {
        const currentRuleData = rawData[index];
        const result =
          currentRuleData?.substring(Number(rule?.startPos) - 1, Number(rule?.endPos)) || '';

        const currentProperty = active?.properties.find((p) => p.id === rule.propertyId);

        if (currentProperty) {
          properties.push({
            id: currentProperty.id,
            externalId: currentProperty.externalId,
            displayName: currentProperty.displayName,
            value: result, // finalDateEpoch to be used here for date, date-time property.
            choices: [],
          });
        }
      });

      dispatch(
        setActiveObject({
          properties,
        } as any),
      );
      setShowAddEditObjectDrawer(true);
    } else {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: 'The template does not match the QR code data.',
        }),
      );
    }
  };

  const createRelationColumns = (relations) =>
    relations.map((relation) => ({
      id: relation.id,
      label: relation.displayName,
      minWidth: 100,
      format: function renderComp(item) {
        return (
          <>
            {item[relation.id]?.length > 0 ? (
              item[relation.id].map((value, index) => (
                <div key={index}>
                  <div
                    style={{ cursor: 'pointer' }}
                    className="primary"
                    onClick={() =>
                      navigate(
                        `/ontology/object-types/${relation.objectTypeId}/objects/${value?.id}`,
                      )
                    }
                  >
                    {value?.displayName} {`(ID: ${value?.externalId})`}
                    {index === item[relation.id].length - 1 ? '' : ', '}
                  </div>
                </div>
              ))
            ) : (
              <div>-</div>
            )}
          </>
        );
      },
      sortOrder: relation.sortOrder,
      type: 'RELATION',
      usageStatus: relation?.usageStatus,
    }));

  const columns = [
    ...properties.reduce<DataTableColumn[]>((acc, property) => {
      if (property.flags !== 1) {
        const propertyColumn = createPropertyColumn(property);
        if (property.externalId === 'displayName') {
          acc.splice(0, 0, propertyColumn);
        } else if (property.externalId === 'externalId') {
          acc.splice(1, 0, propertyColumn);
        } else {
          acc.push(propertyColumn);
        }
      }
      return acc;
    }, []),
    ...createRelationColumns(relations),
    {
      id: 'actions',
      label: 'Actions',
      minWidth: 100,
      format: function renderComp(item: TObject) {
        return (
          <>
            <div
              id="more-actions"
              onClick={(event: MouseEvent<HTMLDivElement>) => {
                setAnchorEl(event.currentTarget);
                setSelectedObject(item);
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
              {checkPermission(['ontology', 'archiveObject']) && (
                <MenuItem
                  onClick={() => {
                    handleClose();
                    if (selectedObject?.id)
                      dispatch(
                        openOverlayAction({
                          type: OverlayNames.REASON_MODAL,
                          props: {
                            modalTitle:
                              selectedObject?.usageStatus === 7
                                ? 'Unarchive Object'
                                : 'Archive Object',
                            modalDesc: `Provide details for ${
                              selectedObject?.usageStatus === 7 ? 'unarchiving' : 'archiving'
                            } the Object`,
                            onSubmitHandler: (
                              reason: string,
                              setFormErrors: (errors?: Error[]) => void,
                            ) => {
                              selectedObject?.usageStatus === 7
                                ? dispatch(
                                    unarchiveObject(
                                      selectedObject?.id,
                                      reason,
                                      setFormErrors,
                                      active?.externalId!,
                                    ),
                                  )
                                : dispatch(
                                    archiveObject(
                                      selectedObject.id,
                                      reason,
                                      setFormErrors,
                                      active?.externalId!,
                                    ),
                                  );
                            },
                            onSubmitModalText:
                              selectedObject?.usageStatus === 7 ? 'Unarchive' : 'Archive',
                          },
                        }),
                      );
                  }}
                >
                  <div className="list-item">
                    <MemoArchive />
                    <span>
                      {selectedObject?.usageStatus === 7 ? 'Unarchive Object' : 'Archive Object'}
                    </span>
                  </div>
                </MenuItem>
              )}
              <MenuItem
                onClick={() => {
                  handleClose();
                  dispatch(
                    fetchQrShortCodeData({
                      object: selectedObject,
                      handlePrintQRCode,
                    }),
                  );
                }}
              >
                <div className="list-item">
                  <CropFree />
                  <span>View QR Code</span>
                </div>
              </MenuItem>
            </ListActionMenu>
          </>
        );
      },
    },
  ];

  const rows =
    list.map((object) => ({
      ...object,
      ...object?.properties?.reduce<Record<string, string | Choice[] | undefined>>(
        (acc, property) => {
          acc[property.id] = property.choices?.length ? property.choices : property.value;
          return acc;
        },
        {},
      ),
      ...object?.relations?.reduce<Record<string, string>>((acc, relation) => {
        acc[relation.id] = relation.targets.map((target) => ({
          id: target.id,
          displayName: target.displayName,
          externalId: target.externalId,
        }));
        return acc;
      }, {}),
    })) || [];

  const sortedColumns = useMemo(() => {
    return columns
      .filter((column) => column.usageStatus !== 7)
      .sort((a, b) => a.sortOrder - b.sortOrder);
  }, [columns]);

  const filterKey = useMemo(() => {
    return properties
      ?.filter(
        (property) =>
          property?.externalId === 'displayName' || property?.externalId === 'externalId',
      )
      ?.map((property) => {
        return {
          label: property?.displayName,
          value: `searchable.${property?.id}`,
          field: `searchable.${property?.id}`,
          operator: FilterOperators.LIKE,
        };
      });
  }, [properties]);

  const onSubmit = (params, callback) => {
    const updatedFilterFields = (params?.filters || []).map((currFilter) => {
      let values;
      if (isArray(currFilter?.value)) {
        values = currFilter.value.map(
          (item: { value: string | number; label: string }) => item.value,
        );
      } else if (currFilter?.value?.value) {
        values = [currFilter.value.value];
      } else {
        values = [currFilter.value];
      }

      return {
        ...currFilter,
        values,
      };
    });

    const payload = {
      ...filters,
      filters: {
        op: FilterOperators.AND,
        fields: updatedFilterFields,
      },
    };
    updateQueryParams({
      newParams: {
        filters: payload,
        page: DEFAULT_PAGE_NUMBER,
      },
    });
    setFilters(payload);
    callback();
  };

  const toggleSwitchFn = (isChecked) => {
    const payload = {
      ...filters,
      usageStatus: isChecked ? 7 : 1,
    };
    navigate(`?filters=${JSON.stringify(payload)}`);
    setFilters(payload);
  };

  const getFilterFields = useCallback((prevFilter, fields, option) => {
    const filters = {
      ...prevFilter?.filters,
      fields: prevFilter?.filters?.fields?.filter((f) => prevFilter.searchKey !== f.field),
    };
    const searchKey = option?.value;

    if (filters?.fields?.length) {
      const isKeyPresent = filters.fields.some(
        (f) => f?.field === searchKey && f?.op === FilterOperators.LIKE,
      );

      const modifiedFields =
        isKeyPresent &&
        filters.fields.map((f) =>
          f?.field === searchKey && f?.op === FilterOperators.LIKE ? fields?.[0] : f,
        );
      const updatedFields =
        isKeyPresent && fields?.length
          ? modifiedFields.filter((f) => !!f)
          : [...filters.fields, ...fields];
      return {
        ...prevFilter,
        searchKey: option?.value || '',
        filters: {
          op: FilterOperators.AND,
          fields: updatedFields,
        },
      };
    } else {
      return {
        ...prevFilter,
        searchKey: option?.value || '',
        filters: {
          op: FilterOperators.AND,
          fields: fields,
        },
      };
    }
  }, []);

  const getPrefilledData = useCallback(() => {
    const fields = filters?.filters?.fields;
    const searchKey = filters?.searchKey;

    if (fields?.length || searchKey) {
      const filteredFields =
        fields?.find((f) => f?.field?.includes(`${searchKey}`) && f.op === FilterOperators.LIKE) ||
        [];
      const prefilledField = {
        field: filteredFields?.field || searchKey || '',
        value: filteredFields?.value || '',
      };
      return prefilledField;
    } else {
      return { field: searchKey || '', value: '' };
    }
  }, [filters]);

  const handleExport = async () => {
    if (!active?.id) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: 'No object type selected',
        }),
      );
      return;
    }

    try {
      const response = await request('GET', apiExportObjects(active.id), { responseType: 'blob' });

      if (!response) {
        throw new Error('Export failed: No response received');
      }

      const url = window.URL.createObjectURL(new Blob([response]));
      const link = document.createElement('a');
      link.href = url;

      link.setAttribute('download', `${active.displayName || 'object-type'}_export.xlsx`);
      document.body.appendChild(link);
      link.click();

      window.URL.revokeObjectURL(url);
      document.body.removeChild(link);

      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Export completed successfully',
        }),
      );
    } catch (error) {
      console.error('Export error:', error);
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: error instanceof Error ? error.message : 'Export failed',
        }),
      );
    }
  };

  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <SearchFilter
            showDropdown
            prefilledSearch={getPrefilledData()}
            dropdownOptions={filterKey}
            updateFilterFields={(fields, option) => {
              navigate(`?filters=${JSON.stringify(getFilterFields(filters, fields, option))}`);
              setFilters((prevFilter) => getFilterFields(prevFilter, fields, option));
            }}
          />
          <ToggleSwitch
            checkedIcon={false}
            uncheckedIcon={false}
            offLabel="Show Archived"
            onLabel="Showing Archived"
            checked={filters.usageStatus === 7}
            onChange={(isChecked) => {
              toggleSwitchFn(isChecked);
            }}
          />
          <div className="icon-filter" onClick={() => setShowFiltersDrawer(true)}>
            <img className="icon" src={filterIcon} />
            {filters?.filters?.fields?.length > 0 && (
              <span>{`(${filters?.filters?.fields?.length})`}</span>
            )}
          </div>
          <Button
            variant="textOnly"
            onClick={() => {
              const selectedColumns = sortedColumns.reduce((acc, column) => {
                if (column.id !== 'actions') {
                  acc.push({
                    id: column.id,
                    label: column.label,
                    format: column.format,
                    minWidth: column.minWidth,
                    sortOrder: column.sortOrder,
                    type: column.type,
                  });
                }
                return acc;
              }, []);
              dispatch(
                openOverlayAction({
                  type: OverlayNames.CONFIGURE_OBJECT_TYPE,
                  props: {
                    selectedColumns,
                    columns: sortedColumns || [],
                    objectTypeId: active?.id,
                  },
                }),
              );
            }}
          >
            <Tune />
            Configure Columns<span>{`(${sortedColumns.length - 1 || 0})`}</span>
          </Button>
          {isFeatureAllowed('bulkImportExport') &&
            checkPermission(['ontology', 'importExportObjects']) && (
              <Tooltip
                title={
                  selectedFacility?.id === ALL_FACILITY_ID
                    ? 'Export is not available for this facility'
                    : filters?.filters?.fields?.length > 0
                    ? 'Export is not available when filters are applied'
                    : ''
                }
                arrow
              >
                <span>
                  <Button
                    variant="textOnly"
                    onClick={handleExport}
                    disabled={
                      selectedFacility?.id === ALL_FACILITY_ID ||
                      filters?.filters?.fields?.length > 0
                    }
                  >
                    <PublishOutlined />
                    Export
                  </Button>
                </span>
              </Tooltip>
            )}
        </div>

        {checkPermission(['ontology', 'createObject']) && (
          <div className="actions">
            {isFeatureAllowed('bulkImportExport') &&
            checkPermission(['ontology', 'importExportObjects']) ? (
              <>
                <div style={{ display: 'none' }} ref={importButtonRef}>
                  <ImageUploadButton
                    icon={GetAppOutlined}
                    label="Import"
                    onUploadSuccess={() => {
                      dispatch(
                        showNotification({
                          type: NotificationType.SUCCESS,
                          msg: 'Objects Imported Successfully',
                        }),
                      );
                      fetchData({ page: pageable.page });
                    }}
                    onUploadError={(error) => {
                      dispatch(
                        showNotification({
                          type: NotificationType.ERROR,
                          msg: getErrorMsg(error) as string,
                        }),
                      );
                    }}
                    apiCall={() => apiImportObjects(active?.id || '')}
                    acceptedTypes={['.xls', '.xlsx']}
                  />
                </div>
                <NestedSelect
                  id="create-object-select"
                  items={{
                    createObject: {
                      label: 'Create',
                      value: 'createObject',
                    },
                    importObject: {
                      label: 'Import',
                      value: 'importObject',
                      disabled: selectedFacility?.id === ALL_FACILITY_ID,
                    },
                  }}
                  label={() => (
                    <PrimaryButtonLabel>
                      Create New <ArrowDropDown style={{ padding: 0 }} fontSize="inherit" />
                    </PrimaryButtonLabel>
                  )}
                  onChildChange={(option) => {
                    if (option.value === 'createObject') {
                      dispatch(setActiveObject());
                      setShowAddEditObjectDrawer(true);
                    } else if (
                      option.value === 'importObject' &&
                      selectedFacility?.id !== ALL_FACILITY_ID
                    ) {
                      const fileInput = importButtonRef.current?.querySelector(
                        'input[type="file"]',
                      ) as HTMLInputElement;
                      if (fileInput) {
                        fileInput.click();
                      }
                    }
                  }}
                />
              </>
            ) : (
              <Button
                onClick={() => {
                  dispatch(setActiveObject());
                  setShowAddEditObjectDrawer(true);
                }}
              >
                Create New
              </Button>
            )}
            {isFeatureAllowed('createObjectFromQR') && (
              <img
                src={qrParserScannerIcon}
                style={{ width: '42px', cursor: 'pointer' }}
                onClick={() => {
                  dispatch(
                    openOverlayAction({
                      type: OverlayNames.QR_CODE_PARSER_MODAL,
                      props: {
                        handleQrScanParser,
                      },
                    }),
                  );
                }}
              />
            )}
          </div>
        )}
      </div>
      <LoadingContainer
        loading={listLoading}
        component={
          <>
            <DataTable columns={sortedColumns} rows={rows} emptyTitle="No Objects Found" />
            <Pagination pageable={pageable} fetchData={true} />
          </>
        }
      />
      {active && showAddEditObjectDrawer && (
        <AddEditObjectDrawer
          onCloseDrawer={setShowAddEditObjectDrawer}
          values={{
            objectTypeId: active.id,
            id: 'new',
          }}
          onCreate={() => fetchData({ page: pageable.page })}
        />
      )}
      {active && showFiltersDrawer && (
        <ObjectFiltersDrawer
          setShowFiltersDrawer={setShowFiltersDrawer}
          activeObjectType={active}
          onSubmit={onSubmit}
          existingFilters={filters}
        />
      )}
    </TabContentWrapper>
  );
};

export default ObjectList;
