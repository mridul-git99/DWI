import { Checklist } from '#PrototypeComposer/checklist.types';
import { actionSpreader } from '#store/helpers';
import { Error } from '#utils/globalTypes';
import { Author, FormValuesOnlyWithAuthorIds, NewPrototypeActions } from './types';

export const addNewPrototype = (
  data: FormValuesOnlyWithAuthorIds,
  setApiFormErrors: (formErrors: Error[]) => void,
) => actionSpreader(NewPrototypeActions.ADD_NEW_PROTOTYPE, { data, setApiFormErrors });

export const addRevisionPrototype = (
  checklistId: Checklist['id'],
  code: Checklist['code'],
  name: Checklist['name'],
) =>
  actionSpreader(NewPrototypeActions.ADD_REVISION_PROTOTYPE, {
    checklistId,
    code,
    name,
  });

export const updatePrototype = (
  data: FormValuesOnlyWithAuthorIds,
  id: Checklist['id'],
  originalAuthors: Author['id'][],
  setApiFormErrors: (formErrors: Error[]) => void,
) =>
  actionSpreader(NewPrototypeActions.UPDATE_PROTOTYPE, {
    data,
    id,
    originalAuthors,
    setApiFormErrors,
  });
