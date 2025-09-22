import QRIcon from '#assets/svg/QR';
import { NestedSelect, NestedSelectProps } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { createFetchList } from '#hooks/useFetchData';
import { apiGetObjectTypes } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { fetchDataParams } from '#utils/globalTypes';
import { TObject } from '#views/Ontology/types';
import { getObjectPartialCall, getQrCodeData } from '#views/Ontology/utils';
import { ExpandMore } from '@material-ui/icons';
import ClearIcon from '@material-ui/icons/Clear';
import MoreHorizIcon from '@material-ui/icons/MoreHoriz';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const ResourceFilterWrapper = styled.div`
  min-width: 85px;
  max-width: 200px;
  align-items: center;
  background-color: hsl(0, 0%, 100%);
  border: 1px solid hsl(0, 0%, 80%);
  cursor: pointer;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  min-height: auto;
  position: relative;
  padding: 1.7px;

  .label-container {
    align-items: center;
    display: grid;
    flex: 1;
    flex-wrap: wrap;
    padding: 2px 8px;
    position: relative;
    overflow: hidden;

    .resource-filter-label {
      color: hsl(0, 0%, 50%);
      grid-area: 1;
      margin-left: 2px;
      margin-right: 2px;
      text-wrap: nowrap;
      font-size: 14px;
    }

    .active {
      color: #000000;
      text-overflow: ellipsis;
      overflow: hidden;
    }
  }
  .resource-filter-icons {
    align-items: center;
    align-self: stretch;
    display: flex;
    flex-shrink: 0;

    svg {
      color: hsl(0, 0%, 50%);
      height: 24px;
      width: 24px;
      margin: 6px 0;

      &:hover {
        color: #101010;
      }
    }

    .clear-icon {
      width: 16px;
      height: 16px;
    }
  }
`;

const QrScannerWrapper = styled.div`
  padding: 2px 4px;
  background-color: #f4f4f4;
  margin-right: 8px;
`;

export const ResourceFilter = ({
  onChange,
  onClear,
  width = 'auto',
  DropdownIcon = ExpandMore,
  defaultValue,
  className,
  disabled,
}: {
  onChange?: (option: Object) => void;
  onClear?: () => void;
  width?: string;
  DropdownIcon?: React.ElementType;
  defaultValue?: any;
  className?: string;
  disabled?: boolean;
}) => {
  const [state, setState] = useState<{
    selectedResource?: TObject;
    resourceOptions: NestedSelectProps['items'];
  }>({
    resourceOptions: {},
  });

  const dispatch = useDispatch();

  const {
    list,
    reset,
    pagination: objectTypePagination,
    status,
    fetchNext,
  } = createFetchList(
    apiGetObjectTypes(),
    {
      usageStatus: 1,
    },
    false,
  );

  const { resourceOptions, selectedResource } = state;

  useEffect(() => {
    if (list.length) {
      const listOptions = list.reduce<any>((acc, item) => {
        acc[item.id] = {
          label: item.displayName,
          fetchItems: async (pageNumber?: number, query = '') => {
            if (typeof pageNumber === 'number') {
              const { data: resData, pageable } = await getObjectPartialCall({
                page: pageNumber,
                size: DEFAULT_PAGE_SIZE,
                collection: item.externalId,
                query,
                usageStatus: 1,
              });
              return {
                options: resData.map((item) => ({
                  ...item,
                  value: item.id,
                  label: item.displayName,
                })),
                pageable,
              };
            }
            return {
              options: [],
            };
          },
        };
        return acc;
      }, {});
      setState((prev) => ({ ...prev, resourceOptions: listOptions }));
    } else {
      setState((prev) => ({
        ...prev,
        resourceOptions: { 'no-options': { label: 'No Options', options: [] } },
      }));
    }
  }, [list.length]);

  useEffect(() => {
    if (!defaultValue) {
      setState((prev) => ({ ...prev, selectedResource: undefined }));
    }
  }, [defaultValue]);

  const fetchResourcesData = (params: fetchDataParams = {}) => {
    const { query, page = DEFAULT_PAGE_NUMBER, ...rest } = params;
    if (page > 0) {
      fetchNext();
    } else {
      reset({ params: { ...rest, displayName: query, usageStatus: 1, page } });
    }
  };

  const ResourceFilterLabel = () => {
    const value = selectedResource?.displayName || defaultValue?.label || `Resource`;
    return (
      <ResourceFilterWrapper className={className}>
        <div className="label-container">
          <div
            className={`resource-filter-label ${
              selectedResource?.id || defaultValue?.value ? 'active' : ''
            }`}
            title={value}
          >
            {value}
          </div>
        </div>
        <div className="resource-filter-icons">
          {(selectedResource?.id || defaultValue?.value) && (
            <ClearIcon onMouseDown={onClearAll} className="clear-icon" />
          )}
          {status === 'loading' && <MoreHorizIcon />}
          <DropdownIcon />
        </div>
      </ResourceFilterWrapper>
    );
  };

  const onChildChange = (option: any) => {
    setState((prev) => ({ ...prev, selectedResource: option }));
    onChange && onChange(option);
  };

  const onClearAll = (e: any) => {
    e.stopPropagation();
    e.preventDefault();
    setState((prev) => ({ ...prev, selectedResource: undefined }));
    onClear && onClear();
  };

  const onSelectWithQR = async (data: string) => {
    const qrData = await getQrCodeData({
      shortCode: data,
    });
    if (qrData) {
      const selectedResource = {
        id: qrData.objectId,
        collection: qrData.collection,
        externalId: qrData.externalId,
        displayName: qrData.displayName,
        value: qrData.objectId,
        label: qrData.displayName,
      };
      onChildChange(selectedResource);
    }
  };

  const QrScanner = () => {
    return (
      <QrScannerWrapper>
        <div
          onClick={() => {
            dispatch(
              openOverlayAction({
                type: OverlayNames.QR_SCANNER,
                props: { onSuccess: onSelectWithQR },
              }),
            );
          }}
        >
          <QRIcon />
        </div>
      </QrScannerWrapper>
    );
  };

  return (
    <NestedSelect
      id="resource-filter-selector"
      width={width}
      label={ResourceFilterLabel}
      items={resourceOptions}
      popOutProps={{ filterOption: () => true }}
      onChildChange={onChildChange}
      pagination={objectTypePagination}
      fetchData={fetchResourcesData}
      maxHeight={350}
      afterElement={<QrScanner />}
      disabled={disabled}
    />
  );
};
