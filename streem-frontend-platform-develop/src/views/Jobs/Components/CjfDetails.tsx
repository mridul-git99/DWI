import { getParameterContent } from '#utils/parameterUtils';
import React, { FC, useEffect, useMemo } from 'react';
import styled from 'styled-components';
import EditCjf from './EditCjf';
import { FilterOperators } from '#utils/globalTypes';
import useRequest from '#hooks/useRequest';
import { getParameterExecutionInfo } from '#utils/apiUrls';
import { Button } from '#components';
import { OverlayNames } from '#components/OverlayContainer/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { useDispatch } from 'react-redux';
import { navigate } from '@reach/router';
import { ParameterExceptionState } from '../ListView/types';
import { Parameter } from '#types';

const CjfDetailsWrapper = styled.div`
  h4 {
    font-size: 14px;
    font-weight: bold;
    line-height: 1.14;
    letter-spacing: 0.16px;
    color: #161616;
    margin-block: 16px;
  }

  .empty-text {
    font-size: 14px;
    line-height: 1.14;
    letter-spacing: 0.16px;
    color: #c2c2c2;
  }

  .cjf-values-container {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-bottom: 16px;

    p {
      margin: 0;
      font-size: 14px;
      line-height: 1.14;
      letter-spacing: 0.16px;
      color: #161616;
    }

    .label {
      font-size: 12px;
      line-height: 1.33;
      letter-spacing: 0.32px;
      color: #525252;
    }
  }
`;

type TCjfDetailsProps = {
  form: any;
  jobInfo: any;
  handleCloseDrawer: () => void;
};

const CjfDetails: FC<TCjfDetailsProps> = ({ jobInfo, form, handleCloseDrawer }) => {
  const dispatch = useDispatch();
  const { watch } = form;

  const isEditing = watch('isEditing');

  if (!jobInfo) {
    return null;
  }

  const { parameterValues, id: jobId } = jobInfo || {};

  const parameterIds = useMemo(
    () => parameterValues?.map((parameter: any) => parameter.id),
    [parameterValues],
  );

  const { data: cjfParameters, fetchData } = useRequest<any>({
    url: getParameterExecutionInfo(jobId!),
    queryParams: {
      filters: {
        op: FilterOperators.AND,
        fields: [
          {
            field: 'id',
            op: FilterOperators.ANY,
            values: parameterIds,
          },
        ],
      },
    },
    fetchOnInit: false,
  });

  useEffect(() => {
    if (!isEditing) {
      fetchData();
    }
  }, [isEditing]);

  return (
    <CjfDetailsWrapper>
      <h4>CJF Parameters</h4>
      {isEditing ? (
        <EditCjf jobInfo={jobInfo} form={form} />
      ) : (
        <>
          {cjfParameters?.length ? (
            <>
              {cjfParameters
                .sort((a: Parameter, b: Parameter) => (a.orderTree > b.orderTree ? 1 : -1))
                .map((parameter: any) => {
                  if (parameter.response[0]?.hidden) {
                    return null;
                  }

                  const cjfParameter = {
                    ...parameter,
                    response: parameter.response[0],
                  };

                  const { exception } = cjfParameter.response;

                  const isParameterExecuted = exception?.length
                    ? exception.every((exception: any) =>
                        [
                          ParameterExceptionState.AUTO_ACCEPTED,
                          ParameterExceptionState.REJECTED,
                          ParameterExceptionState.ACCEPTED,
                        ].includes(exception.status),
                      )
                    : true;

                  return (
                    <div key={parameter.id} className="cjf-values-container">
                      <p className="label">{parameter.label}</p>

                      <p>{getParameterContent(cjfParameter)}</p>

                      {exception?.length && (
                        <Button
                          style={{ width: 'max-content', marginTop: '4px' }}
                          variant="secondary"
                          color="blue"
                          onClick={() => {
                            handleCloseDrawer();
                            if (isParameterExecuted) {
                              dispatch(
                                openOverlayAction({
                                  type: OverlayNames.VIEW_EXCEPTIONS_DETAILS,
                                  props: { parameter: cjfParameter, jobId },
                                }),
                              );
                            } else {
                              navigate(`/inbox/approvals?jobId=${jobId}`);
                            }
                          }}
                        >
                          View exception details
                        </Button>
                      )}
                    </div>
                  );
                })}
            </>
          ) : (
            <div className="empty-text">No CJF parameters</div>
          )}
        </>
      )}
    </CjfDetailsWrapper>
  );
};

export default CjfDetails;
