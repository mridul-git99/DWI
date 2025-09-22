import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import styled from 'styled-components';

const Notification = styled(ToastContainer)`
  .Toastify__toast-container {
  }
  .Toastify__toast {
    min-height: 48px;
    border-radius: unset;
    box-shadow: 0 1px 10px 0 rgba(0, 0, 0, 0.12), 0 4px 5px 0 rgba(0, 0, 0, 0.14),
      0 2px 4px -1px rgba(0, 0, 0, 0.2);
  }
  .Toastify__toast--default {
    padding: 0;
  }
  .Toastify__toast-body {
    color: #666666;
    font-size: 14px;
    display: flex;
    padding: unset;
    margin: 0;
    .notification-layout {
      align-items: center;
      padding: 4px 0;
      width: 100%;
      > div {
        display: flex;
        flex: 1;
        padding: 16px 32px 16px 16px;
        align-items: center;
        width: 100%;
      }
      .content {
        display: flex;
        flex-direction: column;
        color: #161616;
        line-height: 1.14;
        letter-spacing: 0.16px;
        font-size: 14px;
        font-weight: bold;
        span {
          margin-top: 4px;
          font-size: 12px;
          color: inherit;
        }
        .clickable {
          text-decoration: underline;
          font-weight: 600;
        }
        .detail-btn {
          cursor: pointer;
          color: #1d84ff;
          font-size: 14px;
          font-weight: 400;
          margin-top: 8px;
        }
      }
    }
    .notification--success {
      border-left: 4px solid #5aa700;
      background-color: #defbe6;
    }
    .notification--error {
      border-left: 4px solid #da1e28;
      background-color: #fff1f1;
    }
    .notification--warning {
      border-left: 4px solid #f1c21b;
      background-color: #fff8e1;
    }
    .toast_icon {
      font-size: 18px;
      margin-right: 10px;
      align-self: flex-start;
    }
    .toast_icon--success {
      color: #5aa700;
    }
    .toast_icon--error {
      color: #da1e28;
    }
    .toast_icon--warning {
      color: #f1c21b;
    }
  }
  .Toastify__progress-bar {
  }
  .Toastify__close-button {
    position: absolute;
    right: 16px;
    top: 16px;
    opacity: unset;
    color: #161616;
  }
`;

export default Notification;
