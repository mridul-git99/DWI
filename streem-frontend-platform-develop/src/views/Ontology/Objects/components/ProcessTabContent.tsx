import { Checklist } from '#PrototypeComposer/checklist.types';
import { ComposerEntity } from '#PrototypeComposer/types';
import { DataTable, LoadingContainer, Pagination, TextInput, ToggleSwitch } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import checkPermission, { roles } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { apiGetProcessesByResource } from '#utils/apiUrls';
import {
  ALL_FACILITY_ID,
  DEFAULT_PAGE_NUMBER,
  DEFAULT_PAGE_SIZE,
  DEFAULT_PAGINATION,
} from '#utils/constants';
import { FilterField, FilterOperators, fetchDataParams } from '#utils/globalTypes';
import { request } from '#utils/request';
import CreateJob from '#views/Jobs/Components/CreateJob';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { Search } from '@material-ui/icons';
import { navigate } from '@reach/router';
import { debounce } from 'lodash';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';

const getBaseFilter = (): FilterField[] => [
  { field: 'archived', op: FilterOperators.EQ, values: [false] },
];

const ProcessTabContent = () => {
  const dispatch = useDispatch();
  const {
    auth: { selectedFacility: { id: facilityId = '' } = {}, selectedUseCase, roles: userRoles },
    ontology: {
      objects: { active: selectedObject },
      objectTypes: { active: selectedObjectType },
    },
  } = useTypedSelector((state) => state);

  const [searchFilterFields, setSearchFilterFields] = useState<FilterField[]>([]);
  const [createJobDrawerVisible, setCreateJobDrawerVisible] = useState(false);
  const [selectedChecklist, setSelectedChecklist] = useState<Checklist | null>(null);
  const [filterFields, setFilterFields] = useState<FilterField[]>(getBaseFilter());

  const [state, setState] = useState<Record<string, any>>({
    list: [],
    pageable: DEFAULT_PAGINATION,
    loading: false,
  });

  const { list, pageable, loading } = state;

  const handleOnCreateJob = (item: Checklist) => {
    if (userRoles?.some((role) => role === roles.ACCOUNT_OWNER) && facilityId === ALL_FACILITY_ID) {
      dispatch(
        openOverlayAction({
          type: OverlayNames.ENTITY_START_ERROR_MODAL,
          props: {
            entity: ComposerEntity.JOB,
          },
        }),
      );
    } else {
      setSelectedChecklist(item);
      setCreateJobDrawerVisible(true);
    }
  };

  const fetchData = async (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE, filters = filterFields } = params;
    setState((prev) => ({
      ...prev,
      loading: true,
    }));
    try {
      const filteredParams = {};
      [...filters, ...searchFilterFields].forEach((item) => {
        if (item.field) {
          filteredParams[item.field] = item.values[0];
        }
      });
      const { data, pageable } = await request(
        'GET',
        apiGetProcessesByResource(selectedObjectType!.id),
        {
          params: {
            objectId: selectedObject!.id,
            facilityId,
            page,
            size,
            sort: 'createdAt,desc',
            useCaseId: selectedUseCase?.id,
            ...filteredParams,
          },
        },
      );

      if (data) {
        setState({
          list: data,
          pageable,
          loading: false,
        });
      }
    } catch (error) {
      console.error('error from fetch Processes in object view :: ', error);
    }
  };

  useEffect(() => {
    if (selectedObject) {
      fetchData({ filters: filterFields });
    }
  }, [filterFields, searchFilterFields]);

  const columns = [
    {
      id: 'name',
      label: 'Process Name',
      minWidth: 240,
      format: function renderComp(item: Checklist) {
        return (
          <span
            className="primary"
            onClick={() => {
              navigate(`/checklists/${item.id}`);
            }}
            title={item.name}
          >
            {item.name}
          </span>
        );
      },
    },
    {
      id: 'checklist-id',
      label: 'Process ID',
      minWidth: 152,
      format: function renderComp(item: Checklist) {
        return <div key={item.id}>{item.code}</div>;
      },
    },
    {
      id: 'actions',
      label: 'Actions',
      minWidth: 100,
      format: function renderComp(item: Checklist) {
        return (
          <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
            {!item.archived && checkPermission(['checklists', 'createJob']) && (
              <div
                className="primary"
                onClick={async () => {
                  handleOnCreateJob(item);
                }}
              >
                <span>Create Job</span>
              </div>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <TextInput
            afterElementWithoutError
            AfterElement={Search}
            afterElementClass=""
            placeholder="Search..."
            onChange={debounce(
              ({ value }) =>
                setSearchFilterFields([
                  { field: 'name', op: FilterOperators.LIKE, values: [value] },
                ]),
              500,
            )}
          />
          <ToggleSwitch
            checkedIcon={false}
            uncheckedIcon={false}
            offLabel="Show Archived"
            onLabel="Showing Archived"
            checked={!!filterFields.find((field) => field.field === 'archived')?.values[0]}
            onChange={(isChecked) =>
              setFilterFields((currentFields) => {
                const updatedFilterFields = currentFields.map((field) => ({
                  ...field,
                  ...(field.field === 'archived'
                    ? { values: [isChecked] }
                    : { values: field.values }),
                })) as FilterField[];
                return updatedFilterFields;
              })
            }
          />
        </div>
      </div>
      <div style={{ display: 'contents' }}>
        <LoadingContainer
          loading={loading}
          component={
            <DataTable
              columns={columns}
              rows={list.map((item: any) => {
                return {
                  ...item,
                };
              })}
              emptyTitle="No Associated Process Found"
            />
          }
        />
        <Pagination pageable={pageable} fetchData={fetchData} />
      </div>
      {createJobDrawerVisible && selectedChecklist && (
        <CreateJob
          checklist={{ label: selectedChecklist.name, value: selectedChecklist.id }}
          onCloseDrawer={setCreateJobDrawerVisible}
        />
      )}
    </TabContentWrapper>
  );
};

export default ProcessTabContent;
