import { Drawer, DrawerProps } from '@material-ui/core';
import { Close } from '@material-ui/icons';
import { noop } from 'lodash';
import React, { useState } from 'react';
import styled from 'styled-components';

export const DrawerWrapper = styled(Drawer)`
  .MuiDrawer-paper {
    width: 100dvw;
    overflow-y: hidden;
    @media (min-width: 576px) {
      width: 100dvw;
    }
    @media (min-width: 900px) {
      width: 75dvw;
    }
    @media (min-width: 1200px) {
      width: 50dvw;
    }
    .drawer-header {
      padding: 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid #e0e0e0;

      h4 {
        margin: unset;
        font-size: 14px;
        font-weight: bold;
        line-height: 1.14;
        letter-spacing: 0.16px;
      }

      svg {
        font-size: 16px;
        color: rgba(0, 0, 0, 0.3);
        cursor: pointer;
      }
    }
    .drawer-body {
      padding: 0px 12px;
      overflow-y: auto;
      @media (min-width: 900px) {
        padding: 0px 16px;
      }
      @media (min-width: 1200px) {
        padding: 0px 24px;
      }
      display: flex;
      flex: 1;
    }
    .drawer-footer {
      padding: 12px 24px;
      display: flex;
      justify-content: flex-end;
      border-top: 1px solid #e0e0e0;

      button {
        height: 32px;
      }
    }
  }
`;

export type useDrawerProps = {
  title: string;
  bodyContent: JSX.Element;
  footerContent?: JSX.Element;
  isOpen?: boolean;
  drawerProps?: DrawerProps;
  headerProps?: React.HTMLAttributes<HTMLDivElement>;
  bodyProps?: React.HTMLAttributes<HTMLDivElement>;
  footerProps?: React.HTMLAttributes<HTMLDivElement>;
  hideCloseIcon?: boolean;
};

export const useDrawer = ({
  title,
  bodyContent,
  drawerProps,
  footerContent,
  isOpen = false,
  headerProps,
  bodyProps,
  footerProps,
  hideCloseIcon = false,
}: useDrawerProps) => {
  const [drawerOpen, setDrawerOpen] = useState(isOpen);

  const drawer = (
    <DrawerWrapper
      anchor="right"
      open={drawerOpen}
      disableEscapeKeyDown
      disableEnforceFocus
      onClose={noop}
      {...drawerProps}
    >
      <div className="drawer-header" {...headerProps}>
        <h4>{title}</h4>
        {!hideCloseIcon && <Close onClick={() => setDrawerOpen(false)} />}
      </div>
      <div className="drawer-body" {...bodyProps}>
        {bodyContent}
      </div>
      {footerContent && (
        <div className="drawer-footer" {...footerProps}>
          {footerContent}
        </div>
      )}
    </DrawerWrapper>
  );

  return { StyledDrawer: drawer, setDrawerOpen, isOpen: drawerOpen };
};
