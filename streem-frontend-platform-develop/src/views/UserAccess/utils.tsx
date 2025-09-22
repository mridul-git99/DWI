import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { User } from '#store/users/types';
import React from 'react';
import { archiveUser, cancelInvite, unArchiveUser, unLockUser } from './actions';

export const findUsersFromQuery = (upsertUserMap: any, userId: string, searchQuery: string) => {
  const user = upsertUserMap[userId];
  if (
    user &&
    (user.firstName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.lastName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.employeeId.toLowerCase().includes(searchQuery.toLowerCase()))
  ) {
    return user;
  }
  return false;
};

export const modalBody = (text: string, user?: User) => {
  return (
    <div className="body-content" style={{ textAlign: 'left' }}>
      {text}
      {user && <span style={{ fontWeight: 'bold' }}>{` ${user.firstName} ${user.lastName}.`}</span>}
    </div>
  );
};

export const onValidateArchiveUser = ({
  user,
  fetchData,
  error = '',
}: {
  user: User;
  fetchData?: () => void;
  error?: string;
}) => {
  if (error) {
    window.store.dispatch(
      openOverlayAction({
        type: OverlayNames.CONFIRMATION_MODAL,
        props: {
          title: 'Archive User',
          body: (
            <>
              This user is currently assigned to active jobs. Are you sure you want to archive this?
            </>
          ),
          onPrimary: () => {
            onArchiveUser(user, fetchData);
          },
        },
      }),
    );
  } else {
    onArchiveUser(user, fetchData);
  }
};

export const onArchiveUser = (user: User, fetchData?: () => void) => {
  window.store.dispatch(
    openOverlayAction({
      type: OverlayNames.REASON_MODAL,
      props: {
        modalTitle: 'Archive User',
        modalDesc: modalBody('You’re about to archive', user),
        onSubmitHandler: (reason: string, setFormErrors: (errors?: Error[]) => void) => {
          window.store.dispatch(
            archiveUser({
              id: user.id,
              reason,
              setFormErrors,
              fetchData,
              user,
            }),
          );
        },
        onSubmitModalText: 'Confirm',
      },
    }),
  );
};

export const onUnArchiveUser = (user: User, fetchData?: () => void) => {
  window.store.dispatch(
    openOverlayAction({
      type: OverlayNames.REASON_MODAL,
      props: {
        modalTitle: 'Unarchive User',
        modalDesc: modalBody('You’re about to unarchive', user),
        onSubmitHandler: (reason: string, setFormErrors: (errors?: Error[]) => void) => {
          window.store.dispatch(
            unArchiveUser({
              id: user.id,
              reason,
              setFormErrors,
              fetchData,
              user,
            }),
          );
        },
        onSubmitModalText: 'Confirm',
      },
    }),
  );
};

export const onCancelInvite = (user: User, fetchData?: () => void) => {
  window.store.dispatch(
    openOverlayAction({
      type: OverlayNames.CONFIRMATION_MODAL,
      props: {
        title: 'Cancel Pending Invite',
        primaryText: 'Cancel Invite',
        onPrimary: () =>
          window.store.dispatch(
            cancelInvite({
              id: user.id,
              fetchData,
            }),
          ),
        body: modalBody('You are about to cancel the invite sent to', user),
      },
    }),
  );
};

export const onUnlockUser = (user: User, fetchData?: () => void) => {
  window.store.dispatch(
    openOverlayAction({
      type: OverlayNames.CONFIRMATION_MODAL,
      props: {
        title: 'Unlocking a User',
        primaryText: 'Unlock User',
        onPrimary: () =>
          window.store.dispatch(
            unLockUser({
              id: user.id,
              fetchData,
            }),
          ),
        body: modalBody('You’re about to unlock the account of', user),
      },
    }),
  );
};
