import React, { ComponentPropsWithRef, forwardRef } from 'react';
import styled, { css } from 'styled-components';

export type ButtonVariant = 'primary' | 'secondary' | 'textOnly';
export type ButtonColor = 'blue' | 'green' | 'red' | 'dark' | 'gray';

type ButtonProps = {
  color?: ButtonColor;
  variant?: ButtonVariant;
  loading?: boolean;
} & ComponentPropsWithRef<'button'>;

const ColorMap = {
  blue: {
    activeBackgroundColor: '#00387a',
    backgroundColor: '#1d84ff',
    borderColor: '#1d84ff',
    hoverBackgroundColor: '#005dcc',
    textColor: '#1d84ff',
  },
  green: {
    activeBackgroundColor: '#427a00',
    backgroundColor: '#5aa700',
    borderColor: '#5aa700',
    hoverBackgroundColor: '#5aa700',
    textColor: '#5aa700',
  },
  red: {
    activeBackgroundColor: '#cc5656',
    backgroundColor: '#ff6b6b',
    borderColor: '#ff6b6b',
    hoverBackgroundColor: '#ff6b6b',
    textColor: '#ff6b6b',
  },
  dark: {
    activeBackgroundColor: '#999999',
    backgroundColor: '#333333',
    borderColor: '#333333',
    hoverBackgroundColor: '#666666',
    textColor: '#ffffff',
  },
  gray: {
    activeBackgroundColor: '#cccccc',
    backgroundColor: '#f4f4f4',
    borderColor: '#525252',
    hoverBackgroundColor: '#e0e0e0',
    textColor: '#525252',
  },
};

const ButtonWrapper = styled.button.attrs(({ type = 'button', disabled = false }) => ({
  type,
  disabled,
}))<ButtonProps>`
  position: relative;
  align-items: center;
  border: 1px solid transparent;
  cursor: pointer;
  display: flex;
  font-size: 14px;
  justify-content: center;
  margin-right: 12px;
  outline: none;
  padding-block: 12px;
  padding-inline: 24px;

  :last-of-type {
    margin-right: 0;
  }

  :disabled {
    cursor: not-allowed;
  }

  ${({ variant = 'primary', color = variant === 'primary' ? 'blue' : 'gray' }) => {
    const colors = ColorMap[color];

    switch (variant) {
      case 'primary':
        return css`
          background-color: ${colors.backgroundColor};
          color: #ffffff;

          > .icon {
            color: #ffffff;
          }

          :hover {
            background-color: ${colors.hoverBackgroundColor};
          }

          :active {
            background-color: ${colors.activeBackgroundColor};
          }
        `;

      case 'secondary':
        return css`
          background-color: ${color === 'gray' ? `${colors.backgroundColor}` : '#ffffff'};
          border-color: ${colors.borderColor};
          color: ${colors.textColor};

          > .icon {
            color: inherit;
          }

          :hover {
            background-color: ${colors.hoverBackgroundColor};
            color: ${color === 'gray' ? `${colors.textColor}` : '#ffffff'};

            > .icon {
              color: inherit;
            }
          }

          :active {
            background-color: ${colors.activeBackgroundColor};
          }
        `;

      case 'textOnly':
        return css`
          background-color: transparent;
          color: ${colors.textColor};
          padding: 4px 8px;

          :hover {
            background-color: ${color === 'gray' ? `${colors.hoverBackgroundColor}` : '#e0e0e0'};
          }

          :active {
            background-color: ${color === 'gray' ? `${colors.activeBackgroundColor}` : '#fafafa'};
          }
        `;
      default:
        return null;
    }
  }}

  ${({ disabled, variant = 'primary', loading, color = variant === 'primary' ? 'blue' : 'gray' }) =>
    disabled
      ? variant === 'textOnly'
        ? css`
            color: #dadada;
            pointer-events: none;
            background-color: transparent;
          `
        : variant === 'secondary'
        ? css`
            border-color: #bbbbbb;
            color: #bbbbbb;
            background-color: #e8e8e8;

            :hover {
              background-color: #e8e8e8;
              color: #bbbbbb;
            }
          `
        : css`
            background-color: #eeeeee;
            border-color: transparent;
            color: #dadada;
            pointer-events: none;

            ${loading &&
            css`
              color: transparent;
              ::after {
                content: '';
                position: absolute;
                width: 20px;
                height: 20px;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                margin: auto;
                border: 3px solid transparent;
                border-top-color: ${ColorMap[color].textColor};
                border-right-color: ${ColorMap[color].textColor};
                border-radius: 50%;
                animation: button-loading-spinner 1s ease infinite;
              }
            `}
          `
      : null}

  @keyframes button-loading-spinner {
    from {
      transform: rotate(0turn);
    }

    to {
      transform: rotate(1turn);
    }
  }
`;

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(({ children, ...props }, ref) => (
  <ButtonWrapper ref={ref} {...props}>
    {children}
  </ButtonWrapper>
));

Button.displayName = 'Button';
