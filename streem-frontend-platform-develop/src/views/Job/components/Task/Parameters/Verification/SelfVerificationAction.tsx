import { Button, TextInput } from '#components';
import { useTypedSelector } from '#store';
import { InputTypes, SsoStates } from '#utils/globalTypes';
import { ssoSigningRedirect } from '#utils/request';
import { jobActions } from '#views/Job/jobStore';
import { Visibility } from '@material-ui/icons';
import { useLocation } from '@reach/router';
import React, { FC, memo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';

type Inputs = {
  password: string;
};

const SelfVerificationAction: FC<{ parameterResponseId: string; parameterId: string }> = ({
  parameterResponseId,
  parameterId,
}) => {
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
  const {
    auth: { ssoIdToken },
  } = useTypedSelector((state) => state);
  const { pathname, search } = useLocation();

  const onSubmit = (data: Inputs) => {
    if (ssoIdToken) {
      ssoSigningRedirect({
        state: SsoStates.SELF_VERIFICATION,
        parameterResponseId,
        location: pathname + search,
      });
    } else {
      dispatch(
        jobActions.completeSelfVerification({
          parameterResponseId,
          parameterId,
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
    <form className="parameter-verification" onSubmit={handleSubmit(onSubmit)}>
      {!ssoIdToken && (
        <TextInput
          AfterElement={AfterIcon}
          name="password"
          placeholder="Enter Your Account Password"
          ref={register({
            required: true,
          })}
          type={passwordInputType ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE}
        />
      )}
      <Button
        style={{ marginRight: 'unset' }}
        type="submit"
        disabled={ssoIdToken ? false : !isValid || !isDirty}
      >
        Verify
      </Button>
      <Button
        variant="secondary"
        onClick={() => {
          dispatch(
            jobActions.recallPeerVerification({ parameterResponseId, parameterId, type: 'self' }),
          );
        }}
      >
        Cancel
      </Button>
    </form>
  );
};

export default memo(SelfVerificationAction);
