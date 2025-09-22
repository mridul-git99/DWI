import React, { FC } from 'react';
import { useDispatch } from 'react-redux';
import Tooltip from '#components/shared/Tooltip';
import { getFullName } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { User } from '#services/users';

interface ReasonTagProps {
  startedBy: User;
  startedAt: number;
  reason: string;
  reasonType?: 'start' | 'end';
  modalTitle: any;
  badgeText: any;
}

const ReasonTag: FC<ReasonTagProps> = ({
  startedBy,
  startedAt,
  reason,
  reasonType,
  modalTitle,
  badgeText,
}) => {
  const dispatch = useDispatch();

  const getToolTipText = () => {
    return `${badgeText} by ${getFullName(startedBy)} on ${formatDateTime({
      value: startedAt,
    })} . Click the tag to view the reason.`;
  };

  return (
    <div className="reason-wrapper">
      <Tooltip title={getToolTipText()} arrow textAlignment="left">
        <div
          className="badge"
          onClick={() => {
            dispatch(
              openOverlayAction({
                type: OverlayNames.VIEW_REASON_MODAL,
                props: {
                  title: modalTitle,
                  reasonType: reasonType,
                  reason: reason,
                  userDetails: startedBy,
                  timeStamp: startedAt,
                },
              }),
            );
          }}
        >
          {badgeText}
        </div>
      </Tooltip>
    </div>
  );
};

export default ReasonTag;
