import { EffectType, MentionConstantType, MentionNodeType } from '#types/actionsAndEffects';
import { get } from 'lodash';
import { getErrorMsg, request } from './request';
import { apiExecuteEffectSqlQuery } from './apiUrls';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';

export const urlBuilder = (
  apiEndpoint: Record<string, any>,
  resolveDataFunction: (data: any, previousData: Record<string, any>) => string,
  previousData: Record<string, any>,
) => {
  const urlParts: string[] = [];

  const traverse = (node) => {
    if (node.type === MentionNodeType.text) {
      urlParts.push(node.text);
    } else if (node.type === MentionNodeType.custom_beautifulMention) {
      const resolved = resolveDataFunction(node.data, previousData);
      urlParts.push(typeof resolved == 'object' ? JSON.stringify(resolved) : resolved);
    }
    if (node.children) {
      node.children.forEach((child) => traverse(child));
    }
  };

  if (apiEndpoint?.root) {
    traverse(apiEndpoint.root);
  }

  return urlParts.join('');
};

export const getDynamicData = (data: Record<string, any>, previousData: Record<string, any>) => {
  if (
    [
      MentionConstantType.facilityId,
      MentionConstantType.jobId,
      MentionConstantType.useCaseId,
    ].includes(data.id)
  ) {
    return previousData[data.id];
  }
  if (data?.postfix) {
    const dataToUse = data.id ? previousData[data.id] : previousData;
    return get(dataToUse, data.postfix);
  }

  return data.id;
};

export const addStaticData = (previousData: Record<string, any>) => {
  const state = window.store.getState();

  const facilityId = state?.auth?.selectedFacility?.id || null;
  const useCaseId = state?.auth?.selectedUseCase?.id || null;
  const jobId = state?.job?.id || null;
  const tasksActionsAndEffects = state?.job?.tasksActionsAndEffects || {};

  const allTasksData = Object.keys(tasksActionsAndEffects).reduce((acc, taskId) => {
    const taskData = tasksActionsAndEffects[taskId];
    acc[taskId] = {
      start: taskData?.taskStartData || {},
      complete: taskData?.taskCompleteData || {},
    };
    return acc;
  }, {} as Record<string, { start: any; complete: any }>);

  return {
    ...previousData,
    facilityId,
    useCaseId,
    jobId,
    ...allTasksData,
  };
};

export const executeEffects = async ({ effects }: { effects: Array<any> }) => {
  let previousEffectsData: Record<string, any> = {};
  previousEffectsData = addStaticData(previousEffectsData);

  for (const effect of effects) {
    try {
      if (
        effect.effectType === EffectType.SQL_QUERY ||
        effect.effectType === EffectType.MONGO_QUERY
      ) {
        const _payload = urlBuilder(effect.query, getDynamicData, previousEffectsData);
        let payload;
        if (effect?.javascriptEnabled) {
          const newPayload = '(()=>{' + _payload + '})();';
          payload = eval(newPayload);
        } else {
          payload = _payload;
        }

        const response = await request('POST', apiExecuteEffectSqlQuery(), {
          data: {
            effectId: effect.id,
            effectType: effect.effectType,
            query: payload,
          },
          retries: 2,
        });

        previousEffectsData[effect.id] = response;

        if (response?.status === 'OK') {
          triggerNotification({
            type: NotificationType.SUCCESS,
            msg: `Effect ${effect.name} executed successfully`,
          });
        }
      } else {
        const apiUrl = urlBuilder(effect.apiEndpoint, getDynamicData, previousEffectsData);

        const _payload = urlBuilder(effect.apiPayload, getDynamicData, previousEffectsData);
        const _header = urlBuilder(effect.apiHeaders, getDynamicData, previousEffectsData);

        let payload;
        let header = null;
        if (effect?.javascriptEnabled) {
          const newPayload = '(()=>{' + _payload + '})();';
          const newHeader = '(()=>{' + _header + '})();';

          payload = eval(newPayload);
          header = eval(newHeader);
        } else {
          payload = _payload;
        }

        const response = await request(effect.apiMethod, apiUrl, {
          retries: 2,
          ...(payload && { data: payload }),
          ...(header && { headers: header }),
        });

        previousEffectsData[effect.id] = response;

        if (response?.status === 'OK') {
          triggerNotification({
            type: NotificationType.SUCCESS,
            msg: `Effect ${effect.name} executed successfully`,
          });
        }
      }
    } catch (error) {
      triggerNotification({
        type: NotificationType.ERROR,
        msg: getErrorMsg(error),
      });
      console.error('Error while executing effect:', error);
    }
  }

  return previousEffectsData;
};

const triggerNotification = ({
  type,
  msg,
}: {
  type: NotificationType;
  msg: string | JSX.Element;
}) => {
  window.store.dispatch(
    showNotification({
      type,
      msg,
    }),
  );
};
