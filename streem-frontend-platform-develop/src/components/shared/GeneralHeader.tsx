import React from 'react';
import styled from 'styled-components';
import KeyboardArrowLeftOutlinedIcon from '@material-ui/icons/KeyboardArrowLeftOutlined';

const GeneralHeaderWrapper = styled.div`
  display: flex;
  grid-area: header;
  padding-block: 14px;

  .header-title {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  .header-meta {
    display: flex;
    flex-direction: column;
    grid-area: header;
    justify-content: space-between;
    .heading {
      color: #000000;
      font-size: 20px;
      font-weight: bold;
      line-height: normal;
      text-align: left;
    }

    .sub-heading {
      color: #666666;
      font-size: 12px;
      line-height: normal;
      text-align: left;
    }
  }

  .back-button {
    cursor: pointer;
  }

  @media (max-width: 900px) {
    height: 64px;
    .header-meta {
      justify-content: center;
      .sub-heading {
        display: none;
      }
    }
  }
`;

export const GeneralHeader = ({
  heading,
  subHeading,
  showBackButton,
  onBackButtonClick,
}: {
  heading?: string;
  subHeading?: string;
  showBackButton?: boolean;
  onBackButtonClick?: () => void;
}) => {
  return (
    <GeneralHeaderWrapper>
      <div className="header-title">
        {showBackButton && (
          <KeyboardArrowLeftOutlinedIcon className="back-button" onClick={onBackButtonClick} />
        )}
        <div className="header-meta">
          {heading && <div className="heading">{heading}</div>}
          {subHeading && <div className="sub-heading">{subHeading}</div>}
        </div>
      </div>
    </GeneralHeaderWrapper>
  );
};
