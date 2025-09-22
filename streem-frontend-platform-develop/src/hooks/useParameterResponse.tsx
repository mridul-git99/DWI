import { useTypedSelector } from '#store';
import { PartialParameter } from '#types';
import { apiGetLatestParameterInfo } from '#utils/apiUrls';
import { useEffect, useMemo } from 'react';
import useRequest from './useRequest';

function useParameterResponse(jobId: string, parameterIds: string[]) {
  const parameters = useTypedSelector((state) => state.job.activeTask.parameters);
  const activeTaskId = useTypedSelector((state) => state.job.activeTask.id);

  const { unavailableParametersIds, avilablePartialParameters } = useMemo(() => {
    const ids = new Set();
    const avilablePartialParameters: Record<string, PartialParameter> = {};
    parameterIds.forEach((id) => {
      if (id) {
        if (!parameters.has(id)) {
          ids.add(id);
        } else {
          const parameter = parameters.get(id)!;
          avilablePartialParameters[id] = {
            choices: parameter.response.choices,
            value: parameter.response.value,
            hidden: parameter.response.hidden,
            id: parameter.id,
            label: parameter.label,
            type: parameter.type,
            data: parameter.data,
            taskExecutionId: parameter.response.taskExecutionId,
            taskId: activeTaskId,
          };
        }
      }
    });
    return { unavailableParametersIds: ids, avilablePartialParameters };
  }, [parameterIds.length, parameters.size]);

  const { dataById, fetchData } = useRequest<PartialParameter>({
    url: apiGetLatestParameterInfo(jobId!),
    method: 'PATCH',
    fetchOnInit: false,
  });

  useEffect(() => {
    if (unavailableParametersIds.size) {
      fetchData({
        customBody: {
          parameterIds: Array.from(unavailableParametersIds),
        },
      });
    }
  }, [unavailableParametersIds]);

  return {
    ...avilablePartialParameters,
    ...dataById,
  };
}

export default useParameterResponse;
