import leucineLogo from '#assets/svg/leucine-logo.svg';
import { RouteComponentProps, Router } from '@reach/router';
import React, { FC } from 'react';
import styled from 'styled-components';
import BaseView from './BaseView';
import {
  CredentialsInputs,
  EmployeeIdInputs,
  ForgotPasswordInputs,
  LoginInputs,
  NewPasswordInputs,
  PAGE_NAMES,
  RecoveryInputs,
  SecretKeyInputs,
} from './types';

const Wrapper = styled.div.attrs({
  className: 'auth-view',
})`
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex: 1;

  .brand-footer {
    display: flex;
    background-color: #fafafa;
    padding: 1dvh 30px;

    > div {
      display: flex;
      align-items: center;
      margin-left: auto;
      font-size: 12px;
      line-height: 1.33;
      letter-spacing: 0.32px;
      color: #999999;

      > a {
        line-height: 0;
        margin-left: 8px;

        svg {
          width: 7dvw;
          height: 5dvh;

          @media (max-width: 900px) {
            width: 14dvw;
          }
        }
      }
    }
  }
`;

const AuthView: FC<RouteComponentProps> = () => {
  return (
    <Wrapper>
      <Router style={{ display: 'flex', flex: 1, overflow: 'auto' }}>
        <BaseView<EmployeeIdInputs>
          path="register/employee-id"
          pageName={PAGE_NAMES.REGISTER_EMPLOYEE_ID}
        />
        <BaseView<CredentialsInputs>
          path="register/credentials"
          pageName={PAGE_NAMES.REGISTER_CREDENTIALS}
        />
        <BaseView path="register/invite-expired" pageName={PAGE_NAMES.INVITATION_EXPIRED} />
        <BaseView<RecoveryInputs>
          path="register/recovery"
          pageName={PAGE_NAMES.REGISTER_RECOVERY}
        />
        <BaseView<SecretKeyInputs> path="register" pageName={PAGE_NAMES.REGISTER_SECRET_KEY} />
        <BaseView path="forgot-password/key-expired" pageName={PAGE_NAMES.KEY_EXPIRED} />
        <BaseView path="account-locked" pageName={PAGE_NAMES.ACCOUNT_LOCKED} />
        <BaseView path="password-expired" pageName={PAGE_NAMES.PASSWORD_EXPIRED} />
        <BaseView path="notified" pageName={PAGE_NAMES.ADMIN_NOTIFIED} />
        <BaseView path="forgot-password/email-sent" pageName={PAGE_NAMES.FORGOT_EMAIL_SENT} />
        <BaseView path="forgot-password/updated" pageName={PAGE_NAMES.PASSWORD_UPDATED} />
        <BaseView<ForgotPasswordInputs>
          path="forgot-password/recovery"
          pageName={PAGE_NAMES.FORGOT_RECOVERY}
        />
        <BaseView<RecoveryInputs>
          path="forgot-password/challenge"
          pageName={PAGE_NAMES.FORGOT_QUESTIONS}
        />
        <BaseView<SecretKeyInputs>
          path="forgot-password/secret-key"
          pageName={PAGE_NAMES.FORGOT_SECRET_KEY}
        />
        <BaseView<NewPasswordInputs>
          path="forgot-password/new-password"
          pageName={PAGE_NAMES.FORGOT_NEW_PASSWORD}
        />
        <BaseView<ForgotPasswordInputs>
          path="forgot-password"
          pageName={PAGE_NAMES.FORGOT_IDENTITY}
        />
        <BaseView<LoginInputs> path="/login-password" pageName={PAGE_NAMES.LOGIN} />
        <BaseView<LoginInputs> path="/*" pageName={PAGE_NAMES.ACCOUNT_LOOKUP} />
      </Router>
      <div className="brand-footer">
        <div>
          A Product By
          <a href="https://www.leucinetech.com">
            <img src={leucineLogo} width="110px" style={{ marginBottom: '8px' }} />
          </a>
        </div>
      </div>
    </Wrapper>
  );
};

export default AuthView;
