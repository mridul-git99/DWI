import { AddNewItem, Avatar, Button, Select, Textarea, TextInput, Option } from '#components';
import { ComposerEntity } from '#PrototypeComposer/types';
import { User } from '#services/users';
import { useTypedSelector } from '#store/helpers';
import {
  ALL_FACILITY_ID,
  DEFAULT_PAGE_NUMBER,
  DEFAULT_PAGE_SIZE,
  nonEmptyStringRegex,
} from '#utils/constants';
import { Error, FilterOperators } from '#utils/globalTypes';
import { getFullName } from '#utils/stringUtils';
import {
  Close,
  Error as ErrorIcon,
  ErrorOutlineOutlined,
  ReportProblemOutlined,
} from '@material-ui/icons';
import { navigate } from '@reach/router';
import { debounce, isEmpty, pick } from 'lodash';
import React, { FC, FormEvent, useCallback, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { addNewPrototype, updatePrototype } from './actions';
import { Author, FormErrors, FormMode, FormValues, KeyValue, Props, colorCodes } from './types';
import { apiGetProcessProperties, apiGetUsers } from '#utils/apiUrls';
import { createFetchList } from '#hooks/useFetchData';
import { generateUserSearchFilters } from '#utils/smartFilterUtils';
import tickIcon from '#assets/svg/tickIcon.svg';

const FormError = styled.div`
  align-items: center;
  color: #eb5757;
  display: flex;
  font-size: 12px;
  justify-content: flex-start;
  margin-top: 5px;
  margin-bottom: 10px;

  form-error-icon {
    font-size: 16px;
    color: #eb5757;
    margin-right: 5px;
  }
`;

const createUrlParams = (additionalParams = {}) => ({
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  ...additionalParams,
});

const urlParams = createUrlParams({
  sort: 'createdAt,desc',
});

const userUrlParams = createUrlParams();

const validateForm = (values: FormValues) => {
  const formErrors: FormErrors = { name: '', properties: {} };
  let isValid = true;

  values.properties.map((el) => {
    if (values.property && values.property[el.id] && values.property[el.id].value) {
      el.value = values.property[el.id].label;
    }
  });

  if (!nonEmptyStringRegex.test(values.name)) {
    isValid = false;
    formErrors.name = 'Process name is required';
  }

  values.properties.map((property) => {
    if (property.mandatory && !property.value) {
      isValid = false;
      formErrors.properties[property.id.toString()] = 'Property is required';
    }
  });

  return { isValid, formErrors };
};

const PrototypeForm: FC<Props> = (props) => {
  const { formMode, formData } = props;
  const dispatch = useDispatch();
  const { listById } = useTypedSelector((state) => state.properties[ComposerEntity.CHECKLIST]);

  const {
    auth: { selectedFacility: { id: facilityId = '' } = {}, profile, selectedUseCase },
  } = useTypedSelector((state) => state);

  const { list, reset, fetchNext } = createFetchList(apiGetProcessProperties(''), urlParams, false);

  const userState = facilityId === ALL_FACILITY_ID ? 'authors/global' : 'authors';

  const {
    list: usersList,
    reset: resetUsers,
    fetchNext: fetchNextUsers,
    status,
  } = createFetchList<User[]>(apiGetUsers(userState), userUrlParams, false);
  const [propertyWarning, setPropertyWarning] = useState({});
  const [filters, setFilters] = useState<Record<string, any>>({
    params: { ...urlParams },
    url: '',
  });
  const [menuOpenState, setMenuOpenState] = useState<boolean | string>(false);
  const [formErrors, setFormErrors] = useState<FormErrors>({
    name: '',
    properties: {},
  });
  /*
    The UI receives createdBy only after making the API call hence when the user clicks on the "Start a Prototype" the owner details are blank.
    The user creating the Prototype is the owner.
  */
  const [formValues, setFormValues] = useState<FormValues>({
    authors: formData?.authors ?? [],
    description: formData.description ?? '',
    name: formData?.name ?? '',
    colorCode: formData?.colorCode ?? '',
    createdBy: pick(formData.createdBy ?? profile, [
      'id',
      'employeeId',
      'firstName',
      'lastName',
      'email',
    ]),
    properties: [],
    property: {},
  });

  const debouncedReset = useCallback(
    debounce((filters) => {
      reset(filters);
    }, 500),
    [],
  );

  const getPrefillPropertyDetails = (values) => {
    let obj = {};
    for (let key in values) {
      if (formData?.properties?.find((el) => el.id === values[key].id)?.value) {
        obj[key] = {
          value: values[key].id,
          label: formData?.properties?.find((el) => el.id === values[key].id)?.value ?? '',
        };
      }
    }
    return obj;
  };

  // TODO Create a single global error handler for apis
  const getApiFormErrors = (apiFormErrors: Error[]) => {
    const updatedFormErrors = { ...formErrors };
    if (apiFormErrors && apiFormErrors.length) {
      apiFormErrors.forEach((formError) => {
        if (formError.code === 'E124') {
          updatedFormErrors.authors = formError;
        }
        if (formError.code === 'E143') {
          updatedFormErrors.properties[formError.id.toString()] = formError.message;
        }
      });
      setFormErrors(updatedFormErrors);
    }
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { isValid, formErrors } = validateForm(formValues);

    if (isValid) {
      if (formMode === FormMode.ADD) {
        dispatch(
          addNewPrototype(
            {
              ...formValues,
              authors: formValues.authors.map((author) => author.id),
              useCaseId: selectedUseCase!.id,
            },
            getApiFormErrors,
          ),
        );
      } else if (formMode === FormMode.EDIT) {
        dispatch(
          updatePrototype(
            {
              ...formValues,
              authors: formValues.authors.map((author) => author.id),
              useCaseId: selectedUseCase!.id,
            },
            formData?.prototypeId,
            formData?.authors?.map((author) => author.id),
            getApiFormErrors,
          ),
        );
      }
    } else {
      setFormErrors(formErrors);
    }
  };

  const filterUsers = (users: User[]) => {
    const filteredUsers = users.reduce<Option[]>((acc, user) => {
      if (
        user.id !== formValues.createdBy.id &&
        !formValues.authors.some((author) => author.id === user.id)
      ) {
        acc.push({
          ...user,
          label: `${getFullName(user)}, ID : ${user.employeeId}`,
          value: user.id,
        });
      }
      return acc;
    }, []);

    return filteredUsers;
  };

  const checkUniquenessData = (propertyId: string, typeValue: KeyValue) => {
    const isUnique = !list.find(
      (value) => value.trim().toLowerCase() === typeValue?.label?.trim().toLowerCase(),
    );

    setPropertyWarning((prev) => {
      if (typeValue) {
        return {
          ...prev,
          [propertyId]: {
            message: isUnique
              ? "The value you've entered is unique"
              : "The value you've entered already exists.",
            duplicate: !isUnique,
          },
        };
      } else {
        return {
          ...prev,
          [propertyId]: undefined,
        };
      }
    });
  };

  useEffect(() => {
    if (!isEmpty(listById)) {
      setFormValues((values) => ({
        ...values,
        property: getPrefillPropertyDetails(listById),
        properties: Object.values(listById).map((property) => ({
          id: property.id,
          label: property.label,
          mandatory: property.mandatory,
          name: property.name,
          placeHolder: property.placeHolder,
          value: formData?.properties?.find((el) => el.id === property.id)?.value ?? '',
        })),
      }));
    }
  }, [listById]);

  useEffect(() => {
    if (!!filters.url) {
      debouncedReset(filters);
    }
    return () => {
      debouncedReset.cancel();
    };
  }, [filters]);

  return (
    <form className="prototype-form" onSubmit={handleSubmit}>
      <h3 className="heading">New Process Prototype</h3>

      <div className="left-side">
        {formData.revisedCode && (
          <>
            <div className="input-field">
              <h5 className="label">Process Being Revised</h5>
              <h4 className="value">{formData.revisedName}</h4>
            </div>
            <div className="input-field">
              <h5 className="label">Process ID</h5>
              <h4 className="value">{formData.revisedCode}</h4>
            </div>
          </>
        )}
        <div className={formData.revisedCode ? 'owner revised' : 'owner'}>
          <h5 className={formData.revisedCode ? 'label-light' : 'label'}>
            {formData.revisedCode ? 'Being Revised by' : 'Process Owner'}
          </h5>
          <div className="container">
            <Avatar user={formValues.createdBy} allowMouseEvents={false} />
            <div className="owner-details">
              <div className="owner-id">{formValues.createdBy.employeeId}</div>
              <div className="owner-name">{getFullName(formValues.createdBy)}</div>
            </div>
          </div>
        </div>

        <TextInput
          defaultValue={formValues.name}
          error={formErrors.name}
          label="Process Name"
          disabled={formMode === FormMode.VIEW}
          name="name"
          onChange={debounce(({ name, value }) => {
            setFormErrors((errors) => ({ ...errors, name: '' }));
            setFormValues((values) => ({ ...values, [name]: value }));
          }, 500)}
        />
        {formValues.properties.map((property, index) => {
          return (
            <div style={{ margin: '10px 0' }} key={property.id}>
              <Select
                key={index}
                placeholder={property.placeHolder}
                label={property.label}
                components={{
                  DropdownIndicator: null,
                  IndicatorSeparator: null,
                }}
                menuIsOpen={menuOpenState === property.id}
                value={formValues.property && formValues.property[property.id]}
                onBlur={() => {
                  setMenuOpenState(false);
                  checkUniquenessData(property.id, formValues.property?.[property.id]);
                }}
                onChange={(selectedOption) => {
                  setFormValues((values) => ({
                    ...values,
                    property: {
                      ...formValues.property,
                      [property.id]: selectedOption,
                    },
                  }));
                  setMenuOpenState(false);
                  checkUniquenessData(property.id, selectedOption);
                }}
                menuPortalTarget={document.body}
                onMenuScrollToBottom={() => fetchNext()}
                onInputChange={(value, actionMeta) => {
                  if (actionMeta.action === 'input-change') {
                    if (value) {
                      setMenuOpenState(property.id);
                    } else {
                      setMenuOpenState(false);
                    }

                    setFormValues((values) => ({
                      ...values,
                      property: {
                        ...formValues.property,
                        [property.id]: {
                          label: value,
                          value,
                        },
                      },
                    }));
                    if (actionMeta.prevInputValue !== value) {
                      setFilters((filters) => ({
                        ...filters,
                        url: apiGetProcessProperties(property.id),
                        params: { ...filters.params, propertyNameInput: value },
                      }));
                    }
                  }
                }}
                error={formErrors && formErrors.properties[property.id]}
                options={list.map((property) => {
                  return {
                    label: property,
                    value: property,
                  };
                })}
                isDisabled={formMode === FormMode.VIEW}
              />
              {propertyWarning?.[property.id] && formValues.property[property.id]?.label && (
                <div
                  className={`unique-warning-container ${
                    propertyWarning[property.id].duplicate ? 'nonunique' : 'unique'
                  }`}
                >
                  {propertyWarning[property.id].duplicate ? (
                    <ReportProblemOutlined className="icon nonunique" />
                  ) : (
                    <ErrorOutlineOutlined className="icon unique" />
                  )}
                  <span className="label">{propertyWarning[property.id].message}</span>
                </div>
              )}
            </div>
          );
        })}
      </div>

      <div className="right-side">
        <Textarea
          optional
          defaultValue={formValues.description}
          label="Add Description"
          disabled={formMode === FormMode.VIEW}
          name="description"
          onChange={debounce(({ name, value }) => {
            setFormValues((val) => ({ ...val, [name]: value }));
          }, 500)}
          rows={3}
        />

        <div className="color-code">
          <label className="new-form-field-label">
            Process Colour <span className="optional-badge">Optional</span>
          </label>

          <div className="color-palette">
            {Object.keys(colorCodes).map((color) => (
              <>
                <div
                  key={color}
                  className="color-block"
                  style={{ backgroundColor: colorCodes[color] }}
                  onClick={() => {
                    if (formMode !== FormMode.VIEW) {
                      setFormValues((values) => ({
                        ...values,
                        colorCode:
                          values.colorCode === colorCodes[color] ? null : colorCodes[color],
                      }));
                    }
                  }}
                >
                  {formValues.colorCode && colorCodes[color] === formValues.colorCode && (
                    <img src={tickIcon} className="active-color" />
                  )}
                </div>
              </>
            ))}
          </div>
        </div>

        <label className="new-form-field-label">
          Select Authors <span className="optional-badge">Optional</span>
        </label>

        {formErrors.authors && (
          <FormError>
            <ErrorIcon className="form-error-icon" />
            {formErrors.authors.message}
          </FormError>
        )}

        {formValues.authors.map((author, index) => {
          return (
            <div key={`${index}-${author.id}`} className="author">
              <Select
                style={{ width: '100%' }}
                value={
                  // This check is required to create a unselected select component on click of Add New ie line no : 303.
                  author.id !== '0'
                    ? {
                        label: `${getFullName(author)}, ID : ${author.employeeId}`,
                        value: author.id,
                      }
                    : undefined
                }
                placeholder="Choose Users"
                isLoading={status === 'loading'}
                isDisabled={formMode === FormMode.VIEW}
                onMenuScrollToBottom={fetchNextUsers}
                options={filterUsers(usersList)}
                menuPortalTarget={document.body}
                onChange={(selectedOption: any) => {
                  const { label, value, ...selectedUser } = selectedOption;
                  setFormValues((values) => ({
                    ...values,
                    authors: [
                      ...values.authors.slice(0, index),
                      selectedUser as unknown as Author,
                      ...values.authors.slice(index + 1),
                    ],
                  }));
                  // reset authors related form errors
                  if (formErrors.authors) {
                    setFormErrors({ ...formErrors, authors: undefined });
                  }
                }}
                onMenuOpen={() => resetUsers({ params: userUrlParams })}
                onInputChange={debounce((value, actionMeta) => {
                  if (value !== actionMeta.prevInputValue)
                    resetUsers({
                      params: {
                        ...userUrlParams,
                        ...(value && {
                          filters: generateUserSearchFilters(FilterOperators.LIKE, value),
                        }),
                      },
                    });
                }, 500)}
              />
              {formMode !== FormMode.VIEW && (
                <Close
                  id="remove"
                  className="icon"
                  onClick={() => {
                    setFormValues((values) => ({
                      ...values,
                      authors: [
                        ...values.authors.slice(0, index),
                        ...values.authors.slice(index + 1),
                      ],
                    }));
                    // reset authors related form errors
                    if (formErrors.authors) {
                      setFormErrors({ ...formErrors, authors: undefined });
                    }
                  }}
                />
              )}
            </div>
          );
        })}

        {formMode !== FormMode.VIEW && (
          <AddNewItem
            onClick={() => {
              setFormValues((values) => ({
                ...values,
                authors: [...values.authors, { id: '0' } as Author],
              }));
            }}
          />
        )}
      </div>

      <div className="form-submit-buttons">
        <Button color="red" variant="secondary" onClick={() => navigate(-1)}>
          Cancel
        </Button>
        {formMode !== FormMode.VIEW && <Button type="submit">Submit</Button>}
      </div>
    </form>
  );
};

export default PrototypeForm;
