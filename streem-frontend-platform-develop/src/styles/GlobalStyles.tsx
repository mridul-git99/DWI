import { createGlobalStyle } from 'styled-components';

const GlobalStyles = createGlobalStyle`
  html {
    height: 100dvh;
  }

  body {
    height: 100dvh;
    width: 100dvw;
    display: flex;
    flex: 1;
  }

  * {
    font-family: 'Inter', sans-serif !important;
    font-weight: normal;
    box-sizing: border-box;
  }

  /* width */
  ::-webkit-scrollbar {
    width: 18px;
    height: 18px;
  }

  /* Track */
  ::-webkit-scrollbar-track {
    background: #fafafa;
  }

  /* Handle */
  ::-webkit-scrollbar-thumb {
    background: #E0E0E0;
    border-radius: 10px;
    border:4px solid #fafafa;
  }

  /* Handle on hover */
  ::-webkit-scrollbar-thumb:hover {
    background: #C6C6C6;
  }

  #root {
    display: flex;
    flex: 1;
  }

  .icon {
    color: #999999;
    cursor: pointer;
  }

  .hide {
    display: none !important;
  }

  input:-webkit-autofill,
  input:-webkit-autofill:hover,
  input:-webkit-autofill:focus,
  input:-webkit-autofill:active  {
      -webkit-box-shadow: 0 0 0 30px #f4f4f4 inset !important;
  }

  .new-form-field {
    display: flex;
    flex-direction: column;

    &-label {
      color: #161616;
      font-size: 14px;
      letter-spacing: 0.16px;
      line-height: 1.29;
      margin-bottom: 8px;

      .optional-badge {
        color: #999999;
        font-size: 12px;
        margin-left: 4px;
      }
    }

    &-input[type="text"],
    &-input[type="number"] {
      background-color: #f4f4f4;
      border: 1px solid transparent;
      border-bottom-color: #bababa;
      outline: none;
      padding: 10px 16px;

      :active,
      :focus {
        border-color: #1d84ff;
      }

      &.error {
        border-color: #eb5757;
      }
    }

    &-textarea {
      border: 1px solid #bababa;
      border-radius: 4px;
      color: #000000;
      outline: none;
      resize: none;
      padding: 16px;

      :disabled {
        background-color: #fafafa;
        border-color: transparent;
        color: #999999;
      }

      :active,
      :focus {
        border-color: #1d84ff;
      }

      ::-webkit-input-placeholder {
        text-align: center;
        line-height: 74px;
        color: #a8a8a8;
      }

      :-moz-placeholder {
        text-align: center;
        line-height: 74px;
        color: #a8a8a8;
      }

      ::-moz-placeholder {
        text-align: center;
        line-height: 74px;
        color: #a8a8a8;
      }

      :-ms-input-placeholder {
        text-align: center;
        line-height: 74px;
        color: #a8a8a8;
      }

    }

    .field-error {
      color: #eb5757;
      margin-top: 8px;
    }
  }

  .react-datepicker__input-time-container {
    position: absolute;
    margin: 0;
    top:0;
    left:232px;
  }

  .react-datepicker-popper {
    z-index: 1000 !important;
  }

  .react-datepicker__day--today {
    font-weight: normal !important;
  }

  .react-datepicker__day--highlighted-custom {
    background-color:  #216ba5;
    color: white;
    border-radius: 4.8px;
    font-weight: bold;

    &:hover {
      background-color: #216ba5 !important;
      color: white !important;
    }
  }

  .react-datepicker__time-container .react-datepicker__time .react-datepicker__time-box {
    .react-datepicker__time-list {
      height: 225px !important;

      .highlighted-time {
        background-color: #216ba5;
        color: white;
        font-weight: bold;

        &:hover {
          background-color: #216ba5 !important;
          color: white !important;
          font-weight: bold !important;
        }
      }
    }
  }

  // Handle input heights on osx/ios devices
  input[type="number"],
  input[type="text"],
  input[type="email"],
  input[type="date"],
  input[type="datetime-local"] {
    min-height: 18px;
  }
`;

export default GlobalStyles;
