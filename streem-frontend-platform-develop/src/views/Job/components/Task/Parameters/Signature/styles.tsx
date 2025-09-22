import styled from 'styled-components';

export const Wrapper = styled.div.attrs({})`
  /* TODO: make styles better */
  .signature-interaction {
    padding: 16px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    min-height: 240px;
    border: dashed 1px #e0e0e0;
    background-color: #fff;

    > span {
      color: #bdbdbd;
      font-size: 14px;
      text-align: center;
    }

    > .icon-container {
      display: flex;
      flex-direction: column;
      margin-bottom: 13px;
    }
  }

  .signature-interaction.active {
    cursor: pointer;
    border: 1px solid #e0e0e0;
    > span {
      color: #000;
      font-size: 14px;
      text-align: center;
    }

    > .icon-container {
      display: flex;
      flex-direction: column;
      margin-bottom: 13px;
    }
  }
`;
