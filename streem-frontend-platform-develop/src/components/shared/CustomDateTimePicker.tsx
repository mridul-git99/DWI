import { useTypedSelector } from '#store';
import { InputTypes } from '#utils/globalTypes';
import CalendarTodayIcon from '@material-ui/icons/CalendarToday';
import { endOfDay, getUnixTime, set } from 'date-fns';
import { utcToZonedTime, zonedTimeToUtc } from 'date-fns-tz';
import React, { FC, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import DatePicker from 'react-datepicker';
import styled from 'styled-components';

const TimePickerWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f0f0f0;
  border: 1px solid #aeaeae;
  border-radius: 4.8px;
  padding: 8px;

  .time-column {
    display: flex;
    flex-direction: column;
    align-items: center;
    overflow-y: auto;
    height: 242px;
    scroll-behavior: smooth;
    border: none;
    flex: 1;

    &::-webkit-scrollbar {
      width: 5px;
    }

    &::-webkit-scrollbar-thumb {
      background-color: #888;
    }
  }

  .time-item {
    padding: 4px 0;
    text-align: center;
    cursor: pointer;
    color: #000;
    background: #fff;
    padding: 8px;
    width: 100%;

    &:hover {
      background-color: #f0f0f0;
      border-radius: 4.8px;
    }

    &.selected {
      font-weight: bold;
      background-color: #216ba5;
      color: white;
      border-radius: 4.8px;
    }
  }
`;

const Wrapper = styled.div<{
  isExceptionEnabled?: boolean;
  isCorrectionInitiated?: boolean;
  isCorrectionCorrected?: boolean;
}>`
  .react-datepicker-wrapper {
    width: 100%;
    align-items: center;

    .react-datepicker__close-icon::after {
      background-color: #fff;
      color: #000;
      font-size: 16px;
    }

    input {
      width: 100%;
      padding: 8px 10px 8px 30px;
      background-color: ${(props) => {
        if (props.isExceptionEnabled || props.isCorrectionCorrected) return '#E7F1FD';
        if (props.isCorrectionInitiated) return '#FFF8E1';
        return '#fff';
      }};
      border: 1px solid #ccc;
      border-left: ${(props) => {
        if (props.isExceptionEnabled || props.isCorrectionCorrected) return '8px solid #005DCC';
        if (props.isCorrectionInitiated) return '8px solid #F1C21B';
        return '1px solid #ccc';
      }};

      &:focus {
        outline: none;
        border-color: rgb(29, 132, 255);
      }
    }

    .icon {
      width: 14px;
      height: 14px;
      color: #000;
      padding: 10px 8px 8px 8px;
    }
  }
`;

const TimePicker = ({ value, onChange, currentSeconds, onChangeSeconds, timeFormat }: any) => {
  const currentTime = new Date();
  const [initialHour, initialMinute] = value
    ? value.split(':').map(Number)
    : [currentTime.getHours(), currentTime.getMinutes()];
  const [selectedHour, setSelectedHour] = useState(initialHour || 0);
  const [selectedMinute, setSelectedMinute] = useState(initialMinute || 0);
  const [selectedSecond, setSelectedSecond] = useState(currentSeconds || 0);
  const [_, _, secondFormat] = timeFormat.split(':');

  const hours = Array.from({ length: 24 }, (_, i) => i);
  const minutes = Array.from({ length: 60 }, (_, i) => i);
  const seconds = Array.from({ length: 60 }, (_, i) => i);

  const handleHourClick = (hour) => {
    setSelectedHour(hour);
    triggerChange(hour, selectedMinute, selectedSecond);
  };

  const handleMinuteClick = (minute) => {
    setSelectedMinute(minute);
    triggerChange(selectedHour, minute, selectedSecond);
  };

  const handleSecondClick = (second) => {
    setSelectedSecond(second);
    onChangeSeconds(second);
    triggerChange(selectedHour, selectedMinute, second);
  };

  const triggerChange = (hour, minute, second) => {
    if (onChange) {
      onChange(hour + ':' + minute + ':' + second, second);
    }
  };

  return (
    <TimePickerWrapper>
      <div className="time-column">
        {hours.map((hour) => (
          <div
            key={hour}
            className={`time-item ${hour === selectedHour ? 'selected' : ''}`}
            onClick={() => handleHourClick(hour)}
          >
            {hour.toString().padStart(2, '0')}
          </div>
        ))}
      </div>
      <div className="time-column">
        {minutes.map((minute) => (
          <div
            key={minute}
            className={`time-item ${minute === selectedMinute ? 'selected' : ''}`}
            onClick={() => handleMinuteClick(minute)}
          >
            {minute.toString().padStart(2, '0')}
          </div>
        ))}
      </div>
      {secondFormat && (
        <div className="time-column">
          {seconds.map((second) => (
            <div
              key={second}
              className={`time-item ${second === selectedSecond ? 'selected' : ''}`}
              onClick={() => handleSecondClick(second)}
            >
              {second.toString().padStart(2, '0')}
            </div>
          ))}
        </div>
      )}
    </TimePickerWrapper>
  );
};

const CustomDateTimePicker: FC<any> = (props) => {
  const {
    parameter,
    parameterValue,
    onChange,
    isDisabled = false,
    isExceptionEnabled = false,
    isCorrectionInitiated = false,
    isCorrectionCorrected = false,
  } = props;
  const [currentSeconds, setCurrentSeconds] = useState<number>(0);

  const { timeZone = 'UTC' } = useTypedSelector((state) => state.auth.selectedFacility) || {};
  const selectedFacility = useTypedSelector((state) => state.auth.selectedFacility);
  const facilityWiseConstants = useTypedSelector((state) => state.facilityWiseConstants);

  const { dateAndTimeStampFormat, dateFormat, timeFormat } =
    facilityWiseConstants[selectedFacility!.id];

  const updating = useTypedSelector((state) => state.job.updating);

  const [value, setValue] = useState<Date | null>(
    parameterValue ? utcToZonedTime(parameterValue * 1000, timeZone) : null,
  );

  const [isCalendarOpen, setIsCalendarOpen] = useState<boolean>(false);

  const isValueChanged = useRef<boolean>(false);

  const type = useMemo(() => {
    return parameter?.type;
  }, [parameter?.type]);

  const formatByType = useMemo(() => {
    switch (type) {
      case InputTypes.DATE_TIME:
        return dateAndTimeStampFormat;
      case InputTypes.DATE:
        return dateFormat;
      case InputTypes.TIME:
        return timeFormat;
      default:
        return 'dd-MM-yyyy HH:mm:ss';
    }
  }, [type, dateAndTimeStampFormat, dateFormat, timeFormat]);

  const getEpoch = (date: Date | null) => {
    let value = '';
    if (date) {
      if (type === InputTypes.TIME) {
        const hours = date.getHours();
        const minutes = date.getMinutes();
        const seconds = currentSeconds;
        value = getUnixTime(
          zonedTimeToUtc(set(new Date(), { hours, minutes, seconds }), timeZone),
        ).toString();
      } else if (type === InputTypes.DATE) {
        const utcDate = zonedTimeToUtc(endOfDay(date), timeZone);
        value = getUnixTime(utcDate).toString();
      } else if (type === InputTypes.DATE_TIME) {
        const now = new Date();
        if (!date.getHours() && !date.getMinutes()) {
          date.setHours(now.getHours(), now.getMinutes(), now.getSeconds());
        } else {
          date.setSeconds(currentSeconds);
        }
        const utcDate = zonedTimeToUtc(date, timeZone);
        value = getUnixTime(utcDate).toString();
      }
    }
    return value;
  };

  useEffect(() => {
    if (!updating && parameterValue !== getEpoch(value) && !isCalendarOpen) {
      setValue(parameterValue ? utcToZonedTime(parameterValue * 1000, timeZone) : null);
    }
  }, [parameterValue, currentSeconds]);

  const highlightedTime = useCallback(
    (time) => {
      if (value) {
        return '';
      }
      const currentTime = new Date();
      const isCurrentTime =
        time.getHours() === currentTime.getHours() &&
        time.getMinutes() === currentTime.getMinutes() &&
        time.getSeconds() === currentTime.getSeconds();

      if (isCurrentTime) {
        return 'highlighted-time';
      }
    },
    [value],
  );

  const highlightDates = useMemo(() => {
    if (value) {
      return [];
    }
    return [
      {
        'react-datepicker__day--highlighted-custom': [new Date()],
      },
    ];
  }, [value]);

  useEffect(() => {
    const portalRoot = document.getElementById('root-portal');
    if (!portalRoot) {
      const div = document.createElement('div');
      div.id = 'root-portal';
      div.style.position = 'relative';
      div.style.zIndex = '1301';
      document.body.appendChild(div);
    }
  }, []);

  return (
    <Wrapper
      isExceptionEnabled={isExceptionEnabled}
      isCorrectionInitiated={isCorrectionInitiated}
      isCorrectionCorrected={isCorrectionCorrected}
      className="date-time-parameter"
    >
      <DatePicker
        selected={value}
        onChange={(date) => {
          isValueChanged.current = true;
          setValue(date);
          if (!date) {
            onChange({ value: '' });
          }
        }}
        isClearable={!isDisabled}
        showIcon={!isDisabled}
        disabled={isDisabled}
        timeFormat={timeFormat}
        dateFormat={formatByType}
        showTimeInput={type === InputTypes.DATE_TIME}
        onCalendarOpen={() => {
          setIsCalendarOpen(true);
        }}
        onCalendarClose={() => {
          if (isValueChanged.current) {
            onChange({ value: getEpoch(value) });
          }
          setIsCalendarOpen(false);
        }}
        disabledKeyboardNavigation
        showPopperArrow={false}
        placeholderText={formatByType}
        icon={<CalendarTodayIcon className="icon" />}
        portalId="root-portal"
        timeClassName={highlightedTime}
        highlightDates={highlightDates}
        timeInputLabel=""
        customTimeInput={
          <TimePicker
            currentSeconds={currentSeconds}
            onChangeSeconds={(val = 0) => {
              setCurrentSeconds(val);
            }}
            timeFormat={timeFormat}
          />
        }
      />
    </Wrapper>
  );
};

export default CustomDateTimePicker;
