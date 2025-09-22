import { Block, CheckCircle, Error, SvgIconComponent } from '@material-ui/icons';
import React, { ReactNode } from 'react';
import { toast, ToastOptions } from 'react-toastify';
import { call, delay, put, takeLatest } from 'redux-saga/effects';
import { NotificationActions, NotificationActionType, NotificationType } from './types';
import { v4 as uuidv4 } from 'uuid';
import { addRetainedToastId, removeRetainedToastId } from '#store/extras/action';

function* showNotificationGenerator({ payload }: NotificationActionType) {
  const {
    type,
    msg,
    delayTime,
    detail,
    icon,
    iconProps,
    buttonText,
    onClick,
    autoClose,
    onClose,
    toastId,
    ...rest
  } = payload;
  const isRetained = autoClose === false;

  if (delayTime) yield delay(delayTime);

  const showIcon = (IconComponent: SvgIconComponent, classes = '') => (
    <IconComponent className={`toast_icon ${classes}`} {...iconProps} />
  );

  const Layout = (): ReactNode => (
    <div className={`notification-layout notification--${type}`}>
      <div>
        {type === NotificationType.SUCCESS && showIcon(icon || CheckCircle, 'toast_icon--success')}
        {type === NotificationType.ERROR && showIcon(icon || Block, 'toast_icon--error')}
        {type === NotificationType.WARNING && showIcon(icon || Error, 'toast_icon--warning')}
        <div className="content">
          {msg}
          {detail && <span>{detail}</span>}
          {buttonText && (
            <span className="detail-btn" onClick={onClick}>
              {buttonText}
            </span>
          )}
        </div>
      </div>
    </div>
  );

  const _toastId = toastId || isRetained ? uuidv4() : undefined;

  const handleClose: ToastOptions['onClose'] = (p) => {
    if (isRetained && _toastId && window) {
      window.store.dispatch(removeRetainedToastId(_toastId));
    }

    onClose?.(p);
  };

  if (isRetained && _toastId) {
    yield put(addRetainedToastId(_toastId));
  }

  yield call(toast, Layout, { autoClose, onClose: handleClose, toastId: _toastId, ...rest });
}

export function* showNotificationSaga() {
  yield takeLatest(NotificationActions.SHOW_NOTIFICATION, showNotificationGenerator);
}
