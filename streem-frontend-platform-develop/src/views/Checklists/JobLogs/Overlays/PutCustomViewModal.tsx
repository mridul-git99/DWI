import { BaseModal, TextInput, Button } from '#components';
import React, { FC } from 'react';
import styled from 'styled-components';
import { useForm } from 'react-hook-form';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';

export interface PutCustomViewModalProps {
  onPrimary: (data: any) => void;
  isEdit?: boolean;
  view?: any;
}

const Wrapper = styled.div.attrs({})`
  .modal {
    .modal-body {
      padding: 24px !important;
      .buttons-container {
        display: flex;
        flex-direction: row-reverse;
        margin-top: 40px;

        button {
          margin-right: 0;
          margin-left: 16px;
        }
      }
    }
  }
`;

const PutCustomViewModal: FC<CommonOverlayProps<any> & PutCustomViewModalProps> = ({
  closeAllOverlays,
  closeOverlay,
  props: { onPrimary, isEdit, view },
}) => {
  const {
    checklistListView: { customViews },
  } = useTypedSelector((state) => state);
  const form = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
  });

  const {
    handleSubmit,
    register,
    formState: { isDirty, isValid },
  } = form;

  const onSubmit = (data: Record<string, string>) => {
    const { label } = data;
    onPrimary({ label: label.trim() });
  };

  const validateName = (value: string) => {
    if (value.trim() === '') {
      return false;
    }
    return true;
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title={isEdit ? 'Edit View' : 'Create a new saved view'}
        showFooter={false}
      >
        <form onSubmit={handleSubmit(onSubmit)}>
          <TextInput
            name="label"
            label="View Name"
            ref={register({
              required: true,
              validate: validateName,
            })}
            defaultValue={isEdit && view ? view.label : ''}
          />
          <div className="buttons-container">
            <Button
              type="submit"
              loading={customViews.loading}
              disabled={customViews.loading || !isDirty || !isValid}
            >
              Save
            </Button>
            <Button variant="secondary" onClick={closeOverlay}>
              Cancel
            </Button>
          </div>
        </form>
      </BaseModal>
    </Wrapper>
  );
};

export default PutCustomViewModal;
