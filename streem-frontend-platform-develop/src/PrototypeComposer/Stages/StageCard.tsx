import { Textarea } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store/helpers';
import {
  ArrowDropDown,
  ArrowDropUp,
  AssignmentTurnedIn,
  DeleteOutlined,
  Error,
  Error as ErrorIcon,
  FileCopyOutlined,
} from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { forwardRef, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { deleteStage, reOrderStage, setActiveStage, updateStageName } from './actions';
import { StageCardWrapper } from './styles';
import { StageCardProps } from './types';
import { copyEntities } from '#PrototypeComposer/actions';
import { EntityType } from '#PrototypeComposer/types';

const StageCard = forwardRef<HTMLDivElement, StageCardProps>((props, ref) => {
  const { index, isActive, isFirstItem, isLastItem, stage, isReadOnly } = props;

  const { tasksInStage, data, errors, parametersInStage } = useTypedSelector((state) => ({
    tasksInStage: state.prototypeComposer.tasks.tasksOrderInStage[stage.id].map(
      (taskId) => state.prototypeComposer.tasks.listById[taskId],
    ),
    data: state.prototypeComposer.data,
    errors: state.prototypeComposer.errors,
    userId: state.auth.userId,
    parametersInStage: Object.keys(
      state.prototypeComposer.parameters.parameterOrderInTaskInStage[stage.id] ?? {},
    )
      .map(
        (taskId) =>
          state.prototypeComposer.parameters.parameterOrderInTaskInStage[stage.id][taskId],
      )
      .flat()
      .map((parameterId) => state.prototypeComposer.parameters.listById[parameterId]),
  }));

  const approvalNeeded = false;

  const anyParameterHasError = useMemo(() => {
    return parametersInStage.some((parameter) => !!parameter?.errors?.length);
  }, [parametersInStage]);

  const anyTaskHasError = tasksInStage.reduce((anyTaskHasError, task) => {
    return (anyTaskHasError ||= !!task.errors.length);
  }, false);

  const dispatch = useDispatch();

  const copyStageDetails = () => {
    dispatch(
      copyEntities({
        elementId: stage.id,
        type: EntityType.STAGE,
        checklistId: data?.id,
      }),
    );
  };

  // E128 = 'STAGE_MUST_CONTAIN_ATLEAST_ONE_TASK',
  const stageWiseError =
    !!stage.errors.length && errors.find((error) => error.code === 'E128' && error.id === stage.id);

  const stageHasError = anyParameterHasError || anyTaskHasError || stageWiseError;

  return (
    <StageCardWrapper
      ref={ref}
      isActive={isActive}
      onClick={() => {
        if (isReadOnly && isActive) {
          dispatch(
            openOverlayAction({
              type: OverlayNames.EDITING_DISABLED,
              props: { state: data?.state, archived: data?.archived },
            }),
          );
        } else {
          dispatch(setActiveStage({ id: stage.id }));
        }
      }}
      hasError={!!stageHasError}
    >
      <div className="stage-header">
        {!isReadOnly && (
          <div className="order-control">
            <ArrowDropUp
              className="icon"
              fontSize="small"
              onClick={(event) => {
                event.stopPropagation();
                if (!isFirstItem) {
                  dispatch(reOrderStage({ from: index, to: index - 1, id: stage.id }));
                }
              }}
            />
            <ArrowDropDown
              className="icon"
              fontSize="small"
              onClick={(event) => {
                event.stopPropagation();
                if (!isLastItem) {
                  dispatch(reOrderStage({ from: index, to: index + 1, id: stage.id }));
                }
              }}
            />
          </div>
        )}

        <div className="stage-name">Stage {index + 1}</div>
        {!isReadOnly && (
          <div className="stage-actions">
            {isFeatureAllowed('copyElement') && (
              <FileCopyOutlined
                className="icon stage-icon"
                onClick={(event) => {
                  event.stopPropagation();
                  dispatch(
                    openOverlayAction({
                      type: OverlayNames.COPY_ENTITY_MODAL,
                      props: {
                        onPrimary: copyStageDetails,
                        title: 'Copy Stage',
                        body: 'Are you sure you want to copy this Stage?',
                      },
                    }),
                  );
                }}
              />
            )}
            <DeleteOutlined
              className="icon stage-icon"
              onClick={(event) => {
                event.stopPropagation();
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.CONFIRMATION_MODAL,
                    props: {
                      onPrimary: () => dispatch(deleteStage({ id: stage.id })),
                      title: 'Delete Stage',
                      body: (
                        <>
                          <span>You cannot recover your tasks once you delete the stage.</span>
                          <span>Are you sure you want to delete the stage?</span>
                        </>
                      ),
                    },
                  }),
                );
              }}
            />
          </div>
        )}
      </div>

      <div className="stage-body">
        <div className="stage-task-properties">
          {approvalNeeded ? <AssignmentTurnedIn className="icon" id="approval-needed" /> : null}

          {stageHasError && (
            <div className="stage-badge">
              <Error className="icon" />
              <span>Error Found</span>
            </div>
          )}
        </div>
        {stageWiseError && (
          <div className="stage-error-wrapper">
            <ErrorIcon className="stage-error-icon" />
            {stageWiseError.message}
          </div>
        )}
        <Textarea
          defaultValue={stage.name}
          error={!stage.name && stage.errors.find((error) => error.code === 'E303')?.message}
          label="Name the Stage"
          onBlur={(e) => {
            const value = e.target.value;
            dispatch(
              updateStageName({
                id: stage.id,
                name: value,
                orderTree: stage.orderTree,
              }),
            );
          }}
          disabled={isReadOnly}
        />
      </div>
    </StageCardWrapper>
  );
});

StageCard.displayName = 'Stage-Card';

export default StageCard;
