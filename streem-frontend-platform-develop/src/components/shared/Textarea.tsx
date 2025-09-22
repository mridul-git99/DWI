import { Error as ErrorIcon } from '@material-ui/icons';
import React, {
  ChangeEvent,
  ComponentPropsWithRef,
  forwardRef,
  useLayoutEffect,
  useRef,
  useState,
} from 'react';
import styled, { css } from 'styled-components';

type OnChangeType = {
  name: string;
  value: string | number;
};

type TextareaProps = {
  autoResize?: boolean;
  error?: boolean | string;
  label?: string;
  onChange?: ({ name, value }: OnChangeType) => void;
  optional?: boolean;
  onBlur?: ({ name, value }: OnChangeType) => void;
  maxRows?: number;
} & ComponentPropsWithRef<'textarea'>;

type WrapperProps = {
  hasError: boolean;
  autoResize?: boolean;
};

const Wrapper = styled.div.attrs(({ className }) => ({
  className: `textarea ${className ? className : ''}`,
}))<WrapperProps>`
  display: flex;
  flex: 1;
  flex-direction: column;

  .input-label {
    align-items: center;
    color: #525252;
    display: flex;
    font-size: 12px;
    justify-content: flex-start;
    letter-spacing: 0.16px;
    line-height: 1.29;
    margin-bottom: 8px;

    .optional-badge {
      color: #999999;
      font-size: 12px;
      margin-left: 4px;
    }
  }

  .textarea-wrapper {
    height: auto;
    textarea {
      height: auto;
      background-color: #ffffff;
      border: 1px solid #e0e0e0;
      color: #000000;
      outline: none;
      overflow-x: hidden;
      padding: 16px;
      width: 100%;

      @media (max-width: 1200px) {
        padding: 8px;
        font-size: 14px;
      }

      :disabled {
        background-color: #fafafa;
        color: hsl(0, 0%, 20%);
        resize: none;
      }

      :active,
      :focus {
        border-color: #1d84ff;
      }

      :-webkit-input-placeholder {
        text-align: center;
        line-height: 74px;
        color: #a8a8a8;
      }

      :-moz-placeholder {
        text-align: center;
        line-height: 74px;
        color: #a8a8a8;
      }

      :-moz-placeholder {
        text-align: center;
        line-height: 74px;
        color: #a8a8a8;
      }

      :-ms-input-placeholder {
        text-align: center;
        line-height: 74px;
        color: #a8a8a8;
      }

      ${({ hasError }) =>
        hasError
          ? css`
              border-color: #eb5757;
            `
          : null}

      ${({ autoResize }) =>
        autoResize
          ? css`
              resize: none;
            `
          : null}
    }
  }

  .field-error {
    align-items: center;
    color: #eb5757;
    display: flex;
    font-size: 12px;
    justify-content: flex-start;
    margin-top: 8px;

    .icon {
      font-size: 16px;
      color: #eb5757;
      margin-right: 5px;
    }
  }
`;

const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>((props, ref) => {
  const {
    autoResize = true,
    defaultValue,
    disabled = false,
    error,
    label,
    name,
    onChange,
    optional = false,
    placeholder = 'Write here',
    rows = 1,
    onBlur,
    value,
    maxRows,
  } = props;

  const internalRef: any = useRef<HTMLTextAreaElement>();
  const [text, setText] = useState(defaultValue || value);

  useLayoutEffect(() => {
    const animationId = requestAnimationFrame(() => {
      if (internalRef.current && autoResize) {
        if (text && window) {
          const style = window.getComputedStyle(internalRef.current);
          internalRef.current.style.height = '0px';
          const paddingTop = parseFloat(style.paddingTop) || 16;
          const paddingBottom = parseFloat(style.paddingBottom) || 16;
          const lineHeight = parseFloat(style.lineHeight);
          const borderTop = parseFloat(style.borderTopWidth);
          const borderBottom = parseFloat(style.borderBottomWidth);
          const scrollHeight = internalRef?.current?.scrollHeight || 0;

          const currentHeight = scrollHeight + borderTop + borderBottom;

          if (maxRows) {
            const heightBasedOnMaxRows = lineHeight * maxRows + paddingTop + paddingBottom;
            if (currentHeight > heightBasedOnMaxRows) {
              internalRef.current.style.height = heightBasedOnMaxRows + 'px';
              return;
            }
          }

          const heightBasedOnRows = lineHeight * rows + paddingTop + paddingBottom;
          let differenceBwRowsAndCurrentHeight = 0;
          if (currentHeight < heightBasedOnRows) {
            differenceBwRowsAndCurrentHeight = heightBasedOnRows - currentHeight;
          }

          internalRef.current.style.height =
            currentHeight + differenceBwRowsAndCurrentHeight + 'px';
        } else {
          internalRef.current.style.height = 'auto';
        }
      }
    });
    return () => cancelAnimationFrame(animationId);
  }, [text, autoResize]);

  const onChangeHandler = ({ target: { name, value } }: ChangeEvent<HTMLTextAreaElement>) => {
    setText(value);

    if (typeof onChange === 'function') {
      onChange({ name, value });
    }
  };

  const onBlurHandler = (e: React.FocusEvent<HTMLTextAreaElement>) => {
    if (typeof onBlur === 'function') {
      onBlur(e);
    }
  };

  return (
    <Wrapper autoResize={autoResize} hasError={!!error}>
      {label ? (
        <label className="input-label">
          {label}
          {optional ? <span className="optional-badge">Optional</span> : null}
        </label>
      ) : null}

      <div className="textarea-wrapper">
        <textarea
          defaultValue={defaultValue}
          disabled={disabled}
          name={name}
          onBlur={onBlurHandler}
          onChange={onChangeHandler}
          placeholder={placeholder}
          ref={(e) => {
            internalRef.current = e;
            if (typeof ref === 'function') {
              ref(e);
            } else if (ref) {
              ref.current = e;
            }
          }}
          rows={rows}
          value={value}
        />
      </div>

      {typeof error === 'string' && !!error ? (
        <span className="field-error">
          <ErrorIcon className="icon" />
          {error}
        </span>
      ) : null}
    </Wrapper>
  );
});

Textarea.displayName = 'textarea';

export default Textarea;
