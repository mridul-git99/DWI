import {
  addDays,
  addHours,
  differenceInDays,
  differenceInMilliseconds,
  endOfDay,
  format,
  formatDistanceToNowStrict,
  fromUnixTime,
  getUnixTime,
  hoursToSeconds,
  isAfter,
  isBefore,
  minutesToSeconds,
  startOfDay,
  weeksToDays,
  yearsToMonths,
} from 'date-fns';
import { ComparisonOperator, InputTypes } from './globalTypes';
import { utcToZonedTime, zonedTimeToUtc } from 'date-fns-tz';

export const formatDateTime = ({
  value,
  type = InputTypes.DATE_TIME,
  format: _format,
  timezone,
}: {
  value: number | string;
  type?: InputTypes;
  format?: string;
  timezone?: string;
}) => {
  try {
    if (value) {
      const time = typeof value === 'string' ? parseInt(value) : value;
      if (_format && timezone) {
        return format(utcToZonedTime(time * 1000, timezone), _format);
      }
      const {
        auth: { selectedFacility },
        facilityWiseConstants,
      } = window.store.getState();
      const { dateAndTimeStampFormat, dateFormat, timeFormat } =
        facilityWiseConstants[selectedFacility!.id];

      const _timezone = selectedFacility.timeZone || 'UTC';

      if (_format) {
        return format(utcToZonedTime(time * 1000, _timezone), _format);
      }

      return format(
        utcToZonedTime(time * 1000, _timezone),
        type === InputTypes.DATE_TIME
          ? dateAndTimeStampFormat
          : type === InputTypes.DATE
          ? dateFormat
          : timeFormat,
      );
    } else {
      return '-';
    }
  } catch (err) {
    return value;
  }
};

export const convertSecondsToTime = (seconds: number) => {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainingSeconds = seconds % 60;

  return {
    hours,
    minutes,
    seconds: remainingSeconds,
  };
};

export const formatDuration = (seconds: number) => {
  const { hours, minutes, seconds: remainingSeconds } = convertSecondsToTime(seconds);
  const formattedHours = hours.toString().padStart(2, '0');
  const formattedMinutes = minutes.toString().padStart(2, '0');
  const formattedSeconds = remainingSeconds.toString().padStart(2, '0');
  return `${formattedHours}:${formattedMinutes}:${formattedSeconds}`;
};

export const formatDateTimeToHumanReadable = (value: number) => {
  const epochDate = fromUnixTime(value);
  const now = new Date();
  let dateText: string | undefined = formatDistanceToNowStrict(epochDate, { addSuffix: true });
  const startOfDate = startOfDay(epochDate);
  const startOfToday = startOfDay(now);
  const daysDiff = differenceInDays(startOfDate, startOfToday);
  const days: any = {
    '0': 'Today',
    '-1': 'Yesterday',
    '1': 'Tomorrow',
  };
  if (Math.abs(daysDiff) <= 1) {
    dateText = days[daysDiff.toString()];
  } else {
    dateText = undefined;
  }
  if (dateText) {
    return `${dateText}, ${formatDateTime({ value, type: InputTypes.TIME })}`;
  }
  return formatDateTime({ value });
};

export const checkJobExecutionDelay = (actual: number, expected: number) => {
  const actualDate = fromUnixTime(actual);
  const expectedDate = fromUnixTime(expected);
  const difference = differenceInMilliseconds(actualDate, expectedDate);
  if (difference > 0) {
    return true;
  } else {
    return false;
  }
};

export const getLocalTimeOffset = () => {
  const date = new Date();
  const localTimeOffsetMinutes = -date.getTimezoneOffset();
  //We need to negate above because it represents the number of minutes you need to subtract from the current local time to get to Coordinated Universal Time (UTC).
  const hours = Math.floor(Math.abs(localTimeOffsetMinutes) / 60);
  const minutes = Math.abs(localTimeOffsetMinutes) % 60;
  const sign = localTimeOffsetMinutes < 0 ? '-' : '+';
  const formattedHours = hours.toString().padStart(2, '0');
  const formattedMinutes = minutes.toString().padStart(2, '0');
  const localTimeOffset = `${sign}${formattedHours}:${formattedMinutes}`;
  return localTimeOffset;
};

export const getEpochTimeDifference = (epoch: number) => {
  const current = new Date();
  const expectedTime = fromUnixTime(epoch);

  if (isBefore(current, expectedTime)) {
    return 'EARLY';
  } else if (isAfter(current, expectedTime)) {
    return 'LATE';
  } else {
    return 'ON TIME';
  }
};

export const calculateSecondsFromDuration = (duration: Record<string, number>) => {
  let durationSeconds = 0;
  Object.entries(duration).forEach(([key, value]: any) => {
    if (value) {
      switch (key) {
        case 'year':
          durationSeconds += hoursToSeconds(yearsToMonths(value) * 30 * 24);
          break;
        case 'month':
          durationSeconds += hoursToSeconds(value * 30 * 24);
          break;
        case 'week':
          durationSeconds += hoursToSeconds(weeksToDays(value) * 24);
          break;
        case 'day':
          durationSeconds += hoursToSeconds(value * 24);
          break;
        case 'hour':
          durationSeconds += hoursToSeconds(value);
          break;
        case 'minute':
          durationSeconds += minutesToSeconds(value);
          break;
        default:
          break;
      }
    }
  });
  return durationSeconds;
};

export const compareEpochs = (epoch1: number, epoch2: number, operator: ComparisonOperator) => {
  switch (operator) {
    case ComparisonOperator.EQUAL_TO:
      return epoch1 === epoch2;
    case ComparisonOperator.NOT_EQUAL_TO:
      return epoch1 !== epoch2;
    case ComparisonOperator.LESS_THAN:
      return epoch1 < epoch2;
    case ComparisonOperator.LESS_THAN_OR_EQUAL_TO:
      return epoch1 <= epoch2;
    case ComparisonOperator.GREATER_THAN:
      return epoch1 > epoch2;
    case ComparisonOperator.GREATER_THAN_OR_EQUAL_TO:
      return epoch1 >= epoch2;
    default:
      throw new Error('Invalid operator');
  }
};

export const adjustDateByDaysAtEndOfDay = (
  epochSeconds: number,
  daysOffsetStr: string,
  timezone: string,
): number => {
  const daysOffset = parseInt(daysOffsetStr, 10);
  const date = fromUnixTime(epochSeconds);
  const zonedDate = utcToZonedTime(date, timezone);
  const adjustedDate = addDays(zonedDate, daysOffset);
  const endOfDayDate = endOfDay(adjustedDate);
  return getUnixTime(endOfDayDate);
};

export const addHoursOffsetToTime = (epochSeconds: number, hoursOffsetStr: string): number => {
  const hoursOffset: number = parseInt(hoursOffsetStr);
  const date = fromUnixTime(epochSeconds);
  const adjustedDate = addHours(date, hoursOffset);
  return getUnixTime(adjustedDate);
};

export function getStartOfDayEpochInTimezone({
  timezone,
  value,
}: {
  value: number;
  timezone?: string;
}) {
  let _timezone;
  if (!!timezone) {
    _timezone = timezone;
  } else {
    const {
      auth: { selectedFacility },
    } = window.store.getState();

    _timezone = selectedFacility.timeZone || 'UTC';
  }

  const date = fromUnixTime(value);
  const dateInTimezone = utcToZonedTime(date, _timezone);
  const startOfDayInTimezone = startOfDay(dateInTimezone);
  const utcStartOfDay = zonedTimeToUtc(startOfDayInTimezone, _timezone);
  return getUnixTime(utcStartOfDay);
}
