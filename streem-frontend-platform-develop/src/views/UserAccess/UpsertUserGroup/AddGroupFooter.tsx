import { Button } from '#components';
import { navigate } from '@reach/router';
import React from 'react';
import { useFormContext } from 'react-hook-form';

export const AddGroupFooter = ({ addUserGroup, isEdit, isDirtyForm }: any) => {
  const {
    formState: { isValid, isDirty },
  } = useFormContext();
  return (
    <div className="footer-container">
      <Button variant="secondary" color="gray" onClick={() => navigate(-1)}>
        Cancel
      </Button>
      <Button
        variant="primary"
        disabled={isEdit ? !(isDirtyForm.current || isDirty) || !isValid : !isValid || !isDirty}
        onClick={() => {
          addUserGroup();
        }}
      >
        Confirm
      </Button>
    </div>
  );
};
