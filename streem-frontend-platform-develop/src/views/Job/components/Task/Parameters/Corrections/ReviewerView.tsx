import { AssigneeList, FormGroup } from '#components';
import { InputTypes } from '#utils/globalTypes';
import React, { FC } from 'react';
import { CorrectorInfo } from './CorrectorView';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import styled from 'styled-components';

export const ReviewerViewWrapper = styled.div.attrs({
  className: 'reviewer-view-wrapper',
})`
  .read-only-group {
    padding: 8px 0 0 0;
    .read-only {
      margin-bottom: 16px;
      flex-direction: column;
      .content {
        ::before {
          display: none;
        }
        font-size: 12px;
        line-height: 1.33;
        letter-spacing: 0.32px;
        color: #525252;

        :last-child {
          font-size: 14px;
          line-height: 1.14;
          letter-spacing: 0.16px;
          color: #161616;
          padding-top: 4px;
        }
      }
    }
  }

  .assignments {
    margin: 0;
  }
`;

export const ReviewerView: FC<any> = ({ correction, parameter, form, isLoggedInUserReviewer }) => {
  const { register } = form;
  const { reviewer } = correction;
  return (
    <ReviewerViewWrapper>
      <CorrectorInfo correction={correction} parameter={parameter} />
      <ReadOnlyGroup
        className="read-only-group"
        items={[
          {
            label: 'Reviewers',
            value: (
              <AssigneeList
                users={(reviewer || []).map((currReviewer) => ({ ...currReviewer.user }))}
              />
            ),
          },
        ]}
      />
      {isLoggedInUserReviewer && (
        <FormGroup
          key="basic-info-section"
          inputs={[
            {
              type: InputTypes.MULTI_LINE,
              props: {
                placeholder: 'Write here',
                label: 'Reviewer Remark',
                id: 'reviewerReason',
                name: 'reviewerReason',
                rows: 3,
                maxRows: 8,
                ref: register({ required: true }),
              },
            },
          ]}
        />
      )}
    </ReviewerViewWrapper>
  );
};
