import { FormGroup } from '#components';
import { InputTypes } from '#utils/globalTypes';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import { compact } from 'lodash';
import { formatDateTime } from '#utils/timeUtils';
import { fromUnixTime, getUnixTime, getWeekOfMonth, setDefaultOptions } from 'date-fns';
import React, { FC, useEffect, useRef } from 'react';
import { RRule, Weekday } from 'rrule';
import { calculateSecondsFromDuration } from '#utils/timeUtils';
import { zonedTimeToUtc } from 'date-fns-tz';
import { useTypedSelector } from '#store';

export interface SchedulerProps {
  form: any;
  readOnly?: boolean;
}

setDefaultOptions({ weekStartsOn: 1 });

function getMonthlyOption(unixExpectedStartDate: number) {
  const mJsDate = fromUnixTime(unixExpectedStartDate);
  const weekNo = getWeekOfMonth(mJsDate, {
    weekStartsOn: 1,
  });
  return {
    label: `${
      weekNo === 1
        ? 'First'
        : weekNo === 2
        ? 'Second'
        : weekNo === 3
        ? 'Third'
        : weekNo === 4
        ? 'Fourth'
        : 'Last'
    } ${formatDateTime({ value: unixExpectedStartDate, format: 'iiii' })} of every month`,
    weekNo,
  };
}

const ALL_WEEKDAYS = ['MO', 'TU', 'WE', 'TH', 'FR', 'SA', 'SU'];

const getUpdatedWeekDays = (weekDayIndex: number, weekDays?: Record<string, boolean>) => {
  const enumIndex = weekDayIndex - 1;
  const weekDay = ALL_WEEKDAYS[enumIndex];
  if (weekDay) {
    let updatedWeekDays = { ...weekDays };
    updatedWeekDays = {
      ...updatedWeekDays,
      [enumIndex]: !updatedWeekDays?.[enumIndex],
    };
    return { ...updatedWeekDays };
  }
};

export const Scheduler: FC<SchedulerProps> = ({ form, readOnly }) => {
  const { timeZone = 'UTC' } = useTypedSelector((state) => state.auth.selectedFacility) || {};
  const {
    selectedFacility: { dateFormat, timeFormat },
  } = useTypedSelector((state) => state.auth);

  const {
    register,
    unregister,
    setValue,
    watch,
    errors,
    getValues,
    formState: { dirtyFields },
  } = form;

  const {
    expectedStartDate,
    recurrence,
    dueDateDuration,
    repeatEvery,
    repeatCount,
    rRuleOptions,
    weekDays,
  } = watch([
    'expectedStartDate',
    'recurrence',
    'dueDateDuration',
    'repeatEvery',
    'repeatCount',
    'rRuleOptions',
    'weekDays',
  ]);

  register('recurrence', {
    required: true,
  });
  register('dueDateInterval', {
    required: true,
  });
  register('weekDays', {
    validate: (value: Record<string, boolean>) => {
      const _recurrence = getValues('recurrence');
      const _repeatEvery = getValues('repeatEvery');
      if (_recurrence === 'custom' && _repeatEvery === 'WEEKLY') {
        if (!Object.values(value).some((weekDay) => weekDay === true)) {
          return 'Please select at least one day';
        }
      }
    },
  });

  register('repeatEvery');

  const { year, month, week, day, hour, minute } = dueDateDuration;
  const unixExpectedStartDate = getUnixTime(zonedTimeToUtc(expectedStartDate, timeZone));

  useEffect(() => {
    if (dueDateDuration) {
      updateDueDateInterval();
    }
  }, [year, month, week, day, hour, minute]);

  useEffect(() => {
    setValue(
      'rRuleOptions',
      {
        byweekday: compact(Object.keys(weekDays || {}))
          .filter((key) => weekDays[key] !== false)
          .map((key) => new Weekday(Number(key))),
      },
      {
        shouldValidate: true,
      },
    );
  }, [weekDays]);

  useEffect(() => {
    if (dirtyFields?.recurrence) {
      if (recurrence === 'custom') {
        setValue('repeatEvery', 'DAILY', {
          shouldValidate: true,
        });
      } else {
        setValue('repeatEvery', null, {
          shouldValidate: true,
        });
      }
    }
  }, [recurrence]);

  const prevExpectedStartDate = useRef(expectedStartDate);

  useEffect(() => {
    if (expectedStartDate && !readOnly) {
      unregister('rRuleOptions');
      unregister('weekDays');
      if (prevExpectedStartDate.current !== expectedStartDate) {
        setValue('recurrence', 'DAILY', {
          shouldValidate: true,
          shouldDirty: true,
        });
      }
      prevExpectedStartDate.current = expectedStartDate;
    }
  }, [expectedStartDate]);

  const updateDueDateInterval = () => {
    const durationSeconds = calculateSecondsFromDuration(dueDateDuration);
    setValue('dueDateInterval', durationSeconds, {
      shouldValidate: true,
    });
  };

  const updateWeekDays = (weekDayIndex: number) => {
    const updated = getUpdatedWeekDays(weekDayIndex, weekDays);
    setValue('weekDays', updated, { shouldValidate: true });
  };

  const getDueOnSummary = () => {
    if (dueDateDuration) {
      const { isValid, values } = Object.entries(dueDateDuration).reduce<any>(
        (acc, [key, value]: any) => {
          if (value) {
            acc.isValid = true;
            acc.values.push(` ${value} ${value > 1 ? `${key}s` : key}`);
          }
          return acc;
        },
        { isValid: false, values: [] },
      );
      if (isValid) {
        return [
          {
            label: 'Due in',
            value: values.join() + ' from Start',
          },
        ];
      }
    }
    return [];
  };

  const getRecurrenceSummary = () => {
    try {
      if (recurrence !== 'none') {
        const freq = recurrence === 'custom' ? repeatEvery : recurrence;
        const rule = new RRule({
          freq: RRule[freq as keyof typeof RRule],
          interval: repeatCount || 1,
          dtstart: new Date(expectedStartDate),
          ...(rRuleOptions || {}),
        });
        let recurrenceString = rule?.toText() || null;
        if (recurrenceString) {
          if (recurrence === 'custom') {
            switch (freq) {
              case 'DAILY':
              case 'WEEKLY':
              case 'MONTHLY':
                recurrenceString = `Repeat ${recurrenceString} at ${formatDateTime({
                  value: unixExpectedStartDate,
                  type: InputTypes.TIME,
                })}`;
                break;
              case 'YEARLY':
                recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                  value: unixExpectedStartDate,
                  format: `do MMMM 'at' ${timeFormat}`,
                })}`;
                break;

              default:
                break;
            }
          } else {
            switch (freq) {
              case 'DAILY':
                recurrenceString = `Repeat ${recurrenceString} at ${formatDateTime({
                  value: unixExpectedStartDate,
                  type: InputTypes.TIME,
                })}`;
                break;
              case 'WEEKLY':
                recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                  value: unixExpectedStartDate,
                  format: `iiii 'at' ${timeFormat}`,
                })}`;
                break;
              case 'MONTHLY':
                recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                  value: unixExpectedStartDate,
                  format: `do 'at' ${timeFormat}`,
                })}`;
                break;
              case 'YEARLY':
                recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                  value: unixExpectedStartDate,
                  format: `do MMMM 'at' ${timeFormat}`,
                })}`;
                break;

              default:
                break;
            }
          }
          return [
            {
              label: 'Recurrence',
              value: recurrenceString,
            },
          ];
        }
      }
      return [];
    } catch (e) {
      console.error('Error while creating recurrence string', e);
      return [];
    }
  };

  let recurrenceOptions: any[] = [];

  let monthlyOptions: any[] = [];

  if (expectedStartDate) {
    recurrenceOptions = [
      {
        label: `Daily at ${formatDateTime({
          value: unixExpectedStartDate,
          type: InputTypes.TIME,
        })}`,
        value: 'DAILY',
      },
      {
        label: `Weekly on ${formatDateTime({
          value: unixExpectedStartDate,
          format: `iiii 'at' ${timeFormat}`,
        })}`,
        value: 'WEEKLY',
      },
      {
        label: `Monthly on ${formatDateTime({
          value: unixExpectedStartDate,
          format: `do 'at' ${timeFormat}`,
        })}`,
        value: 'MONTHLY',
      },
      {
        label: `Annually on ${formatDateTime({
          value: unixExpectedStartDate,
          format: `do MMMM 'at' ${timeFormat}`,
        })}`,
        value: 'YEARLY',
      },
      {
        label: 'Custom',
        value: 'custom',
      },
    ];

    const { label, weekNo } = getMonthlyOption(unixExpectedStartDate);
    monthlyOptions = [
      {
        label: `${formatDateTime({
          value: unixExpectedStartDate,
          format: 'do',
        })} of every month`,
        value: {
          bymonthday: formatDateTime({
            value: unixExpectedStartDate,
            format: 'dd',
          }),
        },
      },
      {
        label,
        value: {
          byweekday: new Weekday(
            Number(formatDateTime({ value: unixExpectedStartDate, format: 'e' })) - 1,
            weekNo > 4 ? 4 - weekNo : weekNo,
          ),
        },
      },
    ];
  }

  const repeatEveryOptions = [
    {
      label: repeatCount > 1 ? 'Days' : 'Day',
      value: 'DAILY',
    },
    {
      label: repeatCount > 1 ? 'Weeks' : 'Week',
      value: 'WEEKLY',
    },
    {
      label: repeatCount > 1 ? 'Months' : 'Month',
      value: 'MONTHLY',
    },
    {
      label: repeatCount > 1 ? 'Years' : 'Year',
      value: 'YEARLY',
    },
  ];

  const renderSchedulerSummary = () => {
    return (
      <div className="scheduler-summary">
        <h4>Summary</h4>
        <ReadOnlyGroup
          className="read-only-group"
          items={[
            {
              label: 'Start Date and Time',
              value: formatDateTime({
                value: unixExpectedStartDate,
                format: `${dateFormat} 'at' ${timeFormat}`,
              }),
            },
            ...getDueOnSummary(),
            ...getRecurrenceSummary(),
          ]}
        />
      </div>
    );
  };

  return !readOnly ? (
    <>
      <FormGroup
        key="basic-info-section"
        inputs={[
          {
            type: InputTypes.DATE_TIME,
            props: {
              placeholder: 'Start Date & Time',
              label: 'Start Date & Time',
              id: 'expectedStartDate',
              name: 'expectedStartDate',
              ref: register({
                required: true,
              }),
            },
          },
        ]}
      />
      <p className="custom-label">Due After</p>
      <div className="due-after-section">
        <FormGroup
          key="due-after-section"
          inputs={[
            {
              type: InputTypes.NUMBER,
              props: {
                placeholder: 'Year',
                label: 'Year',
                id: 'dueDateDuration.year',
                name: 'dueDateDuration.year',
                defaultValue: 0,
                error: !!errors?.dueDateDuration?.year,
                ref: register({
                  required: true,
                  valueAsNumber: true,
                  min: 0,
                }),
              },
            },
            {
              type: InputTypes.NUMBER,
              props: {
                placeholder: 'Month',
                label: 'Month',
                id: 'dueDateDuration.month',
                name: 'dueDateDuration.month',
                defaultValue: 0,
                error: !!errors?.dueDateDuration?.month,
                ref: register({
                  required: true,
                  valueAsNumber: true,
                  min: 0,
                }),
              },
            },
            {
              type: InputTypes.NUMBER,
              props: {
                placeholder: 'Week',
                label: 'Week',
                id: 'dueDateDuration.week',
                name: 'dueDateDuration.week',
                error: !!errors?.dueDateDuration?.week,
                defaultValue: 0,
                ref: register({
                  required: true,
                  valueAsNumber: true,
                  min: 0,
                }),
              },
            },
            {
              type: InputTypes.NUMBER,
              props: {
                placeholder: 'Day',
                label: 'Day',
                id: 'dueDateDuration.day',
                name: 'dueDateDuration.day',
                error: !!errors?.dueDateDuration?.day,
                defaultValue: 0,
                ref: register({
                  required: true,
                  valueAsNumber: true,
                  min: 0,
                }),
              },
            },
            {
              type: InputTypes.NUMBER,
              props: {
                placeholder: 'Hour',
                label: 'Hour',
                id: 'dueDateDuration.hour',
                name: 'dueDateDuration.hour',
                error: !!errors?.dueDateDuration?.hour,
                defaultValue: 0,
                ref: register({
                  required: true,
                  valueAsNumber: true,
                  min: 0,
                }),
              },
            },
            {
              type: InputTypes.NUMBER,
              props: {
                placeholder: 'Minute',
                label: 'Minute',
                id: 'dueDateDuration.minute',
                name: 'dueDateDuration.minute',
                error: !!errors?.dueDateDuration?.minute,
                defaultValue: 0,
                ref: register({
                  required: true,
                  valueAsNumber: true,
                  min: 0,
                }),
              },
            },
          ]}
        />
      </div>
      <FormGroup
        key="recurrence-section"
        inputs={[
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'recurrence',
              label: 'Recurrence',
              options: recurrenceOptions,
              placeholder: 'Select Recurrence',
              value: recurrence
                ? recurrenceOptions?.filter((curr) => curr?.value === recurrence)
                : null,
              isDisabled: !expectedStartDate,
              onChange: (option: { value: string }) => {
                setValue('repeatEvery', null);
                setValue('recurrence', option.value, {
                  shouldDirty: true,
                  shouldValidate: true,
                });
              },
            },
          },
        ]}
      />
      {recurrence === 'custom' && (
        <>
          <p className="custom-label">Repeat Every</p>
          <div className="custom-recurrence-section">
            <FormGroup
              key="custom-recurrence-section"
              inputs={[
                {
                  type: InputTypes.NUMBER,
                  props: {
                    placeholder: 'Repeat Every',
                    label: '',
                    id: 'repeatCount',
                    name: 'repeatCount',
                    defaultValue: 1,
                    ref: register({
                      required: true,
                      valueAsNumber: true,
                      min: 1,
                    }),
                  },
                },
                {
                  type: InputTypes.SINGLE_SELECT,
                  props: {
                    id: 'repeatEvery',
                    label: '',
                    options: repeatEveryOptions,
                    placeholder: 'Select',
                    value: repeatEveryOptions.filter((o) => o.value === repeatEvery),
                    onChange: (option: { value: string }) => {
                      setValue('repeatEvery', option.value, {
                        shouldDirty: true,
                        shouldValidate: true,
                      });
                      if (['MONTHLY', 'WEEKLY'].includes(option.value)) {
                        register('rRuleOptions', {
                          required: true,
                        });
                        if (option.value === 'MONTHLY') {
                          setValue('rRuleOptions', monthlyOptions[0].value, {
                            shouldValidate: true,
                          });
                        } else {
                          updateWeekDays(
                            Number(
                              formatDateTime({
                                value: unixExpectedStartDate,
                                format: 'e',
                              }),
                            ),
                          );
                        }
                      } else {
                        unregister('rRuleOptions');
                      }
                    },
                  },
                },
              ]}
            />
          </div>
        </>
      )}
      {repeatEvery === 'MONTHLY' && (
        <div className="custom-recurrence-section">
          <FormGroup
            key="custom-recurrence-monthly-section"
            inputs={[
              {
                type: InputTypes.SINGLE_SELECT,
                props: {
                  id: 'rRuleOptions',
                  label: 'Repeat on',
                  options: monthlyOptions,
                  placeholder: 'Select',
                  defaultValue: monthlyOptions[0],
                  onChange: (option: { value: string }) => {
                    setValue('rRuleOptions', option.value, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                  },
                },
              },
            ]}
          />
        </div>
      )}
      {repeatEvery === 'WEEKLY' && (
        <div className="custom-recurrence-section" style={{ flexDirection: 'column' }}>
          <p className="custom-label">Repeat on</p>
          <div className="week-day-container">
            {ALL_WEEKDAYS.map((weekDay, index) => (
              <span
                className="week-day"
                style={
                  weekDays?.[index.toString()] ? { backgroundColor: '#1d84ff', color: '#fff' } : {}
                }
                onClick={() => updateWeekDays(index + 1)}
              >
                {weekDay}
              </span>
            ))}
          </div>
        </div>
      )}
      {expectedStartDate && renderSchedulerSummary()}
    </>
  ) : (
    renderSchedulerSummary()
  );
};
