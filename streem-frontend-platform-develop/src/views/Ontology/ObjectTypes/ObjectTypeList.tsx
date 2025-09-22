import {
  Button,
  DataTable,
  LoadingContainer,
  Pagination,
  TabContentProps,
  TextInput,
} from '#components';
import { DataTableColumn } from '#components/shared/DataTable';
import { createFetchList } from '#hooks/useFetchData';
import { useQueryParams } from '#hooks/useQueryParams';
import checkPermission from '#services/uiPermissions';
import { apiGetObjectTypes } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { Search } from '@material-ui/icons';
import { navigate } from '@reach/router';
import { debounce } from 'lodash';
import React, { FC, useEffect } from 'react';

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  usageStatus: 1,
};

const ObjectTypeList: FC<TabContentProps> = ({ values }) => {
  const { getQueryParam, updateQueryParams } = useQueryParams();
  const page = getQueryParam('page');
  const displayName = getQueryParam('displayName');
  const { list, reset, pagination, status } = createFetchList(
    apiGetObjectTypes(),
    urlParams,
    false,
  );

  useEffect(() => {
    reset({ params: { ...urlParams, displayName, page } });
  }, [displayName, page]);

  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <TextInput
            afterElementWithoutError
            AfterElement={Search}
            afterElementClass=""
            placeholder={`Search with Object Type`}
            defaultValue={displayName}
            onChange={debounce(
              ({ value }) =>
                updateQueryParams({
                  newParams: { displayName: value, page: DEFAULT_PAGE_NUMBER },
                  navigateOptions: { replace: true },
                }),
              500,
            )}
          />
        </div>
        {checkPermission(['ontology', 'createObjectType']) && (
          <div className="actions">
            <Button
              onClick={() => {
                navigate('/ontology/object-types/add');
              }}
            >
              Add New Object Type
            </Button>
          </div>
        )}
      </div>
      <LoadingContainer
        loading={status === 'loading'}
        component={
          <DataTable
            columns={[
              {
                id: 'name',
                label: 'Object Types',
                minWidth: 240,
                maxWidth: 800,
                format: function renderComp(item) {
                  return (
                    <span
                      className="primary"
                      onClick={() => {
                        navigate(`/ontology/${values.rootPath}/${item.id}`);
                      }}
                      title={item.displayName}
                    >
                      {item.displayName}
                    </span>
                  );
                },
              },
              ...(checkPermission(['ontology', 'editObjectType'])
                ? ([
                    {
                      id: 'actions',
                      label: 'Actions',
                      minWidth: 240,
                      align: 'center',
                      format: function renderComp(item) {
                        return (
                          <span
                            className="primary"
                            onClick={() => {
                              navigate(`/ontology/${values.rootPath}/edit/${item.id}`, {
                                state: { objectType: item },
                              });
                            }}
                          >
                            Edit
                          </span>
                        );
                      },
                    },
                  ] as DataTableColumn[])
                : []),
            ]}
            rows={list}
            emptyTitle="No Object Types Found"
          />
        }
      />
      <Pagination pageable={pagination} fetchData={true} />
    </TabContentWrapper>
  );
};

export default ObjectTypeList;
