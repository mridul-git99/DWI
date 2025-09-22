import { Avatar, Button, FormGroup, useScrollableSectionsProps } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { RoleIdByName } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { Facilities } from '#store/facilities/types';
import { fetchSelectedUserSuccess } from '#store/users/actions';
import { ChallengeQuestion, User, UserStates } from '#store/users/types';
import {
  searchDirectoryUsers,
  apiChallengeQuestions,
  apiCheckEmail,
  apiCheckEmployeeId,
  apiGetAllChallengeQuestions,
  apiUpdatePassword,
} from '#utils/apiUrls';
import { ALL_FACILITY_ID } from '#utils/constants';
import { InputTypes, ResponseObj, ValidatorProps } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import { encrypt, getFullName } from '#utils/stringUtils';
import { Create, VisibilityOutlined } from '@material-ui/icons';
import { debounce, uniqueId } from 'lodash';
import React, { useEffect, useState } from 'react';
import { UseFormMethods } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useDispatch } from 'react-redux';
import { Props } from 'react-select';
import styled from 'styled-components';
import { resendInvite } from '../actions';
import { ValidateCredentialsPurpose } from '../types';
import { Credentials, CustomInputGroup, KeyGenerator } from './styles';
import { EditUserRequestInputs, PAGE_TYPE, TogglesState, UserType } from './types';

export enum Toggleables {
  EDIT_PASSWORD = 'editPassword',
  EDIT_QUESTIONS = 'editQuestions',
}

type CreateSectionConfigProps = {
  pageType: PAGE_TYPE;
  isEditable: boolean;
  facilities: Facilities;
  isAccountOwner: boolean;
  formData: UseFormMethods<EditUserRequestInputs>;
  selectedUser?: User;
  toggles?: TogglesState;
  onFacilityChange: (options: Option[]) => void;
  updateToggles: (key: Toggleables) => void;
};

const UserItem = {
  firstName: '',
  username: '',
  employeeId: '',
  lastName: '',
  email: '',
  department: '',
  reason: '',
};

type UserItem = typeof UserItem;

const UserItemWrapper = styled.div`
  display: flex;
  justify-content: space-between;

  .info {
    display: flex;

    .user-detail {
      display: flex;
      flex-direction: column;
      margin-left: 16px;
      justify-content: space-between;

      .user-id {
        opacity: 0.7;
        font-size: 14px;
      }
    }
  }
  .meta {
    font-size: 14px;
    margin-top: 4px;
  }
`;

export const formatOptionLabel: Props<
  {
    option: string;
    label: string;
  } & UserItem
>['formatOptionLabel'] = (option, { context }) => {
  const firstName = option.firstName;
  const lastName = option?.lastName || '';
  return context === 'value' ? (
    option.label
  ) : (
    <UserItemWrapper>
      <div className="info">
        <Avatar
          user={{
            ...option,
            id: uniqueId(),
            firstName,
            lastName,
            employeeId: '',
          }}
          allowMouseEvents={false}
          size="large"
        />
        <div className="user-detail">
          <span className="user-id">{option.employeeId || ''}</span>
          <span className="user-name">{getFullName({ firstName, lastName })}</span>
        </div>
      </div>
      <div className="meta">{option.email || ''}</div>
    </UserItemWrapper>
  );
};

export const createSectionConfig = ({
  pageType,
  isEditable,
  facilities,
  formData,
  isAccountOwner,
  selectedUser,
  toggles,
  onFacilityChange,
  updateToggles,
}: CreateSectionConfigProps) => {
  const dispatch = useDispatch();
  const { t: translate } = useTranslation(['userManagement']);
  const [validatedToken, setValidatedToken] = useState<string | undefined>();
  const [selectedRole, setSelectedRole] = useState<string | undefined>(
    selectedUser?.roles && selectedUser.roles[0].id,
  );
  const {
    settings: { ssoType },
    userId,
  } = useTypedSelector((state) => state.auth);
  const [currentlySelectedFacilities, setCurrentlySelectedFacilities] = useState<
    Option[] | undefined
  >(
    selectedUser?.facilities?.map((facility) => ({
      value: facility.id,
      label: facility.name,
    })) || undefined,
  );
  const [users, setUsers] = useState<UserItem[]>([]);
  const [disabledKeys, setDisabledKeys] = useState<Record<string, boolean>>({});
  const {
    register,
    errors,
    getValues,
    setValue,
    formState: { isDirty },
  } = formData;
  const { roles: rolesValues, userType } = getValues(['roles', 'userType']);
  const shouldShowAllFacilities = [
    RoleIdByName.ACCOUNT_OWNER,
    RoleIdByName.SYSTEM_ADMIN,
    RoleIdByName.GLOBAL_ADMIN,
  ].includes(rolesValues as RoleIdByName);

  const fetchUsers = async (query?: string) => {
    const response = await request('GET', searchDirectoryUsers(), {
      ...(query && {
        params: {
          query,
        },
      }),
    });
    if (response?.data) {
      setUsers(response.data);
    }
  };

  useEffect(() => {
    if (userType && userType !== UserType.LOCAL && !users.length) {
      fetchUsers();
    }
  }, [userType]);

  const onSelectUser = (user: UserItem) => {
    const keysToDisable: Record<string, boolean> = {};
    setValue('username', user['username'], {
      shouldDirty: true,
      shouldValidate: true,
    });
    (Object.keys(UserItem) as Array<keyof UserItem>).forEach((key) => {
      setValue(key, user?.[key], {
        shouldDirty: true,
        shouldValidate: true,
      });
      if (user?.[key]) {
        keysToDisable[key] = true;
      }
    });
    setDisabledKeys(keysToDisable);
  };

  const getUserPermissionChangeRole = () => {
    if (selectedUser && selectedRole === RoleIdByName.SYSTEM_ADMIN && userId === selectedUser.id) {
      return true;
    }
    return !isEditable || isAccountOwner;
  };

  const onChangeUserType = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.value === UserType.LOCAL) {
      (Object.keys(UserItem) as Array<keyof UserItem>).forEach((key) => {
        setValue(key, selectedUser?.[key], {
          shouldDirty: true,
          shouldValidate: true,
        });
      });
    }
    setValue('userType', e.target.value, {
      shouldDirty: true,
      shouldValidate: true,
    });
    setDisabledKeys({});
  };

  const config: useScrollableSectionsProps = {
    title: translate('userManagement:edit-title'),
    items: [
      {
        label: 'User Type',
        view:
          pageType === PAGE_TYPE.ADD ? (
            <FormGroup
              inputs={[
                {
                  type: InputTypes.RADIO,
                  props: {
                    groupProps: {
                      id: 'userType',
                      name: 'userType',
                      defaultValue: selectedUser?.userType || UserType.LOCAL,
                      onChange: onChangeUserType,
                    },
                    items: [
                      {
                        key: UserType.LOCAL,
                        label: 'Add Manually',
                        value: UserType.LOCAL,
                      },
                      {
                        key: ssoType,
                        label: 'Add from Active Directory',
                        value: ssoType,
                        disabled: ssoType === null,
                      },
                    ],
                  },
                },
                ...(userType !== UserType.LOCAL
                  ? [
                      {
                        type: InputTypes.SINGLE_SELECT,
                        props: {
                          id: 'userSelected',
                          formatOptionLabel,
                          options: users.map((user) => ({
                            ...user,
                            label: user?.email || user.firstName,
                            value: user?.email || user.firstName,
                          })),
                          filterOption: () => true,
                          placeholder: 'Select with Name, Employee ID or Email ID',
                          onChange: (value: UserItem) => {
                            onSelectUser(value);
                          },
                          onInputChange: debounce((v) => {
                            fetchUsers(v);
                          }, 500),
                        },
                      },
                    ]
                  : []),
              ]}
            />
          ) : (
            <div style={{ padding: '24px 16px' }}>
              {selectedUser?.userType !== UserType.LOCAL
                ? 'Added from Active Directory'
                : 'Added Manually'}
            </div>
          ),
      },
      {
        label: 'Basic Details',
        view: (
          <FormGroup
            inputs={[
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  placeholder: 'Enter Employee’s First Name',
                  label: 'First Name',
                  id: 'firstName',
                  name: 'firstName',
                  error: errors['firstName']?.message,
                  readOnly: disabledKeys?.['firstName'],
                  disabled: !isEditable,
                  ref: register({
                    required: true,
                  }),
                },
              },
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  placeholder: 'Enter Employee’s Last Name',
                  label: 'Last Name',
                  id: 'lastName',
                  name: 'lastName',
                  optional: true,
                  error: errors['lastName']?.message,
                  readOnly: disabledKeys?.['lastName'],
                  disabled: !isEditable,
                  ref: register,
                },
              },
            ]}
          />
        ),
      },
      {
        label: 'Work Details',
        view: (
          <FormGroup
            inputs={[
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  placeholder: 'Enter Employee’s ID',
                  label: 'Employee ID',
                  id: 'employeeId',
                  name: 'employeeId',
                  error: errors['employeeId']?.message,
                  disabled: pageType !== PAGE_TYPE.ADD,
                  ref:
                    pageType === PAGE_TYPE.ADD
                      ? register({
                          required: true,
                          maxLength: {
                            value: 45,
                            message: "Shouldn't be greater than 45 characters.",
                          },
                          validate: async (value) => {
                            if (value) {
                              const res = await request('POST', apiCheckEmployeeId(), {
                                data: {
                                  employeeId: value,
                                },
                              });
                              if (res?.errors?.length)
                                return res?.errors?.[0]?.message || 'Employee ID already exists';
                              return true;
                            }
                            return false;
                          },
                        })
                      : register,
                },
              },
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  placeholder: 'Enter Employee’s Email Address',
                  label: 'Email ID',
                  id: 'email',
                  name: 'email',
                  error: errors['email']?.message,
                  readOnly: disabledKeys?.['email'],
                  disabled: !isEditable,
                  optional: true,
                  ref: register({
                    pattern: {
                      value: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+$/i,
                      message: 'Invalid email address',
                    },
                    validate: async (value) => {
                      if (!value || selectedUser?.email === value) return true;
                      const res = await request('POST', apiCheckEmail(), {
                        data: { email: value },
                      });
                      if (res?.errors?.length)
                        return res?.errors?.[0]?.message || 'Email ID already exists';
                      return true;
                    },
                  }),
                },
              },
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  placeholder: 'Enter Employee’s Department',
                  label: 'Department',
                  id: 'department',
                  name: 'department',
                  ref: register,
                  optional: true,
                  disabled: !isEditable,
                },
              },
            ]}
          />
        ),
      },
      ...(selectedUser?.state === UserStates.REGISTERED ||
      selectedUser?.state === UserStates.PASSWORD_EXPIRED ||
      selectedUser?.state === UserStates.REGISTERED_LOCKED
        ? [
            {
              label: 'Login Credentials',
              view: (
                <KeyGenerator>
                  <h3>Username : {selectedUser?.username}</h3>
                  {selectedUser.userType === UserType.LOCAL && !isAccountOwner && isEditable && (
                    <>
                      <Button
                        className="primary-button"
                        variant="secondary"
                        onClick={() => dispatch(resendInvite({ id: selectedUser.id }))}
                      >
                        Generate Secret Key
                      </Button>
                      <p>
                        If the user has no access to their account, they can go to the login page
                        and choose Forgot Password option. Here the user uses the Secret Key to
                        change their password.
                      </p>
                    </>
                  )}
                </KeyGenerator>
              ),
            },
          ]
        : []),
      {
        label: 'Role',
        view: (
          <FormGroup
            inputs={[
              {
                type: InputTypes.ROLE,
                props: {
                  id: 'roles',
                  selected: selectedRole,
                  disabled: getUserPermissionChangeRole(),
                  error: errors['roles']?.message,
                  onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
                    if (
                      [
                        RoleIdByName.ACCOUNT_OWNER,
                        RoleIdByName.SYSTEM_ADMIN,
                        RoleIdByName.GLOBAL_ADMIN,
                      ].includes(e.target.value as RoleIdByName)
                    ) {
                      setValue('facilities', [{ id: ALL_FACILITY_ID }], {
                        shouldDirty: true,
                        shouldValidate: true,
                      });
                    } else {
                      setValue(
                        'facilities',
                        currentlySelectedFacilities?.map((o) => ({
                          id: o.value as string,
                        })),
                        {
                          shouldDirty: true,
                          shouldValidate: true,
                        },
                      );
                    }
                    setValue('roles', e.target.value, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                    setSelectedRole(e.target.value);
                  },
                },
              },
            ]}
          />
        ),
      },
      {
        label: 'Facility',
        view: (
          <>
            {shouldShowAllFacilities && (
              <div style={{ padding: '24px 16px' }}>
                The user with the selected role has access to all the facilities. Facility selection
                is disabled for such users.
              </div>
            )}
            <FormGroup
              inputs={[
                {
                  type: InputTypes.MULTI_SELECT,
                  props: {
                    id: 'facilities',
                    isMulti: true,
                    options: shouldShowAllFacilities
                      ? [{ label: 'All Facilities', value: ALL_FACILITY_ID }]
                      : facilities.map((i) => ({
                          label: i.name,
                          value: i.id,
                        })),
                    placeholder: shouldShowAllFacilities ? 'All Facilities' : 'Select',
                    defaultValue: selectedUser?.facilities?.map((f) => ({
                      label: f.name,
                      value: f.id,
                    })),
                    onChange: (options: Option[]) => {
                      setCurrentlySelectedFacilities(options);
                      onFacilityChange(options);
                    },
                    isDisabled: !isEditable || shouldShowAllFacilities,
                    ...(shouldShowAllFacilities
                      ? { value: { label: 'All Facilities', value: ALL_FACILITY_ID } }
                      : {}),
                  },
                },
              ]}
            />
          </>
        ),
      },
      ...(isDirty
        ? [
            {
              label: 'Reason',
              view: (
                <>
                  <FormGroup
                    inputs={[
                      {
                        type: InputTypes.MULTI_LINE,
                        props: {
                          placeholder: 'Enter Reason',
                          label: 'Reason',
                          id: 'reason',
                          name: 'reason',
                          error: errors['reason']?.message,
                          readOnly: disabledKeys?.['reason'],
                          disabled: !isEditable,
                          rows: 3,
                          ref: register({
                            required: true,
                            maxLength: {
                              value: 255,
                              message: "Shouldn't be greater than 255 characters.",
                            },
                          }),
                        },
                      },
                    ]}
                  />
                </>
              ),
            },
          ]
        : []),
    ],
  };

  if (pageType === PAGE_TYPE.ADD) {
    config.title = translate('userManagement:add-title');
    config.items = [
      ...config.items.slice(0, 3),
      ...config.items.slice(
        selectedUser?.state === UserStates.REGISTERED ? 4 : 3,
        config.items.length,
      ),
    ];
  }

  if (pageType === PAGE_TYPE.PROFILE) {
    config.title = translate('userManagement:profile-title');
    config.items = [
      ...config.items.slice(0, 3),
      {
        label: 'Login Credentials',
        view: (
          <Credentials>
            <div className="row">
              <span className="custom-span">Username</span>
              <span className="custom-span">{selectedUser?.username}</span>
              <span className="custom-span" />
            </div>
            {selectedUser?.userType === UserType.LOCAL && (
              <>
                <div className="row">
                  <span className="custom-span">Password</span>
                  {!toggles?.[Toggleables.EDIT_PASSWORD] ? (
                    <>
                      <span className="custom-span">••••••</span>
                      <span className="custom-span">
                        <Button
                          variant="textOnly"
                          className="with-icon"
                          onClick={() =>
                            dispatch(
                              openOverlayAction({
                                type: OverlayNames.VALIDATE_CREDENTIALS_MODAL,
                                props: {
                                  purpose: ValidateCredentialsPurpose.PASSWORD_UPDATE,
                                  onSuccess: (token: string) => {
                                    setValidatedToken?.(token);
                                    updateToggles?.(Toggleables.EDIT_PASSWORD);
                                  },
                                },
                              }),
                            )
                          }
                        >
                          Edit <Create />
                        </Button>
                      </span>
                    </>
                  ) : (
                    <UpdatePassword
                      updateToggles={updateToggles}
                      token={validatedToken as string}
                    />
                  )}
                </div>
                <div className="row">
                  <span className="custom-span">Challenge Question</span>
                  {!toggles?.[Toggleables.EDIT_QUESTIONS] ? (
                    <>
                      <span className="custom-span">
                        {selectedUser?.challengeQuestion?.question || 'Not Set'}
                      </span>
                      <span className="custom-span">
                        <Button
                          variant="textOnly"
                          className="with-icon"
                          onClick={() =>
                            dispatch(
                              openOverlayAction({
                                type: OverlayNames.VALIDATE_CREDENTIALS_MODAL,
                                props: {
                                  purpose: ValidateCredentialsPurpose.CHALLENGE_QUESTION_UPDATE,
                                  onSuccess: (token: string) => {
                                    setValidatedToken?.(token);
                                    updateToggles?.(Toggleables.EDIT_QUESTIONS);
                                  },
                                },
                              }),
                            )
                          }
                        >
                          {selectedUser?.challengeQuestion?.question ? 'Edit ' : 'Set Now '}
                          <Create />
                        </Button>
                      </span>
                    </>
                  ) : (
                    <UpdateChallengeQuestion
                      updateToggles={updateToggles}
                      token={validatedToken as string}
                    />
                  )}
                </div>
              </>
            )}
          </Credentials>
        ),
      },
      ...config.items.slice(
        selectedUser?.state === UserStates.REGISTERED ? 4 : 3,
        config.items.length,
      ),
    ];
  }

  return config;
};

type Option = {
  value: string;
  label: string;
};

const UpdateChallengeQuestion = ({
  updateToggles,
  token,
}: Pick<CreateSectionConfigProps, 'updateToggles'> & { token: string }) => {
  const dispatch = useDispatch();
  const { userId } = useTypedSelector((state) => state.auth);
  const { selectedUser } = useTypedSelector((state) => state.users);
  const [state, setState] = useState<{
    answer?: string;
    questions?: Option[];
    selected?: Option;
  }>();

  useEffect(() => {
    const fetchQuestionsAndCurrentAnswer = async () => {
      const { data }: ResponseObj<ChallengeQuestion[]> = await request(
        'GET',
        apiGetAllChallengeQuestions(),
      );

      const {
        data: { answer },
      }: ResponseObj<{ answer: string }> = await request('GET', apiChallengeQuestions(userId!), {
        params: { token },
      });

      const questions = data.map(({ question, id }) => ({
        value: id,
        label: question,
      }));

      setState({
        ...state,
        answer,
        questions,
        selected: selectedUser?.challengeQuestion
          ? {
              value: selectedUser?.challengeQuestion.id,
              label: selectedUser?.challengeQuestion.question,
            }
          : questions?.[0],
      });
    };

    fetchQuestionsAndCurrentAnswer();
  }, []);

  const onUpdate = async () => {
    if (userId && selectedUser && state?.selected) {
      await request('PATCH', apiChallengeQuestions(userId), {
        data: {
          id: state?.selected?.value,
          answer: state?.answer,
          token,
        },
      });

      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Challenge Question Updated.',
        }),
      );

      dispatch(
        fetchSelectedUserSuccess({
          data: {
            ...selectedUser,
            challengeQuestion: {
              id: state?.selected?.value,
              question: state?.selected?.label,
            },
          },
        }),
      );

      updateToggles?.(Toggleables.EDIT_QUESTIONS);
    }
  };

  return (
    <CustomInputGroup>
      <FormGroup
        inputs={[
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'challengeQuestion',
              options: state?.questions,
              placeholder: 'Challenge Question',
              label: 'Challenge Question',
              value: state?.selected,
              onChange: (value: Option) => {
                setState({
                  ...state,
                  selected: value,
                });
              },
            },
          },
          {
            type: InputTypes.SINGLE_LINE,
            props: {
              placeholder: 'Enter Your Answer',
              label: 'Your Answer',
              id: 'challengeAnswer',
              name: 'challengeAnswer',
              defaultValue: state?.answer,
              onChange: ({ value }: { value: string }) => {
                setState({
                  ...state,
                  answer: value,
                });
              },
            },
          },
        ]}
      />
      <div className="actions-bar" style={{ paddingLeft: '16px' }}>
        <Button color="dark" onClick={() => updateToggles?.(Toggleables.EDIT_QUESTIONS)}>
          Cancel
        </Button>
        <Button disabled={!state?.selected || !state.answer} onClick={onUpdate}>
          Update
        </Button>
      </div>
    </CustomInputGroup>
  );
};

const UpdatePassword = ({
  updateToggles,
  token,
}: Pick<CreateSectionConfigProps, 'updateToggles'> & { token: string }) => {
  const dispatch = useDispatch();
  const {
    userId,
    settings: {
      passwordPolicy: { minimumPasswordLength },
    },
  } = useTypedSelector((state) => state.auth);
  const [state, setState] = useState<{
    password: string;
    confirmPassword: string;
    errors: {
      password?: string[];
      confirmPassword?: string[];
    };
  }>({
    password: '',
    confirmPassword: '',
    errors: {
      password: ['smallLength', 'caseCheck', 'digitLetter', 'specialChar'],
      confirmPassword: ['passwordMatch'],
    },
  });

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

  const validators: {
    password: ValidatorProps;
    confirmPassword: ValidatorProps;
  } = {
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
          state?.errors?.password?.length || value !== state?.password ? false : true,
      },
      messages: {
        passwordMatch: 'Passwords Match',
      },
    },
  };

  const onInputChange = (value: string, name: 'password' | 'confirmPassword') => {
    const errors: string[] = [];
    Object.keys(validators[name].functions).forEach((key) => {
      if (!validators[name].functions[key](value)) errors.push(key);
    });

    let passwordMatchError = {};
    if (name === 'password' && value !== state.confirmPassword) {
      passwordMatchError = {
        confirmPassword: ['passwordMatch'],
      };
    } else {
      passwordMatchError = {
        confirmPassword: [],
      };
    }

    setState({
      ...state,
      [name]: value,
      errors: {
        ...state.errors,
        ...passwordMatchError,
        [name]: errors,
      },
    });
  };

  const onUpdate = async () => {
    if (userId) {
      const { password, confirmPassword } = state;
      const { errors }: ResponseObj<unknown> = await request('PATCH', apiUpdatePassword(userId), {
        data: {
          password: encrypt(password),
          confirmPassword: encrypt(confirmPassword),
          token,
        },
      });

      if (errors) {
        const error = getErrorMsg(errors);
        dispatch(
          showNotification({
            type: NotificationType.ERROR,
            msg: error,
          }),
        );
      } else {
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: 'Password updated successfully.',
          }),
        );

        updateToggles?.(Toggleables.EDIT_PASSWORD);
      }
    }
  };

  return (
    <CustomInputGroup>
      <FormGroup
        inputs={[
          {
            type: InputTypes.PASSWORD,
            props: {
              type: isPasswordInputType ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE,
              placeholder: 'Create Password',
              label: 'Create Password',
              id: 'password',
              name: 'password',
              AfterElement: PasswordAfterIcon,
              afterElementWithoutError: true,
              onChange: ({ value }: { value: string }) => {
                onInputChange(value, 'password');
              },
            },
          },
          {
            type: InputTypes.ERROR_CONTAINER,
            props: {
              messages: validators.password.messages,
              errorsTypes: state?.errors?.password,
            },
          },
          {
            type: InputTypes.PASSWORD,
            props: {
              type: isConfirmPasswordTextHidden ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE,
              placeholder: 'Confirm New Password',
              label: 'Confirm New Password',
              id: 'confirmPassword',
              name: 'confirmPassword',
              AfterElement: ConfirmPasswordAfterIcon,
              afterElementWithoutError: true,
              onChange: ({ value }: { value: string }) => {
                onInputChange(value, 'confirmPassword');
              },
            },
          },
          {
            type: InputTypes.ERROR_CONTAINER,
            props: {
              messages: validators.confirmPassword.messages,
              errorsTypes: state?.errors?.confirmPassword,
            },
          },
        ]}
      />
      <div className="actions-bar">
        <Button color="dark" onClick={() => updateToggles?.(Toggleables.EDIT_PASSWORD)}>
          Cancel
        </Button>
        <Button
          onClick={onUpdate}
          disabled={
            state?.errors?.confirmPassword?.length || state?.errors?.password?.length ? true : false
          }
        >
          Update
        </Button>
      </div>
    </CustomInputGroup>
  );
};
