import React, { FC, ReactElement } from 'react';
import { withStyles } from '@material-ui/core/styles';
import { Tooltip as muiTooltip, TooltipProps as muiTooltipProps } from '@material-ui/core';

interface TooltipProps extends muiTooltipProps {
  title: string | ReactElement;
  children: ReactElement;
  textAlignment?: 'left' | 'center' | 'right';
}

const StyledTooltip = withStyles({
  tooltip: {
    width: 'fit-content',
    maxWidth: '205px',
    backgroundColor: '#393939',
    borderRadius: '0px',
    color: '#fff',
    fontSize: '14px',
  },
  arrow: {
    color: '#393939',
  },
})(muiTooltip);

const Tooltip: FC<TooltipProps> = ({ title, textAlignment, children, style, ...props }) => {
  if (!title) return <>{children}</>;

  return (
    <StyledTooltip
      title={title}
      style={{ textAlign: textAlignment ? textAlignment : 'center', ...style }}
      {...props}
    >
      {children}
    </StyledTooltip>
  );
};

export default Tooltip;
