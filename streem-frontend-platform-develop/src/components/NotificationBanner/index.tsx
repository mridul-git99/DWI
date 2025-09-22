import { useTypedSelector } from '#store';
import { InputTypes } from '#utils/globalTypes';
import { formatDateTime } from '#utils/timeUtils';
import { LicenseState } from '#views/Auth/types';
import { Info } from '@material-ui/icons';
import { getUnixTime } from 'date-fns';
import React from 'react';
import styled from 'styled-components';

enum MessageType {
  ERROR = 'ERROR',
  WARNING = 'WARNING',
}

const HeaderWrapper = styled.div<{ msgType: MessageType }>`
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: unset !important;
  flex: unset !important;

  .msg-wrapper {
    padding-left: 5px;
  }

  .alert {
    padding: 4px;
    border: solid 1px ${(p) => (p.msgType === MessageType.ERROR ? '#ff6b6b' : '#ffe58f')};
    background-color: ${(p) =>
      p.msgType === MessageType.ERROR ? 'rgba(255, 107, 107, 0.16)' : '#fffbe6'};
    display: flex;
    flex-direction: row;
    flex: 1;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    overflow: auto;

    .msg-title {
      font-weight: bold;
      color: #000;
      line-height: 12px;
      margin: 0px 4px 0px 8px;
    }

    svg {
      color: ${(p) => (p.msgType === MessageType.ERROR ? '#cc5656' : '#faad14')};
      font-size: 16px;
      line-height: 14px;
    }
  }
`;

const NotificationBanner = () => {
  const { connected } = useTypedSelector((state) => state.extras);
  const { NonGenuineLicenseMap, selectedFacility } = useTypedSelector((state) => state.auth);
  const license = selectedFacility?.id && NonGenuineLicenseMap?.[selectedFacility.id];

  let msgObj: { msgText: JSX.Element | string; msgType: MessageType } | undefined = undefined;
  if (!connected) {
    msgObj = {
      msgText: (
        <span>
          <span className="msg-title">NO INTERNET.</span>Please make sure your are connected to
          internet to use the Leucine App.
        </span>
      ),
      msgType: MessageType.ERROR,
    };
  } else if (license) {
    const expiredDate = formatDateTime({
      value: getUnixTime(new Date(license.renewalDate)),
      type: InputTypes.DATE,
    });
    msgObj = {
      msgText: `Your Leucine subscription will expire on ${expiredDate}. Please get in touch with your Account Manager to renew the subscription.`,
      msgType: MessageType.WARNING,
    };
    if (license.state !== LicenseState.INTIMATE) {
      const graceEndsOn = formatDateTime({
        value: getUnixTime(new Date(license.graceEndsOn)),
        type: InputTypes.DATE,
      });
      msgObj = {
        msgText: `Your Leucine subscription expired on ${expiredDate}. Please renew before ${graceEndsOn} to ensure uninterrupted access to Leucine.`,
        msgType: MessageType.ERROR,
      };
    }
  }

  if (msgObj) {
    return (
      <HeaderWrapper msgType={msgObj.msgType}>
        <div className="alert">
          <Info />
          <span className="msg-wrapper">{msgObj.msgText}</span>
        </div>
      </HeaderWrapper>
    );
  }

  return <></>;
};

export default NotificationBanner;
