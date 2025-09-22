import { ResponseObj } from '#utils/globalTypes';
import {
  apiGetObjects,
  apiGetPartialObject,
  apiGetResourceOptions,
  apiQrShortCode,
} from '#utils/apiUrls';
import { getErrorMsg, request } from '#utils/request';
import { TObject } from './types';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { DataTableColumn } from '#components/shared/DataTable';

export const getObjectData = async (data: Record<string, string | number | undefined>) => {
  try {
    const { id, ...params } = data;
    const response: ResponseObj<TObject> = await request('GET', apiGetObjects(id as string), {
      params,
    });
    if (response.data) {
      return response.data;
    }
    throw response?.errors;
  } catch (error) {
    console.error('Error in get objects data function :: ', error);
    throw getErrorMsg(error as any);
  }
};

export const getObjectPartialCall = async (data: Record<string, any>) => {
  try {
    const response: ResponseObj<Array<TObject>> = await request('GET', apiGetPartialObject(), {
      params: { page: DEFAULT_PAGE_NUMBER, size: DEFAULT_PAGE_SIZE, ...data },
    });
    if (response?.errors) {
      throw response?.errors;
    }
    return response;
  } catch (error) {
    throw getErrorMsg(error as any);
  }
};

export const qrCodeValidator = async ({
  data,
  callBack,
  objectTypeValidation,
  filters = {},
  parameterResponseId = '',
  shortCode = '',
}: {
  data: Record<string, string>;
  callBack: () => void;
  objectTypeValidation: boolean;
  filters?: any;
  parameterResponseId?: string;
  shortCode?: string;
}) => {
  const invalidObjectMessage =
    'Alert: Invalid Object Detected! The scanned object is not compatible with the selected Resource parameter in this Job. Please scan a valid object.';

  if (objectTypeValidation && data?.entityType === 'OBJECTS') {
    if (parameterResponseId && shortCode) {
      const { data: options } = await request('GET', apiGetResourceOptions(parameterResponseId), {
        params: {
          page: DEFAULT_PAGE_NUMBER,
          size: DEFAULT_PAGE_SIZE,
          sort: 'createdAt,desc',
          shortCode,
        },
      });

      if (options?.length > 0) {
        const match = options.find((option) => option.externalId === data.externalId);
        if (!match) {
          throw invalidObjectMessage;
        }
        callBack();
        return;
      }
    } else {
      const { data: fetchedData } = await getObjectPartialCall({
        collection: data.collection,
        filters: filters || {},
      });
      if (fetchedData?.length > 0) {
        callBack();
        return;
      }
    }
  }
  throw invalidObjectMessage;
};

export const getQrCodeData = async (params: any) => {
  const { data, errors } = await request('GET', apiQrShortCode(), {
    params,
  });
  if (data) {
    return data;
  }
  if (errors) {
    throw getErrorMsg(errors as any);
  }
};

// TODO : Seems like a duplicate and wrongly implemented version of : 'FlagPositions' 'getBooleanFromDecimal'
export enum PropertyFlags {
  SYSTEM = 1,
  EXTERNAL_ID = 27,
  EXTERNAL_ID_AUTO_GENERATE = 59,
  DISPLAY_NAME = 21,
  MANDATORY = 16,
  OPTIONAL = 0,
}

export const objectJobLogColumns: DataTableColumn[] = [
  {
    id: '-1',
    type: 'TEXT',
    displayName: 'Job Id',
    triggerType: 'JOB_ID',
  },
  {
    id: '-1',
    type: 'TEXT',
    displayName: 'Job State',
    triggerType: 'JOB_STATE',
  },
  {
    id: '-1',
    type: 'DATE_TIME',
    displayName: 'Job Started At',
    triggerType: 'JOB_START_TIME',
  },
  {
    id: '-1',
    type: 'TEXT',
    displayName: 'Job Started By',
    triggerType: 'JOB_STARTED_BY',
  },
  {
    id: '-1',
    type: 'DATE_TIME',
    displayName: 'Job Created At',
    triggerType: 'JOB_CREATED_AT',
  },
  {
    id: '-1',
    type: 'TEXT',
    displayName: 'Job Created By',
    triggerType: 'JOB_CREATED_BY',
  },
  {
    id: '-1',
    type: 'TEXT',
    displayName: 'Process Id',
    triggerType: 'CHK_ID',
  },
  {
    id: '-1',
    type: 'TEXT',
    displayName: 'Process Name',
    triggerType: 'CHK_NAME',
  },
  {
    id: '-1',
    type: 'TEXT',
    displayName: 'Annotation Remark',
    triggerType: 'ANNOTATION_REMARK',
  },
  {
    id: '-1',
    type: 'FILE',
    displayName: 'Annotation Media',
    triggerType: 'ANNOTATION_MEDIA',
  },
  {
    id: '-1',
    type: 'DATE_TIME',
    displayName: 'Job Ended At',
    triggerType: 'JOB_END_TIME',
  },
  {
    id: '-1',
    type: 'TEXT',
    displayName: 'Job Ended By',
    triggerType: 'JOB_ENDED_BY',
  },
];

export const findHighestSortOrder = (arr: any) => {
  let highestSortOrder = 0;
  if (arr?.length > 0) {
    arr.forEach((currentObject: any) => {
      if (currentObject.sortOrder > highestSortOrder) {
        highestSortOrder = currentObject.sortOrder;
      }
    });
  }
  return highestSortOrder;
};
