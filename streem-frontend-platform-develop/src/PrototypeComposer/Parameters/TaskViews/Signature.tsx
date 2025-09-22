import SignatureIcon from '#assets/svg/Signature';
import React, { FC } from 'react';
import styled from 'styled-components';

const SignatureTaskViewWrapper = styled.div`
  align-items: center;
  background-color: #f4f4f4;
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  padding: 40px;
  gap: 8px;

  .icon {
    font-size: 48px;
    margin-bottom: 8px;
    cursor: not-allowed;
  }

  span {
    font-size: 14px;
    line-height: 1.14;
    letter-spacing: 0.16px;
    color: #525252;
  }
`;

const SignatureTaskView: FC = () => {
  return (
    <SignatureTaskViewWrapper>
      <SignatureIcon className="icon" />
      <span>User will tap here to record his signature</span>
    </SignatureTaskViewWrapper>
  );
};

export default SignatureTaskView;
