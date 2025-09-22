import React, { Dispatch, FC, SetStateAction } from 'react';
import { BaseModal, Link } from '#components';
import { CommonOverlayProps, OverlayNames } from '#components/OverlayContainer/types';
import { TObject } from '#views/Ontology/types';
import JobLogsTabContent from '../components/JobLogsTabContent';
import { DataTableColumn } from '#components/shared/DataTable';
import { useDispatch } from 'react-redux';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import KeyboardArrowLeftIcon from '@material-ui/icons/KeyboardArrowLeft';

type TObjectJobLogPreviewModalProps = {
  selectedObject: TObject;
  objectJobLogColumns: DataTableColumn[];
  customView: Record<string, any>;
  viewFilters: {
    key: string;
    constraint: string;
    value: string;
  }[];
  setSelectedView: Dispatch<SetStateAction<Record<string, any> | null>>;
};

const ObjectJobLogPreviewModal: FC<CommonOverlayProps<TObjectJobLogPreviewModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { selectedObject, objectJobLogColumns, customView, viewFilters, setSelectedView },
}) => {
  const dispatch = useDispatch();

  const handleClose = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.OBJECT_PREVIEW_MODAL,
        props: {
          collection: selectedObject?.collection,
          customView,
          setSelectedView,
        },
      }),
    );
    closeOverlay();
  };

  const onCloseModal = () => {
    setSelectedView(null);
    closeOverlay();
  };

  return (
    <BaseModal
      closeAllModals={closeAllOverlays}
      closeModal={onCloseModal}
      title={
        <Link
          label="Back to Object Type"
          backIcon={KeyboardArrowLeftIcon}
          onClick={handleClose}
          iconColor="#000000"
          labelColor="#000000"
          addMargin={false}
        />
      }
      showFooter={false}
    >
      <JobLogsTabContent
        values={{
          selectedObject,
          columns: objectJobLogColumns,
          showFilters: false,
          viewFilters,
        }}
      />
    </BaseModal>
  );
};

export default ObjectJobLogPreviewModal;
