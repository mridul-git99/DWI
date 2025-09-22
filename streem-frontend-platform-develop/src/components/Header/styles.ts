import styled from 'styled-components';

export const Wrapper = styled.div.attrs({})`
  align-items: center;
  display: flex;
  flex-direction: row;
  grid-area: header;
  justify-content: space-between;
  padding: 0 16px;
  background-color: #161616;
  height: 48px;

  .select {
    margin-left: auto;
    margin-right: 12px;
    min-width: 200px;
  }

  .right-section {
    display: flex;
    gap: 16px;

    .header-item {
      display: flex;
      align-items: center;
      padding-left: 10px;
      cursor: pointer;
    }

    .qr-scanner {
      display: flex;
      align-items: center;
      cursor: pointer;
    }

    .MuiMenu-paper {
      margin-top: 14px;
      background-color: #393939;
    }

    .MuiListItem-root {
      color: #fff !important;

      div {
        margin: 0 !important;
      }
    }

    .custom-select__control {
      background-color: #161616;
      border: none;
    }

    .custom-select__single-value {
      color: #fff;
    }

    .custom-select__indicator-separator {
      display: none;
    }

    .custom-select__option {
      color: #fff;
      background-color: #393939;
      font-size: 14px;
    }
  }

  .left-section {
    display: flex;
    align-items: center;
    gap: 16px;

    .header-logo {
      cursor: pointer;
    }
  }
`;

export const HeaderMenu = styled.div`
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 4px;

  .thumb {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #666666;
    color: #fff;
    font-size: 12px;
  }
`;
