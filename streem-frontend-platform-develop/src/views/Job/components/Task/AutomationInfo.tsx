import { LoadingContainer } from '#components';
import { useTypedSelector } from '#store';
import { getAutomationActionTexts } from '#utils/parameterUtils';
import { getParametersInfo } from '#views/Job/utils';
import React, { useCallback, useEffect, useState } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div.attrs({
  className: 'automation-info',
})`
  display: flex;
  flex-direction: column;
  grid-area: task-automation;
  display: flex;
  gap: 16px;

  .automation {
    .automation-left-container {
      display: grid;
      width: 100%;
      grid-template-columns: auto 1fr auto;
      grid-gap: 8px;

      span {
        word-break: break-word;
      }

      .link-objects {
        .link-object-url {
          color: #1d84ff;
          text-decoration: none;
          cursor: pointer;
        }
      }
    }
  }

  .automation-executed-disclaimer {
    color: #ccc;
    text-align: center;
  }
`;

const RenderAutomationInfo = ({ automation, parameterRefData, parameter }: any) => {
  return (
    <div className="automation">
      <span className="automation-text">
        {getAutomationActionTexts({
          automation,
          forNotify: null,
          parameterRefData,
          isExecuted: false,
          parameter,
        })}
      </span>
    </div>
  );
};

const AutomationInfo = () => {
  const [state, setState] = useState({ loading: true, isAutomationPresent: false });
  const task = useTypedSelector((state) => state.job.activeTask);
  const id: string = useTypedSelector((state) => state.job.id) || '';
  const { automations } = task;
  const [allRefParams, setAllRefParams] = useState<any>({});

  const fetchReferenceParameters = useCallback(
    async (referencedParameterIds: string[]) => {
      const data: any = await getParametersInfo(id, referencedParameterIds);
      setAllRefParams(data);
      setState((prevState) => ({ ...prevState, loading: false }));
    },
    [id],
  );

  useEffect(() => {
    if (automations.length === 0) {
      setState({ ...state, loading: false });
      return;
    }
    setState((prevState) => ({ ...prevState, isAutomationPresent: true }));
    const automationParameters = new Set<string>(
      automations.flatMap(({ actionDetails }: any) =>
        [actionDetails.parameterId, actionDetails.referencedParameterId].filter(Boolean),
      ),
    );
    if (automationParameters.size !== 0) {
      fetchReferenceParameters(Array.from(automationParameters));
    } else {
      setAllRefParams({});
      setState((prevState) => ({ ...prevState, loading: false }));
    }
  }, [automations?.length]);

  if (state.loading) {
    return <LoadingContainer loading={true} />;
  }

  if (automations?.length) {
    return (
      <Wrapper>
        {automations.map((automation: any) => {
          const parameterRefData = allRefParams[automation.actionDetails.referencedParameterId];
          const parameter = allRefParams[automation.actionDetails.parameterId];
          return (
            <RenderAutomationInfo
              parameterRefData={parameterRefData}
              automation={automation}
              parameter={parameter}
            />
          );
        })}
        {!state.isAutomationPresent && (
          <div className="automation-executed-disclaimer">No automations executed yet</div>
        )}
      </Wrapper>
    );
  }

  return null;
};

export default AutomationInfo;
