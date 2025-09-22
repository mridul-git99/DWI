import { Button } from '#components';
import styled from 'styled-components';

const HeaderWrapper = styled.div`
  display: flex;
  flex-direction: column;

  .before-header {
    display: flex;
    align-items: center;
    justify-content: center;

    .alert {
      padding: 4px 80px;
      background-color: #e7f1fd;
      border: solid 1px #d6e9ff;
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: center;

      span {
        color: #1d84ff;
        font-size: 14px;
      }

      svg {
        color: #1d84ff;
        font-size: 16px;
        line-height: 14px;
        margin-right: 10px;
      }
    }

    .success {
      border: solid 1px #b2ef6c;
      background-color: #e1fec0;

      span,
      svg {
        color: #427a00;
      }
    }

    .secondary {
      border: solid 1px #f7b500;
      background-color: rgba(247, 181, 0, 0.16);

      span,
      svg {
        color: #000;
      }
    }
  }

  .main-header {
    background-color: #ffffff;
    display: flex;

    .checklist-errors {
      padding: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      background-color: #fff1f1;
      color: #da1e28;
      border: 1px solid #da1e28;
      cursor: pointer;
    }

    .error-popover {
      margin: 0px 16px;
    }

    @media (max-width: 900px) {
      height: 64px;
    }

    .checklist-nav-header {
      padding: 16px 16px 0px 16px;

      .checklist-nav-link {
        color: #1d84ff;
        font-size: 14px;
        cursor: pointer;
      }
    }

    .header-content {
      display: flex;
      flex: 1;
      padding: 12px 16px;
      align-items: center;
      gap: 8px;

      &-left {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .checklist {
          &-name {
            font-size: 20px;
            color: #000000;
            font-weight: 600;
            overflow-wrap: anywhere;
          }

          &-state {
            align-items: center;
            display: flex;
            text-transform: capitalize;

            .icon {
              color: #1d84ff;
              font-size: 15px;
            }

            span {
              font-size: 14px;
              margin-left: 8px;
              line-height: 24px;
            }
          }
        }
      }

      &-right {
        align-items: center;
        display: flex;
        margin-left: auto;
        position: relative;

        button {
          margin-right: 8px;
          min-width: max-content;
        }

        #more {
          padding: 10px 4px;
          border: none;
        }

        #edit,
        #view-collaborators {
          padding-inline: 10px;
          @media (min-width: 900px) {
            padding-block: 6px;
          }
          @media (min-width: 1200px) {
            padding-block: 10px;
          }
        }
      }
    }
  }
`;

const ArrowButtonContainer = styled.div`
  position: relative;
`;

const ArrowButtonStyled = styled(Button)`
  padding: 8px 16px;
  right: 0px;
  margin: 0px;
  width: 48px;
`;
interface PublishDropdownProps {
  show?: boolean;
}

const PublishDropdown = styled.div<PublishDropdownProps>`
  position: relative;
  right: 50px;
  width: 200px;
  z-index: 1;
  position: absolute;
  top: 50px;
  left: 50px;
  display: ${({ show }) => (show ? 'block' : 'none')};

  button {
    width: calc(100% + 28px);
  }
`;

export { HeaderWrapper, ArrowButtonContainer, ArrowButtonStyled, PublishDropdown };
