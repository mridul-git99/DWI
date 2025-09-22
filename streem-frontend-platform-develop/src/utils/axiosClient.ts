import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import NoConnection from '#assets/svg/NoConnection';
import { closeAllOverlayAction } from '#components/OverlayContainer/actions';
import { logoutSuccess, refreshTokenSuccess } from '#views/Auth/actions';
import { RefreshTokenResponse } from '#views/Auth/types';
import axios from 'axios';
import { toast } from 'react-toastify';
import { apiRefreshToken } from './apiUrls';
import {
  ErrorCodesToLogout,
  EXCULDE_BY_REGEX_FOR_NO_INTERNET_TOAST,
  LoginErrorCodes,
} from './constants';
import { ResponseObj } from './globalTypes';
import { isMatchAny } from './stringUtils';
import { setGlobalError } from '#store/extras/action';
import * as Sentry from '@sentry/react';

// REFRESH TOKEN LOGIC
const REFRESH_TOKEN_URL = apiRefreshToken();

let refreshPromise: Promise<ResponseObj<RefreshTokenResponse>> | null = null;
async function refreshTokenRequest(accessToken: string, refreshToken: string) {
  refreshPromise =
    refreshPromise ||
    axiosInstance.request({
      method: 'POST',
      url: apiRefreshToken(),
      data: {
        accessToken,
        refreshToken,
      },
    });

  try {
    if (refreshPromise) {
      const data = await refreshPromise;
      return data;
    }
  } finally {
    refreshPromise = null;
  }
}

// AXIOS SETUP
const axiosInstance = axios.create();

axiosInstance.defaults.headers.post['Content-Type'] = 'application/json';
axiosInstance.defaults.headers.common['Access-Control-Allow-Origin'] = '*';

axiosInstance.interceptors.response.use(
  (resp) => resp.data,

  async (error) => {
    try {
      const { config: originalReq, response } = error;
      const { code, message } = response?.data?.errors?.[0] || {};

      const parseOriginalReqData = () => {
        // axios return data as string, so we need to parse it
        if (typeof originalReq.data === 'string') {
          try {
            originalReq.data = JSON.parse(originalReq.data);
          } catch (e) {
            /* Ignore */
            console.error('can not JSON.parse the response', e);
          }
        }
      };

      if (code !== LoginErrorCodes.JWT_ACCESS_TOKEN_EXPIRED) {
        if (originalReq.retries) {
          originalReq.retries--;
          parseOriginalReqData();
          return await axiosInstance.request(originalReq);
        }
        const {
          auth: { isLoggedIn },
        } = window.store.getState();
        if (isLoggedIn && Object.values(ErrorCodesToLogout).some((val) => val === code)) {
          const msg = typeof message === 'string' ? message : 'Oops! Please Try Again.';
          window.store.dispatch(closeAllOverlayAction());
          window.store.dispatch(
            logoutSuccess({
              msg,
              type: NotificationType.ERROR,
            }),
          );
        } else {
          return response?.data;
        }
      } else if (originalReq.url !== REFRESH_TOKEN_URL && !originalReq?.isRetryAttempt) {
        const {
          auth: { refreshToken, accessToken },
        } = window.store.getState();

        removeAuthHeader();

        const { data } = (await refreshTokenRequest(
          accessToken,
          refreshToken,
        )) as ResponseObj<RefreshTokenResponse>;
        window.store.dispatch(refreshTokenSuccess(data));

        originalReq.isRetryAttempt = true;
        setAuthHeader(data.accessToken);
        originalReq.headers['Authorization'] = `Bearer ${data.accessToken}`;
        parseOriginalReqData();
        const originalReqResponse = await axiosInstance.request(originalReq);
        return originalReqResponse;
      } else {
        return response.data;
      }
    } catch (e) {
      toast.dismiss();

      const {
        extras: { connected },
      } = window.store.getState();
      const { config: originalReq } = error;

      if (!connected) {
        if (!isMatchAny(originalReq.url, EXCULDE_BY_REGEX_FOR_NO_INTERNET_TOAST)) {
          window.store.dispatch(
            showNotification({
              type: NotificationType.ERROR,
              msg: 'No Internet Connection.',
              detail: 'Please check your internet and try again.',
              delayTime: 10,
              icon: NoConnection,
              iconProps: {
                height: '69px',
                width: '101px',
              },
            }),
          );
        }
      } else {
        window.store.dispatch(closeAllOverlayAction());
        Sentry.captureException(e);
        window.store.dispatch(setGlobalError(true));
      }
      throw e;
    }
  },
);

export const removeAuthHeader = () => delete axiosInstance.defaults.headers.common['Authorization'];

export const setAuthHeader = (token: string) =>
  (axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${token}`);

export default axiosInstance;
