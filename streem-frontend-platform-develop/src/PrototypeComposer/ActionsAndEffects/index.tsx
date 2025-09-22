import { Button, CustomMenu, DataTable, Link, LoadingContainer } from '#components';
import React, { Dispatch, FC, SetStateAction, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { useDispatch } from 'react-redux';
import EditOutlinedIcon from '@material-ui/icons/EditOutlined';
import ArchiveIcon from '#assets/svg/archiveIcon.svg';
import { useQueryParams } from '#hooks/useQueryParams';
import { EffectsList } from '#views/Checklists/Effects';
import KeyboardArrowLeftIcon from '@material-ui/icons/KeyboardArrowLeft';
import useRequest from '#hooks/useRequest';
import { apiArchiveAction, apiGetChecklistActions, apiReorderEffects } from '#utils/apiUrls';
import { useTypedSelector } from '#store';
import { request } from '#utils/request';
import CreateEffectsDrawer from './CreateEffectsDrawer';
import { TriggerType } from '#types/actionsAndEffects';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';

const Wrapper = styled.div`
  margin: 8px;
  padding: 16px;
  height: 100%;
  background-color: #fff;

  .empty-effects {
    display: flex;
    justify-content: center;
    align-items: center;
    color: #525252;
    font-size: 14px;
    padding: 16px;
  }
`;

type TActionsAndEffectsProps = {
  isReadOnly: boolean;
  checklistId: string;
};

const ActionItem: FC<{
  item: any;
  isReadOnly: boolean;
  actionList: any;
  setRefetchActions: Dispatch<SetStateAction<boolean>>;
}> = ({ item, isReadOnly, actionList, setRefetchActions }) => {
  const dispatch = useDispatch();

  const deleteAction = async (id: string) => {
    const { data } = await request('PATCH', apiArchiveAction(id));

    if (data) {
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Action deleted successfully`,
        }),
      );
    }
  };

  const actionItems = useMemo(
    () => [
      {
        label: 'Edit',
        icon: <EditOutlinedIcon />,
        onClick: () => {
          dispatch(
            openOverlayAction({
              type: OverlayNames.CREATE_ACTION_MODAL,
              props: {
                isReadOnly,
                action: item,
                actionList,
                setRefetchActions,
              },
            }),
          );
        },
      },
      {
        label: 'Delete',
        icon: <img src={ArchiveIcon} alt="Archive Icon" />,
        onClick: () => {
          deleteAction(item.id);
        },
      },
    ],
    [item],
  );

  return <CustomMenu items={actionItems} type="list-menu" />;
};

const ActionsAndEffects: FC<TActionsAndEffectsProps> = ({ isReadOnly, checklistId }) => {
  const dispatch = useDispatch();

  const tasks = useTypedSelector((state) => state.prototypeComposer.tasks.listById);

  const { updateQueryParams, getQueryParam, removeQueryParam } = useQueryParams();

  const { data, dataById, fetchData } = useRequest<any>({
    url: apiGetChecklistActions(checklistId!),
    fetchOnInit: false,
  });

  const actionId = getQueryParam('actionId');
  const [createEffectDrawer, setCreateEffectDrawer] = useState<boolean>(false);
  const [refetchActions, setRefetchActions] = useState<boolean>(false);
  const [effects, setEffects] = useState<any[]>([]);
  const [effectsReordered, setEffectsReordered] = useState<boolean>(false);

  const reorderEffects = async () => {
    const payload = {};

    effects.forEach((effect, index) => {
      payload[effect.id] = index;
    });

    const { data } = await request('PATCH', apiReorderEffects(), {
      data: {
        effectOrder: payload,
      },
    });

    if (data) {
      setEffectsReordered(false);
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Effects reordered successfully`,
        }),
      );
    }
  };

  useEffect(() => {
    fetchData();
  }, [refetchActions]);

  useEffect(() => {
    if (actionId) {
      setEffects(dataById?.[actionId]?.effects || []);
    }
  }, [actionId, dataById]);

  return (
    <Wrapper>
      <TabContentWrapper>
        <div className="before-table-wrapper">
          {actionId && (
            <div className="filters">
              <Link
                label="Back to Actions"
                backIcon={KeyboardArrowLeftIcon}
                onClick={() => {
                  removeQueryParam({ key: 'actionId' });
                }}
                iconColor="#000000"
                labelColor="#000000"
                addMargin={false}
              />
            </div>
          )}
          {!isReadOnly && (
            <div className="actions">
              {actionId ? (
                <>
                  <Button onClick={reorderEffects} disabled={!effectsReordered}>
                    Reorder
                  </Button>
                  <Button
                    onClick={() => {
                      setCreateEffectDrawer(true);
                    }}
                  >
                    Add Effect
                  </Button>
                </>
              ) : (
                <Button
                  onClick={() => {
                    dispatch(
                      openOverlayAction({
                        type: OverlayNames.CREATE_ACTION_MODAL,
                        props: {
                          isReadOnly,
                          actionList: data,
                          setRefetchActions,
                        },
                      }),
                    );
                  }}
                >
                  Create new Action
                </Button>
              )}
            </div>
          )}
        </div>
        <LoadingContainer
          loading={false}
          component={
            <>
              {actionId ? (
                <>
                  {effects.length ? (
                    <EffectsList
                      isReadOnly={isReadOnly}
                      effects={effects}
                      actions={dataById}
                      setEffects={setEffects}
                      setEffectsReordered={setEffectsReordered}
                    />
                  ) : (
                    <div className="empty-effects">No effects added</div>
                  )}
                </>
              ) : (
                <DataTable
                  columns={[
                    {
                      id: 'actionName',
                      label: 'Action Name',
                      minWidth: 400,
                      format: (item) => {
                        return (
                          <span
                            className="primary"
                            onClick={() => {
                              updateQueryParams({
                                newParams: { actionId: item.id },
                              });
                            }}
                          >
                            {item.name}
                          </span>
                        );
                      },
                    },
                    {
                      id: 'triggerType',
                      label: 'Trigger Type',
                      minWidth: 400,
                      format: (item) =>
                        item?.triggerType === TriggerType.START_TASK
                          ? 'When Task is Started'
                          : item?.triggerType === TriggerType.COMPLETE_TASK
                          ? 'When Task is Completed'
                          : '--',
                    },
                    {
                      id: 'triggerEntityId',
                      label: 'Task',
                      minWidth: 400,
                      format: (item) => tasks?.[item.triggerEntityId]?.name,
                    },
                    ...(isReadOnly
                      ? []
                      : [
                          {
                            id: 'actions',
                            label: 'Actions',
                            minWidth: 170,
                            format: (item) => (
                              <ActionItem
                                item={item}
                                isReadOnly={isReadOnly}
                                actionList={data}
                                setRefetchActions={setRefetchActions}
                              />
                            ),
                          },
                        ]),
                  ]}
                  emptyTitle="No actions  configured"
                  rows={data || []}
                />
              )}
            </>
          }
        />
        {createEffectDrawer && (
          <CreateEffectsDrawer
            onCloseDrawer={setCreateEffectDrawer}
            actions={dataById}
            isReadOnly={isReadOnly}
            setEffects={setEffects}
          />
        )}
      </TabContentWrapper>
    </Wrapper>
  );
};

export default ActionsAndEffects;
