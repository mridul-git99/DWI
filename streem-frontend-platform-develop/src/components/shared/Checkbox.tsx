import React, { FC } from 'react';
import styled, { css } from 'styled-components';

interface CheckboxProps {
  label?: string | JSX.Element;
  value?: string | number;
  name?: string;
  checked?: boolean;
  disabled?: boolean;
  onClick?: (e: boolean) => void;
  refFun?: (el: HTMLInputElement) => void;
  partial?: boolean;
  preventDefault?: boolean;
}

const Wrapper = styled.div.attrs({
  className: 'checkbox-input',
})<Pick<CheckboxProps, 'partial' | 'disabled'>>`
  .container {
    display: block;
    position: relative;
    padding-left: 35px;
    cursor: pointer;
    font-size: 16px;
    color: '#525252';
    user-select: none;

    ${({ disabled }) =>
      disabled
        ? css`
            opacity: 0.5;
            pointer-events: none;
          `
        : css``}
  }

  /* Hide the browser's default checkbox */
  .container input {
    position: absolute;
    opacity: 0;
    cursor: pointer;
    height: 0;
    width: 0;
  }

  /* Create a custom checkbox */
  .checkmark {
    position: absolute;
    top: 0;
    left: 0;
    height: 18px;
    width: 18px;
    background-color: #fff;
    border: 2px solid #333;
  }

  /* On mouse-over, add a grey background color */
  .container:hover input ~ .checkmark {
    background-color: #ccc;
  }

  /* When the checkbox is checked, add a blue background */
  .container input:checked ~ .checkmark {
    background-color: #1d84ff;
    border: 2px solid #1d84ff;
  }

  /* When the checkbox is disabled, add a grey background */
  .container input:disabled ~ .checkmark {
    background-color: #eeeeee;
    border: none;
  }

  /* Create the checkmark/indicator (hidden when not checked) */
  .checkmark:after {
    content: '';
    position: absolute;
    display: block;
  }

  /* Style the checkmark/indicator */
  ${({ partial }) =>
    partial
      ? css`
          .container {
            input ~ .checkmark {
              background-color: #1d84ff !important;
              border: 2px solid #1d84ff;
            }

            .checkmark:after {
              left: 2.5px;
              top: 7px;
              width: 10px;
              height: 0px;
              border: solid white;
              border-width: 2px 0px 0px 0;
            }
          }
        `
      : css`
          .container .checkmark:after {
            left: 5px;
            top: 2px;
            width: 3px;
            height: 7px;
            border: solid white;
            border-width: 0 2px 2px 0;
            -webkit-transform: rotate(45deg);
            -ms-transform: rotate(45deg);
            transform: rotate(45deg);
          }
        `}
`;

Wrapper.defaultProps = {
  partial: false,
};

export const Checkbox: FC<CheckboxProps> = ({
  label = '',
  checked,
  onClick,
  value = '',
  name = '',
  disabled = false,
  refFun,
  partial = false,
  preventDefault = false,
}) => (
  <Wrapper partial={partial} disabled={disabled}>
    <label
      className="container"
      onClick={(e) => {
        if (preventDefault) {
          e.preventDefault();
          onClick?.(!checked);
        }
      }}
    >
      {label}
      <input
        type="checkbox"
        value={value}
        name={name}
        onChange={onClick ? (e) => onClick(e.target.checked) : undefined}
        checked={checked}
        ref={refFun}
      />
      <span className="checkmark"></span>
    </label>
  </Wrapper>
);
