import React, { Dispatch, FC, SetStateAction, useMemo, useState } from 'react';
import styled from 'styled-components';
import { BaseModal, FormGroup, TextInput } from '#components';
import { CommonOverlayProps, OverlayNames } from '#components/OverlayContainer/types';
import { debounce } from 'lodash';
import { Search } from '@material-ui/icons';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { createFetchList } from '#hooks/useFetchData';
import { apiGetObjects } from '#utils/apiUrls';
import { FilterOperators, InputTypes } from '#utils/globalTypes';
import { TObject } from '#views/Ontology/types';
import { useDispatch } from 'react-redux';
import { openOverlayAction } from '#components/OverlayContainer/actions';

const Wrapper = styled.div`
  .modal {
    .modal-body {
      display: flex;
      flex-direction: column;
      gap: 16px;
      font-size: 14px;
      color: #525252;

      p {
        margin: 0;
      }

      .MuiTypography-root {
        font-weight: normal;
      }

      .form-group {
        padding: 8px;
      }

      .objects-list {
        height: 200px;
        overflow: auto;
      }

      .MuiFormControlLabel-root {
        border-bottom: 1px solid #e0e0e0;
        padding: 8px 0px;
      }
    }

    .modal-footer {
      flex-direction: row-reverse;
    }
  }
`;

type TObjectPreviewModalProps = {
  collection: string;
  customView: Record<string, any>;
  setSelectedView: Dispatch<SetStateAction<Record<string, any> | null>>;
};

const ObjectPreviewModal: FC<CommonOverlayProps<TObjectPreviewModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { collection, customView, setSelectedView },
}) => {
  const dispatch = useDispatch();

  const { columns, filters } = customView || {};

  const urlParams = useMemo(() => {
    return {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      sort: 'createdAt,desc',
      collection,
      usageStatus: 1,
    };
  }, []);

  const { list, reset, fetchNext, listById } = createFetchList(apiGetObjects(), urlParams);

  const [selectedObject, setSelectedObject] = useState<TObject>();

  const handleOnScroll = (e: React.UIEvent<HTMLElement>) => {
    e.stopPropagation();
    const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
    if (scrollTop + clientHeight >= scrollHeight - clientHeight * 0.7) fetchNext();
  };

  const handlePrimary = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.OBJECT_JOB_LOG_PREVIEW_MODAL,
        props: {
          selectedObject,
          objectJobLogColumns: columns,
          viewFilters: filters,
          customView,
          setSelectedView,
        },
      }),
    );
  };

  const onCloseModal = () => {
    setSelectedView(null);
    closeOverlay();
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={onCloseModal}
        title="Object Preview"
        primaryText="Proceed"
        secondaryText="Cancel"
        disabledPrimary={!selectedObject}
        onPrimary={handlePrimary}
        onSecondary={onCloseModal}
      >
        <p>Select one object to preview</p>
        <TextInput
          afterElementWithoutError
          AfterElement={Search}
          afterElementClass=""
          placeholder={`Search for object name`}
          onChange={debounce(({ value }) => {
            reset({
              params: {
                ...urlParams,
                filters: {
                  op: 'AND',
                  fields: [
                    {
                      field: 'displayName',
                      op: FilterOperators.LIKE,
                      values: [value],
                    },
                  ],
                },
              },
            });
          }, 500)}
        />
        <div className="objects-list" onScroll={handleOnScroll}>
          <FormGroup
            inputs={[
              {
                type: InputTypes.RADIO,
                props: {
                  groupProps: {
                    id: 'objects',
                    name: 'objects',
                    onChange: (e) => {
                      const item = listById[e.target.value];
                      setSelectedObject(item);
                    },
                  },
                  items: list.map((item: any) => ({
                    key: item?.id,
                    label: item?.displayName,
                    value: item?.id,
                  })),
                },
              },
            ]}
          />
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default ObjectPreviewModal;
