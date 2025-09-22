import { BaseModal, Button, TextInput } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { encrypt } from '#utils/stringUtils';
import { Visibility, VisibilityOutlined } from '@material-ui/icons';
import React, { FC, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { validateCredentials } from '../actions';
import { ValidateCredentialsPurpose } from '../types';
import { InputTypes } from '#utils/globalTypes';

const Wrapper = styled.div`
  .modal {
    max-width: 468px !important;
    min-width: 300px !important;

    h2 {
      color: #333333 !important;
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
        color: #999999;
        line-height: 19px;
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
`;

type Inputs = {
  password: string;
};

const ValidateCredentialsModal: FC<
  CommonOverlayProps<{
    purpose: ValidateCredentialsPurpose;
    onSuccess: (token: string) => void;
  }>
> = ({ closeAllOverlays, closeOverlay, props: { purpose, onSuccess } }) => {
  const dispatch = useDispatch();
  const { profile } = useTypedSelector((state) => state.auth);
  const [passwordInputType, setPasswordInputType] = useState(true);
  const { register, handleSubmit, formState } = useForm<Inputs>({
    mode: 'onChange',
    criteriaMode: 'all',
  });

  const onSubmit = (data: Inputs) => {
    if (profile && profile.username) {
      dispatch(
        validateCredentials({
          password: encrypt(data.password),
          purpose,
          onSuccess,
        }),
      );
    }
  };

  const AfterIcon = () => (
    <div onClick={() => setPasswordInputType(!passwordInputType)} style={{ cursor: 'pointer' }}>
      {!passwordInputType ? (
        <Visibility style={{ color: '#1d84ff' }} />
      ) : (
        <VisibilityOutlined style={{ color: '#666' }} />
      )}
    </div>
  );

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        title="Current Password"
      >
        <span>Confirm your current password.</span>
        <form onSubmit={handleSubmit(onSubmit)}>
          <TextInput
            ref={register({
              required: true,
            })}
            AfterElement={AfterIcon}
            name="password"
            label="Password"
            placeholder="Password"
            error={true}
            type={passwordInputType ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE}
          />
          <Button type="submit" disabled={!formState.isValid || !formState.isDirty}>
            Confirm
          </Button>
        </form>
      </BaseModal>
    </Wrapper>
  );
};

export default ValidateCredentialsModal;
