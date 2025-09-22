import styled from 'styled-components';

export const Wrapper = styled.div`
  .parameter-header {
    color: #393939;
    font-size: 14px;
    letter-spacing: 0.32px;
    line-height: 1.33;
  }

  .editor-class {
    overflow-wrap: break-word;
    background-color: #fff;
    border: 1px solid #bababa;
    padding: 0 16px;
    pointer-events: none;

    * {
      font-weight: unset;
    }

    .public-* {
      margin: 0;
    }
  }

  .toolbar-class {
    display: none;
  }
`;
