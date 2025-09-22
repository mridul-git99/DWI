import { BaseModal, Button, Select } from '#components';
import { CommonOverlayProps, OverlayNames } from '#components/OverlayContainer/types';
import React, { FC, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { createFetchList } from '#hooks/useFetchData';
import { apiQrCodeParsers } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators } from '#utils/globalTypes';
import { useTypedSelector } from '#store';

const Wrapper = styled.div`
  .modal {
    min-width: 406px !important;
  }

  .title {
    font-size: 14px;
    font-weight: 700;
    line-height: 16px;
    letter-spacing: 0.16px;
  }

  .qr-parser-modal-footer {
    border-top: 1px solid #f4f4f4 !important;
    display: flex;
    align-items: center;
    padding: 12px 16px;
    justify-content: flex-end;
  }

  .qr-parser-modal-body {
    padding-bottom: 24px;
    min-height: 40dvh;
  }
`;

const modalTitle = () => {
  return <div className="title">Select a QR Code Parser</div>;
};

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  sort: 'createdAt,desc',
};

const QrCodeParserModal: FC<CommonOverlayProps<{}>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { handleQrScanParser },
}) => {
  const {
    objectTypes: { active },
  } = useTypedSelector((state) => state.ontology);

  const objectTypeId = active?.id;

  const [qrCodeParser, setQrCodeParser] = useState();

  const qrParserFilter = {
    op: FilterOperators.AND,
    fields: [
      { field: 'objectTypeId', op: FilterOperators.EQ, values: [objectTypeId] },
      { field: 'usageStatus', op: FilterOperators.EQ, values: [1] },
    ],
  };

  const { list } = createFetchList(
    apiQrCodeParsers(),
    { ...urlParams, filters: qrParserFilter },
    true,
  );

  const onSuccess = (rawData) => {
    handleQrScanParser(rawData, qrCodeParser);
  };

  const dispatch = useDispatch();

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showHeader={true}
        showFooter={false}
        title={modalTitle()}
      >
        <div className="qr-parser-modal-body">
          <Select
            label="Select a QR Code Parser"
            options={list.map((option) => ({ label: option.displayName, value: option }))}
            onChange={(option) => setQrCodeParser(option.value)}
            placeholder="Select"
          />
        </div>
        <div className="qr-parser-modal-footer">
          <Button variant="secondary" onClick={() => closeOverlay()}>
            Cancel
          </Button>
          <Button
            variant="primary"
            color="blue"
            disabled={!qrCodeParser}
            onClick={() => {
              dispatch(
                openOverlayAction({
                  type: OverlayNames.QR_SCANNER,
                  props: { onSuccess },
                }),
              );
              closeOverlay();
            }}
          >
            Select
          </Button>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default QrCodeParserModal;
