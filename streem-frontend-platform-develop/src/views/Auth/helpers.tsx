import LoginBackground from '#assets/svg/LoginBackground.svg';
import RegisterBackground from '#assets/svg/RegisterBackground.svg';
import { Button, Option } from '#components';
import { useTypedSelector } from '#store';
import { switchFacility } from '#store/facilities/actions';
import { apiCheckUsername } from '#utils/apiUrls';
import { InputTypes } from '#utils/globalTypes';
import { request } from '#utils/request';
import { encrypt } from '#utils/stringUtils';
import {
  accountLookUp,
  additionalVerification,
  checkTokenExpiry,
  login,
  notifyAdmin,
  register as registerAction,
  resetByMail,
  resetPassword,
  setChallengeQuestion,
  validateIdentity,
  validateQuestion,
} from '#views/Auth/actions';
import {
  ArrowBack,
  CheckCircle,
  LockOutlined,
  VisibilityOutlined,
  VpnKeyOutlined,
} from '@material-ui/icons';
import { Link, navigate, useLocation } from '@reach/router';
import { keys } from 'lodash';
import React, { useState } from 'react';
import { UseFormMethods } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import {
  AdditionalVerificationTypes,
  BaseViewConfigType,
  CARD_POSITIONS,
  ChallengeQuestionPurpose,
  CredentialsInputs,
  EmployeeIdInputs,
  ForgotPasswordInputs,
  ForgotPasswordRecoveryInputs,
  LoginInputs,
  NewPasswordInputs,
  PAGE_NAMES,
  RecoveryInputs,
  RecoveryOptions,
  SecretKeyInputs,
  TokenTypes,
} from './types';

type CreateBaseViewConfigProps = {
  pageName: PAGE_NAMES;
  loading: boolean;
  register: UseFormMethods['register'];
  formState: UseFormMethods['formState'];
  getValues: UseFormMethods['getValues'];
  setValue: UseFormMethods['setValue'];
  setError: UseFormMethods['setError'];
  clearErrors: UseFormMethods['clearErrors'];
  questions: Option[] | undefined;
};

const LockIcon = () => (
  <div
    style={{
      display: 'flex',
      padding: '12px',
      fontSize: '18px',
      backgroundColor: '#dadada',
      borderRadius: '50%',
    }}
  >
    <LockOutlined />
  </div>
);

const leftCardConfig: BaseViewConfigType = {
  wrapperStyle: {
    background: `url(${LoginBackground}) no-repeat center center fixed`,
    backgroundSize: 'cover',
  },
  cardPosition: CARD_POSITIONS.LEFT,
  cardStyle: {
    backgroundColor: 'transparent',
  },
};

const centerCardConfig: BaseViewConfigType = {
  wrapperStyle: {
    background: `url(${RegisterBackground}) no-repeat center center fixed`,
    backgroundSize: 'cover',
  },
  cardPosition: CARD_POSITIONS.CENTER,
  cardStyle: {
    backgroundColor: '#FFF',
    boxShadow:
      '0 0 1px 0 rgba(0, 0, 0, 0.04), 0 2px 6px 0 rgba(0, 0, 0, 0.04), 0 16px 24px 0 rgba(0, 0, 0, 0.06)',
  },
};

const ContactAdminButton = styled(Button)`
  display: inline-block;
  padding: 0;
  margin-right: 4px !important;
  font-size: inherit;
`;

export const createBaseViewConfig = ({
  pageName,
  loading,
  register,
  formState,
  getValues,
  setValue,
  setError,
  clearErrors,
  questions,
}: CreateBaseViewConfigProps) => {
  const { isDirty, isValid, errors } = formState;
  const dispatch = useDispatch();
  const location = useLocation();
  const {
    firstName,
    lastName,
    employeeId,
    token,
    email,
    hasSetChallengeQuestion,
    facilities,
    userId,
    identity,
    settings: {
      passwordPolicy: { minimumPasswordLength },
    },
  } = useTypedSelector((state) => state.auth);
  const [isPasswordInputType, setIsPasswordInputType] = useState(true);
  const [isConfirmPasswordTextHidden, setIsConfirmPasswordTextHidden] = useState(true);

  const PasswordAfterIcon = () => (
    <VisibilityOutlined
      onClick={() => setIsPasswordInputType(!isPasswordInputType)}
      style={{ color: isPasswordInputType ? '#999' : '#1d84ff' }}
    />
  );

  const ConfirmPasswordAfterIcon = () => (
    <VisibilityOutlined
      onClick={() => setIsConfirmPasswordTextHidden(!isConfirmPasswordTextHidden)}
      style={{ color: isConfirmPasswordTextHidden ? '#999' : '#1d84ff' }}
    />
  );

  if (pageName === PAGE_NAMES.LOGIN && !email) {
    navigate('/auth/login');
  }

  if (
    ([
      PAGE_NAMES.REGISTER_CREDENTIALS,
      PAGE_NAMES.REGISTER_EMPLOYEE_ID,
      PAGE_NAMES.REGISTER_RECOVERY,
      PAGE_NAMES.FORGOT_NEW_PASSWORD,
      PAGE_NAMES.INVITATION_EXPIRED,
      PAGE_NAMES.KEY_EXPIRED,
    ].includes(pageName) &&
      !token) ||
    ([PAGE_NAMES.FORGOT_RECOVERY, PAGE_NAMES.FORGOT_QUESTIONS, PAGE_NAMES.ACCOUNT_LOCKED].includes(
      pageName,
    ) &&
      !identity)
  ) {
    navigate('/auth/login');
  }

  const passwordValidators = (password: string) => ({
    password: {
      functions: {
        smallLength: (value: string) => value.length >= minimumPasswordLength,
        caseCheck: (value: string) => /^(?=.*[a-z])(?=.*[A-Z])/.test(value),
        digitLetter: (value: string) => /[0-9]/.test(value),
        specialChar: (value: string) => /.*[!@#$%^&*() =+_-]/.test(value),
      },
      messages: {
        smallLength: `${minimumPasswordLength} characters minimum`,
        caseCheck: 'Upper and lowercase letters',
        digitLetter: 'At least one number',
        specialChar: 'At least one special character',
      },
    },
    confirmPassword: {
      functions: {
        passwordMatch: (value: string) =>
          errors?.password?.length || value !== password ? false : true,
      },
      messages: {
        passwordMatch: 'Passwords Match',
      },
    },
  });

  const reverseValidationCheckOnPasswords = (confirmPassword: string) => ({
    passwordMatch: (value: string) => {
      if (confirmPassword) {
        if (value !== confirmPassword) {
          setError('confirmPassword', {
            type: 'passwordMatch',
            message: '',
          });
        } else {
          clearErrors('confirmPassword');
        }
      }
      return true;
    },
  });

  switch (pageName) {
    case PAGE_NAMES.ACCOUNT_LOOKUP:
      return {
        ...leftCardConfig,
        heading: 'Welcome User',
        subHeading: 'Provide your credentials below to login',
        formData: {
          formInputs: [
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Write your Username or Email ID here',
                label: 'Username or Email ID',
                id: 'username',
                name: 'username',
                ref: register({
                  required: true,
                }),
              },
            },
          ],
          onSubmit: async (data: LoginInputs) => {
            dispatch(accountLookUp(data.username, location.search));
          },
          buttons: [
            <Button
              key="login"
              type="submit"
              loading={loading}
              disabled={loading || !isDirty || !isValid}
            >
              Continue
            </Button>,
          ],
        },
        footerAction: (
          <div>
            New User ? <Link to="/auth/register">Register Yourself</Link>
          </div>
        ),
      };

    case PAGE_NAMES.LOGIN:
      return {
        ...leftCardConfig,
        heading: 'Welcome User',
        subHeading: 'Provide your credentials below to login',
        formData: {
          formInputs: [
            {
              type: InputTypes.PASSWORD,
              props: {
                type: isPasswordInputType ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE,
                placeholder: 'Enter your Password',
                label: 'Password',
                id: 'password',
                name: 'password',
                ref: register({
                  required: true,
                }),
                AfterElement: PasswordAfterIcon,
                afterElementWithoutError: true,
                secondaryAction: {
                  text: 'Forgot Password?',
                  action: () => {
                    navigate('/auth/forgot-password');
                  },
                },
              },
            },
          ],
          onSubmit: async (data: LoginInputs) => {
            dispatch(login({ ...data, username: email! }));
          },
          buttons: [
            <Button
              key="login"
              type="submit"
              loading={loading}
              disabled={loading || !isDirty || !isValid}
            >
              Continue
            </Button>,
          ],
        },
        footerAction: (
          <div>
            New User ? <Link to="/auth/register">Register Yourself</Link>
          </div>
        ),
      };

    case PAGE_NAMES.FORGOT_IDENTITY:
      return {
        ...centerCardConfig,
        heading: 'Forgot Password',
        subHeading:
          'Use your Credentials to see your Recovery Options or click the option below if you already have a Secret Key.',
        formData: {
          formInputs: [
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Write your Username or Email ID here',
                label: 'Username or Email ID',
                id: 'identity',
                name: 'identity',
                ref: register({
                  required: true,
                }),
              },
            },
          ],
          onSubmit: (data: ForgotPasswordInputs) => {
            dispatch(validateIdentity(data));
          },
          buttons: [
            <Button
              key="forgot"
              type="submit"
              disabled={loading || !isDirty || !isValid}
              style={{ marginLeft: 'auto' }}
            >
              Show Recovery Options
            </Button>,
          ],
        },
        footerAction: (
          <div>
            <div>
              Already have a Secret Key?{' '}
              <Link to="/auth/forgot-password/secret-key">Click here</Link>
            </div>
            <div style={{ marginTop: 20 }}>
              Go back to <Link to="/auth/login">Login Page</Link>
            </div>
          </div>
        ),
      };

    case PAGE_NAMES.FORGOT_RECOVERY: {
      register('recoveryOption', { required: true });

      const items = [];

      if (email) {
        items.push({
          key: RecoveryOptions.EMAIL,
          label: 'Get an email',
          value: RecoveryOptions.EMAIL,
          desc: `You will receive recovery instructions to your registered email '${email}'.`,
        });
      }

      if (hasSetChallengeQuestion) {
        items.push({
          key: RecoveryOptions.CHALLENGE_QUESTION,
          label: 'Answer a Challenge Question',
          value: RecoveryOptions.CHALLENGE_QUESTION,
          desc: 'Answer to a Challenge Question that you have set.',
        });
      } else {
        items.push({
          key: RecoveryOptions.CHALLENGE_QUESTION,
          label: 'Challenge Question not set',
          value: RecoveryOptions.CHALLENGE_QUESTION,
          disabled: true,
          desc: 'You have not set challenge question',
        });
      }

      if (!email && !hasSetChallengeQuestion) {
        items.push({
          key: RecoveryOptions.CONTACT_ADMIN,
          label: 'Contact Administrator',
          value: RecoveryOptions.CONTACT_ADMIN,
          desc: 'Send a request to your administrator to resest password for your account.',
        });
      }

      return {
        ...centerCardConfig,
        heading: 'Recovery Options',
        subHeading: 'Select how you want to recover your password.',
        formData: {
          formInputs: [
            {
              type: InputTypes.RADIO,
              props: {
                groupProps: {
                  id: 'recoveryOption',
                  name: 'recoveryOption',
                  onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
                    setValue('recoveryOption', e.target.value, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                  },
                },
                items,
              },
            },
          ],
          onSubmit: (data: ForgotPasswordRecoveryInputs) => {
            if (data.recoveryOption === RecoveryOptions.EMAIL) {
              dispatch(
                resetByMail({
                  identity,
                }),
              );
            } else if (data.recoveryOption === RecoveryOptions.CHALLENGE_QUESTION) {
              navigate('/auth/forgot-password/challenge');
            } else if (data.recoveryOption === RecoveryOptions.CONTACT_ADMIN) {
              dispatch(
                notifyAdmin({
                  identity,
                  purpose: ChallengeQuestionPurpose.PASSWORD_RECOVERY_CHALLENGE_QUESTION_NOT_SET,
                }),
              );
            }
          },
          buttons: [
            <Button
              key="forgot"
              type="submit"
              disabled={loading || !isDirty || !isValid}
              style={{ marginLeft: 'auto' }}
            >
              Continue
            </Button>,
          ],
        },
        footerAction: (
          <div>
            <div>
              Go back to <Link to="/auth/login">Login Page</Link>
            </div>
          </div>
        ),
      };
    }

    case PAGE_NAMES.FORGOT_QUESTIONS: {
      register('id', { required: true });

      return {
        ...centerCardConfig,
        heading: 'Challenge Question',
        subHeading:
          'Select your Challenge Question and provide correct answer to it to set your new password.',
        formData: {
          formInputs: [
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'question',
                name: 'question',
                options: questions,
                placeholder: 'Select your challenge question',
                label: 'Challenge Question',
                onChange: (option: Option | null) => {
                  if (!option) {
                    setValue('id', undefined as any, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                  } else {
                    setValue('id', option.value as any, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                  }
                },
              },
            },
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Write your Answer here',
                label: 'Enter Your Answer',
                id: 'answer',
                name: 'answer',
                ref: register({
                  required: true,
                }),
              },
            },
          ],
          onSubmit: (data: RecoveryInputs) => {
            dispatch(
              validateQuestion({
                ...data,
                identity,
              }),
            );
          },
          buttons: [
            <Button
              key="forgot"
              type="submit"
              disabled={loading || !isDirty || !isValid}
              style={{ marginLeft: 'auto' }}
            >
              Verify Answer
            </Button>,
          ],
        },
        footerAction: (
          <div>
            <Link
              to="/auth/forgot-password/recovery"
              style={{
                color: '#1c1c1c',
                display: 'flex',
                alignItems: 'center',
              }}
            >
              <ArrowBack style={{ marginRight: '16px' }} /> Recovery Options
            </Link>
          </div>
        ),
      };
    }

    case PAGE_NAMES.REGISTER_SECRET_KEY:
    case PAGE_NAMES.FORGOT_SECRET_KEY:
      return {
        ...centerCardConfig,
        heading: 'Identify Yourself',
        subHeading: 'Provide your Secret Key to identify yourself',
        formData: {
          formInputs: [
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Enter your Secret Key here',
                label: 'Secret Key',
                id: 'token',
                name: 'token',
                ref: register({
                  required: true,
                }),
              },
            },
          ],
          onSubmit: (data: SecretKeyInputs) => {
            dispatch(
              checkTokenExpiry({
                ...data,
                type:
                  pageName === PAGE_NAMES.REGISTER_SECRET_KEY
                    ? TokenTypes.REGISTRATION
                    : TokenTypes.PASSWORD_RESET,
              }),
            );
          },
          buttons: [
            <Button
              key="forgot"
              type="submit"
              disabled={loading || !isDirty || !isValid}
              style={{ marginLeft: 'auto' }}
            >
              {pageName === PAGE_NAMES.REGISTER_SECRET_KEY ? 'Identify me' : 'Verify'}
            </Button>,
          ],
        },
        footerAction:
          pageName === PAGE_NAMES.REGISTER_SECRET_KEY ? (
            <div>
              Already Registered? <Link to="/auth/login">Login to your Account</Link>
            </div>
          ) : (
            <div>
              Go back to <Link to="/auth/login">Login Page</Link>
            </div>
          ),
      };
    case PAGE_NAMES.ADMIN_NOTIFIED:
      return {
        ...centerCardConfig,
        footerAction: (
          <div>
            <div
              style={{
                margin: '0px 0px 64px',
                display: 'flex',
                alignItems: 'center',
                fontSize: '14px',
                lineHeight: '16px',
                letterSpacing: '0.16px',
                color: '#333333',
                fontWeight: 'bold',
              }}
            >
              <CheckCircle
                style={{
                  color: '#5AA700',
                  fontSize: '34px',
                  marginRight: '16px',
                }}
              />
              Notification is sent. <br /> Administrator will contact you soon.
            </div>
            Go back to <Link to="/auth/login">Login Page</Link>
          </div>
        ),
      };
    case PAGE_NAMES.FORGOT_EMAIL_SENT:
      return {
        ...centerCardConfig,
        footerAction: (
          <div>
            <div
              style={{
                margin: '24px 0px 64px',
                display: 'flex',
                alignItems: 'center',
                fontSize: '14px',
                lineHeight: '16px',
                letterSpacing: '0.16px',
                color: '#333333',
                fontWeight: 'bold',
              }}
            >
              <CheckCircle
                style={{
                  color: '#5AA700',
                  fontSize: '34px',
                  marginRight: '16px',
                }}
              />
              We’ve sent an email to {email} <br />
              to reset your password.
            </div>
            Go back to <Link to="/auth/login">Login Page</Link>
          </div>
        ),
      };
    case PAGE_NAMES.PASSWORD_UPDATED:
      return {
        ...centerCardConfig,
        footerAction: (
          <div>
            <div
              style={{
                margin: '24px 0px 64px',
                display: 'flex',
                alignItems: 'center',
                fontSize: '14px',
                lineHeight: '16px',
                letterSpacing: '0.16px',
                color: '#333333',
                fontWeight: 'bold',
              }}
            >
              <CheckCircle
                style={{
                  color: '#5AA700',
                  fontSize: '34px',
                  marginRight: '16px',
                }}
              />
              You have successfully set your new password.
            </div>
            Go back to <Link to="/auth/login">Login Page</Link>
          </div>
        ),
      };
    case PAGE_NAMES.FORGOT_NEW_PASSWORD: {
      const { password, confirmPassword } = getValues(['password', 'confirmPassword']);
      const validators = passwordValidators(password);

      return {
        ...centerCardConfig,
        heading: 'Set New Password',
        formData: {
          formInputs: [
            {
              type: InputTypes.PASSWORD,
              props: {
                type: isPasswordInputType ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE,
                placeholder: 'Enter your new Password',
                label: 'New Password',
                id: 'password',
                name: 'password',
                AfterElement: PasswordAfterIcon,
                afterElementWithoutError: true,
                ref: register({
                  required: true,
                  validate: {
                    ...validators.password.functions,
                    ...reverseValidationCheckOnPasswords(confirmPassword),
                  },
                }),
              },
            },
            {
              type: InputTypes.ERROR_CONTAINER,
              props: {
                messages: validators.password.messages,
                errorsTypes: keys(errors?.password?.types) || [],
              },
            },
            {
              type: InputTypes.PASSWORD,
              props: {
                type: isConfirmPasswordTextHidden ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE,
                placeholder: 'Enter your new Password again',
                label: 'Confirm Password',
                id: 'confirmPassword',
                name: 'confirmPassword',
                AfterElement: ConfirmPasswordAfterIcon,
                afterElementWithoutError: true,
                ref: register({
                  required: true,
                  validate: validators.confirmPassword.functions,
                }),
              },
            },
            {
              type: InputTypes.ERROR_CONTAINER,
              props: {
                messages: validators.confirmPassword.messages,
                errorsTypes:
                  keys({
                    [errors?.confirmPassword?.type]: true,
                    ...errors?.confirmPassword?.types,
                  }) || [],
              },
            },
          ],
          onSubmit: (data: NewPasswordInputs) => {
            if (token) {
              dispatch(
                resetPassword({
                  password: encrypt(data.password),
                  confirmPassword: encrypt(data.confirmPassword),
                  token,
                }),
              );
            }
          },
          buttons: [
            <Button
              key="forgot"
              type="submit"
              disabled={loading || !isDirty || password !== confirmPassword}
              style={{ marginLeft: 'auto' }}
            >
              Set Password
            </Button>,
          ],
        },
      };
    }
    case PAGE_NAMES.REGISTER_EMPLOYEE_ID:
      return {
        ...centerCardConfig,
        heading: 'Nearly Done...',
        subHeading: 'Provide your Employee ID to verify yourself',
        formData: {
          formInputs: [
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Enter your Employee ID',
                label: 'Employee ID',
                id: 'identifier',
                name: 'identifier',
                ref: register({
                  required: true,
                }),
              },
            },
          ],
          onSubmit: (data: EmployeeIdInputs) => {
            if (token) {
              dispatch(
                additionalVerification({
                  identifier: data.identifier,
                  type: AdditionalVerificationTypes.EMPLOYEE_ID,
                  token,
                }),
              );
            }
          },
          buttons: [
            <Button
              key="forgot"
              type="submit"
              disabled={loading || !isDirty || !isValid}
              style={{ marginLeft: 'auto' }}
            >
              Verify
            </Button>,
          ],
        },
      };
    case PAGE_NAMES.REGISTER_CREDENTIALS: {
      const { password, confirmPassword } = getValues(['password', 'confirmPassword']);
      const validators = passwordValidators(password);

      return {
        ...centerCardConfig,
        heading: 'Create User Name & Password',
        formData: {
          formInputs: [
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Full Name (Not Editable)',
                label: 'Full Name (Not Editable)',
                disabled: true,
                value: `${firstName} ${lastName}`,
                id: 'fullName',
                name: 'fullName',
              },
            },
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Employee ID (Not Editable)',
                label: 'Employee ID (Not Editable)',
                disabled: true,
                value: employeeId,
                id: 'employeeId',
                name: 'employeeId',
              },
            },
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Create Username',
                label: 'Create Username',
                id: 'username',
                name: 'username',
                error: errors['username']?.message,
                ref: register({
                  required: true,
                  maxLength: {
                    value: 45,
                    message: "Shouldn't be greater than 45 characters.",
                  },
                  validate: async (value) => {
                    if (!value) return 'Invalid Username';
                    const res = await request('POST', apiCheckUsername(), {
                      data: {
                        username: value.toLowerCase(),
                      },
                    });
                    if (res?.errors?.length)
                      return res?.errors?.[0]?.message || 'Username Already Taken';
                    return true;
                  },
                }),
              },
            },
            {
              type: InputTypes.PASSWORD,
              props: {
                type: isPasswordInputType ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE,
                placeholder: 'Enter your new Password',
                label: 'Create Password',
                id: 'password',
                name: 'password',
                AfterElement: PasswordAfterIcon,
                afterElementWithoutError: true,
                ref: register({
                  required: true,
                  validate: {
                    ...validators.password.functions,
                    ...reverseValidationCheckOnPasswords(confirmPassword),
                  },
                }),
              },
            },
            {
              type: InputTypes.ERROR_CONTAINER,
              props: {
                messages: validators.password.messages,
                errorsTypes: keys(errors?.password?.types) || [],
              },
            },
            {
              type: InputTypes.PASSWORD,
              props: {
                type: isConfirmPasswordTextHidden ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE,
                placeholder: 'Enter your new Password again',
                label: 'Confirm Password',
                id: 'confirmPassword',
                name: 'confirmPassword',
                AfterElement: ConfirmPasswordAfterIcon,
                afterElementWithoutError: true,
                ref: register({
                  required: true,
                  validate: validators.confirmPassword.functions,
                }),
              },
            },
            {
              type: InputTypes.ERROR_CONTAINER,
              props: {
                messages: validators.confirmPassword.messages,
                errorsTypes:
                  keys({
                    [errors?.confirmPassword?.type]: true,
                    ...errors?.confirmPassword?.types,
                  }) || [],
              },
            },
          ],
          onSubmit: (data: CredentialsInputs) => {
            if (token) {
              dispatch(
                registerAction({
                  username: data.username,
                  password: encrypt(data.password),
                  confirmPassword: encrypt(data.confirmPassword),
                  token,
                }),
              );
            }
          },
          buttons: [
            <Button
              key="forgot"
              type="submit"
              disabled={loading || !isDirty || !isValid}
              style={{ marginLeft: 'auto' }}
            >
              Register
            </Button>,
          ],
        },
      };
    }
    case PAGE_NAMES.REGISTER_RECOVERY: {
      register('id', { required: true });

      return {
        ...centerCardConfig,
        heading: 'Recovery Option',
        formData: {
          formInputs: [
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'question',
                name: 'question',
                options: questions,
                placeholder: 'Select a Question',
                label: 'Select a Challenge Question',
                onChange: (option: Option | null) => {
                  if (!option) {
                    setValue('id', undefined as any, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                  } else {
                    setValue('id', option.value as any, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                  }
                },
              },
            },
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Write your Answer here',
                label: 'Enter Your Answer',
                id: 'answer',
                name: 'answer',
                ref: register({
                  required: true,
                }),
              },
            },
          ],
          onSubmit: (data: RecoveryInputs) => {
            if (token) {
              dispatch(
                setChallengeQuestion({
                  ...data,
                  token,
                }),
              );
            }
          },
          buttons: [
            <Button
              key="forgot"
              type="submit"
              disabled={loading || !isDirty || !isValid}
              style={{ marginLeft: 'auto' }}
            >
              Continue
            </Button>,
          ],
        },
      };
    }
    case PAGE_NAMES.KEY_EXPIRED:
      return {
        ...centerCardConfig,
        heading: 'Key Expired',
        headingIcon: <LockIcon />,
        subHeading: 'Your secret key has expired as you failed to use it under 24 hours.',
        footerAction: (
          <div>
            <ContactAdminButton
              variant="textOnly"
              onClick={() => {
                dispatch(
                  notifyAdmin({
                    identity,
                    purpose: ChallengeQuestionPurpose.PASSWORD_RECOVERY_KEY_EXPIRED,
                  }),
                );
              }}
            >
              Contact
            </ContactAdminButton>
            your Administrator to generate a new Secret Key for your registration.
          </div>
        ),
      };
    case PAGE_NAMES.ACCOUNT_LOCKED:
      return {
        ...centerCardConfig,
        heading: 'Account Locked',
        headingIcon: <LockIcon />,
        subHeading: 'Access to to your account has been locked due to multiple failed attempts.',
        footerAction: (
          <div>
            <ContactAdminButton
              variant="textOnly"
              onClick={() => {
                dispatch(
                  notifyAdmin({
                    identity,
                    purpose: ChallengeQuestionPurpose.PASSWORD_RECOVERY_ACCOUNT_LOCKED,
                  }),
                );
              }}
            >
              Contact
            </ContactAdminButton>
            your Administrator to unlock registration.
            <div style={{ marginTop: 64 }}>
              Go back to <Link to="/auth/login">Login Page</Link>
            </div>
          </div>
        ),
      };
    case PAGE_NAMES.INVITATION_EXPIRED:
      return {
        ...centerCardConfig,
        heading: 'Invitation Expired',
        headingIcon: <LockIcon />,
        subHeading: 'Your invitation expired as you failed to verify yourself.',
        footerAction: (
          <div>
            <ContactAdminButton
              variant="textOnly"
              onClick={() => {
                dispatch(
                  notifyAdmin({
                    identity,
                    purpose: ChallengeQuestionPurpose.INVITE_EXPIRED,
                  }),
                );
              }}
            >
              Contact
            </ContactAdminButton>
            your Administrator to unlock registration.
          </div>
        ),
      };
    case PAGE_NAMES.PASSWORD_EXPIRED:
      return {
        ...leftCardConfig,
        heading: 'Password Expired',
        headingIcon: (
          <div
            style={{
              display: 'flex',
              padding: '12px',
              fontSize: '18px',
              backgroundColor: '#dadada',
              borderRadius: '50%',
            }}
          >
            <VpnKeyOutlined />
          </div>
        ),
        footerAction: (
          <div style={{ marginTop: '-3dvh' }}>
            <div
              style={{
                fontWeight: 600,
                color: '#333333',
                marginBottom: '4dvh',
                fontSize: '1.6dvh',
                lineHeight: '2dvh',
                letterSpacing: '0.16px',
              }}
            >
              {`You're unable to login as your password has expired. Please set a
              new password for your Account.`}
            </div>
            <Button
              key="forgot"
              style={{ marginRight: 'auto' }}
              onClick={() => navigate('/auth/forgot-password')}
            >
              Set a New Password
            </Button>
          </div>
        ),
      };
    case PAGE_NAMES.FACILITY_SELECTION: {
      register('id', { required: true });

      return {
        wrapperStyle: {
          background: 'none',
          backgroundSize: 'cover',
        },
        cardPosition: CARD_POSITIONS.CENTER,
        cardStyle: {
          backgroundColor: '#FFF',
          boxShadow:
            '0 0 1px 0 rgba(0, 0, 0, 0.04), 0 2px 6px 0 rgba(0, 0, 0, 0.04), 0 16px 24px 0 rgba(0, 0, 0, 0.06)',
          maxWidth: '30dvw',
        },
        heading: 'Choose Facility',
        subHeading: 'Select a facility to login to from the list of facilities give below.',
        formData: {
          formInputs: [
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'facility',
                name: 'facility',
                options: facilities.map((facility) => ({
                  label: facility.name,
                  value: facility.id,
                })),
                placeholder: 'Select a Facility',
                label: 'Facility',
                onChange: (option: Option | null) => {
                  if (!option) {
                    setValue('id', undefined as any, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                  } else {
                    setValue('id', option.value as any, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                  }
                },
              },
            },
          ],
          onSubmit: (data: { id: string }) => {
            if (userId) {
              dispatch(
                switchFacility({
                  facilityId: data.id,
                  loggedInUserId: userId,
                }),
              );
            }
          },
          buttons: [
            <Button
              key="forgot"
              type="submit"
              style={{ marginLeft: 'auto' }}
              disabled={loading || !isDirty || !isValid}
            >
              Proceed
            </Button>,
          ],
        },
      };
    }
    default:
      return leftCardConfig;
  }
};
