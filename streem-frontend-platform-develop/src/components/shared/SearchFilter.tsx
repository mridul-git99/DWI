import { FilterField, FilterOperators } from '#utils/globalTypes';
import { MenuItem } from '@material-ui/core';
import { ArrowDropDown } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, MouseEvent, useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { Button } from './Button';
import { TextInput } from './Input';
import SearchFilterSelect from './SearchFilterSelect';
import { StyledMenu } from './StyledMenu';

const Wrapper = styled.div`
  display: flex;
  position: relative;

  .dropdown-button {
    padding: 2px 2px 2px 8px;
    background-color: #fff;
    color: #1d84ff;
    border: 1px solid #ccc;
    font-size: 12px;
  }

  .input-wrapper {
    padding: 0px 0px 0px 16px;
    font-size: 14px;
    max-width: 200px;

    @media (max-width: 900px) {
      max-width: 150px;
    }
  }
`;

type DropdownOption = {
  label: string;
  value: string;
  field: string;
  operator: FilterOperators;
  url?: string;
  urlFilterField?: string;
  urlParams?: any;
  labelKey?: string[];
  valueKey?: string[];
};

type SearchFilterProps = {
  showDropdown?: boolean;
  dropdownOptions?: DropdownOption[];
  updateFilterFields: (fields: FilterField[], option?: DropdownOption) => void;
  placeholderText?: string;
  showSelectDropdown?: boolean;
  defaultValue?: any;
  prefilledSearch?: any;
} & React.HTMLAttributes<HTMLDivElement>;

const SearchFilter: FC<SearchFilterProps> = ({
  dropdownOptions,
  showDropdown = false,
  updateFilterFields,
  placeholderText = 'Search...',
  showSelectDropdown = false,
  defaultValue,
  prefilledSearch = {},
  ...rest
}) => {
  const [selectedOption, setSelectedOption] = useState<DropdownOption>(dropdownOptions!?.[0]);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const handleClick = (event: MouseEvent<HTMLButtonElement>) => setAnchorEl(event.currentTarget);
  const handleClose = () => setAnchorEl(null);

  useEffect(() => {
    if (
      prefilledSearch &&
      (prefilledSearch?.value || prefilledSearch?.value === '') &&
      (prefilledSearch?.field || prefilledSearch?.field === '')
    ) {
      if (inputRef.current) inputRef.current.value = prefilledSearch.value;
      var optionPrefilled = dropdownOptions?.filter((el) => el.field === prefilledSearch.field);
      setSelectedOption(optionPrefilled?.[0] || dropdownOptions?.[0]);
    }
  }, [prefilledSearch]);

  const onUpdate = ({ updatedOption = selectedOption }: { updatedOption?: DropdownOption }) => {
    const searchFilterFields = inputRef?.current?.value
      ? [
          {
            field: updatedOption?.field || '',
            op: FilterOperators.LIKE,
            value: inputRef.current.value,
            values: [inputRef.current.value],
          },
        ]
      : [];
    updateFilterFields(searchFilterFields, updatedOption);
  };

  return (
    <Wrapper {...rest}>
      {showDropdown ? (
        <>
          <Button onClick={handleClick} className="dropdown-button">
            {selectedOption?.label} <ArrowDropDown />
          </Button>

          <StyledMenu
            keepMounted
            disableEnforceFocus
            anchorEl={anchorEl}
            onClose={handleClose}
            open={Boolean(anchorEl)}
            style={{ marginTop: 40 }}
          >
            {dropdownOptions?.map((option, index) => (
              <MenuItem
                key={index}
                onClick={() => {
                  setSelectedOption(option);
                  rest.selectedOption && rest.selectedOption(option);
                  onUpdate({
                    updatedOption: option,
                  });
                  handleClose();
                }}
              >
                {option.label}
              </MenuItem>
            ))}
          </StyledMenu>
        </>
      ) : null}

      {showSelectDropdown ? (
        <SearchFilterSelect
          selectedDropDownOption={selectedOption}
          updateFilterFields={updateFilterFields}
          defaultValue={defaultValue}
        />
      ) : (
        <TextInput
          placeholder={placeholderText}
          onChange={debounce(() => {
            onUpdate({});
          }, 500)}
          ref={inputRef}
        />
      )}
    </Wrapper>
  );
};

export default SearchFilter;
