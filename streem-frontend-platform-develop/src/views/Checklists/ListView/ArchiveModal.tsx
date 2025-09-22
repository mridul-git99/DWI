import ArchiveSuccessful from '#assets/svg/ArchiveSuccessful';
import CannotArchive from '#assets/svg/CannotArchive';
import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    min-width: 420px !important;
    width: 420px !important;
  }

  .modal-body {
    padding: 0 !important;
  }

  .close-icon {
    top: 24px !important;
    right: 32px !important;
  }

  .body {
    padding: 80px;

    .editing-disabled-icon {
      font-size: 120px;
    }

    .text1 {
      color: #000000;
      font-size: 20px;
      font-weight: bold;
      margin-top: 16px;
    }

    .text2 {
      color: #666666;
      font-size: 14px;
      font-weight: normal;
      margin-top: 8px;
    }
  }
`;

const ArchiveModal: FC<
  CommonOverlayProps<{
    mode: 'archive' | 'unarchive' | 'cannotArchive' | 'cannotArchiveActiveSchedulers';
  }>
> = ({ closeAllOverlays, closeOverlay, props: { mode } = {} }) => (
  <Wrapper>
    <BaseModal
      closeAllModals={closeAllOverlays}
      closeModal={closeOverlay}
      showHeader={false}
      showFooter={false}
    >
      <div className="body">
        {(() => {
          if (mode === 'archive') {
            return (
              <>
                <ArchiveSuccessful fontSize="140px" />
                <div className="text1">Archive Successful</div>
                <div className="text2">Great Job! Process Archived Successfully</div>
              </>
            );
          }

          if (mode === 'unarchive') {
            return (
              <>
                <ArchiveSuccessful fontSize="140px" />
                <div className="text1">Unarchive Successful</div>
              </>
            );
          }

          if (mode === 'cannotArchive') {
            return (
              <>
                <CannotArchive fontSize="160px" />
                <div className="text1">Cannot Archive</div>
                <div className="text2">
                  Oops! You can't archive a Process that has running Jobs.
                </div>
              </>
            );
          }

          if (mode === 'cannotArchiveActiveSchedulers') {
            return (
              <>
                <CannotArchive fontSize="160px" />
                <div className="text1">Cannot Archive</div>
                <div className="text2">
                  Oops! You Cannot archive process. All related schedulers must be archived first.
                  Please archive the schedulers and retry.
                </div>
              </>
            );
          }
        })()}
      </div>
    </BaseModal>
  </Wrapper>
);

export default ArchiveModal;
