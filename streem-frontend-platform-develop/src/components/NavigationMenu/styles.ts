import styled from 'styled-components';

export const Wrapper = styled.div`
  flex: 1;
  grid-area: nav-menu;
  justify-content: space-between;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;

  .nested-menu {
    display: flex;
    flex-direction: column;
    color: #161616;
  }

  .nested-menu-item {
    padding: 16px 16px 16px 48px;
    font-size: 14px;
    cursor: pointer;
  }

  .active {
    background-color: #e7f1fd;
    color: #1d84ff;
  }
`;

export const Menu = styled.nav`
  display: flex;
  flex-direction: column;
`;

export const NavItem = styled.div`
  align-items: center;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  padding: 12px;
  cursor: pointer;

  > span {
    font-size: 14px;
    line-height: 1.29;
  }

  .secondary-icon {
    margin-left: auto;
    font-size: 18px;
  }
`;
