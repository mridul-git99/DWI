import { SvgIconProps } from '@material-ui/core';
import { SvgIconComponent } from '@material-ui/icons';
import { actionSpreader } from './../../store/helpers';
import { NotificationActions, NotificationType } from './types';
import { ToastOptions } from 'react-toastify';

export interface TShowNotificationPayload extends ToastOptions {
  type: NotificationType;
  msg: string | JSX.Element;
  delayTime?: number;
  detail?: string;
  icon?: SvgIconComponent;
  iconProps?: SvgIconProps;
  buttonText?: string;
}

export const showNotification = (payload: TShowNotificationPayload) =>
  actionSpreader(NotificationActions.SHOW_NOTIFICATION, payload);
