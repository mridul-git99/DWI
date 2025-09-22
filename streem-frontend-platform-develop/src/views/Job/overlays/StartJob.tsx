import React, { FC } from 'react';
import styled from 'styled-components';
import { formatDateTime } from '#utils/timeUtils';
import { Error } from '@material-ui/icons';
import { NotificationType } from '#components/Notification/types';
import { getUnixTime } from 'date-fns';
import AlertBar from '#components/shared/AlertBar';

const Wrapper = styled.div`
  p {
    margin: 0px;
    font-size: 14px;
    color: #525252;
    line-height: 16px;
  }

  .warning {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    background: #fff8e1;
    padding: 16px;
    color: #f1c21b;
  }
`;

const StartJobModalBody: FC<{ isJobStartingEarly: boolean; expectedStartDate: number | null }> = ({
  isJobStartingEarly,
  expectedStartDate,
}) => {
  return (
    <Wrapper>
      <div>
        <p>It looks like you're ready to start working on this Job.</p>
        <br />
        {!isJobStartingEarly && (
          <p>
            Before you begin, you need to press the ‘Start Job’
            <br /> button.
          </p>
        )}

        {isJobStartingEarly && (
          <AlertBar
            msgText={`You are initiating this job at ${formatDateTime({
              value: getUnixTime(new Date()),
            })}, which is earlier than the scheduled start time of ${formatDateTime({
              value: expectedStartDate!,
            })}.`}
            msgType={NotificationType.WARNING}
            Icon={Error}
          />
        )}
      </div>
    </Wrapper>
  );
};

export default StartJobModalBody;
