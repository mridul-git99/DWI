import { createFetchList } from '#hooks/useFetchData';
import { transformDataToOptions } from '#utils';
import { FilterOperators } from '#utils/globalTypes';
import { Search } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useState } from 'react';
import { Select } from './Select';
import { selectStyles } from '#utils/selectStyleUtils';

type SearchFilterSelectProps = {
  selectedDropDownOption: any;
  updateFilterFields: (fields: any) => void;
  defaultValue?: any;
};

const styles = {
  ...selectStyles,
  control: (styles, { isDisabled }) => ({
    ...selectStyles?.control(styles, { isDisabled }),
    padding: '2px',
  }),
};

const SearchFilterSelect: FC<SearchFilterSelectProps> = ({
  selectedDropDownOption,
  updateFilterFields,
  defaultValue,
}) => {
  const { field, operator, url, urlFilterField, urlParams, labelKey, valueKey } =
    selectedDropDownOption;

  const { list, reset, fetchNext } = createFetchList(url, urlParams, false);

  const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);

  return (
    <Select
      placeholder="Search..."
      components={{
        DropdownIndicator: () => <Search />,
        IndicatorSeparator: null,
      }}
      menuIsOpen={isMenuOpen}
      styles={styles}
      isClearable
      value={defaultValue}
      options={transformDataToOptions(list, labelKey, valueKey)}
      onChange={(option) => {
        if (option) {
          updateFilterFields([
            {
              field,
              op: operator,
              values: [option.value],
              label: {
                label: option.label,
                value: option.value,
              },
            },
          ]);
        } else {
          updateFilterFields([]);
        }
      }}
      onMenuScrollToBottom={fetchNext}
      onInputChange={debounce((value, actionMeta) => {
        if (value) {
          setIsMenuOpen(true);
        } else {
          setIsMenuOpen(false);
        }

        if (value && value !== actionMeta.prevInputValue)
          reset({
            url,
            params: {
              ...urlParams,
              filters: {
                op: FilterOperators.AND,
                fields: [
                  ...(urlParams.filters && urlParams.filters.fields
                    ? urlParams.filters.fields
                    : []),
                  {
                    field: urlFilterField,
                    op: FilterOperators.LIKE,
                    values: [value],
                  },
                ],
              },
            },
          });
      }, 500)}
    />
  );
};

export default SearchFilterSelect;
