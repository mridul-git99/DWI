import styled, { css } from 'styled-components';

export const Wrapper = styled.div`
  display: flex;
  flex: 1;

  .main-layout-view {
    grid-template-areas:
      'header header'
      'workarea workarea';
  }

  .home-view {
    grid-area: workarea;
    flex-direction: column;
    background-color: #f4f4f4;
    border-top: 1.25px solid rgb(184, 184, 184);
    padding: 16px;
    overflow-y: auto;

    @media (min-width: 1200px) {
      padding: 40px 160px;
    }

    @media (min-width: 1536px) {
      padding: 40px 200px;
    }

    .greeting-text {
      font-size: 20px;
      color: #161616;
      font-weight: 600;
      margin-bottom: 16px;

      @media (min-width: 1200px) {
        font-size: 32px;
        margin-bottom: 24px;
      }
    }

    .use-case-list-wrapper {
      display: grid;
      row-gap: 16px;
      column-gap: 16px;
      grid-template-columns: 1fr 1fr 1fr;

      @media (min-width: 1200px) {
        row-gap: 24px;
        column-gap: 24px;
        grid-template-columns: 1fr 1fr 1fr 1fr;
      }
    }
  }
`;

export const UseCaseCard = styled.div<{
  cardColor: string;
  cardEnabled: boolean;
}>`
  height: 240px;
  background: #ffffff;
  display: grid;
  grid-template-rows: 1fr auto;
  border-top: 4px solid ${(props) => props.cardColor};
  position: relative;

  @media (min-width: 1200px) {
    height: 280px;
  }

  .use-case-lock-icon {
    position: absolute;
    top: -15px;
    left: -15px;

    @media (min-width: 1200px) {
      top: -21px;
      left: -21px;
      width: 42px;
      height: 42px;
    }
  }

  .use-case-card-body {
    padding: 16px;

    .use-case-label {
      font-size: 20px;
      color: #161616;
      margin-bottom: 8px;

      @media (min-width: 1200px) {
        font-size: 28px;
      }

      @media (min-width: 1536px) {
        font-size: 32px;
      }
    }

    .use-case-desc {
      max-height: 115px;
      color: #6f6f6f;
      font-size: 14px;
      overflow: hidden;
      text-overflow: ellipsis;

      @media (min-width: 1200px) {
        max-height: 140px;
      }
    }
  }

  .use-case-card-footer {
    display: grid;
    justify-content: space-between;
    align-items: center;
    grid-auto-flow: column;
    color: #161616;
    padding: 20px 14px;
    background: ${(props) => props.cardColor};
    ${(props) =>
      props.cardEnabled
        ? css`
            cursor: pointer;
          `
        : undefined}

    .use-case-card-footer-text {
      font-size: 14px;
    }

    .use-case-card-footer-icon {
      font-size: 16px;
    }
  }
`;
