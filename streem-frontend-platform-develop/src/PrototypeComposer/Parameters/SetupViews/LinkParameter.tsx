import InfoIcon from '#assets/svg/info.svg';
import { Button, FormGroup } from '#components';
import Tooltip from '#components/shared/Tooltip';
import { useTypedSelector } from '#store';
import {
  MandatoryParameter,
  ParameterType,
  ParameterVerificationTypeEnum,
  TargetEntityType,
} from '#types';
import { apiGetObjectTypes, apiGetParameters, apiSingleParameter } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER } from '#utils/constants';
import { FilterOperators, InputTypes, ResponseObj } from '#utils/globalTypes';
import { request } from '#utils/request';
import { ObjectType } from '#views/Ontology/types';
import { LinkOffOutlined, LinkOutlined } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useEffect, useRef, useState } from 'react';
import { useFormContext } from 'react-hook-form';
import { CommonWrapper } from './styles';

const LinkParameter: FC<{
  isReadOnly: boolean;
  type: ParameterType;
  loadOptions?: boolean;
}> = ({ isReadOnly, loadOptions = false }) => {
  const {
    prototypeComposer: {
      data: processData,
      parameters: { addParameter },
    },
  } = useTypedSelector((state) => state);

  const { id: checklistId } = processData!;
  const { watch, setValue, register, getValues, trigger } = useFormContext();
  const type = watch('type', {});
  const formData = watch('data', {});
  const leastCountEnabled = watch('leastCountEnabled', false);
  const verificationType = watch('verificationType', {});
  const autoInitialized = watch('autoInitialized', false);
  const autoInitialize = watch('autoInitialize', {});
  const validations = watch('validations', {});
  const metadata = watch('metadata');
  const { resourceParameterValidations: numbervalidations, criteriaValidations } = validations;
  const {
    propertyFilters: resourceFilters = { op: '', fields: [] },
    validations: resourceValidations = [],
  } = formData;
  const [loading, setLoading] = useState<Boolean>(false);
  const [resourceParameters, setResourceParameters] = useState<
    { id: string; type: ParameterType; label: string; taskId: string }[]
  >([]);
  const [loadingProperties, setLoadingProperties] = useState<Boolean>(false);
  const [objectProperties, setObjectProperties] = useState<any[]>([]);
  const [referencedParameter, setReferencedParameter] = useState<any>(null);
  const pagination = useRef({
    current: -1,
    isLast: false,
  });
  const isResourceValidationPresent = !!resourceValidations.length;
  const isResourceFilterPresent = !!resourceFilters?.op || !!resourceFilters?.fields.length;
  const isNumberValidationPresent = !!validations.length;
  const shouldDisableLinking =
    [
      ParameterVerificationTypeEnum.SELF,
      ParameterVerificationTypeEnum.PEER,
      ParameterVerificationTypeEnum.BOTH,
    ].includes(verificationType) ||
    isResourceValidationPresent ||
    isResourceFilterPresent ||
    isNumberValidationPresent ||
    leastCountEnabled ||
    (!metadata && loadOptions);
  const searchedValue = useRef<string>('');

  const fetchResourceParameters = async ({
    page = DEFAULT_PAGE_NUMBER,
    query = searchedValue.current,
  }) => {
    if (checklistId && !loading) {
      setLoading(true);
      const resources = await request('GET', apiGetParameters(checklistId), {
        params: {
          page,
          sort: 'createdAt,desc',
          filters: {
            op: FilterOperators.AND,
            fields: [
              {
                field: 'archived',
                op: FilterOperators.EQ,
                values: [false],
              },
              {
                field: 'type',
                op: FilterOperators.EQ,
                values: [MandatoryParameter.RESOURCE],
              },
              {
                field: 'targetEntityType',
                op: FilterOperators.NE,
                values: [TargetEntityType.UNMAPPED],
              },
              ...(addParameter?.parameterId
                ? [
                    {
                      field: 'id',
                      op: FilterOperators.NE,
                      values: [addParameter.parameterId || ''],
                    },
                  ]
                : []),
              ...(query ? [{ field: 'label', op: FilterOperators.LIKE, values: [query] }] : []),
            ],
          },
        },
      });
      if (resources.pageable) {
        pagination.current = {
          current: resources.pageable?.page,
          isLast: resources.pageable?.last,
        };
      }
      if (resources.data) {
        setResourceParameters((prev) =>
          pagination.current.current === 0 ? resources.data : [...prev, ...resources.data],
        );
      }
      setLoading(false);
    }
  };

  const getProperties = async (id: string) => {
    setLoadingProperties(true);
    const response: ResponseObj<ObjectType> = await request('GET', apiGetObjectTypes(id));
    if (response?.data) {
      if (type === MandatoryParameter.RESOURCE) {
        setObjectProperties(
          response.data.relations.filter(
            (relation: any) =>
              relation.objectTypeId === formData?.objectTypeId &&
              relation.target.cardinality !== InputTypes.ONE_TO_MANY,
          ) || [],
        );
      } else {
        setObjectProperties(
          response.data.properties.filter((property) => property.inputType === type) || [],
        );
      }
    }
    setLoadingProperties(false);
  };

  const fetchReferenceParameter = async (referencedParameterId: string) => {
    const { data } = await request('GET', apiSingleParameter(referencedParameterId));
    if (data) {
      setReferencedParameter(data);
      getProperties(data?.data?.objectTypeId);
    }
  };

  const getResourceParameterOptions = (resourceParameters: any[]) => {
    const filterCondition =
      type === MandatoryParameter.SINGLE_SELECT && metadata?.objectTypeId
        ? (parameter) => parameter.data.objectTypeId === metadata.objectTypeId
        : () => true;

    return resourceParameters.reduce((acc, parameter) => {
      if (filterCondition(parameter)) {
        acc.push({ ...parameter, value: parameter.id });
      }
      return acc;
    }, []);
  };

  const getObjectPropertyOptions = (objectProperties: any[]) => {
    const filterCondition =
      type === MandatoryParameter.SINGLE_SELECT && metadata?.propertyId
        ? (property) => property.id === metadata.propertyId
        : () => true;

    return objectProperties.reduce((acc, property) => {
      if (filterCondition(property)) {
        acc.push({
          externalId: property.externalId,
          label: property.displayName,
          value: property.id,
        });
      }
      return acc;
    }, []);
  };

  useEffect(() => {
    if (!autoInitialized) {
      setValue('autoInitialize', null, { shouldDirty: true, shouldValidate: true });
    } else {
      trigger('autoInitialize');
    }
  }, [autoInitialized]);

  const onRemoveLink = () => {
    setValue('autoInitialized', false);
  };

  useEffect(() => {
    if (type === MandatoryParameter.SINGLE_SELECT && !metadata && loadOptions) {
      setValue('autoInitialized', false);
    }
  }, [metadata]);

  useEffect(() => {
    if (autoInitialize?.parameterId) {
      fetchReferenceParameter(autoInitialize.parameterId);
    }
  }, [autoInitialize?.parameterId]);

  const renderLinkingSection = () => {
    register('autoInitialize', {
      validate: (value) => {
        const _autoInitialized = getValues('autoInitialized');
        let isValid = !_autoInitialized;
        if (!isValid && value) {
          if (MandatoryParameter.RESOURCE === type) {
            isValid = 'parameterId' in value && 'relation' in value;
          } else {
            isValid = 'parameterId' in value && 'property' in value;
          }
        }
        return isValid;
      },
    });
    return (
      <>
        <FormGroup
          inputs={[
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'resourceParameter',
                label: 'Resource Parameter',
                isLoading: loading,
                options: getResourceParameterOptions(resourceParameters),
                value: autoInitialize?.parameterId
                  ? [
                      {
                        label: referencedParameter?.label,
                        value: autoInitialize.parameterId,
                      },
                    ]
                  : undefined,
                onMenuOpen: () => fetchResourceParameters({}),
                onInputChange: debounce((searchedString: string, actionMeta) => {
                  if (searchedString !== actionMeta.prevInputValue) {
                    searchedValue.current = searchedString;
                    fetchResourceParameters({ query: searchedString });
                  }
                }, 500),
                onMenuScrollToBottom: () => {
                  if (!pagination.current.isLast) {
                    fetchResourceParameters({ page: pagination.current.current + 1 });
                  }
                },
                placeholder: 'Select',
                isDisabled: isReadOnly,
                onChange: (value: any) => {
                  setValue(
                    'autoInitialize',
                    {
                      parameterId: value.id,
                    },
                    {
                      shouldDirty: true,
                      shouldValidate: true,
                    },
                  );
                  getProperties(value.data.objectTypeId);
                },
              },
            },
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'resourceParameter',
                label: 'Object Property to be linked',
                isLoading: loadingProperties,
                options: getObjectPropertyOptions(objectProperties),
                value:
                  type === MandatoryParameter.RESOURCE
                    ? autoInitialize?.relation
                      ? [
                          {
                            label: autoInitialize?.relation?.displayName,
                            value: autoInitialize?.relation?.id,
                            externalId: autoInitialize?.relation?.externalId,
                          },
                        ]
                      : null
                    : autoInitialize?.property
                    ? [
                        {
                          label: autoInitialize?.property?.displayName,
                          value: autoInitialize?.property?.id,
                          externalId: autoInitialize?.property?.externalId,
                        },
                      ]
                    : null,
                placeholder: 'Select',
                isDisabled: isReadOnly,
                onChange: (value: any) => {
                  setValue(
                    'autoInitialize',
                    {
                      ...autoInitialize,
                      ...(type === MandatoryParameter.RESOURCE
                        ? {
                            relation: {
                              id: value.value,
                              displayName: value.label,
                              externalId: value.externalId,
                            },
                          }
                        : {
                            property: {
                              id: value.value,
                              displayName: value.label,
                              externalId: value.externalId,
                            },
                          }),
                    },
                    {
                      shouldDirty: true,
                      shouldValidate: true,
                    },
                  );
                },
              },
            },
          ]}
        />
        {!isReadOnly && (
          <Button
            type="button"
            variant="textOnly"
            color="red"
            style={{ padding: '8px', marginBlock: 16 }}
            onClick={onRemoveLink}
          >
            <LinkOffOutlined style={{ marginRight: 8 }} /> Remove Link
          </Button>
        )}
      </>
    );
  };

  return (
    <CommonWrapper>
      {isReadOnly && !autoInitialized ? null : (
        <>
          <h5>Link to a Resource Parameters Property</h5>
          <h6>
            This Parameter will get auto initialized with the value of on Object Property of
            Resource Parameter. The Type of the linked object property should match the type of this
            Parameter
          </h6>
        </>
      )}

      <div className="disabled-tooltip-info-icon">
        {!isReadOnly && !autoInitialized && (
          <Button
            type="button"
            variant="secondary"
            style={{ padding: '8px', marginBlock: 16 }}
            disabled={shouldDisableLinking}
            onClick={() => {
              setValue('autoInitialized', true, {
                shouldDirty: true,
                shouldValidate: true,
              });
            }}
          >
            <LinkOutlined style={{ marginRight: 8 }} /> Link Resource
          </Button>
        )}
        {!isReadOnly && shouldDisableLinking && (
          <Tooltip
            title={
              loadOptions && !metadata
                ? 'Please select Object Type and Object Property to enable Linking.'
                : 'Linking is disabled for parameters with Least count, Filter, Validation, or Verification. Remove it to enable Linking.'
            }
            arrow
            textAlignment="left"
          >
            <img src={InfoIcon} alt="parameter-info" style={{ marginRight: 8 }} />
          </Tooltip>
        )}
      </div>
      {autoInitialized && renderLinkingSection()}
    </CommonWrapper>
  );
};

export default LinkParameter;
