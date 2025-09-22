import React, { FC, useMemo, useRef } from 'react';
import { useDispatch } from 'react-redux';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import { Select } from '#components';
import { getRangeFilter } from '#utils/smartFilterUtils';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { formatDateTime } from '#utils/timeUtils';
import { RangeFilter } from '#views/Inbox/ListView/types';

type TDateRangeFilterProps = {
  placeholder?: string;
  filterKey: string;
  filterValue: (key: string) => Record<string, any>;
  applyFilter: (key: string, value: any) => void;
  removeFilter: (key: string) => void;
  isDisabled?: boolean;
};

const DropdownIcon = () => <ArrowDropDownIcon fontSize="small" />;

const DateRangeFilter: FC<TDateRangeFilterProps> = ({
  placeholder = 'Start Date',
  filterKey,
  filterValue,
  applyFilter,
  removeFilter,
  isDisabled = false,
}) => {
  const dispatch = useDispatch();

  const dateRangeRef = useRef<{ start: number | null; end: number | null }>({
    start: null,
    end: null,
  });

  const handleCustomRangeFilter = (data: {
    expectedStartDate: number;
    expectedEndDate: number;
  }) => {
    const { expectedStartDate, expectedEndDate } = data;
    dateRangeRef.current = {
      start: expectedStartDate,
      end: expectedEndDate,
    };
    const range = `${formatDateTime({
      value: expectedStartDate,
    })} to ${formatDateTime({
      value: expectedEndDate,
    })}`;
    applyFilter(filterKey, {
      label: {
        label: range,
        value: RangeFilter.CUSTOM,
      },
      value: getRangeFilter({
        type: RangeFilter.CUSTOM,
        customStart: expectedStartDate,
        customEnd: expectedEndDate,
      }),
    });
  };

  const resetDates = () => {
    dateRangeRef.current = { start: null, end: null };
  };

  const options = useMemo(() => {
    return Object.values(RangeFilter).map((value) => ({ label: value, value }));
  }, []);

  return (
    <Select
      placeholder={placeholder}
      floatingLabel={true}
      components={{ DropdownIndicator: DropdownIcon, IndicatorSeparator: null }}
      options={options}
      value={filterValue(filterKey)}
      showTooltip={true}
      onChange={(data) => {
        if (data?.value) {
          if (data?.value === RangeFilter.CUSTOM) {
            dispatch(
              openOverlayAction({
                type: OverlayNames.RANGE_FILTER_MODAL,
                props: {
                  defaultStartDate: dateRangeRef.current.start,
                  defaultEndDate: dateRangeRef.current.end,
                  onSubmit: handleCustomRangeFilter,
                  onCancel: () => {
                    removeFilter(filterKey);
                    resetDates();
                  },
                },
              }),
            );
          } else {
            applyFilter(filterKey, {
              label: {
                label: data.label,
                value: data.value,
              },
              value: getRangeFilter({ type: data?.value }),
            });
            resetDates();
          }
        } else {
          removeFilter(filterKey);
          resetDates();
        }
      }}
      isClearable={true}
      isDisabled={isDisabled}
    />
  );
};

export default DateRangeFilter;
