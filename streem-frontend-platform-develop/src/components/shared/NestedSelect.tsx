import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, DEFAULT_PAGINATION } from '#utils/constants';
import { Pageable, fetchDataParams } from '#utils/globalTypes';
import MenuItem from '@material-ui/core/MenuItem';
import Select, { SelectProps } from '@material-ui/core/Select';
import { makeStyles } from '@material-ui/core/styles';
import { NavigateBefore, NavigateNext, Search } from '@material-ui/icons';
import { debounce, get } from 'lodash';
import React, { FC, useEffect, useRef, useState } from 'react';
import { Props, StylesConfig } from 'react-select';
import styled, { css } from 'styled-components';
import { TextInput } from './Input';
import { Select as CustomSelect } from './Select';

const PopOutWrapper = styled.div.attrs({
  className: 'popout-wrapper',
})`
  padding-inline: 4px;
  border-top: 1px solid #f4f4f4;
`;

const NestedOption = styled.div<{ isBack?: boolean; isForward?: boolean }>`
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 12px;
  justify-content: ${(p) => (p.isForward ? 'space-between' : 'unset')};
  font-weight: ${(p) => (p.isBack ? 'bold' : '400')};
  border-bottom: 1px solid #e0e0e0;
  label {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  svg {
    font-size: ${(p) => (p.isBack ? '20px !important' : '16px')};
    margin-inline: ${(p) => (p.isBack ? '0 16px' : '16px 0')};
    color: #161616;
  }
`;

const StyledSelect = styled(Select)<{ width: string }>`
  width: ${(p) => p.width};
  .MuiMenuItem-root {
    padding: unset !important;
  }
  .MuiInputBase-input {
    padding: 0px;
  }
  .MuiList-padding {
    padding-top: 0px;
    padding-bottom: 0px;
  }

  .nested-select-search {
    padding: 8px 0px 8px 12px;
  }
  .nested-select-search > div {
    background-color: #f4f4f4;
  }

  .search-container {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  ${(p) =>
    p.disabled &&
    css`
      .MuiSelect-root {
        > div {
          pointer-events: none;
          background-color: hsl(0, 0%, 95%);
        }
      }
    `}
`;

const nestedSelectStyles: StylesConfig<any> = {
  option: (styles) => ({
    ...styles,
    overflowWrap: 'break-word',
  }),
  control: (provided) => ({
    ...provided,
    margin: 8,
    borderRadius: 0,
    backgroundColor: '#f4f4f4',
  }),
  menu: () => ({
    boxShadow: 'inset 0 1px 0 rgba(0, 0, 0, 0.1)',
    width: 'max-content',
    minWidth: '100%',
    maxWidth: 'calc(100% + calc(250px - 100%))',
  }),
};

const useSelectStyles = makeStyles({
  icon: {
    display: 'none',
  },
  styles: {
    '&:before': {
      display: 'none',
    },
    '&:after': {
      display: 'none',
    },
  },

  select: {
    paddingRight: 'unset !important',
    '&:focus': {
      backgroundColor: 'unset',
    },
  },
});

const useMenuStyles = makeStyles({
  paper: {
    borderRadius: 0,
    maxWidth: 'calc(100% + calc(250px - 100%))',
  },
  styles: {
    '.MuiMenuItem-root': {
      padding: '0 !important',
    },
  },
});

const useMenuItemStyles = makeStyles({
  root: {
    fontSize: '14px !important',
    justifyContent: 'space-between !important',
    fontWeight: 'normal !important',
    lineHeight: 'normal !important',
    letterSpacing: 'normal !important',
    color: '#666666 !important',
  },
});

const DropdownIndicator = () => <Search />;

type PopOutProps = {
  handleMenuScrollToBottom: () => void;
  selectOptions: ItemType[];
  onInputChange: (newValue: string) => void;
  popOutProps?: Props;
} & Pick<NestedSelectProps, 'onChildChange'>;

export type NestedSelectProps = {
  items: ItemsType;
  label: (value: unknown) => React.ReactNode;
  onChildChange: (option: any) => void;
  width?: string;
  id: string;
  pagination?: Pageable;
  fetchData?: ({ page, size }: fetchDataParams) => void;
  maxHeight?: string | number;
  popOutProps?: Props;
  afterElement?: JSX.Element;
} & SelectProps;

type State = {
  parentPath: string[];
  options: ItemType[];
  selectOptions: ItemType[];
  isLoading: boolean;
  openPopOut: boolean;
  openSelect: boolean;
};

type ItemType = {
  label: string | React.ReactNode;
  items?: ItemsType;
  fetchItems?: (
    pageNumber?: number,
    query?: string,
  ) => Promise<{ options: ItemType[]; pageable?: Pageable }>;
  value?: string;
  disabled?: boolean;
};

type ItemsType = Record<string, ItemType>;

type MenuTreeProps = {
  items: ItemsType;
  onChildChange: NestedSelectProps['onChildChange'];
  state: State;
  parent: React.MutableRefObject<ItemType | null>;
  pagination: React.MutableRefObject<Pageable>;
  setState: React.Dispatch<React.SetStateAction<State>>;
  popOutProps?: Props;
};

const initialState: State = {
  parentPath: [],
  options: [],
  selectOptions: [],
  isLoading: false,
  openPopOut: false,
  openSelect: false,
};

const initialPagination = DEFAULT_PAGINATION;

const PopOut: FC<PopOutProps> = ({
  handleMenuScrollToBottom,
  selectOptions,
  onChildChange,
  onInputChange,
  popOutProps,
}) => {
  const { components, ...rest } = popOutProps || {};
  const [inputValue, setInputValue] = useState<string | undefined>();
  const debounceInputRef = useRef(debounce((event, functor) => functor(event), 500));

  useEffect(() => {
    if (typeof inputValue === 'string') {
      debounceInputRef.current(inputValue, (value: string) => {
        onInputChange(value);
      });
    }
  }, [inputValue]);

  return (
    <PopOutWrapper onKeyDown={(e) => e.stopPropagation()}>
      <CustomSelect
        autoFocus
        backspaceRemovesValue={false}
        components={{ DropdownIndicator, IndicatorSeparator: null, ...components }}
        controlShouldRenderValue={false}
        hideSelectedOptions={false}
        menuIsOpen
        onChange={(newValue) => {
          onChildChange(newValue);
        }}
        options={selectOptions}
        placeholder="Search..."
        inputValue={inputValue}
        styles={nestedSelectStyles}
        tabSelectsValue={false}
        onMenuScrollToBottom={handleMenuScrollToBottom}
        onInputChange={(value, actionMeta) => {
          if (actionMeta.prevInputValue !== value) setInputValue(value);
        }}
        {...rest}
      />
    </PopOutWrapper>
  );
};

const MenuTree: FC<MenuTreeProps> = ({
  items,
  onChildChange,
  state,
  parent,
  pagination,
  setState,
  popOutProps,
}) => {
  const { parentPath, options, isLoading, selectOptions, openPopOut } = state;
  const menuItemClasses = useMenuItemStyles();

  const getItems = async (fn: ItemType['fetchItems'], pageNumber?: number) => {
    if (fn) {
      setState((prev) => ({ ...prev, isLoading: true }));
      const { options: awaitedOptions, pageable } = await fn(pageNumber);
      if (pageable) {
        pagination.current = pageable;
      }
      setState((prev) => ({
        ...prev,
        isLoading: false,
        selectOptions:
          pageable?.page === 0 ? awaitedOptions : [...prev.selectOptions, ...awaitedOptions],
      }));
    }
  };

  const onInputChange = async (value: string) => {
    if (parent.current?.fetchItems) {
      setState((prev) => ({ ...prev, isLoading: true }));
      const { options: awaitedOptions, pageable } = await parent.current.fetchItems(
        initialPagination.page,
        value,
      );
      if (pageable) {
        pagination.current = pageable;
      }
      setState((prev) => ({
        ...prev,
        isLoading: false,
        selectOptions: awaitedOptions,
      }));
    }
  };

  useEffect(() => {
    let nodes = items;
    let updatedParent: any = undefined;
    let updatedOpenPopOut = false;
    if (parentPath.length) {
      updatedParent = { ...get(items, parentPath, {}) };
      if (updatedParent.fetchItems) {
        getItems(updatedParent.fetchItems, pagination.current.page);
        updatedOpenPopOut = true;
      }
      nodes = {
        back: { label: updatedParent.label },
        ...(updatedParent?.items || {}),
      };
    }

    parent.current = updatedParent;

    setState((prev) => {
      return {
        ...prev,
        options: Object.entries(nodes).map(([key, value]: [string, any]) => ({
          ...value,
          label: value.label,
          value: key,
        })),
        openPopOut: updatedOpenPopOut,
      };
    });
    return () => {
      setState((prev) => ({ ...prev, selectOptions: [] }));
    };
  }, [parentPath, items]);

  const handleMenuScrollToBottom = () => {
    if (!isLoading && !pagination.current.last && parent.current?.fetchItems) {
      getItems(parent.current.fetchItems, pagination.current.page + 1);
    }
  };

  const displayLabel = (currOption: ItemType) => {
    if (currOption.value === 'back') {
      return (
        <NestedOption isBack>
          <NavigateBefore />
          <label>{currOption.label}</label>
        </NestedOption>
      );
    }

    if (currOption.items || currOption.fetchItems) {
      return (
        <NestedOption isForward>
          <label>{currOption.label}</label>
          <NavigateNext />
        </NestedOption>
      );
    }
    return <NestedOption>{currOption.label}</NestedOption>;
  };

  return (
    <>
      {options?.map((currOption, index) => (
        <>
          <MenuItem
            key={index}
            classes={{ root: menuItemClasses.root }}
            disabled={currOption.disabled}
            onClick={() => {
              if (currOption.value === 'back') {
                pagination.current = initialPagination;
                setState((prev) => ({
                  ...prev,
                  parentPath: prev.parentPath.filter((_, i) => i < prev.parentPath.length - 2),
                  selectOptions: [],
                }));
              } else if (currOption?.items || currOption?.fetchItems) {
                setState((prev) => ({
                  ...prev,
                  parentPath: [
                    ...prev.parentPath,
                    ...(prev.parentPath.length ? ['items'] : []),
                    ...(currOption?.value ? [currOption.value] : []),
                  ],
                }));
              } else if (currOption?.value === 'media') {
                return;
              } else {
                onChildChange(currOption);
              }
            }}
          >
            {displayLabel(currOption)}
          </MenuItem>
        </>
      ))}
      {openPopOut && (
        <PopOut
          handleMenuScrollToBottom={handleMenuScrollToBottom}
          selectOptions={selectOptions}
          onChildChange={onChildChange}
          onInputChange={onInputChange}
          popOutProps={popOutProps}
        />
      )}
    </>
  );
};

export const NestedSelect: FC<NestedSelectProps> = ({
  items,
  onChildChange,
  label,
  width = 'auto',
  maxHeight = 'auto',
  id,
  popOutProps,
  fetchData,
  pagination,
  ...rest
}) => {
  const selectClasses = useSelectStyles();
  const menuClasses = useMenuStyles();
  const [state, setState] = useState<State>(initialState);
  const parent = useRef<ItemType | null>(null);
  const _pagination = useRef<Pageable>(initialPagination);
  const { openSelect, openPopOut } = state;

  const handleOnChildChange = (option: any) => {
    onChildChange(option);
    onClose();
  };

  const onClose = (e?: React.ChangeEvent<{}>) => {
    e && e.stopPropagation();
    _pagination.current = initialPagination;
    setState((prev) => ({
      ...prev,
      openSelect: false,
    }));
    setTimeout(() => {
      setState(initialState);
    }, 200);
  };

  const handleOnScroll = (e: React.UIEvent<HTMLElement>) => {
    const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
    if (
      fetchData &&
      pagination &&
      !pagination.last &&
      scrollHeight - Math.ceil(scrollTop) <= clientHeight
    ) {
      fetchData({
        page: pagination?.page + 1,
        size: pagination?.pageSize || DEFAULT_PAGE_SIZE,
      });
    }
  };

  const { MenuProps, ...restStyledSelectProps } = rest;

  return (
    <StyledSelect
      id={id}
      width={width}
      MenuProps={{
        disableEnforceFocus: true,
        classes: { paper: menuClasses.paper },
        className: menuClasses.styles,
        anchorOrigin: {
          vertical: 'bottom',
          horizontal: 'left',
        },
        style: { maxHeight: openPopOut ? 'unset' : maxHeight },
        PaperProps: {
          onScroll: handleOnScroll,
        },
        container: document.getElementById(id),
        ...MenuProps,
      }}
      displayEmpty
      classes={{
        icon: selectClasses.icon,
        select: selectClasses.select,
      }}
      className={selectClasses.styles}
      renderValue={label}
      open={openSelect}
      onOpen={(e) => {
        e.stopPropagation();
        fetchData?.({ page: DEFAULT_PAGE_NUMBER });
        setState((prev) => ({ ...prev, openSelect: true }));
      }}
      onClose={onClose}
      {...restStyledSelectProps}
    >
      {fetchData && !openPopOut && (
        <div className="search-container">
          <TextInput
            className="nested-select-search"
            autoFocus
            placeholder="Search..."
            onKeyDown={(e) => {
              e.stopPropagation();
            }}
            afterElementClass="search-bar"
            afterElementWithoutError={true}
            AfterElement={Search}
            onChange={debounce(({ value }) => {
              fetchData({ query: value, page: DEFAULT_PAGE_NUMBER });
            }, 500)}
          />
          {rest.afterElement && rest.afterElement}
        </div>
      )}
      <MenuTree
        items={items}
        onChildChange={handleOnChildChange}
        state={state}
        pagination={_pagination}
        parent={parent}
        setState={setState}
        popOutProps={popOutProps}
      />
    </StyledSelect>
  );
};
