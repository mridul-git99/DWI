import { Button, TextInput } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { InputTypes, SsoStates } from '#utils/globalTypes';
import { ssoSigningRedirect } from '#utils/request';
import { jobActions } from '#views/Job/jobStore';
import { Visibility } from '@material-ui/icons';
import { useLocation } from '@reach/router';
import React, { FC, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';

type Inputs = {
  password: string;
};

const PeerVerificationAction: FC<{ parameterResponseId: string; parameterId: string }> = ({
  parameterResponseId,
  parameterId,
}) => {
  const [showPasswordField, setShowPasswordField] = useState(false);
  const [passwordInputType, setPasswordInputType] = useState(true);
  const dispatch = useDispatch();
  const {
    register,
    handleSubmit,
    formState: { isDirty, isValid },
  } = useForm<Inputs>({
    mode: 'onChange',
    criteriaMode: 'all',
  });
  const { ssoIdToken } = useTypedSelector((state) => state.auth);
  const { pathname, search } = useLocation();

  const onSubmit = (data: Inputs) => {
    dispatch(
      jobActions.acceptPeerVerification({
        parameterResponseId,
        parameterId,
        password: data.password,
      }),
    );
  };

  const AfterIcon = () => (
    <Visibility
      onClick={() => setPasswordInputType(!passwordInputType)}
      style={{ color: passwordInputType ? '#000' : '#1d84ff' }}
    />
  );

  return (
    <>
      {showPasswordField ? (
        <form className="parameter-verification" onSubmit={handleSubmit(onSubmit)}>
          <TextInput
            AfterElement={AfterIcon}
            name="password"
            placeholder="Enter Your Account Password"
            ref={register({
              required: true,
            })}
            type={passwordInputType ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE}
          />
          <Button style={{ marginRight: 'unset' }} type="submit" disabled={!isValid || !isDirty}>
            Verify
          </Button>
          <Button
            variant="secondary"
            onClick={() => {
              setShowPasswordField(false);
            }}
          >
            Cancel
          </Button>
        </form>
      ) : (
        <div className="parameter-verification">
          <Button
            onClick={() => {
              if (ssoIdToken) {
                ssoSigningRedirect({
                  state: SsoStates.PEER_VERIFICATION,
                  parameterResponseId,
                  location: pathname + search,
                });
              } else {
                setShowPasswordField(true);
              }
            }}
          >
            Approve
          </Button>
          <Button
            variant="secondary"
            color="red"
            onClick={() => {
              dispatch(
                openOverlayAction({
                  type: OverlayNames.REASON_MODAL,
                  props: {
                    modalTitle: 'Reject Verification',
                    modalDesc: 'Provide reason for rejection',
                    onSubmitHandler: (reason: string) =>
                      dispatch(
                        jobActions.rejectPeerVerification({
                          parameterResponseId,
                          parameterId,
                          comment: reason,
                        }),
                      ),
                  },
                }),
              );
            }}
          >
            Reject
          </Button>
        </div>
      )}
    </>
  );
};

export default PeerVerificationAction;
