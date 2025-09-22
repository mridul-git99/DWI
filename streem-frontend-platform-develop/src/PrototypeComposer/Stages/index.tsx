import { Button } from '#components';
import { useQueryParams } from '#hooks/useQueryParams';
import { useTypedSelector } from '#store/helpers';
import { AddCircleOutline } from '@material-ui/icons';
import React, { createRef, FC, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { addNewStage, setActiveStage } from './actions';
import StageCard from './StageCard';
import { StageListWrapper } from './styles';

const Stages: FC<{ isReadOnly: boolean }> = ({ isReadOnly }) => {
  const { getQueryParam } = useQueryParams();

  const stageId = getQueryParam('stageId');

  const dispatch = useDispatch();
  const { activeStageId, listOrder, listById } = useTypedSelector(
    (state) => state.prototypeComposer.stages,
  );

  const refMap = listOrder.map(() => createRef<HTMLDivElement>());

  useEffect(() => {
    if (stageId) {
      dispatch(setActiveStage({ id: stageId }));

      const index = listOrder.findIndex((id) => id === stageId);
      refMap[index]?.current?.scrollIntoView();
    }
  }, [stageId]);

  return (
    <StageListWrapper>
      {listOrder?.map((stageId, index) => (
        <StageCard
          index={index}
          isActive={stageId === activeStageId}
          isFirstItem={index === 0}
          isLastItem={index === listOrder.length - 1}
          key={`${stageId}-${index}`}
          ref={refMap[index]}
          stage={listById[stageId]}
          isReadOnly={isReadOnly}
        />
      ))}
      {!isReadOnly && (
        <Button variant="secondary" className="add-item" onClick={() => dispatch(addNewStage())}>
          <AddCircleOutline className="icon" fontSize="small" />
          Add New Stage
        </Button>
      )}
    </StageListWrapper>
  );
};

export default Stages;
