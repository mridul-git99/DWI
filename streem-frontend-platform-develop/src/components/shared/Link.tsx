import { ArrowBack, ArrowForward, SvgIconComponent } from '@material-ui/icons';
import { navigate } from '@reach/router';
import React from 'react';
import styled, { css } from 'styled-components';

type Props = {
  className?: string;
  addMargin?: boolean;
  iconPosition?: 'before' | 'after';
  label: string;
  labelColor?: string;
  forwardIcon?: SvgIconComponent;
  backIcon?: SvgIconComponent;
  iconColor?: string;
  link?: string;
  withIcon?: boolean;
  onClick?: () => void;
};

type WrapperProps = Pick<
  Props,
  'withIcon' | 'className' | 'labelColor' | 'iconColor' | 'addMargin'
>;

const Wrapper = styled.div.attrs(({ className }) => ({
  className: className ?? 'link',
}))<WrapperProps>`
  display: flex;

  > div {
    align-items: center;
    cursor: pointer;
    display: flex;
    margin: ${({ addMargin }) => (addMargin ? '8px 0 16px' : '0')};

    .icon {
      color: ${({ iconColor }) => iconColor ?? '#1d84ff'};
      font-size: 16px;

      &.before {
        margin-right: 8px;
      }

      &.after {
        margin-left: 8px;
      }

      ${({ withIcon }) =>
        withIcon
          ? css`
              display: block;
            `
          : css`
              display: none;
            `}
    }

    .label {
      color: ${({ labelColor }) => labelColor ?? '#1d84ff'};
      font-size: 14px;
    }
  }
`;

export const Link = ({
  className,
  addMargin = true,
  label,
  link,
  withIcon = true,
  iconPosition = 'before',
  labelColor = '#1d84ff',
  iconColor = '#1d84ff',
  forwardIcon: ForwardIcon = ArrowForward,
  backIcon: BackIcon = ArrowBack,
  onClick,
}: Props) => {
  const handleClick = () => {
    if (onClick) {
      onClick();
    } else {
      navigate((link as string) ?? -1);
    }
  };

  return (
    <Wrapper
      className={className}
      withIcon={withIcon}
      labelColor={labelColor}
      iconColor={iconColor}
      addMargin={addMargin}
    >
      <div onClick={handleClick}>
        {iconPosition === 'before' ? <BackIcon className="icon before" /> : null}
        <span className="label">{label}</span>
        {iconPosition === 'after' ? <ForwardIcon className="icon after" /> : null}
      </div>
    </Wrapper>
  );
};
