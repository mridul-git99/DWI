import { BaseModal, Link } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import styled from 'styled-components';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import {
  Avatar,
  Button,
  CustomTag,
  DataTable,
  LoadingContainer,
  Pagination,
  TextInput,
} from '#components';
import { Search } from '@material-ui/icons';
import { debounce } from 'lodash';
import { formatDateTime } from '#utils/timeUtils';
import { FilterField, FilterOperators } from '#utils/globalTypes';
import AddCircleOutlineIcon from '@material-ui/icons/AddCircleOutline';
import { OverlayNames } from '#components/OverlayContainer/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { useDispatch } from 'react-redux';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { createFetchList } from '#hooks/useFetchData';
import { apiGetJobAnnotation } from '#utils/apiUrls';
import { openLinkInNewTab } from '#utils';
import { Media } from '#PrototypeComposer/checklist.types';
import { InboxWrapper } from '#views/Inbox/styles';
import KeyboardArrowLeftIcon from '@material-ui/icons/KeyboardArrowLeft';

const Wrapper = styled.div`
  .modal {
    height: 100%;
    min-height: 100dvh;
    min-width: 100dvw !important;
  }

  .modal-body {
    height: 100%;
  }
`;

const JobAnnotations: FC<CommonOverlayProps<{ jobId: string }>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { jobId },
}) => {
  const dispatch = useDispatch();

  const urlParams = useMemo(() => {
    return {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      sort: 'createdAt,desc',
    };
  }, []);

  const { list, reset, pagination, status } = createFetchList(
    apiGetJobAnnotation(),
    urlParams,
    false,
  );

  const [filterFields, setFilterFields] = useState<FilterField[]>([
    { field: 'job.id', op: FilterOperators.EQ, values: [jobId] },
  ]);
  const [refetchAnnotations, setRefetchAnnotations] = useState<boolean>(false);

  useEffect(() => {
    reset({
      params: {
        ...urlParams,
        filters: {
          op: FilterOperators.AND,
          fields: filterFields,
        },
      },
    });
  }, [filterFields, refetchAnnotations]);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showHeader={true}
        title={
          <Link
            label="Job Annotations"
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
        <InboxWrapper>
          <TabContentWrapper>
            <div className="before-table-wrapper">
              <div className="filters">
                <TextInput
                  afterElementWithoutError
                  AfterElement={Search}
                  afterElementClass=""
                  placeholder={`Search by Remark`}
                  onChange={debounce((option) => {
                    setFilterFields((prev) => {
                      return [
                        ...prev.filter((f) => f.field !== 'remarks'),
                        {
                          field: 'remarks',
                          op: FilterOperators.LIKE,
                          values: [option.value],
                        },
                      ];
                    });
                  }, 500)}
                />
              </div>
              <Button
                onClick={() => {
                  dispatch(
                    openOverlayAction({
                      type: OverlayNames.ADD_REMARK_MODAL,
                      props: {
                        jobId,
                        setRefetchAnnotations,
                      },
                    }),
                  );
                }}
              >
                <AddCircleOutlineIcon fontSize="small" style={{ marginRight: '8px' }} />
                Add Annotation
              </Button>
            </div>

            <LoadingContainer
              loading={status === 'loading'}
              component={
                <>
                  <DataTable
                    columns={[
                      {
                        id: 'id',
                        label: 'ID',
                        minWidth: 100,
                        format: (item) => {
                          return item?.code;
                        },
                      },
                      {
                        id: 'remarks',
                        label: 'Remarks',
                        minWidth: 200,
                        format: (item) => {
                          return item?.remarks;
                        },
                      },
                      {
                        id: 'attachment',
                        label: 'Attachment',
                        minWidth: 200,
                        format: (item) => {
                          const hasMedia = item?.medias?.length > 0;
                          return (
                            <>
                              {hasMedia ? (
                                <div style={{ color: '#1d84ff', cursor: 'pointer' }}>
                                  {item.medias.map((media, index, array) => (
                                    <CustomTag
                                      as="div"
                                      onClick={() => openLinkInNewTab(`/media?link=${media.link}`)}
                                    >
                                      <span>
                                        {media.originalFilename}
                                        {index < array.length - 1 && (
                                          <span style={{ color: '#333333' }}>, </span>
                                        )}
                                      </span>
                                    </CustomTag>
                                  ))}
                                </div>
                              ) : (
                                <span>-</span>
                              )}
                            </>
                          );
                        },
                      },
                      {
                        id: 'createdBy',
                        label: 'Created By',
                        minWidth: 100,
                        format: (item) => {
                          return (
                            <div style={{ display: 'flex' }}>
                              <Avatar user={item?.createdBy} />
                            </div>
                          );
                        },
                      },
                      {
                        id: 'createdDateTime',
                        label: 'Created Date Time',
                        minWidth: 100,
                        format: (item) => {
                          return formatDateTime({
                            value: item?.createdAt,
                          });
                        },
                      },
                    ]}
                    rows={list}
                    emptyTitle="No Annotations Found"
                  />
                  <Pagination
                    pageable={pagination}
                    fetchData={(p) =>
                      reset({
                        params: {
                          ...urlParams,
                          filters: {
                            op: FilterOperators.AND,
                            fields: filterFields,
                          },
                          page: p.page,
                          size: p.size,
                        },
                      })
                    }
                  />
                </>
              }
            />
          </TabContentWrapper>
        </InboxWrapper>
      </BaseModal>
    </Wrapper>
  );
};

export default JobAnnotations;
