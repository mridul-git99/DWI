import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { closeOverlayAction, openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { fetchSelectedUserSuccess } from '#store/users/actions';
import { User } from '#store/users/types';
import {
  apiArchiveUser,
  apiCancelInvite,
  apiGetUsers,
  apiResendInvite,
  apiUnArchiveUser,
  apiUnLockUser,
  apiValidateArchiveUser,
  apiValidatePassword,
} from '#utils/apiUrls';
import { ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { navigate } from '@reach/router';
import { call, put, takeLatest } from 'redux-saga/effects';
import { UserType } from './ManageUser/types';
import {
  addUser,
  archiveUser,
  cancelInvite,
  resendInvite,
  unArchiveUser,
  unLockUser,
  validateArchiveUser,
  validateCredentials,
} from './actions';
import { UserAccessAction } from './types';
import { getFullName } from '#utils/stringUtils';

function* resendInviteSaga({ payload }: ReturnType<typeof resendInvite>) {
  try {
    const { id } = payload;
    const { data, errors }: ResponseObj<User> = yield call(request, 'PATCH', apiResendInvite(id));

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(fetchSelectedUserSuccess({ data }));

    yield put(
      openOverlayAction({
        type: OverlayNames.SECRET_KEY_MODAL,
        props: {
          heading: 'New Secret Key Created',
          subHeading: 'The user can now register using the new Secret Key',
          key: data?.token,
        },
      }),
    );
  } catch (error) {
    yield* handleCatch('UserAccess', 'resendInviteSaga', error, true);
  }
}

function* cancelInviteSaga({ payload }: ReturnType<typeof cancelInvite>) {
  try {
    const { id, fetchData } = payload;
    const { data, errors }: ResponseObj<User> = yield call(request, 'PATCH', apiCancelInvite(id));

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'Invite Canceled Successfully.',
      }),
    );
    if (fetchData) {
      yield call(fetchData);
    } else {
      yield put(fetchSelectedUserSuccess({ data }));
    }
  } catch (error) {
    yield* handleCatch('UserAccess', 'cancelInviteSaga', error, true);
  }
}

function* archiveUserSaga({ payload }: ReturnType<typeof archiveUser>) {
  try {
    const { id, reason, setFormErrors, fetchData, user } = payload;
    const { data, errors }: ResponseObj<User> = yield call(request, 'PATCH', apiArchiveUser(id), {
      data: { reason },
    });

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'User has been Archived',
        detail: `${getFullName(user)} (ID: ${user.employeeId}) has been archived successfully.`,
      }),
    );
    setFormErrors(errors);
    if (fetchData) {
      yield call(fetchData);
    } else {
      yield put(fetchSelectedUserSuccess({ data }));
    }
  } catch (error) {
    yield* handleCatch('UserAccess', 'archiveUserSaga', error, true);
  }
}

function* unArchiveUserSaga({ payload }: ReturnType<typeof unArchiveUser>) {
  try {
    const { id, reason, setFormErrors, fetchData, user } = payload;
    const { data, errors }: ResponseObj<User> = yield call(request, 'PATCH', apiUnArchiveUser(id), {
      data: { reason },
    });

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'User has been Unarchived',
        detail: `${getFullName(user)} (ID: ${user.employeeId}) has been unarchived successfully.`,
      }),
    );
    setFormErrors(errors);
    if (fetchData) {
      yield call(fetchData);
    } else {
      yield put(fetchSelectedUserSuccess({ data }));
    }
  } catch (error) {
    yield* handleCatch('UserAccess', 'unArchiveUserSaga', error, true);
  }
}

function* unLockUserSaga({ payload }: ReturnType<typeof unLockUser>) {
  try {
    const { id, fetchData } = payload;
    const { data, errors }: ResponseObj<User> = yield call(request, 'PATCH', apiUnLockUser(id));

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'User Unlocked !!',
      }),
    );
    if (fetchData) {
      yield call(fetchData);
    } else {
      yield put(fetchSelectedUserSuccess({ data }));
    }
  } catch (error) {
    yield* handleCatch('UserAccess', 'unLockUserSaga', error, true);
  }
}

function* addUserSaga({ payload }: ReturnType<typeof addUser>) {
  try {
    const { data, errors }: ResponseObj<Partial<User>> = yield call(
      request,
      'POST',
      apiGetUsers(),
      {
        data: payload,
      },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    navigate('/users');
    let heading = 'Invitation Sent';
    let subHeading =
      'Invitation with Secret Key to register has been sent to the Employee’s Email ID.';
    if (!data.email) {
      heading = 'User Added';
      subHeading = 'User will need to register themselves using the Secret Key shown below.';
    }
    yield put(
      openOverlayAction({
        type: OverlayNames.SECRET_KEY_MODAL,
        props: {
          heading,
          subHeading,
          key: data.token,
          showSecretKeyInfo: payload.userType === UserType.LOCAL,
        },
      }),
    );
  } catch (error) {
    yield* handleCatch('UserAccess', 'addUserSaga', error, true);
  }
}

function* validateCredentialsSaga({ payload }: ReturnType<typeof validateCredentials>) {
  try {
    const { onSuccess, password, purpose } = payload;
    const { data, errors }: ResponseObj<{ token: string }> = yield call(
      request,
      'PATCH',
      apiValidatePassword(),
      { data: { password, purpose } },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    if (onSuccess) {
      yield put(closeOverlayAction(OverlayNames.VALIDATE_CREDENTIALS_MODAL));
      onSuccess(data.token);
    }
  } catch (error) {
    yield* handleCatch('UserAccess', 'validateCredentialsSaga', error, true);
  }
}

function* validateArchiveUserSaga({ payload }: ReturnType<typeof validateArchiveUser>) {
  try {
    const { user, onArchiveUser, fetchData } = payload;
    const { data, errors }: ResponseObj<User> = yield call(
      request,
      'PATCH',
      apiValidateArchiveUser(user.id),
    );

    if (errors) {
      const errorMsg = getErrorMsg(errors);
      if (onArchiveUser) {
        onArchiveUser({
          user,
          error: errorMsg,
          fetchData,
        });
      }
    } else if (data) {
      if (onArchiveUser) {
        onArchiveUser({
          user,
          fetchData,
        });
      }
    }
  } catch (error) {
    yield* handleCatch('UserAccess', 'archiveUserSaga', error, true);
  }
}

export function* UserAccessSaga() {
  yield takeLatest(UserAccessAction.RESEND_INVITE, resendInviteSaga);
  yield takeLatest(UserAccessAction.CANCEL_INVITE, cancelInviteSaga);
  yield takeLatest(UserAccessAction.ARCHIVE_USER, archiveUserSaga);
  yield takeLatest(UserAccessAction.UNARCHIVE_USER, unArchiveUserSaga);
  yield takeLatest(UserAccessAction.UNLOCK_USER, unLockUserSaga);
  yield takeLatest(UserAccessAction.ADD_USER, addUserSaga);
  yield takeLatest(UserAccessAction.VALIDATE_CREDENTIALS, validateCredentialsSaga);
  yield takeLatest(UserAccessAction.VALIDATE_ARCHIVE_USER, validateArchiveUserSaga);
}
