import { MandatoryParameter, NonMandatoryParameter, Parameter, ParameterType } from '#types';
import { v4 as uuidv4 } from 'uuid';
import { Checklist } from '../checklist.types';
import { ParameterOrderInTaskInStage, ParametersById } from './reducer.types';

const getParameters = (checklist: Checklist | Partial<Checklist>) => {
  const listById: ParametersById = {},
    parameterOrderInTaskInStage: ParameterOrderInTaskInStage = {};

  checklist?.stages?.map((stage) => {
    parameterOrderInTaskInStage[stage.id] = {};

    stage?.tasks?.map((task) => {
      parameterOrderInTaskInStage[stage.id][task.id] = [];

      task?.parameters?.map((parameter) => {
        parameterOrderInTaskInStage[stage.id][task.id].push(parameter.id);

        listById[parameter.id] = { ...parameter, errors: [] };
      });
    });
  });

  return { listById, parameterOrderInTaskInStage };
};

const generateNewParameter = ({
  type,
  orderTree,
  mandatory,
  label,
  description,
}: Pick<Parameter, 'label' | 'description' | 'mandatory' | 'orderTree'> & {
  type: ParameterType;
}): Partial<Parameter> | null => {
  switch (type) {
    case MandatoryParameter.CHECKLIST:
      return {
        orderTree,
        type,
        data: [{ id: uuidv4(), name: '' }],
        label,
        mandatory,
        description,
        validations: [],
      };

    case MandatoryParameter.YES_NO:
      return {
        orderTree,
        type,
        data: [
          { id: uuidv4(), name: '', type: 'yes' },
          { id: uuidv4(), name: '', type: 'no' },
        ],
        label,
        mandatory,
        description,
        validations: [],
      };

    case MandatoryParameter.MULTISELECT:
    case MandatoryParameter.SINGLE_SELECT:
      return {
        orderTree,
        type,
        data: [{ id: uuidv4(), name: '' }],
        label,
        mandatory,
        description,
        validations: [],
      };

    case MandatoryParameter.SHOULD_BE:
      return {
        orderTree,
        type,
        data: {
          uom: '',
          type: '',
          value: '',
          operator: '',
          parameter: '',
        },
        label,
        mandatory,
        description,
        validations: [],
      };

    case MandatoryParameter.MEDIA:
    case MandatoryParameter.SIGNATURE:
    case MandatoryParameter.MULTI_LINE:
    case MandatoryParameter.SINGLE_LINE:
    case MandatoryParameter.FILE_UPLOAD:
      return {
        orderTree,
        type,
        data: {},
        label,
        mandatory,
        description,
        validations: [],
      };

    case MandatoryParameter.CALCULATION:
      return {
        orderTree,
        type,
        data: {
          expression: '',
          uom: '',
          variables: {},
          precision: 9,
        },
        label,
        mandatory,
        description,
        validations: [],
      };

    case MandatoryParameter.RESOURCE:
    case MandatoryParameter.MULTI_RESOURCE:
      return {
        orderTree,
        type,
        data: {
          variables: {},
          urlPath: '',
          collection: '',
          objectTypeExternalId: '',
          objectTypeDisplayName: '',
          objectTypeId: '',
        },
        label,
        mandatory,
        description,
        validations: [],
      };

    case MandatoryParameter.DATE:
    case MandatoryParameter.DATE_TIME:
      return {
        orderTree,
        type,
        data: {},
        label,
        mandatory,
        description,
        validations: [],
      };

    case MandatoryParameter.NUMBER:
      return {
        orderTree,
        type,
        data: {},
        label,
        mandatory,
        description,
        validations: [],
      };

    case NonMandatoryParameter.INSTRUCTION:
      return {
        orderTree,
        type,
        data: {},
        label,
        mandatory,
        description,
        validations: [],
      };

    case NonMandatoryParameter.MATERIAL:
      return {
        orderTree,
        type,
        data: [
          {
            link: '',
            name: '',
            type: 'image',
            fileName: '',
            quantity: 0,
            id: uuidv4(),
            mediaId: '',
          },
        ],
        label,
        mandatory,
        description,
        validations: [],
      };

    default:
      return null;
  }
};

const updateHiddenParameterIds = (data: Record<string, Array<string>>[]) => {
  let hiddenIds: Record<string, boolean> = {};
  if (data.length) {
    data.forEach((currData) => {
      currData.hide.forEach((id: string) => {
        hiddenIds[id] = true;
      });
      currData.show.forEach((id: string) => {
        hiddenIds[id] = false;
      });
    });
  } else {
    data?.hide?.forEach((id: string) => {
      hiddenIds[id] = true;
    });
    data?.show?.forEach((id: string) => {
      hiddenIds[id] = false;
    });
  }
  return hiddenIds;
};

export { generateNewParameter, getParameters, updateHiddenParameterIds };
