import EditUser from '#assets/svg/EditUser';
import InviteExpired from '#assets/svg/InviteExpired';
import PushNew from '#assets/svg/PushNew';
import { Button, useScrollableSections, Option } from '#components';
import { User, UserStates } from '#store/users/types';
import { updateUserProfile } from '#views/Auth/actions';
import { navigate } from '@reach/router';
import React, { FC, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import { addUser, resendInvite, validateArchiveUser } from '../actions';
import { createSectionConfig, Toggleables } from './helpers';
import { Composer } from './styles';
import { EditUserProps, EditUserRequestInputs, PAGE_TYPE, TogglesState, UserType } from './types';
import { onCancelInvite, onUnArchiveUser, onUnlockUser, onValidateArchiveUser } from '../utils';

const ManageUser: FC<EditUserProps> = ({
  user: selectedUser,
  facilities: list,
  isAccountOwner,
  isEditable,
  pageType,
}) => {
  const dispatch = useDispatch();
  const [toggles, setToggles] = useState<TogglesState>({
    [Toggleables.EDIT_PASSWORD]: false,
    [Toggleables.EDIT_QUESTIONS]: false,
  });

  const updateToggles = (key: Toggleables) => {
    setToggles({
      ...toggles,
      [key]: !toggles[key],
    });
  };

  const formData = useForm<EditUserRequestInputs>({
    mode: 'onChange',
    criteriaMode: 'all',
  });

  const { register, handleSubmit, formState, setValue, reset, getValues } = formData;
  const { isDirty, isValid } = formState;

  useEffect(() => {
    reset({
      firstName: selectedUser?.firstName,
      lastName: selectedUser?.lastName,
      employeeId: selectedUser?.employeeId,
      username: selectedUser?.username,
      email: selectedUser?.email,
      department: selectedUser?.department,
      facilities: selectedUser?.facilities?.map((f) => ({ id: f.id })),
      roles: selectedUser?.roles?.[0]?.id,
      userType: selectedUser?.userType || UserType.LOCAL,
    });
  }, [selectedUser?.id]);

  register('facilities', { required: true });
  register('roles', { required: true });
  register('userType', { required: true });
  register('username');

  const onSubmit = () => {
    const { reason, ...data } = getValues();
    const body = {
      ...data,
      reason,
      email: data.email ? data.email.toLowerCase() : null,
      roles: [{ id: data.roles }],
    };
    if (selectedUser?.id && data.facilities) {
      Object.keys(data).forEach((key) => data[key] === undefined && delete data[key]);
      reset({
        ...(selectedUser as unknown as EditUserRequestInputs),
        ...data,
      });
      dispatch(
        updateUserProfile({
          body,
          id: selectedUser.id,
        }),
      );
    } else {
      dispatch(addUser(body));
    }
  };

  const onFacilityChange = (options: Option[] | null) => {
    if (!options) {
      setValue('facilities', undefined, {
        shouldDirty: true,
        shouldValidate: true,
      });
    } else {
      setValue(
        'facilities',
        options.map((o) => ({ id: o.value as string })),
        {
          shouldDirty: true,
          shouldValidate: true,
        },
      );
    }
  };

  const { renderLabels, renderViews } = useScrollableSections(
    createSectionConfig({
      pageType,
      isEditable,
      isAccountOwner,
      selectedUser,
      onFacilityChange,
      updateToggles,
      toggles,
      formData,
      facilities: list,
    }),
  );

  const ArchiveButton = () => (
    <Button
      className="primary-button"
      onClick={() =>
        dispatch(
          validateArchiveUser({ user: selectedUser as User, onArchiveUser: onValidateArchiveUser }),
        )
      }
    >
      Archive
    </Button>
  );

  const UnArchiveButton = () => (
    <Button className="primary-button" onClick={() => onUnArchiveUser(selectedUser)}>
      Unarchive
    </Button>
  );

  const UnlockButton = () => (
    <Button className="primary-button" onClick={() => onUnlockUser(selectedUser)}>
      Unlock
    </Button>
  );

  const ResendInviteButton = () => (
    <Button
      className="primary-button"
      onClick={() => dispatch(resendInvite({ id: selectedUser.id }))}
    >
      Reset Invite
    </Button>
  );

  const CancelInviteButton = () => (
    <Button className="primary-button" color="dark" onClick={() => onCancelInvite(selectedUser)}>
      Cancel Invite
    </Button>
  );

  const GenerateNewSecretButton = () => (
    <Button
      className="primary-button"
      onClick={() => dispatch(resendInvite({ id: selectedUser.id }))}
    >
      Generate New Secret Key
    </Button>
  );

  const renderButtons = () => {
    if (pageType === PAGE_TYPE.EDIT && !isAccountOwner && isEditable) {
      if (selectedUser.archived) {
        return <UnArchiveButton />;
      } else {
        return (
          <>
            {(() => {
              switch (selectedUser.state) {
                case UserStates.ACCOUNT_LOCKED:
                  return (
                    <>
                      <UnlockButton />
                    </>
                  );
                case UserStates.INVITE_CANCELLED:
                  return (
                    <>
                      <ArchiveButton />
                      <ResendInviteButton />
                    </>
                  );
                case UserStates.INVITE_EXPIRED:
                  return (
                    <>
                      <ArchiveButton />
                      <ResendInviteButton />
                    </>
                  );
                case UserStates.REGISTERED_LOCKED:
                  return (
                    <>
                      <ArchiveButton />
                      <UnlockButton />
                    </>
                  );
                case UserStates.UNREGISTERED:
                  return (
                    <>
                      <GenerateNewSecretButton />
                      <CancelInviteButton />
                    </>
                  );
                case UserStates.UNREGISTERED_LOCKED:
                  return (
                    <>
                      <ArchiveButton />
                      <UnlockButton />
                    </>
                  );
                default:
                  return <ArchiveButton />;
              }
            })()}
          </>
        );
      }
    }

    return null;
  };

  const isInviteExpired = selectedUser?.state === UserStates.INVITE_EXPIRED;

  const renderIcon = () => {
    if (pageType === PAGE_TYPE.ADD) {
      return <PushNew />;
    } else if (isInviteExpired) {
      return <InviteExpired />;
    }

    return <EditUser />;
  };

  return (
    <Composer onSubmit={handleSubmit(onSubmit)}>
      {renderLabels()}
      {renderViews()}
      <div className="action-sidebar">
        {isInviteExpired && (
          <span className="registration-info alert">
            Invitation to register has expired for this user.
          </span>
        )}

        {renderIcon()}

        {selectedUser?.state === UserStates.UNREGISTERED && (
          <span className="registration-info">User has not registered yet.</span>
        )}

        {renderButtons()}

        {isEditable && (
          <Button
            className="primary-button"
            type="submit"
            variant={pageType === PAGE_TYPE.EDIT ? 'secondary' : 'primary'}
            disabled={!isValid || !isDirty}
          >
            Save Changes
          </Button>
        )}
        <Button className="cancel-button" variant="textOnly" onClick={() => navigate('/users')}>
          Go Back
        </Button>
      </div>
    </Composer>
  );
};

export default ManageUser;
