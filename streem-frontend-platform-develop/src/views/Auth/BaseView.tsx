import logo from '#assets/svg/leucine-logo.svg';
import { FormGroup, Option } from '#components';
import { useTypedSelector } from '#store';
import { ChallengeQuestion } from '#store/users/types';
import { apiGetAllChallengeQuestions } from '#utils/apiUrls';
import { ResponseObj } from '#utils/globalTypes';
import { request } from '#utils/request';
import { cleanUp, resetError } from '#views/Auth/actions';
import { CancelRounded, CloseRounded } from '@material-ui/icons';
import React, { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

import { createBaseViewConfig } from './helpers';
import { BaseViewConfigType, BaseViewProps, PAGE_NAMES } from './types';
import { navigate } from '@reach/router';

const Wrapper = styled.div.attrs({
  className: 'auth-base-view',
})<{ config: BaseViewConfigType }>`
  display: flex;
  flex: 1;
  padding: 6dvh 64px;
  align-items: flex-start;
  overflow: auto;
  justify-content: ${({ config: { cardPosition } }) => cardPosition};

  .card {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    width: 30dvw;
    overflow: visible;
    padding: 3.5dvh;

    .logo {
      width: 7dvw;
    }

    .logo-caption {
      margin: 8px 9px 0 0px;
      font-size: 1.6dvh;
      line-height: 1.14;
      letter-spacing: 0.16px;
      color: #8e8e8e;
    }

    .header-wrapper {
      margin: 6dvh 0 4px 0;
      display: flex;
      flex: 1;
      align-items: center;

      .heading-icon {
        margin-right: 8px;
      }

      .heading {
        font-size: 4dvh;
        font-weight: 300;
        color: #333333;
      }
    }

    .sub-heading {
      font-size: 1.6dvh;
      letter-spacing: 0.16px;
      color: #999999;
    }

    .errors-container {
      display: flex;
      margin-top: 4dvh;
      justify-content: flex-start;
      align-items: center;
      padding: 2dvh;
      background-color: #ffebeb;
      border-left: 2px solid #ff6b6b;

      svg {
        width: 18px;
        height: 18px;
      }

      .side {
        height: 18px;
        display: flex;
        align-self: flex-start;
        align-items: center;
        font-size: 1.6dvh;
        font-weight: bold;
        color: #333333;

        span {
          cursor: pointer;
        }
      }

      .main {
        display: flex;
        flex: 1;
        padding: 0 32px 0 6px;
        font-size: 1.6dvh;
        font-weight: bold;
        color: #333333;
      }
    }

    form {
      display: flex;
      flex: 1;
      flex-direction: column;

      button {
        padding: 1.2dvh 2.4dvh;
      }

      .form-group {
        padding: 4dvh 0;
        width: 100%;

        > div {
          margin-bottom: 3dvh;

          :last-child {
            margin-bottom: unset;
          }
        }

        .input-label {
          font-size: 1.8dvh;
        }

        .input-wrapper {
          padding: 1dvh 1.6dvh;
        }
      }
    }

    .footer-action {
      margin-top: 5.5dvh;
      font-size: 1.8dvh;
      line-height: 1.14;
      letter-spacing: 0.16px;
      color: #1c1c1c;

      a {
        color: #1d84ff;
        text-decoration: none;
      }
    }
  }

  @media (max-width: 1000px) {
    padding: 6dvh 24px;

    .card {
      width: calc(100dvw - 48px);
      max-width: 80dvw !important;
      .logo {
        width: 14dvw;
      }
    }
  }
`;

function BaseView<T = Record<string, unknown>>({ pageName }: BaseViewProps) {
  const dispatch = useDispatch();
  const { error, loading, isLoggedIn, ssoIdToken } = useTypedSelector((state) => state.auth);
  const [state, setState] = useState<{
    questions?: Option[];
  }>();
  const { register, handleSubmit, formState, getValues, trigger, setValue, setError, clearErrors } =
    useForm<T>({
      mode: 'onChange',
      criteriaMode: 'all',
    });

  const config = createBaseViewConfig({
    loading,
    pageName,
    register,
    formState,
    getValues,
    setValue,
    setError,
    clearErrors,
    questions: state?.questions,
  });

  useEffect(() => {
    dispatch(resetError());
    if (pageName === PAGE_NAMES.ACCOUNT_LOOKUP) {
      dispatch(cleanUp());
    }
    trigger();
    if (pageName === PAGE_NAMES.REGISTER_RECOVERY || pageName === PAGE_NAMES.FORGOT_QUESTIONS) {
      const fetchQuestions = async () => {
        try {
          const { data }: ResponseObj<ChallengeQuestion[]> = await request(
            'GET',
            apiGetAllChallengeQuestions(),
          );

          const questions = data.map(({ question, id }) => ({
            value: id,
            label: question,
          }));

          setState({
            questions,
          });
        } catch (e) {
          console.error('Error In Fetching Challenge Questions', e);
        }
      };

      fetchQuestions();
    }

    if (pageName === PAGE_NAMES.FACILITY_SELECTION) {
      if (isLoggedIn && ssoIdToken) {
        navigate('/facility/selection');
      }
    }
  }, []);

  return (
    <Wrapper config={config} style={config.wrapperStyle}>
      <div className="card" style={config.cardStyle}>
        <img src={logo} className="logo" />
        <div className="logo-caption">Orchestration Platform for Pharma</div>
        <div className="header-wrapper">
          {config.headingIcon && <div className="heading-icon">{config.headingIcon}</div>}
          {config.heading && <div className="heading">{config.heading}</div>}
        </div>
        <div className="sub-heading">{config.subHeading}</div>
        {error && (
          <div className="errors-container">
            <div className="side">
              <CancelRounded style={{ marginRight: '16px' }} /> Error :
            </div>
            <div className="main">{error}</div>
            <div className="side">
              <span onClick={() => dispatch(resetError())}>
                <CloseRounded />
              </span>
            </div>
          </div>
        )}
        {config.formData && (
          <form onSubmit={handleSubmit(config.formData.onSubmit)}>
            <FormGroup inputs={config.formData.formInputs} />
            {config.formData.buttons.map((Button) => Button)}
          </form>
        )}
        {config.footerAction && <div className="footer-action">{config.footerAction}</div>}
      </div>
    </Wrapper>
  );
}

export default BaseView;
