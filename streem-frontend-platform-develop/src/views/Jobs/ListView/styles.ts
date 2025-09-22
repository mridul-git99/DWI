import styled from 'styled-components';

const ViewWrapper = styled.div`
  display: grid;
  grid-row-gap: 16px;
  grid-template-areas: 'header' 'list-table';
  grid-template-rows: 50px minmax(0, 1fr);
  padding-inline: 8px;
  overflow: hidden;
  flex: 1;

  .list-table {
    display: grid;
    grid-area: list-table;
    grid-template-areas: 'tab-header' 'tab-body';
    grid-template-rows: 48px minmax(0, 1fr);
    overflow: hidden;
    flex: 1;
  }
`;

const TabContentWrapper = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  height: 100%;

  #more-actions {
    display: flex;
    align-items: center;
    flex: 1;
    color: #1d84ff;
    cursor: pointer;

    > .icon {
      color: #1d84ff;
    }

    &-disabled {
      display: flex;
      align-items: center;
      flex: 1;
      color: #bbbbbb;
      cursor: not-allowed;

      > .icon {
        color: #bbbbbb;
      }
    }
  }

  #archive-unarchive {
    flex: 1;
    display: flex;
    align-items: center;
    cursor: pointer;

    :hover {
      color: #1d84ff;
    }
  }

  .before-table-wrapper {
    display: grid;
    padding: 0 0 16px;
    justify-content: space-between;
    grid-template-areas: 'filters actions';
    grid-gap: 8px;

    .dropdown-filter {
      color: #1d84ff;

      &.disabled {
        color: #d3d3d3;
      }
    }

    .filters {
      align-items: center;
      display: flex;
      column-gap: 8px;
      row-gap: 12px;
      flex-wrap: wrap;
      flex: 1;
      grid-area: filters;
      font-size: 14px;

      .custom-select__control {
        .custom-select__value-container {
          max-width: 200px;
        }
        .custom-select__value-container--is-multi {
          max-width: unset;
        }
        .custom-select__placeholder {
          font-size: 14px;
        }
      }
    }

    .actions {
      display: flex;
      align-items: flex-start;
      justify-content: flex-end;
      column-gap: 8px;
      row-gap: 12px;
      grid-area: actions;
      flex-wrap: wrap;
      @media (max-width: 769px) {
        width: min-content;
      }
    }

    button {
      text-wrap: nowrap;
      margin: 0;
    }

    .icon-filter {
      cursor: pointer;
      color: #1d84ff;
      margin: 4px;
      display: flex;
      gap: 4px;
      align-items: center;
      .icon {
        height: 18px;
      }
    }

    .custom-select__indicators,
    .dropdown-button {
      .MuiSvgIcon-root {
        width: 20px;
        height: 24px;
        margin: 6px 4px;
      }
    }

    .upload-image {
      .MuiSvgIcon-root {
        margin: 6px 0 4px;
        height: 20px;
      }
    }

    .input-wrapper {
      .MuiSvgIcon-root {
        margin: 0px;
      }
    }
  }
`;

export { TabContentWrapper, ViewWrapper };
