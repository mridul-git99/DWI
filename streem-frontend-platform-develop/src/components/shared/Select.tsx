import { selectStyles } from '#utils/selectStyleUtils';
import { CheckBox, CropSquare, ExpandMore } from '@material-ui/icons';
import React, { FC, useState } from 'react';
import ReactSelect, {
  MultiValueGenericProps,
  Props,
  components as selectComponents,
} from 'react-select';
import styled from 'styled-components';
import { Button } from './Button';
import Tooltip from './Tooltip';

export const formatOptionLabel: Props<{
  option: string;
  label: string;
  externalId: string;
}>['formatOptionLabel'] = ({ externalId, label }) => (
  <div
    style={{
      display: 'flex',
      justifyContent: 'space-between',
      gap: '16px',
      width: '100%',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
    }}
  >
    <div
      style={{
        overflow: 'hidden',
        flex: 1,
        textOverflow: 'ellipsis',
      }}
      title={label}
    >
      {label}
    </div>
    {externalId && (
      <div
        title={externalId}
        style={{
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          flex: 1,
          textAlign: 'right',
        }}
      >
        {externalId}
      </div>
    )}
  </div>
);

type SelectProps = Props<any> & {
  error?: string;
  label?: string | React.ReactNode;
  optional?: boolean;
  style?: React.CSSProperties;
  formatOptionLabel?: Props<any>['formatOptionLabel'];
  filterProp?: string[];
  countAsValues?: boolean;
  floatingLabel?: boolean;
  onRemove?: (index: number) => void;
  showTooltip?: boolean;
  extraCount?: number;
};

const Wrapper = styled.div.attrs({
  className: 'react-custom-select',
})`
  .label {
    align-items: center;
    color: #525252;
    display: flex;
    font-size: 12px;
    justify-content: flex-start;
    letter-spacing: 0.16px;
    line-height: 1.29;
    margin-bottom: 8px;
  }

  .floating-label {
    position: absolute;
    transform: translate(10px, -7px);
    color: #1d84ff;
    font-size: 12px;
    background: white;
    z-index: 10;
    padding: 0 2px;
  }

  .optional-badge {
    color: #999999;
    font-size: 12px;
    margin-left: 4px;
  }

  .MuiSvgIcon-root {
    color: hsl(0, 0%, 50%);
    height: 24px;
    width: 24px;
    margin: 6px 0;

    &:hover {
      color: #101010;
    }
  }

  .field-error {
    color: #eb5757;
    display: flex;
    justify-content: flex-start;
    margin-top: 8px;
  }
`;

export type Option = { label: string; value: string | number };

export const countAsValuesSelectStyles: Props['styles'] = {
  ...selectStyles,
  control: (styles, { isDisabled }) => ({
    ...selectStyles.control(styles, { isDisabled }),
    backgroundColor: '#fff',
    padding: '2.7px 8px',
  }),

  multiValue: (styles) => ({
    ...styles,
    display: 'none',
  }),

  option: (styles, { isFocused, isSelected }) => ({
    ...selectStyles.option(styles, { isFocused, isSelected }),
    backgroundColor: isSelected || isFocused ? '#dadada' : '#f4f4f4',
  }),

  menu: (provided, { isMulti }) => ({
    ...provided,
    zIndex: 2,
    border: isMulti && '1px solid transparent',

    '&:hover': {
      border: isMulti && '1px solid #1d84ff', // Change border color on hover
    },
  }),
};

const DropdownIndicator = () => <ExpandMore />;

const ParameterItemWrapper = styled.div`
  .item-wrapper {
    position: relative;
    background: white;
    bottom: 32px;
    left: 12px;
    width: fit-content;
  }
  .item-box {
    display: flex;
    flex-direction: row;
    background: #dde1e6;
    color: #121619;
    padding: 4px 8px;
    margin: 4px;
    width: fit-content;
    gap: 10px;
  }
`;

const ParameterOptionWrapper = styled.div`
  margin: 8px;
  cursor: pointer;
  .select-all,
  .deselect-all {
    color: #1d84ff;
  }
`;

const ParameterOption = styled.div`
  padding: 4px;
  height: 40px;
  border-bottom: 1px solid #ccc;
  display: flex;
  align-items: center;
  .icon {
    margin: 0px 4px;
  }
  .label {
    margin: 4px 0;
    font-size: 14px;
    color: #161616;
    width: 100%;
  }
`;

export const Select: FC<SelectProps> = ({
  styles = selectStyles,
  optional = false,
  label = '',
  error = '',
  style = {},
  filterProp = [],
  components,
  countAsValues = false,
  floatingLabel = false,
  showTooltip = false,
  extraCount = 0,
  ...rest
}) => {
  const _filterProp = [
    'label',
    ...filterProp,
    ...(rest?.options?.[0]?.externalId ? ['externalId'] : []),
  ];
  const [isOpen, setIsOpen] = useState(false);
  const [showFloatingLabel, setShowFloatingLabel] = useState(false);

  const Option = (props: MultiValueGenericProps) => {
    return (
      <ParameterOptionWrapper>
        <selectComponents.MultiValueLabel {...props}>
          <ParameterOption>
            {!(props.data?.isDeselectAll || props.data?.isSelectAll) && (
              <>
                {!props.isSelected ? (
                  <CropSquare className="icon" />
                ) : (
                  <CheckBox className="icon" style={{ color: '#1d84ff' }} />
                )}
              </>
            )}
            <div className="label">{props.children}</div>
          </ParameterOption>
        </selectComponents.MultiValueLabel>
      </ParameterOptionWrapper>
    );
  };

  const handleFilterOption = (option, inputValue) => {
    if (!inputValue) {
      return true;
    }
    let valid = false;
    _filterProp.every((key) => {
      if (
        typeof option?.data?.[key] === 'string' &&
        option?.data?.[key]?.toLowerCase()?.indexOf?.(inputValue.toLowerCase()) >= 0
      ) {
        valid = true;
        return false;
      }
      return true;
    });
    return valid;
  };

  const handleMenuOpen = () => {
    if (countAsValues) {
      setIsOpen(true);
    }
    rest.onMenuOpen && rest.onMenuOpen();
  };

  const handleMenuClose = () => {
    if (countAsValues) {
      setIsOpen(false);
    }
    rest.onMenuClose && rest.onMenuClose();
  };

  const handleOnChange = (value, selectedOption) => {
    if (floatingLabel) {
      if (Array.isArray(value)) {
        const length = value.length;

        if (length > 0) {
          setShowFloatingLabel(true);
        } else {
          setShowFloatingLabel(false);
        }
      } else if (value) {
        setShowFloatingLabel(true);
      } else {
        setShowFloatingLabel(false);
      }
    }

    rest.onChange && rest.onChange(value, selectedOption);
  };

  const getTooltipContent = (value: any[] | Record<string, any>) => {
    if (!value) {
      return '';
    } else if (Array.isArray(value)) {
      return value.length > 0 ? value.map((val) => val.label).join(', ') : '';
    } else if (value?.label) {
      return value.label || '';
    }
  };

  return (
    <Wrapper style={style}>
      {label && (
        <label className="label">
          {label} {optional && <span className="optional-badge">Optional</span>}
        </label>
      )}
      {(floatingLabel || showFloatingLabel) && !label && rest?.value && (
        <div className="floating-label">{rest.placeholder}</div>
      )}
      <Tooltip
        title={
          showTooltip
            ? rest?.value
              ? getTooltipContent(rest.value)
              : rest?.defaultValue
              ? getTooltipContent(rest.defaultValue)
              : ''
            : ''
        }
        arrow
      >
        <div>
          <ReactSelect
            classNamePrefix="custom-select"
            menuPlacement="auto"
            styles={countAsValues ? countAsValuesSelectStyles : styles}
            isClearable={optional}
            captureMenuScroll={true}
            formatOptionLabel={formatOptionLabel as any}
            components={
              countAsValues
                ? { ...components, Option, DropdownIndicator }
                : { ...components, DropdownIndicator }
            }
            filterOption={handleFilterOption}
            blurInputOnSelect={rest.isMulti ? false : undefined}
            closeMenuOnSelect={rest.isMulti ? false : undefined}
            {...rest}
            onChange={handleOnChange}
            onMenuOpen={handleMenuOpen}
            onMenuClose={handleMenuClose}
            menuIsOpen={countAsValues ? isOpen : rest.menuIsOpen}
            hideSelectedOptions={countAsValues ? false : rest.hideSelectedOptions}
          />
        </div>
      </Tooltip>
      {error && <span className="field-error">{error}</span>}
      {countAsValues && (
        <ParameterItemWrapper>
          <div>
            <span className="item-wrapper">
              {((rest?.value || []).length || extraCount > 0) && !isOpen
                ? `${rest.value.length + extraCount} Selected`
                : ''}
            </span>
            <div
              style={{
                display: 'flex',
                flexWrap: 'wrap',
                position: 'relative',
                bottom: '4px',
                paddingTop: '4px',
              }}
            >
              {(rest?.value || []).map((el, index) => {
                return (
                  <div className="item-box">
                    <div>
                      {el.label} {el?.option?.externalId && `(ID: ${el?.option?.externalId})`}{' '}
                    </div>
                    {!rest.isDisabled && (
                      <Button
                        variant="textOnly"
                        style={{ padding: 0 }}
                        onClick={() => rest.onRemove?.(index)}
                      >
                        X
                      </Button>
                    )}
                  </div>
                );
              })}
              {!!extraCount && <div className="item-box"> + {extraCount}</div>}
            </div>
          </div>
        </ParameterItemWrapper>
      )}
    </Wrapper>
  );
};
