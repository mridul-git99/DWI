import React, { FC } from 'react';
import { Popover, Zoom } from '@material-ui/core';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import ReviewerAssignmentModal from '#PrototypeComposer/Overlays/ReviewerAssignmentModal';
import { Checklist } from '#PrototypeComposer/checklist.types';

//TODO: make this common with the task user assignment
export const ReviewerAssignmentPopover: FC<
  CommonOverlayProps<{
    checklistId: Checklist['id'];
  }>
> = ({ closeOverlay, closeAllOverlays, popOverAnchorEl, type, props: { checklistId } }) => {
  return (
    <Popover
      id={`ReviewerAssginmentPopOver_${checklistId}`}
      open={!!popOverAnchorEl}
      anchorEl={popOverAnchorEl}
      TransitionComponent={Zoom}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'center',
      }}
      transformOrigin={{
        vertical: 'top',
        horizontal: 'right',
      }}
    >
      <ReviewerAssignmentModal
        props={{ checklistId, isModal: false }}
        closeAllOverlays={closeAllOverlays}
        closeOverlay={closeOverlay}
        type={type}
        key="ReviewerAssginementPopOver"
      />
    </Popover>
  );
};
