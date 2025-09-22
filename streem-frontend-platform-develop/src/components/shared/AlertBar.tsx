import React from 'react';
import { Info } from '@material-ui/icons';
import styled from 'styled-components';

enum MessageType {
  ERROR = 'error',
  WARNING = 'warning',
  SUCCESS = 'success',
}

const Wrapper = styled.div<{ msgType: MessageType }>`
  .alert {
    padding: 8px;
    border: solid 1px
      ${(p) =>
        p.msgType === MessageType.ERROR
          ? '#ff6b6b'
          : p.msgType === MessageType.WARNING
          ? '#ffe58f'
          : '#93ff8f'};
    background-color: ${(p) =>
      p.msgType === MessageType.ERROR
        ? 'rgba(255, 107, 107, 0.16)'
        : p.msgType === MessageType.WARNING
        ? '#fffbe6'
        : '#ddffdb'};
    display: flex;
    flex-direction: row;
    flex: 1;
    gap: 16px;
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

    .msg-wrapper {
      color: #000;
    }

    svg {
      color: ${(p) => (p.msgType === MessageType.ERROR ? '#cc5656' : '#faad14')};
      font-size: 16px;
      line-height: 14px;
    }
  }
`;

const AlertBar = ({ msgText, msgType, Icon }: any) => {
  return (
    <Wrapper msgType={msgType}>
      <div className="alert">
        {Icon ? <Icon /> : <Info />}
        <span className="msg-wrapper">{msgText}</span>
      </div>
    </Wrapper>
  );
};

export default AlertBar;
