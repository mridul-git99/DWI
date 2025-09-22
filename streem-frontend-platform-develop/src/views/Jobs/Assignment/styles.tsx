import styled from 'styled-components';

export const Wrapper = styled.div`
  .modal {
    max-height: 80dvh;
    min-width: 500px !important;

    &-body {
      height: calc(80dvh - 120px);
      padding: 0 !important;

      .list-controller {
        align-items: center;
        display: flex;
        justify-content: space-between;
        padding: 24px;

        .deselect {
          color: #1d84ff;
          cursor: pointer;
          font-size: 14px;
          margin-left: 24px;
        }
      }

      .users-list {
        height: calc(100% - 90px);
        overflow-y: scroll;
        padding: 0 24px 24px;

        .list-item {
          align-items: center;
          border-bottom: 1px solid #eeeeee;
          display: flex;
          padding: 8px 0;

          .checkbox-input {
            margin-top: -19px;

            label.container {
              color: #333333;
              font-weight: bold;
            }
          }

          .avatar {
            margin-right: 16px;
          }

          .user {
            &-detail {
              display: flex;
              flex-direction: column;
            }

            &-id {
              color: #999999;
              font-size: 12px;
              line-height: 1.33;
              letter-spacing: 0.32px;
              text-align: left;
            }

            &-name {
              color: #333333;
              font-size: 20px;
              font-weight: 600;
              line-height: 1.2;
              text-align: left;
            }
          }
        }
      }
    }

    &-footer {
      border-top-color: #eeeeee !important;
      flex-direction: row-reverse;
      justify-content: space-between;
    }
  }
`;
