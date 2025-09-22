import { BaseModal, Button, LoadingContainer } from '#components';
import { useTypedSelector } from '#store';
import { ChevronLeft, ChevronRight } from '@material-ui/icons';
import {
  add,
  eachDayOfInterval,
  endOfMonth,
  endOfWeek,
  format,
  getDay,
  isSameMonth,
  isToday,
  parse,
  startOfToday,
  startOfWeek,
  startOfHour,
} from 'date-fns';
import { capitalize } from 'lodash';
import React, { useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import EventWrapper from './EventWrapper';
import { formatDateTime } from '#utils/timeUtils';
import { InputTypes } from '#utils/globalTypes';

const Wrapper = styled.div`
  height: 100%;
  display: grid;
  grid-template-areas:
    'top-header'
    'header'
    'content';
  grid-template-rows: auto auto 1fr;
  padding-top: 16px;

  .top-header {
    grid-area: top-header;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title {
      font-weight: 600;
      font-size: 18px;
    }

    .actions {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
      .action-icon {
        cursor: pointer;
        :hover {
          color: #039ae5;
        }
      }
    }
  }

  .content,
  .header {
    display: grid;
    grid-template-columns: repeat(7, minmax(0, 1fr));
    place-items: center;
    grid-area: header;
    overflow-y: scroll;
    border: 1px solid #e2e8f0;
  }

  .content {
    grid-area: content;
  }

  .day {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-block: 4px;
  }

  .text-gray-400 {
    color: rgb(156 163 175 / 1);
  }

  .text-gray-900 {
    color: rgb(17 24 39 / 1);
  }

  .text-white {
    color: rgb(255 255 255 / 1);
  }

  .bg-today {
    background-color: #039ae5;
  }

  .column {
    width: 100%;
    border: 1px solid #e2e8f0;
    height: 100%;
    padding: 8px;
    display: flex;
    flex-direction: column;

    .date {
      align-self: center;
      border-radius: 50%;
      padding: 4px;
    }
  }

  .col-start-2 {
    grid-column-start: 2;
  }
  .col-start-3 {
    grid-column-start: 3;
  }
  .col-start-4 {
    grid-column-start: 4;
  }
  .col-start-5 {
    grid-column-start: 5;
  }
  .col-start-6 {
    grid-column-start: 6;
  }
  .col-start-7 {
    grid-column-start: 7;
  }
`;

const moreButtonStyle = {
  fontSize: '11px',
  background: 'none',
  color: '#039ae5',
  cursor: 'pointer',
  textDecoration: 'underline',
  padding: '2px 6px',
  display: 'inline-block',
};

const hourStyle = {
  fontSize: '10px',
  borderBottom: '1px solid #ccc',
  marginBottom: '8px',
};

export const CalendarContent = ({ fetchData }: any) => {
  const today = startOfToday();
  const isLoading = useTypedSelector((state) => state.schedule.isLoading);
  const list = useTypedSelector((state) => state.schedule.list);
  const days = useMemo(() => ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'], []);
  const colStartClasses = useMemo(
    () => [
      '',
      'col-start-2',
      'col-start-3',
      'col-start-4',
      'col-start-5',
      'col-start-6',
      'col-start-7',
    ],
    [],
  );
  const [isMonthView, setIsMonthView] = useState(true);
  const [currMonth, setCurrMonth] = useState(() => format(today, 'MMM-yyyy'));
  const firstDayOfMonth = parse(currMonth, 'MMM-yyyy', new Date());

  const [currWeekStart, setCurrWeekStart] = useState(() => startOfWeek(today, { weekStartsOn: 0 }));

  const start = useMemo(() => {
    return isMonthView ? startOfWeek(firstDayOfMonth, { weekStartsOn: 0 }) : currWeekStart;
  }, [firstDayOfMonth, isMonthView, currWeekStart]);

  const end = useMemo(() => {
    return isMonthView
      ? endOfWeek(endOfMonth(firstDayOfMonth), { weekStartsOn: 0 })
      : endOfWeek(currWeekStart, { weekStartsOn: 0 });
  }, [firstDayOfMonth, isMonthView, currWeekStart]);

  const daysInMonth = eachDayOfInterval({
    start,
    end,
  });

  const getPrevMonth = (event: React.MouseEvent<SVGSVGElement>) => {
    event.preventDefault();
    const firstDayOfPrevMonth = add(firstDayOfMonth, { months: -1 });
    setCurrMonth(format(firstDayOfPrevMonth, 'MMM-yyyy'));
  };

  const getNextMonth = (event: React.MouseEvent<SVGSVGElement>) => {
    event.preventDefault();
    const firstDayOfNextMonth = add(firstDayOfMonth, { months: 1 });
    setCurrMonth(format(firstDayOfNextMonth, 'MMM-yyyy'));
  };

  const getPrevWeek = (event: React.MouseEvent<SVGSVGElement>) => {
    event.preventDefault();
    const prevWeek = add(currWeekStart, { weeks: -1 });
    setCurrWeekStart(prevWeek);
  };

  const getNextWeek = (event: React.MouseEvent<SVGSVGElement>) => {
    event.preventDefault();
    const nextWeek = add(currWeekStart, { weeks: 1 });
    setCurrWeekStart(nextWeek);
  };

  useEffect(() => {
    if (start && end) {
      const startEpoch = start.getTime();
      const endEpoch = end.getTime();
      fetchData(startEpoch, endEpoch);
    }
  }, [start.getTime(), end.getTime()]);

  const title = isMonthView
    ? format(firstDayOfMonth, 'MMMM yyyy')
    : start.getMonth() === end.getMonth()
    ? `${format(start, 'MMM d')} – ${format(end, 'd, yyyy')}`
    : `${format(start, 'MMM d')} – ${format(end, 'MMM d, yyyy')}`;

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedEvents, setSelectedEvents] = useState<any[]>([]);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const handleShowMore = (epoch: number) => {
    setSelectedEvents(list[epoch]);
    setIsModalOpen(true);
    setSelectedDate(new Date(epoch));
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setSelectedEvents([]);
    setSelectedDate(null);
  };

  const groupEventsByHour = (events: any[]) => {
    return events.reduce((acc, event) => {
      const hour = startOfHour(new Date(event.start)).toISOString();
      if (!acc[hour]) {
        acc[hour] = [];
      }
      acc[hour].push(event);
      return acc;
    }, {} as Record<string, any[]>);
  };

  const groupedEvents = groupEventsByHour(selectedEvents);

  return (
    <LoadingContainer
      loading={isLoading}
      component={
        <Wrapper>
          <div className="top-header">
            <span className="title">{title}</span>
            <div className="actions">
              <Button onClick={() => setIsMonthView((p) => !p)}>
                {isMonthView ? 'Weekly' : 'Monthly'}
              </Button>
              <>
                <ChevronLeft
                  className="action-icon"
                  onClick={isMonthView ? getPrevMonth : getPrevWeek}
                />
                <ChevronRight
                  className="action-icon"
                  onClick={isMonthView ? getNextMonth : getNextWeek}
                />
              </>
            </div>
          </div>
          <div className="header">
            {days.map((day, idx) => {
              return (
                <div key={idx} className="day">
                  {capitalize(day)}
                </div>
              );
            })}
          </div>
          <div className="content">
            {daysInMonth.map((day, idx) => {
              const epoch = day.getTime();
              return (
                <div key={idx} className={`${colStartClasses[getDay(day)]} column`}>
                  <span
                    className={`date h-8 w-8 rounded-full hover:text-white ${
                      isSameMonth(day, today) ? 'text-gray-900' : 'text-gray-400'
                    } ${!isToday(day) && 'hover:bg-blue-500'} ${
                      isToday(day) && 'bg-today text-white'
                    }`}
                  >
                    {format(day, 'd')}
                  </span>
                  {list[epoch] && (
                    <div>
                      {list[epoch].slice(0, 4).map((item: any, idx: number) => (
                        <EventWrapper key={idx} start={item.start} title={item.title} />
                      ))}
                      {list[epoch].length > 4 && (
                        <Button onClick={() => handleShowMore(epoch)} style={moreButtonStyle}>
                          {list[epoch].length - 4} more
                        </Button>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
            {isModalOpen && (
              <BaseModal
                closeModal={closeModal}
                closeAllModals={closeModal}
                title={`Events on ${selectedDate ? format(selectedDate, 'MMMM d, yyyy') : ''}`}
                showFooter={false}
              >
                <div>
                  {Object.keys(groupedEvents).map((hour) => {
                    const hourEpochSeconds = Math.floor(new Date(hour).getTime() / 1000);

                    // Use formatDateTime with facility timezone
                    const formattedHour = formatDateTime({
                      value: hourEpochSeconds,
                      type: InputTypes.TIME,
                    });

                    return (
                      <div key={hour}>
                        <h5 style={hourStyle}>{formattedHour}</h5>
                        {groupedEvents[hour].map((item: any, idx: number) => (
                          <EventWrapper key={idx} start={item.start} title={item.title} />
                        ))}
                      </div>
                    );
                  })}
                </div>
              </BaseModal>
            )}
          </div>
        </Wrapper>
      }
    />
  );
};
