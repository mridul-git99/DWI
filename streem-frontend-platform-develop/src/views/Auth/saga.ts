import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import {
  closeAllOverlayAction,
  closeOverlayAction,
  openOverlayAction,
} from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { RootState } from '#store';
import { setInitialFacilityWiseConstants } from '#store/facilityWiseConstants/actions';
import { resetPropertiesState } from '#store/properties/actions';
import { fetchSelectedUserSuccess } from '#store/users/actions';
import { User } from '#store/users/types';
import {
  apiAccountLookUp,
  apiAdditionalVerification,
  apiChallengeQuestions,
  apiCheckTokenExpiry,
  apiGetUseCaseList,
  apiGetUser,
  apiLogin,
  apiLogOut,
  apiNotifyAdmin,
  apiRegister,
  apiReLogin,
  apiResetPassword,
  apiResetByEmail,
  apiValidateChallengeQuestion,
  apiValidateIdentity,
} from '#utils/apiUrls';
import { removeAuthHeader, setAuthHeader } from '#utils/axiosClient';
import { LoginErrorCodes } from '#utils/constants';
import { ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { encrypt } from '#utils/stringUtils';
import { ValidateCredentialsPurpose } from '#views/UserAccess/types';
import { navigate } from '@reach/router';
import { call, put, select, takeLeading } from 'redux-saga/effects';
import {
  accountLookUp,
  accountLookUpSuccess,
  additionalVerification,
  authError,
  checkTokenExpiry,
  cleanUp,
  fetchProfile,
  fetchProfileSuccess,
  fetchUseCaseListError,
  fetchUseCaseListOngoing,
  fetchUseCaseListSuccess,
  login,
  loginSuccess,
  logout,
  logoutSuccess,
  notifyAdmin,
  register,
  reLogin,
  resetPassword,
  resetByMail,
  setChallengeQuestion,
  setChallengeQuestionSuccess,
  setIdentityToken,
  updateUserProfile,
  validateIdentity,
  validateQuestion,
} from './actions';
import { AuthAction, LoginResponse, TokenTypes, UseCaseType } from './types';
import { compressState, decompressUrl } from '#utils/decompressUrl';

const getUserId = (state: RootState) => state.auth.userId;
const getIsLoggedIn = (state: RootState) => state.auth.isLoggedIn;
const getAccessToken = (state: RootState) => state.auth.accessToken;

function* loginSaga({ payload }: ReturnType<typeof login>) {
  try {
    const { username, password, code, state, pathname } = payload;
    const isLoggedIn = (yield select(getIsLoggedIn)) as boolean;
    const { data, errors }: ResponseObj<LoginResponse> = yield call(request, 'POST', apiLogin(), {
      data: {
        username,
        password: password ? encrypt(password!) : null,
        code,
        state,
      },
    });

    if (errors) {
      if (isLoggedIn) {
        if (errors?.[0]?.code !== LoginErrorCodes.INVALID_CREDENTIALS) {
          yield put(closeAllOverlayAction());
          yield put(cleanUp());
        }
        yield put(
          showNotification({
            type: NotificationType.ERROR,
            msg: errors[0]?.message || 'Oops! Please Try Again.',
          }),
        );
      } else {
        if (errors?.[0]?.code === LoginErrorCodes.PASSWORD_EXPIRED) {
          yield put(authError(undefined));
          navigate('/auth/password-expired');
        } else if (errors?.[0]?.code === LoginErrorCodes.SSO_INVALID_CREDENTIALS) {
          navigate('/auth/login');
          yield handleCatch('Auth SSO', 'loginSaga', getErrorMsg(errors), true);
        } else {
          throw getErrorMsg(errors);
        }
      }
    } else {
      setAuthHeader(data.accessToken);
      yield put(loginSuccess(data));
      yield put(fetchProfile({ id: data.id }));
      yield put(setInitialFacilityWiseConstants(data.facilities));
      if (pathname) {
        setTimeout(() => {
          navigate(pathname);
        }, 0);
      }
    }
  } catch (error) {
    error = yield* handleCatch('Auth', 'loginSaga', error);
    yield put(authError(error));
  }
}

function* reLoginSaga({ payload }: ReturnType<typeof reLogin>) {
  try {
    const { username, password, code, state, pathname } = payload;
    const accessToken = (yield select(getAccessToken)) as string;
    const { data, errors }: ResponseObj<LoginResponse> = yield call(request, 'POST', apiReLogin(), {
      data: {
        username,
        accessToken,
        password: password ? encrypt(password) : null,
        code,
        state,
      },
    });

    if (errors) {
      if (errors?.[0]?.code === LoginErrorCodes.SSO_INVALID_CREDENTIALS) {
        if (pathname) {
          navigate(pathname);
        }
      }
      if (
        ![LoginErrorCodes.INVALID_CREDENTIALS, LoginErrorCodes.SSO_INVALID_CREDENTIALS]?.includes(
          errors?.[0]?.code,
        )
      ) {
        yield put(closeAllOverlayAction());
        yield put(cleanUp());
      }
      yield put(
        showNotification({
          type: NotificationType.ERROR,
          msg: errors[0]?.message || 'Oops! Please Try Again.',
        }),
      );
    } else {
      setAuthHeader(data.accessToken);
      yield put(loginSuccess(data));
      yield put(fetchProfile({ id: data.id }));
      yield put(setInitialFacilityWiseConstants(data.facilities));
      yield put(closeOverlayAction(OverlayNames.SESSION_EXPIRE));
      if (pathname) {
        navigate(pathname);
      }
    }
  } catch (error) {
    error = yield* handleCatch('Auth', 'reLoginSaga', error);
    yield put(authError(error));
  }
}

function* logoutSaga({ payload }: ReturnType<typeof logout>) {
  try {
    const { ssoIdToken = '' } = payload;
    const { data, errors }: ResponseObj<LoginResponse> = yield call(request, 'POST', apiLogOut(), {
      data: { idToken: ssoIdToken },
    });
    // TODO: Remove id token payload from logout api and navigate redirect url need to sort in below navigate

    if (data?.logoutUrl) {
      navigate(data.logoutUrl);
    }

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(logoutSuccess());
  } catch (error) {
    yield* handleCatch('Auth', 'logoutSaga', error);
    yield put(cleanUp());
  }
}

function* cleanUpSaga() {
  try {
    removeAuthHeader();
    yield put(closeAllOverlayAction());
    yield call(window.persistor.purge);
  } catch (error) {
    yield* handleCatch('Auth', 'cleanUpSaga', error);
  }
}

function* logoutSuccessSaga({ payload }: ReturnType<typeof logoutSuccess>) {
  try {
    const userId = (yield select(getUserId)) as string;
    if (userId) {
      yield put(cleanUp());
      yield put(
        showNotification({
          type: payload?.type || NotificationType.SUCCESS,
          msg: payload?.msg || 'Logged Out successfully',
          delayTime: payload?.delayTime,
        }),
      );
    }
  } catch (error) {
    yield* handleCatch('Auth', 'logoutSuccessSaga', error);
  }
}

function* fetchProfileSaga({ payload }: ReturnType<typeof fetchProfile>) {
  try {
    const { id } = payload;
    const { data, errors }: ResponseObj<User> = yield call(request, 'GET', apiGetUser(id));

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(fetchProfileSuccess(data));
  } catch (error) {
    yield* handleCatch('Auth', 'fetchProfileSaga', error);
  }
}

function* registerSaga({ payload }: ReturnType<typeof register>) {
  try {
    const { data, errors }: ResponseObj<LoginResponse & { token: string }> = yield call(
      request,
      'PATCH',
      apiRegister(),
      {
        data: payload,
      },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    setAuthHeader(data.accessToken);
    yield put(loginSuccess(data));
    yield put(fetchProfile({ id: data.id }));
  } catch (error) {
    error = yield* handleCatch('Auth', 'registerSaga', error);
    yield put(authError(error));
  }
}

function* resetPasswordSaga({ payload }: ReturnType<typeof resetPassword>) {
  try {
    const { errors }: ResponseObj<User> = yield call(request, 'PATCH', apiResetPassword(), {
      data: payload,
    });

    if (errors) {
      throw getErrorMsg(errors);
    }
    yield put(authError(undefined));
    navigate('/auth/forgot-password/updated');
  } catch (error) {
    error = yield* handleCatch('Auth', 'resetPasswordSaga', error);
    yield put(authError(error));
  }
}

function* updateUserProfileSaga({ payload }: ReturnType<typeof updateUserProfile>) {
  try {
    const { body, id } = payload;
    const userId = (yield select(getUserId)) as string;
    const { data, errors }: ResponseObj<User> = yield call(request, 'PATCH', apiGetUser(id), {
      data: body,
    });

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(fetchSelectedUserSuccess({ data }));
    if (id === userId) yield put(fetchProfileSuccess(data));

    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'User updated successfully',
      }),
    );
  } catch (error) {
    yield* handleCatch('Auth', 'updateUserProfileSaga', error, true);
  }
}

function* checkTokenExpirySaga({ payload }: ReturnType<typeof checkTokenExpiry>) {
  try {
    const { data, errors, token } = yield call(request, 'PATCH', apiCheckTokenExpiry(), {
      data: payload,
    });

    if (errors) {
      if (errors?.[0].code === LoginErrorCodes.FORGOT_PASSWORD_TOKEN_EXPIRED) {
        yield put(setIdentityToken({ token }));
        navigate('/auth/forgot-password/key-expired');
      } else if (errors?.[0].code === LoginErrorCodes.REGISTRATION_TOKEN_EXPIRED) {
        yield put(setIdentityToken({ token }));
        navigate('/auth/register/invite-expired');
      } else {
        throw getErrorMsg(errors);
      }
    } else {
      yield put(setIdentityToken(data));

      if (payload.type === TokenTypes.REGISTRATION) {
        navigate('/auth/register/employee-id');
      } else {
        navigate('/auth/forgot-password/new-password');
      }
    }
  } catch (error) {
    error = yield* handleCatch('Auth', 'checkTokenExpirySaga', error);
    yield put(authError(error));
  }
}

function* additionalVerificationSaga({ payload }: ReturnType<typeof additionalVerification>) {
  try {
    const { data, errors, token } = yield call(request, 'PATCH', apiAdditionalVerification(), {
      data: payload,
    });

    if (errors) {
      if (errors?.[0].code === LoginErrorCodes.USER_INVITE_EXPIRED) {
        yield put(setIdentityToken({ token }));
        navigate('/auth/register/invite-expired');
      } else {
        throw getErrorMsg(errors);
      }
    } else {
      yield put(setIdentityToken(data));
      navigate('/auth/register/credentials');
    }
  } catch (error) {
    error = yield* handleCatch('Auth', 'additionalVerificationSaga', error);
    yield put(authError(error));
  }
}

function* setChallengeQuestionSaga({ payload }: ReturnType<typeof setChallengeQuestion>) {
  try {
    const userId = (yield select(getUserId)) as string;
    const { errors } = yield call(request, 'PATCH', apiChallengeQuestions(userId), {
      data: payload,
    });

    if (errors) {
      if (errors[0].code === LoginErrorCodes.JWT_TOKEN_EXPIRED) {
        yield put(
          openOverlayAction({
            type: OverlayNames.VALIDATE_CREDENTIALS_MODAL,
            props: {
              purpose: ValidateCredentialsPurpose.CHALLENGE_QUESTION_UPDATE,
              onSuccess: (token: string) => {
                window.store.dispatch(setIdentityToken({ token }));
              },
            },
          }),
        );
      } else {
        throw getErrorMsg(errors);
      }
    } else {
      yield put(setChallengeQuestionSuccess());
    }
  } catch (error) {
    error = yield* handleCatch('Auth', 'setChallengeQuestionSaga', error);
    yield put(authError(error));
  }
}

function* validateIdentitySaga({ payload }: ReturnType<typeof validateIdentity>) {
  try {
    const { data, errors } = yield call(request, 'PATCH', apiValidateIdentity(), {
      data: payload,
    });

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(setIdentityToken(data));
    navigate('/auth/forgot-password/recovery');
  } catch (error) {
    error = yield* handleCatch('Auth', 'validateIdentitySaga', error);
    yield put(authError(error));
  }
}

function* validateQuestionSaga({ payload }: ReturnType<typeof validateQuestion>) {
  try {
    const { data, errors } = yield call(request, 'PATCH', apiValidateChallengeQuestion(), {
      data: payload,
    });

    if (errors) {
      if (errors?.[0].code === LoginErrorCodes.USER_ACCOUNT_LOCKED) {
        navigate('/auth/account-locked');
      } else {
        throw getErrorMsg(errors);
      }
    } else {
      yield put(setIdentityToken(data));
      navigate('/auth/forgot-password/new-password');
    }
  } catch (error) {
    error = yield* handleCatch('Auth', 'validateQuestionSaga', error);
    yield put(authError(error));
  }
}

function* resetByEmailSaga({ payload }: ReturnType<typeof resetByMail>) {
  try {
    const { errors } = yield call(request, 'PATCH', apiResetByEmail(), {
      data: payload,
    });

    if (errors) {
      throw getErrorMsg(errors);
    }
    yield put(authError(undefined));
    navigate('/auth/forgot-password/email-sent');
  } catch (error) {
    error = yield* handleCatch('Auth', 'resetByEmailSaga', error);
    yield put(authError(error));
  }
}

function* notifyAdminSaga({ payload }: ReturnType<typeof notifyAdmin>) {
  try {
    const { errors } = yield call(request, 'PATCH', apiNotifyAdmin(), {
      data: payload,
    });

    if (errors) {
      throw getErrorMsg(errors);
    }
    yield put(authError(undefined));
    navigate('/auth/notified');
  } catch (error) {
    error = yield* handleCatch('Auth', 'notifyAdminSaga', error);
    yield put(authError(error));
  }
}

function* fetchUseCaseListSaga() {
  try {
    yield put(fetchUseCaseListOngoing());
    const response: UseCaseType[] = yield call(request, 'GET', apiGetUseCaseList());
    if (Array.isArray(response)) {
      yield put(fetchUseCaseListSuccess(response));
      yield put(resetPropertiesState());
    } else {
      throw getErrorMsg(response?.errors);
    }
  } catch (error) {
    console.error('error from fetchUseCaseListSaga function in AuthSaga :: ', error);
    yield put(fetchUseCaseListError(error));
  }
}

function* accountLookUpSaga({ payload }: ReturnType<typeof accountLookUp>) {
  try {
    const { username, query } = payload;
    const { data, errors }: ResponseObj<{ type: string; username: string; redirectUrl: string }> =
      yield call(request, 'GET', apiAccountLookUp(), {
        params: { username },
      });
    if (errors) {
      throw getErrorMsg(errors);
    } else {
      yield put(accountLookUpSuccess(data));
      if (data.type === 'LOCAL') {
        if (query?.includes('?link=')) {
          navigate(`/auth/login-password${query}`);
        } else {
          navigate(`/auth/login-password`);
        }
      } else {
        const redirectUrl = new URL(data.redirectUrl);
        if (query) {
          const existingState = redirectUrl.searchParams.get('state');
          const formattedState = existingState.replace(/^["']|["']$/g, '').replace(/ /g, '+');
          decompressUrl(formattedState)
            .then((decompressedState) => {
              const parsedState = JSON.parse(decompressedState);
              parsedState.location = query;

              compressState(parsedState)
                .then((compressedUpdatedState) => {
                  redirectUrl.searchParams.set('state', compressedUpdatedState);
                  navigate(redirectUrl.href);
                })
                .catch((error) => {
                  console.error('Error recompressing the state:', error);
                });
            })
            .catch((error) => {
              console.error('Error decompressing the state:', error);
            });
        } else {
          navigate(redirectUrl.href);
        }
      }
    }
  } catch (error) {
    error = yield* handleCatch('Auth', 'accountLookUpSaga', error);
    yield put(authError(error));
  }
}

export function* AuthSaga() {
  yield takeLeading(AuthAction.LOGIN, loginSaga);
  yield takeLeading(AuthAction.RE_LOGIN, reLoginSaga);
  yield takeLeading(AuthAction.LOGOUT, logoutSaga);
  yield takeLeading(AuthAction.LOGOUT_SUCCESS, logoutSuccessSaga);
  yield takeLeading(AuthAction.CLEANUP, cleanUpSaga);
  yield takeLeading(AuthAction.FETCH_PROFILE, fetchProfileSaga);
  yield takeLeading(AuthAction.REGISTER, registerSaga);
  yield takeLeading(AuthAction.RESET_PASSWORD, resetPasswordSaga);
  yield takeLeading(AuthAction.UPDATE_USER_PROFILE, updateUserProfileSaga);
  yield takeLeading(AuthAction.CHECK_TOKEN_EXPIRY, checkTokenExpirySaga);
  yield takeLeading(AuthAction.ADDITIONAL_VERIFICATION, additionalVerificationSaga);
  yield takeLeading(AuthAction.SET_CHALLENGE_QUESTION, setChallengeQuestionSaga);
  yield takeLeading(AuthAction.VALIDATE_IDENTITY, validateIdentitySaga);
  yield takeLeading(AuthAction.VALIDATE_QUESTION, validateQuestionSaga);
  yield takeLeading(AuthAction.RESET_BY_MAIL, resetByEmailSaga);
  yield takeLeading(AuthAction.NOTIFY_ADMIN, notifyAdminSaga);
  yield takeLeading(AuthAction.FETCH_USE_CASE_LIST, fetchUseCaseListSaga);
  yield takeLeading(AuthAction.ACCOUNT_LOOKUP, accountLookUpSaga);
}
