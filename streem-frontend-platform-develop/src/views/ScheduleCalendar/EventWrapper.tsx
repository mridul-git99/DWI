import React from 'react';
import styled from 'styled-components';
import { formatDateTime } from '#utils/timeUtils';
import { InputTypes } from '#utils/globalTypes';
import { useTypedSelector } from '#store';

const EventWrapper = styled.div`
  background: #fff;
  color: #039ae5;
  font-weight: 600;
  padding: 0 4px;
  font-size: 12px;
  box-shadow: 0px 0px 10px #ccc;
  border-radius: 8px;
  border: 1px solid #039ae5;
  margin-top: 4px;
  min-width: min-content;
  display: flex;
  align-items: center;
  overflow: hidden;

  :hover {
    cursor: pointer;
  }

  p {
    margin: 2px 0;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .time {
    font-weight: 400;
    color: #039ae5;
    flex-shrink: 0;
  }

  .title {
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 60px;
    display: inline-block;
  }
`;

interface EventItemProps {
  start: string | number;
  title: string;
}

const EventItem: React.FC<EventItemProps> = ({ start, title }) => {
  const timeZone = useTypedSelector((state) => state.auth.selectedFacility?.timeZone);

  // Backend sends milliseconds timestamp, convert to seconds for formatDateTime
  const startMilliseconds = typeof start === 'string' ? parseInt(start) : start;
  const startEpochSeconds = Math.floor(startMilliseconds / 1000);

  const formattedTime = formatDateTime({
    value: startEpochSeconds,
    type: InputTypes.TIME,
  });

  return (
    <EventWrapper>
      <p>
        <span className="time">{formattedTime}</span>{' '}
        <span className="title" title={title}>
          {title}
        </span>
      </p>
    </EventWrapper>
  );
};

export default EventItem;
