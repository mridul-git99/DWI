import { Avatar, BaseModal, Button, TextInput } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { SsoStates } from '#utils/globalTypes';
import { ssoSigningRedirect } from '#utils/request';
import { UserType } from '#views/UserAccess/ManageUser/types';
import { VisibilityOutlined } from '@material-ui/icons';
import React, { FC, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { logout, reLogin } from '../actions';

// TODO Handle closing of this modal if relogin api fails for some reason.
const Wrapper = styled.div`
  #modal-container {
    z-index: 1500 !important;
    .modal {
      max-width: 468px !important;
      min-width: 300px !important;

      .close-icon {
        display: none !important;
      }

      h2 {
        color: #000 !important;
        font-weight: bold !important;
        font-size: 24px !important;
        line-height: 29px !important;
      }

      .modal-header {
        padding: 24px 24px 8px !important;
        border-bottom: none !important;
      }

      .modal-body {
        text-align: left;
        padding: 0px 24px 24px !important;
        display: flex;
        flex-direction: column;

        > span {
          font-size: 14px;
          line-height: 1.33;
          letter-spacing: 0.32px;
          text-align: left;
          color: #999999;
        }

        form {
          margin-top: 24px;

          .input {
            .input-label {
              font-size: 12px;
            }

            .input-wrapper {
              border-color: transparent;
              border-bottom-color: #999999;
            }
          }

          button {
            width: 100%;
            margin-top: 40px;
            justify-content: center;
          }
        }
      }
    }
  }
`;

type Inputs = {
  password: string;
};

const SessionExpireModal: FC<CommonOverlayProps<unknown>> = ({
  closeAllOverlays,
  closeOverlay,
}) => {
  const dispatch = useDispatch();
  const { profile, userType, ssoIdToken } = useTypedSelector((state) => state.auth);
  const [passwordInputType, setPasswordInputType] = useState(true);
  const { register, handleSubmit, formState } = useForm<Inputs>({
    mode: 'onChange',
    criteriaMode: 'all',
  });
  const { isDirty, isValid } = formState;

  const onSubmit = async (data: Inputs) => {
    if (profile && profile.username) {
      if (ssoIdToken) {
        ssoSigningRedirect({ state: SsoStates.RE_LOGIN, location: window?.location?.pathname });
      } else {
        dispatch(
          reLogin({
            ...data,
            username: profile.username,
          }),
        );
      }
    }
  };

  const AfterIcon = () => (
    <VisibilityOutlined
      onClick={() => setPasswordInputType(!passwordInputType)}
      style={{ color: passwordInputType ? '#000' : '#1d84ff' }}
    />
  );

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        title="Session Expired!"
        allowCloseOnOutsideClick={false}
      >
        {profile && (
          <div style={{ display: 'flex', flexDirection: 'row', gap: '8px' }}>
            <Avatar user={profile!} size="large" allowMouseEvents={false} />
            <div>
              <div style={{ fontSize: '12px', color: '#c2c2c2' }}>{profile!.employeeId}</div>
              <div style={{ color: '#161616', fontSize: '16px', marginTop: '4px' }}>
                {profile!.firstName} {profile!.lastName}
              </div>
            </div>
          </div>
        )}
        <span style={{ marginTop: '8px' }}>
          Your current session has expired. You may continue by log in again.
        </span>
        <form onSubmit={handleSubmit(onSubmit)}>
          {userType === UserType.LOCAL && (
            <TextInput
              ref={register({
                required: true,
              })}
              AfterElement={AfterIcon}
              name="password"
              label="Password"
              placeholder="Password"
              error={true}
              type={passwordInputType ? 'password' : 'text'}
            />
          )}
          <div style={{ display: 'flex' }}>
            <Button
              style={{ width: 'auto' }}
              variant="secondary"
              onClick={() => {
                dispatch(logout({ ssoIdToken: ssoIdToken }));
              }}
            >
              Logout
            </Button>
            <Button
              type="submit"
              style={{ marginLeft: 'auto', width: 'auto' }}
              disabled={userType === UserType.LOCAL ? !isValid || !isDirty : false}
            >
              Proceed to Login
            </Button>
          </div>
        </form>
      </BaseModal>
    </Wrapper>
  );
};

export default SessionExpireModal;
