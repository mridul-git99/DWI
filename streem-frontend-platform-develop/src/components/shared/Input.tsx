import InfoIcon from '#assets/svg/info-icon.svg';
import { useTypedSelector } from '#store';
import { validateNumber } from '#utils';
import { InputTypes } from '#utils/globalTypes';
import { formatDateTime } from '#utils/timeUtils';
import { Error as ErrorIcon, SvgIconComponent } from '@material-ui/icons';
import { endOfDay, getUnixTime, parseISO, set } from 'date-fns';
import { zonedTimeToUtc } from 'date-fns-tz';
import { noop } from 'lodash';
import React, { ComponentPropsWithRef, forwardRef, useMemo } from 'react';
import styled, { css } from 'styled-components';
import Tooltip from './Tooltip';

type OnChangeType = {
  name: string;
  value: string | number;
};

type InputProps = {
  AfterElement?: SvgIconComponent;
  afterElementClass?: string;
  afterElementClick?: () => void;
  afterElementWithoutError?: boolean;
  BeforeElement?: SvgIconComponent;
  beforeElementClass?: string;
  beforeElementClick?: () => void;
  error?: boolean | string | null;
  label?: string;
  optional?: boolean;
  onChange?: ({ name, value }: OnChangeType) => void;
  secondaryAction?: {
    text: string;
    action: () => void;
  };
  disabled?: boolean;
  description?: string;
  type?: InputTypes;
  tooltipLabel?: string;
} & Omit<ComponentPropsWithRef<'input'>, 'onChange' | 'type'>;

type WrapperProps = {
  hasError: boolean;
};

const Wrapper = styled.div<WrapperProps>`
  display: flex;
  flex: 1;
  flex-direction: column;

  .input-label {
    align-items: center;
    color: #525252;
    display: flex;
    font-size: 12px;
    justify-content: flex-start;
    letter-spacing: 0.32px;
    line-height: 1.33;
    margin-bottom: 8px;

    .info-icon-wrapper {
      height: 16px;
      width: 16px;
      margin-left: 6px;
    }

    .optional-badge {
      color: #999999;
      font-size: 12px;
      margin-left: 4px;
    }

    .secondary-action {
      color: #1d84ff;
      cursor: pointer;
      margin-left: auto;
    }
  }

  .input-wrapper {
    align-items: center;
    background-color: #fff;
    border: 1px solid #ccc;
    display: flex;
    flex: 1;
    padding: 8px 16px;

    @media (max-width: 1200px) {
      padding: 8px;
      font-size: 14px;
    }

    :focus-within {
      border-color: #1d84ff;
    }

    > .icon {
      &#before-icon {
        margin-left: 0;
        margin-right: 12px;
      }

      &#after-icon {
        margin-left: 12px;
        margin-right: 0;
      }

      &.error {
        color: #eb5757;
      }
    }

    input {
      background: transparent;
      border: none;
      flex: 1;
      outline: none;
      padding: 0;
      line-height: 1;
      width: 100%;
      min-width: 60px;

      :placeholder-shown {
        text-overflow: ellipsis;
      }

      :disabled {
        color: hsl(0, 0%, 20%);
      }
    }

    ${({ hasError }) =>
      hasError
        ? css`
            border-color: #eb5757;
          `
        : null}
  }

  .field-error {
    font-size: 12px;
    color: #eb5757;
    display: flex;
    justify-content: flex-start;
    margin-top: 8px;
  }

  .description {
    font-size: 12px;
    line-height: 1.33;
    letter-spacing: 0.32px;
    color: #6f6f6f;
    margin-top: 8px;
  }
`;

const TextInput = forwardRef<HTMLInputElement, InputProps>((props, ref) => {
  const {
    AfterElement = ErrorIcon,
    afterElementClass = 'error',
    afterElementClick = noop,
    afterElementWithoutError = false,
    BeforeElement,
    beforeElementClass,
    beforeElementClick = noop,
    error = '',
    label,
    // NATIVE HTML INPUT PROPS
    optional = false,
    defaultValue = '',
    disabled = false,
    placeholder = 'Write here',
    type = InputTypes.SINGLE_LINE,
    name,
    onChange,
    className,
    secondaryAction,
    description = '',
    tooltipLabel,
    ...rest
  } = props;

  const { timeZone = 'UTC' } = useTypedSelector((state) => state.auth.selectedFacility) || {};

  const formatByType =
    type === InputTypes.DATE
      ? 'yyyy-MM-dd'
      : type === InputTypes.TIME
      ? 'HH:mm'
      : 'yyyy-MM-dd HH:mm';

  const onChangeHandler = ({ target }: any) => {
    let { name, value } = target;
    if (
      [InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(
        type as unknown as InputTypes,
      )
    ) {
      if (InputTypes.TIME === type) {
        const [hours, minutes] = value.split(':');
        value =
          hours && minutes
            ? getUnixTime(zonedTimeToUtc(set(new Date(), { hours, minutes }), timeZone)).toString()
            : '';
      } else if (InputTypes.DATE === type) {
        const utcDate = zonedTimeToUtc(endOfDay(parseISO(value)), timeZone);
        value = value ? getUnixTime(utcDate).toString() : '';
      } else {
        const utcDate = zonedTimeToUtc(value, timeZone);
        value = value ? getUnixTime(utcDate).toString() : '';
      }
    }
    if (typeof onChange === 'function') {
      onChange({ name, value });
    }
  };

  const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    const { key, currentTarget } = e;
    const allowedKeys = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab'];
    if (allowedKeys.includes(key)) return; // Allow essential keys
    const value = currentTarget.value;
    if (key === '-' && value === '') return;
    if (key === '.' && !value.includes('.')) return;
    if (!/^\d$/.test(key)) {
      e.preventDefault();
    }
  };

  const propsByType = useMemo(() => {
    const isDateOrTime = [InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(type);
    let valueKey;

    if (type === InputTypes.NUMBER) {
      valueKey = rest.value ? 'value' : validateNumber(defaultValue) ? 'defaultValue' : null;
    } else {
      valueKey = rest.value ? 'value' : defaultValue ? 'defaultValue' : null;
    }

    return {
      type:
        type === InputTypes.SINGLE_LINE
          ? 'text'
          : type === InputTypes.DATE_TIME
          ? 'datetime-local'
          : type.toLowerCase(),
      ...(isDateOrTime && {
        max: type === InputTypes.DATE_TIME ? '2999-12-31T00:00' : '2999-12-31',
      }),
      ...(valueKey && {
        [valueKey]: isDateOrTime
          ? formatDateTime({
              value: (rest.value || defaultValue) as number,
              format: formatByType,
              timezone: timeZone,
            })
          : rest.value || defaultValue,
      }),
      ...(type === InputTypes.NUMBER && {
        step: 'any',
        onKeyDown,
      }),
    };
  }, [type, rest.value]);

  return (
    <Wrapper
      hasError={!!error}
      className={`input ${className ? className : ''}`}
      onWheel={(e) => (e.target as HTMLInputElement).blur()}
    >
      {label ? (
        <label className="input-label">
          {label}
          {tooltipLabel && (
            <span className="info-icon-wrapper">
              <Tooltip title={tooltipLabel} arrow placement="right">
                <img src={InfoIcon}></img>
              </Tooltip>
            </span>
          )}
          {optional ? <span className="optional-badge">Optional</span> : null}
          {secondaryAction && (
            <span className="secondary-action" onClick={secondaryAction.action}>
              {secondaryAction.text}
            </span>
          )}
        </label>
      ) : null}

      <div className="input-wrapper">
        {BeforeElement ? (
          <BeforeElement
            className={`icon ${beforeElementClass ? beforeElementClass : ''}`}
            id="before-icon"
            data-testid="input-element-before-icon"
            onClick={beforeElementClick}
          />
        ) : null}

        <input
          data-testid="input-element"
          {...rest}
          name={name}
          onChange={onChangeHandler}
          placeholder={placeholder}
          ref={ref}
          disabled={disabled}
          {...propsByType}
        />

        {afterElementWithoutError ? (
          <AfterElement
            className={`icon ${afterElementClass ? afterElementClass : ''}`}
            id="after-icon"
            data-testid="input-element-after-icon"
            onClick={afterElementClick}
          />
        ) : null}

        {error ? (
          <AfterElement
            className={`icon ${afterElementClass ? afterElementClass : ''}`}
            id="after-icon"
            data-testid="input-element-error-icon"
            onClick={afterElementClick}
          />
        ) : null}
      </div>
      {description && <span className="description">{description}</span>}
      {typeof error === 'string' && !!error ? <span className="field-error">{error}</span> : null}
    </Wrapper>
  );
});

export { TextInput };
