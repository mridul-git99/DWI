import React, { useCallback, useEffect } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { JobPermissionContainer } from '../TrainedUser/JobPermission';
import { Button, useDrawer } from '#components';
import styled from 'styled-components';
import { request } from '#utils/request';

const EditPermissionWrapper = styled.form`
  width: 100%;
`;

export const EditPermission = ({ onCloseDrawer }: any) => {
  const form = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      permission: [],
    },
  });

  const {
    handleSubmit,
    formState: { isDirty, isValid },
    control,
  } = form;

  const {
    fields: permissionField,
    append: permissionAppend,
    remove: permissionRemove,
  } = useFieldArray({
    control,
    name: 'permission',
  });

  const handleSubmitHandler = async () => {
    const response = await request('GET', apiUrl(checklistId as string));
  };

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'Add User Group',
    hideCloseIcon: true,
    bodyContent: (
      <EditPermissionWrapper onSubmit={handleSubmit(handleSubmitHandler)}>
        <JobPermissionContainer
          permissionField={permissionField}
          permissionAppend={permissionAppend}
          permissionRemove={permissionRemove}
        />
      </EditPermissionWrapper>
    ),
    footerContent: (
      <>
        <Button
          variant="secondary"
          style={{ marginLeft: 'auto' }}
          onClick={() => {
            handleCloseDrawer();
          }}
        >
          Cancel
        </Button>
        <Button type="submit" disabled={!isDirty || !isValid} onClick={handleSubmitHandler}>
          Save
        </Button>
      </>
    ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  const handleCloseDrawer = useCallback(() => {
    setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
    }, 200);
  }, [setDrawerOpen, onCloseDrawer]);

  return StyledDrawer;
};
