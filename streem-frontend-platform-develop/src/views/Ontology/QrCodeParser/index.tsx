import QRIcon from '#assets/svg/QR.svg';
import { Button, DataTable, FormGroup, NestedSelect } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { FilterOperators, InputTypes } from '#utils/globalTypes';
import { navigate } from '@reach/router';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import ArrowDown from '#assets/svg/ArrowDown.svg';
import { Controller, useFieldArray, useForm } from 'react-hook-form';
import { capitalize } from 'lodash';
import { request } from '#utils/request';
import { apiQrCodeParsers } from '#utils/apiUrls';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { PropertyFlags } from '../utils';

const QrCodeParserWrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 20px;
  background: #fff;
  overflow: auto;

  .header {
    display: flex;
    width: 100%;
    align-items: flex-start;
    border-bottom: 1px solid #e0e0e0;
    > div {
      display: flex;
      padding: 16px 24px;
      font-size: 20px;
      font-weight: 700;
    }
  }

  .body {
    padding: 24px 16px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    .form-group {
      padding: unset;
    }
  }

  .footer {
    display: flex;
    border-top: 1px solid #e0e0e0;
    justify-content: flex-end;
    margin-top: auto;
    box-shadow: 0px -1px 0px 0px #f4f4f4;
    padding: 12px 16px;
    > div {
      display: flex;
      > button {
        display: flex;
        height: 32px;
        padding: 2px 16px;
        align-items: center;
      }
    }
  }
`;

const Wrapper = styled.div.attrs({
  className: 'parser-details-wrapper',
})`
  .form-group {
    padding: unset;
    width: 50%;
  }

  > button {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 2px 24px;
    height: 40px;
  }

  .delimiter {
    > button {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 2px 16px;
      height: 40px;
    }
  }
`;

const LabelWrapper = styled.div`
  display: flex;
  padding: 8px 16px;
  align-items: center;
  gap: 8px;
  flex: 1 0 0;
  align-self: stretch;
  border: 1px solid #e0e0e0;
  background: #fff;
`;

const AddQrCodeParser: FC<{ id?: string }> = ({ id: qrParserId }) => {
  const {
    objectTypes: { active: activeObjectType },
  } = useTypedSelector((state) => state.ontology);
  const dispatch = useDispatch();
  const [qrData, setQrData] = useState('');

  const form = useForm({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      rules: [],
      displayName: '',
      objectTypeId: activeObjectType?.id,
      rawData: '',
      delimiter: '',
    },
  });

  const { control, getValues, watch, register, reset } = form;

  const { fields } = useFieldArray({
    control,
    name: 'rules',
    keyName: 'key',
  });

  const { rules } = watch(['rules']);

  const getParserDataById = async () => {
    try {
      const response = await request('GET', apiQrCodeParsers(), {
        params: {
          op: FilterOperators.AND,
          fields: [
            { field: 'objectTypeId', op: FilterOperators.EQ, values: [activeObjectType?.id] },
            { field: 'usageStatus', op: FilterOperators.EQ, values: [1] },
            { field: 'id', op: FilterOperators.LIKE, values: [qrParserId] },
          ],
        },
      });
      if (response.data) {
        const { displayName, objectTypeId, rules, rawData, delimiter } = response.data[0];
        reset({
          displayName,
          objectTypeId,
          rawData,
          delimiter,
          rules: rules?.map((currRule: any, index: number) => ({
            ...currRule,
            index,
          })),
        });
        setQrData(rawData);
      }
    } catch (error) {
      console.error(error);
    }
  };

  const columns = [
    {
      id: 'extractedData',
      label: 'Extracted Data',
      minWidth: 166,
      format: function renderComp(item: any) {
        return (
          <div>
            <FormGroup
              style={{ display: 'none' }}
              inputs={[
                {
                  type: InputTypes.SINGLE_LINE,
                  props: {
                    placeholder: 'Write here',
                    id: 'extractedData',
                    name: `rules.${item.index}.extractedData`,
                    ref: register({ required: true }),
                    value: item.extractedData,
                  },
                },
              ]}
            />
            <span>{item.extractedData}</span>
          </div>
        );
      },
    },
    {
      id: 'type',
      label: 'Type',
      minWidth: 166,
      format: function renderComp(item: any) {
        return (
          <>
            <Controller
              control={control}
              name={`rules.${item.index}.dataType`}
              defaultValue={item?.dataType || null}
              rules={{
                required: true,
              }}
              render={({ onChange, value }) => {
                return (
                  <NestedSelect
                    id="type-selector"
                    items={{
                      number: {
                        label: 'Number',
                      },
                      date: {
                        label: 'Date',
                        items: {
                          'dd/mm/yyyy': {
                            label: 'DD/MM/YYYY',
                          },
                          'mm/dd/yyyy': {
                            label: 'MM/DD/YYYY',
                          },
                        },
                      },
                      text: {
                        label: 'Text',
                      },
                    }}
                    label={() => (
                      <LabelWrapper>
                        {value ? capitalize(value) : 'Select'}
                        <img src={ArrowDown} alt="Arrow Down" />
                      </LabelWrapper>
                    )}
                    onChildChange={(option) => {
                      onChange(option.value);
                    }}
                  />
                );
              }}
            />
            <FormGroup
              style={{ display: 'none' }}
              inputs={[
                {
                  type: InputTypes.SINGLE_LINE,
                  props: {
                    placeholder: 'Write here',
                    id: 'dateFormat',
                    name: `rules.${item.index}.dateFormat`,
                    ref: register({ required: true }),
                    defaultValue: item.dateFormat,
                  },
                },
              ]}
            />
          </>
        );
      },
    },
    {
      id: 'startPosition',
      label: 'Start Position',
      minWidth: 166,
      format: function renderComp(item: any) {
        return (
          <FormGroup
            inputs={[
              {
                type: InputTypes.NUMBER,
                props: {
                  placeholder: 'Write here',
                  id: 'startPosition',
                  name: `rules.${item.index}.startPos`,
                  ref: register({ required: true }),
                  defaultValue: item.startPos,
                },
              },
            ]}
          />
        );
      },
    },
    {
      id: 'endPosition',
      label: 'End Position',
      minWidth: 166,
      format: function renderComp(item: any) {
        return (
          <FormGroup
            inputs={[
              {
                type: InputTypes.NUMBER,
                props: {
                  placeholder: 'Write here',
                  id: 'endPosition',
                  name: `rules.${item.index}.endPos`,
                  ref: register({ required: true }),
                  defaultValue: item.endPos,
                },
              },
            ]}
          />
        );
      },
    },
    {
      id: 'result',
      label: 'Result',
      minWidth: 166,
      format: function renderComp(item: any) {
        const current = rules[item.index];

        const result = current?.startPos
          ? current?.extractedData?.substring(
              Number(current?.startPos) - 1,
              current?.endPos ? Number(current.endPos) : current?.extractedData?.length,
            )
          : '';

        return (
          <div>
            <FormGroup
              style={{ display: 'none' }}
              inputs={[
                {
                  type: InputTypes.SINGLE_LINE,
                  props: {
                    placeholder: 'Result',
                    id: 'result',
                    disabled: true,
                    name: `rules.${item.index}.result`,
                    value: result,
                    ref: register({ required: true }),
                  },
                },
              ]}
            />
            <span>{result}</span>
          </div>
        );
      },
    },
    {
      id: 'objectProperty',
      label: 'Object Property',
      minWidth: 166,
      format: function renderComp(item: any) {
        const currentType: string = rules[item.index]?.dataType;
        let selectedPropertyIds: string[] = [];
        rules?.forEach((currRule: any) => {
          if (currRule.propertyId) {
            selectedPropertyIds.push(currRule.propertyId);
          }
        });
        const propertyOptions = activeObjectType?.properties.filter((currProperty) => {
          if (![PropertyFlags.EXTERNAL_ID, PropertyFlags.SYSTEM].includes(currProperty.flags)) {
            if (['number', 'NUMBER'].includes(currentType)) {
              return currProperty.inputType === InputTypes.NUMBER;
            } else if (['dd/mm/yyyy', 'mm/dd/yyyy', 'DATE'].includes(currentType)) {
              return [InputTypes.DATE, InputTypes.DATE_TIME].includes(currProperty.inputType);
            } else if (['text', 'TEXT'].includes(currentType)) {
              return [InputTypes.SINGLE_LINE, InputTypes.MULTI_LINE].includes(
                currProperty.inputType,
              );
            }
          }
        });

        const filteredPropertyOptions =
          selectedPropertyIds.length > 0
            ? propertyOptions?.filter((option) => !selectedPropertyIds.includes(option.id))
            : propertyOptions;

        return (
          <Controller
            control={control}
            name={`rules.${item.index}.propertyId`}
            defaultValue={item?.propertyId || null}
            rules={{
              required: true,
            }}
            render={({ onChange, value }) => {
              const matchedProperty = (propertyOptions || []).find(
                (currProperty) => currProperty.id === value,
              );
              return (
                <FormGroup
                  inputs={[
                    {
                      type: InputTypes.SINGLE_SELECT,
                      props: {
                        placeholder: 'Select',
                        id: 'objectProperty',
                        name: 'objectProperty',
                        options: (filteredPropertyOptions || [])?.map((currProperty) => ({
                          label: currProperty.displayName,
                          value: currProperty.id,
                        })),
                        value: matchedProperty?.id
                          ? [{ label: matchedProperty.displayName, value: matchedProperty.id }]
                          : null,
                        onChange: (_value: any) => {
                          onChange(_value.value);
                        },
                      },
                    },
                  ]}
                />
              );
            }}
          />
        );
      },
    },
  ];

  const onSubmit = async () => {
    const _data = getValues();

    const newRules = _data?.rules?.map((currRule: any) => {
      switch (currRule.dataType) {
        case 'number':
        case 'NUMBER':
          return { ...currRule, dataType: InputTypes.NUMBER, dateFormat: null };
        case 'text':
        case 'TEXT':
          return { ...currRule, dataType: 'TEXT', dateFormat: null };
        case 'dd/mm/yyyy':
        case 'mm/dd/yyyy':
          return { ...currRule, dateFormat: currRule.dataType, dataType: InputTypes.DATE };
      }
    });

    try {
      const response = await request('POST', apiQrCodeParsers(), {
        data: { ..._data, rules: newRules },
      });
      if (response.data) {
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: `${response.data.displayName} saved successfully!`,
          }),
        );
        navigate(-1);
      }
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    if (qrParserId !== 'new') {
      getParserDataById();
    }
  }, [qrParserId]);

  return (
    <QrCodeParserWrapper>
      <div className="header">
        <div>Create New QR Code Parser</div>
      </div>
      <div className="body">
        <ParserDetailSection
          objectType={activeObjectType}
          form={form}
          qrData={qrData}
          setQrData={setQrData}
        />
        {fields.length > 0 && (
          <DataTable columns={columns} rows={fields} emptyTitle="No Extracted Data Found" />
        )}
      </div>
      <div className="footer">
        <div>
          <Button
            variant="secondary"
            onClick={() => {
              navigate(-1);
            }}
          >
            Cancel
          </Button>
          <Button type="submit" onClick={onSubmit}>
            Save
          </Button>
        </div>
      </div>
    </QrCodeParserWrapper>
  );
};

const ParserDetailSection: FC<any> = ({ objectType, form, qrData, setQrData }) => {
  const dispatch = useDispatch();

  const [delimiterValue, setDelimiterValue] = useState('');

  const { register, setValue } = form;
  const onSelectWithQR = (data: string) => {
    setQrData(data);
    setValue('rawData', data);
  };

  const delimiterSection = () => {
    const onDelimiterSubmit = () => {
      const extractedData = qrData.split(delimiterValue)?.map((value: string, index: number) => ({
        extractedData: value,
        result: '',
        index: index,
        startPos: null,
        endPos: null,
        dataType: null,
        dateFormat: null,
        propertyId: null,
      }));

      // append(extractedData);
      setValue('rules', extractedData);
    };
    return (
      <div className="delimiter">
        <FormGroup
          style={{ marginBlock: 24 }}
          inputs={[
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                id: 'rawData',
                label: 'Raw Data',
                name: 'rawData',
                disabled: true,
                value: qrData,
                ref: register,
              },
            },
          ]}
        />
        <FormGroup
          style={{ marginBottom: 24 }}
          inputs={[
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Write here',
                label: 'Delimiter',
                id: 'delimiter',
                name: 'delimiter',
                ref: register({ required: true }),
                onChange: ({ value }: { value: string }) => {
                  setDelimiterValue(value);
                },
              },
            },
          ]}
        />
        <Button variant="secondary" onClick={onDelimiterSubmit}>
          Submit
        </Button>
      </div>
    );
  };

  return (
    <Wrapper>
      <FormGroup
        style={{ marginBottom: 24 }}
        inputs={[
          {
            type: InputTypes.SINGLE_LINE,
            props: {
              placeholder: 'Write here',
              label: 'Parser Name',
              id: 'displayName',
              name: 'displayName',
              ref: register({ required: true }),
            },
          },
          {
            type: InputTypes.SINGLE_LINE,
            props: {
              label: 'Object Type',
              placeholder: 'Auto Generated',
              id: 'objectTypeId',
              value: objectType?.displayName,
              disabled: true,
              ref: register('objectTypeId', { required: true }),
            },
          },
        ]}
      />
      <Button
        variant="secondary"
        onClick={() => {
          dispatch(
            openOverlayAction({
              type: OverlayNames.QR_SCANNER,
              props: { onSuccess: onSelectWithQR },
            }),
          );
        }}
      >
        <img src={QRIcon} alt="QR Icon" />
        Scan Sample QR
      </Button>
      {qrData && delimiterSection()}
    </Wrapper>
  );
};

export default AddQrCodeParser;
