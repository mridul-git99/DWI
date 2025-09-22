import { BaseModal, Button, Select } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { v4 as uuidv4 } from 'uuid';
import { fetchFacilities } from '#store/facilities/actions';
import { Add, Close } from '@material-ui/icons';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { getErrorMsg, request } from '#utils/request';
import { apiProcessSharing } from '#utils/apiUrls';
import { ResponseObj } from '#utils/globalTypes';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';

const Wrapper = styled.div`
  .modal {
    min-width: 40dvw !important;

    &-header {
      h2 {
        color: #000 !important;
      }
    }

    &-body {
      padding: 0 !important;
      overflow: auto;
    }

    &-footer {
      padding: 12px 24px !important;
      flex-direction: row-reverse;

      button {
        padding: 8px 16px;
      }
    }
  }

  .body {
    display: flex;
    flex-direction: column;
    padding: 24px 24px 12px;
    min-height: 40dvh;

    .row {
      display: flex;
      margin-bottom: 16px;

      .select-column {
        flex: 1;
      }

      .remove-column {
        display: flex;
        align-items: center;
        padding: 0px 24px;

        > svg {
          cursor: pointer;
        }
      }
    }

    > button {
      width: fit-content;
      > svg {
        font-size: 14px;
        margin-right: 12px;
      }
    }
  }
`;

type Props = {
  checklistId: string;
};

type SelectionRowsType = Record<string, { id?: string; name?: string }>;

const ProcessSharing: FC<CommonOverlayProps<Props>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { checklistId },
}) => {
  const dispatch = useDispatch();
  const { list, loading } = useTypedSelector((state) => state.facilities);
  const [selectionRows, setSelectionRows] = useState<SelectionRowsType>({});
  const [previousIds, setPreviousIds] = useState<Record<string, boolean>>({});
  const [selectedIds, setSelectedIds] = useState<Record<string, boolean>>({});
  const [removedIds, setRemovedIds] = useState<Record<string, boolean>>({});
  const selectionRowsLength = Object.keys(selectionRows).length;
  const selectedIdsLength = Object.keys(selectedIds).length;
  const removedIdsLength = Object.keys(removedIds).length;

  useEffect(() => {
    if (!list?.length) {
      dispatch(fetchFacilities());
    }
    getSharedFacilities();
  }, []);

  const getSharedFacilities = async () => {
    const res: ResponseObj<{ id: string; name: string }[]> = await request(
      'GET',
      apiProcessSharing(checklistId!),
    );
    if (res.data && res.data.length) {
      const { _selectionRows, _selectedIds } = res.data.reduce<{
        _selectionRows: SelectionRowsType;
        _selectedIds: Record<string, boolean>;
      }>(
        (acc, facility) => {
          acc['_selectionRows'][uuidv4()] = {
            id: facility.id,
            name: facility.name,
          };
          acc['_selectedIds'][facility.id] = true;
          return acc;
        },
        {
          _selectionRows: {},
          _selectedIds: {},
        },
      );
      setSelectionRows(_selectionRows);
      setPreviousIds(_selectedIds);
    } else {
      setSelectionRows({ [uuidv4()]: {} });
    }
  };

  const onSubmit = async () => {
    const assignedFacilityIds = Object.keys(selectedIds).map((facilityId) => facilityId);
    const unassignedFacilityIds = Object.keys(removedIds).map((facilityId) => facilityId);
    const { data, errors }: ResponseObj<{ id: string; name: string }[]> = await request(
      'PATCH',
      apiProcessSharing(checklistId!),
      {
        data: { assignedFacilityIds, unassignedFacilityIds },
      },
    );
    if (!data) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors!),
        }),
      );
    } else {
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Action Completed Successfully',
        }),
      );
    }
  };

  const onChangePreviousSelected = (id?: string) => {
    if (id) {
      if (previousIds?.[id]) {
        setRemovedIds((prev) => ({ ...prev, [id]: true }));
      } else {
        setSelectedIds((prev) => {
          const _selectedIds = { ...prev };
          delete _selectedIds[id];
          return _selectedIds;
        });
      }
    }
  };

  const getOptions = () => {
    return list?.reduce<Array<{ value: string; label: string }>>((acc, option) => {
      if ((!selectedIds?.[option.id] && !previousIds?.[option.id]) || removedIds?.[option.id]) {
        acc.push({
          value: option.id,
          label: option.name,
        });
      }
      return acc;
    }, []);
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title="Process Sharing"
        primaryText="Save"
        secondaryText="Cancel"
        disabledPrimary={!selectedIdsLength && !removedIdsLength}
        onPrimary={onSubmit}
      >
        <div className="body">
          {Object.entries(selectionRows).map(([key, { id, name }]) => (
            <div className="row" key={key}>
              <div className="select-column">
                <Select
                  isLoading={loading}
                  options={getOptions()}
                  value={
                    id
                      ? [
                          {
                            value: id,
                            label: name,
                          },
                        ]
                      : undefined
                  }
                  placeholder="Select facility"
                  onChange={(option) => {
                    setSelectionRows((prev) => {
                      prev[key] = {
                        id: option?.value,
                        name: option?.label,
                      };
                      return prev;
                    });
                    if (!previousIds?.[option!.value]) {
                      setSelectedIds((prev) => ({ ...prev, [option!.value]: true }));
                    } else if (removedIds?.[option!.value]) {
                      setRemovedIds((prev) => {
                        const _removedIds = { ...prev };
                        delete _removedIds[option!.value];
                        return _removedIds;
                      });
                    }
                    onChangePreviousSelected(id);
                  }}
                />
              </div>
              <div className="remove-column">
                <Close
                  onClick={() => {
                    onChangePreviousSelected(id);
                    setSelectionRows((prev) => {
                      const _selectionRows = { ...prev };
                      delete _selectionRows[key];
                      return _selectionRows;
                    });
                  }}
                />
              </div>
            </div>
          ))}
          <Button
            type="button"
            variant="textOnly"
            onClick={() => setSelectionRows((prev) => ({ ...prev, [uuidv4()]: {} }))}
            disabled={selectionRowsLength === list?.length}
          >
            <Add /> Add New
          </Button>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default ProcessSharing;
