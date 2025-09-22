import { Checklist } from '#PrototypeComposer/checklist.types';
import { releasePrototype, signOffPrototype } from '#PrototypeComposer/reviewer.actions';
import { BaseModal, Button, TextInput } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { InputTypes } from '#utils/globalTypes';
import { UserType } from '#views/UserAccess/ManageUser/types';
import { Visibility } from '@material-ui/icons';
import React, { FC, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    max-width: 368px !important;
    min-width: 300px !important;

    .close-icon {
      top: 22px !important;
      right: 16px !important;
      font-size: 24px !important;
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
        color: #000;
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

const PasswordInputModal: FC<
  CommonOverlayProps<{
    isReleasing: boolean;
  }>
> = ({ closeAllOverlays, closeOverlay, props }) => {
  const isReleasing = props?.isReleasing || false;
  const dispatch = useDispatch();
  const { data: checklist } = useTypedSelector((state) => ({
    approvers: state.prototypeComposer.approvers,
    data: state.prototypeComposer.data as Checklist,
  }));
  const { userType } = useTypedSelector((state) => state.auth);
  const [passwordInputType, setPasswordInputType] = useState(true);
  const { register, handleSubmit, formState } = useForm<Inputs>({
    mode: 'onChange',
    criteriaMode: 'all',
  });
  const { isDirty, isValid } = formState;

  const onSubmit = (data: Inputs) => {
    if (isReleasing) {
      dispatch(
        releasePrototype({
          checklistId: checklist.id,
          password: data.password,
        }),
      );
    } else {
      dispatch(
        signOffPrototype({
          checklistId: checklist.id,
          password: data.password,
        }),
      );
    }
  };

  const AfterIcon = () => (
    <Visibility
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
        title="Ready for Release"
      >
        <div>
          {isReleasing ? (
            <>
              <div>
                Ongoing Jobs will be executed as per the previous version of the the Process. All
                new Jobs will be created as per the latest version.
              </div>
              <div style={{ marginTop: '10px' }}>
                By Continuing you will Release the Process for creating Jobs.
              </div>
            </>
          ) : (
            'By Continuing you will sign the Prototype and make it ready for release.'
          )}
        </div>
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
              type={passwordInputType ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE}
            />
          )}
          <Button
            type="submit"
            disabled={userType === UserType.LOCAL ? !isValid || !isDirty : false}
          >
            Approve
          </Button>
        </form>
      </BaseModal>
    </Wrapper>
  );
};

export default PasswordInputModal;
