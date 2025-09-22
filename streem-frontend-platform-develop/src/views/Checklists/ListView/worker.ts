import { logsParser, logsResourceChoicesMapper } from './../../../utils/parameterUtils';

onmessage = function (e) {
  const data = e.data;
  const resourceParameterChoicesMap = logsResourceChoicesMapper(data);
  const result = data.reduce(
    (acc, jobLog, index) => {
      jobLog.logs.forEach((log: any) => {
        acc[index + 1] = {
          ...acc[index + 1],
          [log.entityId + log.triggerType]: logsParser(
            { ...log, jobId: jobLog.id },
            jobLog.id,
            resourceParameterChoicesMap,
          ),
        };
      });
      return acc;
    },
    [{}],
  );
  postMessage(result);
};
