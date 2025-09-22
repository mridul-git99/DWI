import React, { Dispatch, FC, SetStateAction, useEffect, useState } from 'react';
import styled from 'styled-components';
import CreateEffectsDrawer from '../../../../src/PrototypeComposer/ActionsAndEffects/CreateEffectsDrawer';
import {
  DndContext,
  DragEndEvent,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { restrictToVerticalAxis } from '@dnd-kit/modifiers';
import {
  SortableContext,
  arrayMove,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { findIndex } from 'lodash';
import { DragIndicator } from '@material-ui/icons';
import { CSS } from '@dnd-kit/utilities';
import DeleteOutlineOutlinedIcon from '@material-ui/icons/DeleteOutlineOutlined';
import { apiArchiveEffect, apiGetEffectsByActionId } from '#utils/apiUrls';
import { request } from '#utils/request';
import { useDispatch } from 'react-redux';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import useRequest from '#hooks/useRequest';
import { useQueryParams } from '#hooks/useQueryParams';

const Wrapper = styled.div`
  .effect-card {
    display: flex;
    border: 1px solid #e0e0e0;
    padding: 16px;
    width: 100%;
    align-items: center;
    justify-content: space-between;

    p {
      margin: 0;
    }
  }

  .effects-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .effect-name {
    font-size: 14px;
    color: #1d84ff;
    cursor: pointer;
  }

  .draggable {
    cursor: grab;
  }
`;

const SortableEffect: FC<{
  effect: any;
  isReadOnly: boolean;
  actions: Record<string, any>;
  setEffects: Dispatch<SetStateAction<any[]>>;
}> = ({ effect, isReadOnly, actions, setEffects }) => {
  const dispatch = useDispatch();
  const { getQueryParam } = useQueryParams();
  const actionId = getQueryParam('actionId');

  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: effect?.id,
    disabled: isReadOnly,
  });

  const style = {
    display: 'flex',
    alignItems: 'center',
    transform: CSS.Transform.toString(transform),
    transition,
  };

  const [editEffectDrawer, setEditEffectDrawer] = useState(false);

  const deleteEffect = async (id: string) => {
    const { data } = await request('PATCH', apiArchiveEffect(id));

    if (data) {
      const effectsResponse = await request('GET', apiGetEffectsByActionId(actionId!));
      if (effectsResponse.data && setEffects) {
        setEffects(effectsResponse.data);
      }
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Effect deleted successfully`,
        }),
      );
    }
  };

  return (
    <>
      <div
        ref={setNodeRef}
        style={style}
        className={isDragging ? 'container dragging' : 'container'}
      >
        {!isReadOnly && (
          <div className="draggable" {...attributes} {...listeners}>
            <DragIndicator />
          </div>
        )}
        <div className="effect-card">
          <p
            className="effect-name"
            onClick={() => {
              setEditEffectDrawer(true);
            }}
          >
            {effect.name}
          </p>
          {!isReadOnly && (
            <DeleteOutlineOutlinedIcon
              style={{ cursor: 'pointer' }}
              onClick={() => {
                deleteEffect(effect.id);
              }}
            />
          )}
        </div>
      </div>
      {editEffectDrawer && (
        <CreateEffectsDrawer
          onCloseDrawer={setEditEffectDrawer}
          effectData={effect}
          actions={actions}
          isReadOnly={isReadOnly}
        />
      )}
    </>
  );
};

export const EffectsList: FC<{
  isReadOnly: boolean;
  effects: any[];
  setEffects: Dispatch<SetStateAction<any[]>>;
  actions: Record<string, any>;
  setEffectsReordered: Dispatch<SetStateAction<boolean>>;
}> = ({ isReadOnly, effects, actions, setEffects, setEffectsReordered }) => {
  const sensors = useSensors(useSensor(PointerSensor));

  const handleDragEnd = (e: DragEndEvent) => {
    const { active, over } = e;
    if (over && active.id !== over.id) {
      setEffects((items) => {
        const oldIndex = findIndex(items, (item) => {
          if (active.id === `${item.id}`) {
            return true;
          }
          return false;
        });
        const newIndex = findIndex(items, (item) => {
          if (over.id === `${item.id}`) {
            return true;
          }
          return false;
        });
        return arrayMove(items, oldIndex, newIndex);
      });
      setEffectsReordered(true);
    }
  };

  // const { getQueryParam } = useQueryParams();

  // const actionId = getQueryParam('actionId');

  // const { data, fetchData } = useRequest<any>({
  //   url: apiGetEffectsByActionId(actionId!),
  //   fetchOnInit: false,
  // });

  // console.log(data);

  // useEffect(() => {
  //   if (actionId) {
  //     fetchData();
  //   }
  // }, [actionId]);

  return (
    <Wrapper>
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
        modifiers={[restrictToVerticalAxis]}
      >
        <SortableContext items={effects} strategy={verticalListSortingStrategy}>
          <div className="effects-list">
            {effects.map((effect) => (
              <SortableEffect
                key={effect.id}
                effect={effect}
                isReadOnly={isReadOnly}
                actions={actions}
                setEffects={setEffects}
              />
            ))}
          </div>
        </SortableContext>
      </DndContext>
    </Wrapper>
  );
};
