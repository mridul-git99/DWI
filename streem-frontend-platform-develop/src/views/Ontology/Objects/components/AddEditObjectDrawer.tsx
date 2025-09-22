import { useDrawer } from '#components';
import React, { FC, useEffect } from 'react';
import ObjectView from '../ObjectView';

const AddEditObjectDrawer: FC<{
  onCloseDrawer: React.Dispatch<React.SetStateAction<any>>;
  values?: Record<string, string>;
  onCreate?: () => void;
}> = ({ onCloseDrawer, values, onCreate }) => {
  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
    }, 200);
  };

  const onDone = () => {
    setTimeout(() => {
      onCreate && onCreate();
    }, 200);
    handleCloseDrawer();
  };

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: values?.id === 'new' ? 'Create Object' : 'Edit Object',
    hideCloseIcon: true,
    bodyContent: (
      <ObjectView values={{ ...values, onCancel: handleCloseDrawer, onDone }} label="" />
    ),
    footerContent: <></>,
    footerProps: {
      style: {
        display: 'none',
      },
    },
  });

  return StyledDrawer;
};

export default AddEditObjectDrawer;
