import styled from 'styled-components';

export const LabelValueRow = styled.div.attrs(
  ({ className = 'label-value-row', id = 'label-value-row' }) => ({
    className,
    id,
  }),
)`
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  max-width: 100%;
  @media (max-width: 900px) {
    display: grid;
    grid-template-columns: auto auto auto;
  }
  .card-item {
    width: 20%;
    display: flex;
    flex-direction: column;
    gap: 2px;
    line-height: 16px;
    margin-block: 8px;
    @media (max-width: 900px) {
      width: 100%;
    }
    .info-item-label {
      font-size: 12px;
      letter-spacing: 0.32px;
      color: #525252;
    }
    .info-item-value {
      font-size: 14px;
      letter-spacing: 0.16px;
      color: #161616;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
  .info-item {
    width: 20%;
    display: flex;
    flex-direction: column;
    gap: 2px;
    line-height: 16px;
    margin-block: 8px;
    padding-inline: 8px;
    @media (max-width: 900px) {
      width: 100%;
      justify-content: space-between;
      padding: 1rem;
      margin-block: 0;
      border-left: 1px solid #e0e0e0;
    }
    .info-item-label {
      font-size: 12px;
      letter-spacing: 0.32px;
      color: #525252;
    }
    .info-item-value {
      font-size: 14px;
      letter-spacing: 0.16px;
      color: #161616;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
  .info-item:nth-child(3n + 1) {
    @media (max-width: 900px) {
      padding: 1rem 0;
      border-left: none;
    }
  }
`;

const JobHeaderWrapper = styled.div<{
  isInfoExpanded: boolean;
}>`
  display: flex;
  border-bottom: 1px solid #e0e0e0;
  flex-direction: column;
  background-color: #fff;

  .verification-banner {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 8px 0px;
    background-color: #ffedd7;
    color: #ff541e;
    font-size: 12px;

    span {
      color: #161616;
      cursor: pointer;
    }
  }

  .main-header {
    display: flex;
    position: relative;

    .job-primary-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      border-bottom: 1px solid #f4f4f4;
      padding: 12px 16px;

      .checklist-name {
        font-size: 20px;
        color: #000000;
        line-height: 24px;
        font-weight: 600;
      }

      .job-state {
        display: flex;
        align-items: center;
      }

      .buttons-container {
        display: flex;
        align-items: center;
        gap: 8px;

        .more {
          padding: 8px;
          cursor: pointer;
          background-color: #f4f4f4;
          color: #161616;
          display: flex;
          align-items: center;
        }

        .open-overview {
          @media (min-width: 900px) {
            display: none;
          }
        }

        .bulk-assign,
        .print-job,
        .sign-off,
        .view-info {
          margin-right: 8px;
        }
      }
    }

    .job-secondary-header {
      display: flex;
      padding: 16px;

      .job-id,
      .checklist-id {
        display: flex;
        align-items: center;
        padding-right: 16px;
        border-right: 1px solid #f4f4f4;
        font-size: 14px;
        color: #000000;
      }

      .checklist-id {
        padding-left: 16px;
      }

      .job-assignees {
        padding: 0px 16px;
        display: flex;

        .avatar {
          background: #f4f4f4;
          color: #1d84ff;
          border: 1px solid #ffffff;
          transition: margin 0.1s ease-in-out;
        }

        .avatar:not(:first-child) {
          margin-left: -8px;
        }

        &:hover {
          .avatar:not(:last-child) {
            margin-right: 10px;
          }
        }
      }

      .job-activities {
        display: flex;
        align-items: center;
        padding: 0px 16px;
        color: #1d84ff;
        font-size: 14px;
        cursor: pointer;
      }
    }
  }

  .expand-job-meta {
    z-index: 3;
    position: absolute;
    width: 32px;
    height: 32px;
    left: calc(50% - 16px);
    border-radius: 50%;
    background: #f4f4f4;
    transition: all 0.15s ease-in;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 1px 10px 0 rgba(0, 0, 0, 0.12), 0 4px 5px 0 rgba(0, 0, 0, 0.14),
      0 2px 4px -1px rgba(0, 0, 0, 0.2);
    cursor: pointer;
    bottom: -16px;
    opacity: ${({ isInfoExpanded }) => (isInfoExpanded ? 1 : 0)};
    pointer-events: ${({ isInfoExpanded }) => (!isInfoExpanded ? 'none' : 'unset')};
    cursor: ${({ isInfoExpanded }) => (!isInfoExpanded ? 'unset' : 'pointer')};

    &.in-active {
      pointer-events: ${({ isInfoExpanded }) => (isInfoExpanded ? 'none' : 'unset')};
      cursor: ${({ isInfoExpanded }) => (isInfoExpanded ? 'unset' : 'pointer')};
      opacity: ${({ isInfoExpanded }) => (isInfoExpanded ? 0 : 1)};
      left: calc(50% - 16px);
      top: calc(100% - 16px);
      bottom: unset;
    }
  }

  .job-info {
    display: grid;
    position: relative;

    .content {
      z-index: 2;
      background-color: #fff;
      box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.1);
      height: ${({ isInfoExpanded }) => (isInfoExpanded ? '30dvh' : '0px')};
      overflow: visible;
      position: absolute;
      transition: all 0.15s ease-in;
      top: 0;
      right: 0%;
      left: 0;

      .meta-content {
        height: ${({ isInfoExpanded }) => (isInfoExpanded ? '30dvh' : '0px')};
        padding: ${({ isInfoExpanded }) => (isInfoExpanded ? '0px 24px 24px' : '0px')};
        overflow: auto;
        h4 {
          font-weight: 700;
          font-size: 14px;
          line-height: 16px;
          letter-spacing: 0.16px;
          color: #161616;
          margin-bottom: 8px;
        }
      }
    }
  }

  @media (max-width: 900px) {
    .job-primary-header {
      .checklist-name {
        font-size: 0.875rem;
        line-height: 1;
        word-break: break-word;
      }

      .job-state {
        font-size: 0.75rem;
      }
    }
  }
`;

export default JobHeaderWrapper;
