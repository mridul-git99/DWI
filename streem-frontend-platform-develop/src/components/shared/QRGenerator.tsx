import { BaseModal, Button, Select, Textarea } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { CommonOverlayProps, OverlayNames } from '#components/OverlayContainer/types';
import checkPermission from '#services/uiPermissions';
import { nonEmptyStringRegex } from '#utils/constants';
import { editQrData } from '#views/Ontology/actions';
import { TObject } from '#views/Ontology/types';
import { BrowserMultiFormatReader, BrowserQRCodeSvgWriter } from '@zxing/library';
import React, { FC, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

type Props = {
  id: string;
  title: string;
  onPrimary: () => void;
  secondaryText?: string;
  primaryText: string;
  data: any;
  selectedObject: TObject;
};

const QRGeneratorWrapper = styled.div`
  display: flex;
  flex: 1;
  justify-content: center;
`;

const Wrapper = styled.div`
  .modal-body {
    min-height: 30dvh;
  }
  .modal-footer {
    .modal-footer-buttons {
      margin-left: auto;
    }
  }
  .error,
  .success {
    font-size: 14px;
  }
  .error {
    margin-block: 8px 24px;
    color: #fa4d56;
  }
  .success {
    text-align: center;
    margin-block: 8px 24px;
    color: #42be65;
  }
`;

const GenerateQRorBarCode: FC<{ value: string; id: string }> = ({ value, id }) => {
  const codeWriter = new BrowserQRCodeSvgWriter();
  const svgElement = codeWriter.write(value, 400, 400);
  svgElement.setAttribute('shape-rendering', 'crispEdges');
  const svgHtml = svgElement.outerHTML;

  return <div dangerouslySetInnerHTML={{ __html: svgHtml }} id={id}></div>;
};

type QRGeneratorState = {
  isModifying: boolean;
  error: boolean;
  success: boolean;
  newQrData: string;
  reason: string;
};

export const QRGenerator: FC<CommonOverlayProps<Props>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { id, title, onPrimary, data, primaryText, selectedObject },
}) => {
  const dispatch = useDispatch();
  const [modifyMeta, setModifyMeta] = useState<QRGeneratorState>({
    isModifying: false,
    error: false,
    success: false,
    newQrData: '',
    reason: '',
  });
  const { isModifying, error, success, newQrData, reason } = modifyMeta;

  const onUpdate = () => {
    if (newQrData) {
      dispatch(
        editQrData({
          objectId: selectedObject?.id,
          objectTypeId: selectedObject?.objectType?.id,
          data: newQrData,
          reason,
        }),
      );
      closeAllOverlays();
    }
  };

  return (
    <Wrapper>
      <BaseModal
        title={title}
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        primaryText="Save"
        secondaryText="Cancel"
        onSecondary={closeOverlay}
        onPrimary={onUpdate}
        disabledPrimary={!reason}
        showPrimary={isModifying && success}
        showSecondary={isModifying && success}
        showFooter={!error}
        modalFooterOptions={
          isModifying ? undefined : (
            <Button
              onClick={() => {
                onPrimary();
                closeOverlay();
              }}
              variant="secondary"
              color="blue"
            >
              {primaryText}
            </Button>
          )
        }
      >
        {isModifying && error && (
          <div className="error">This is not a valid QR Code. Kindly retry!</div>
        )}
        <UploadButton setModifyMeta={setModifyMeta} />
        {isModifying ? (
          <>
            {newQrData && success && (
              <>
                <QRGeneratorWrapper>
                  <GenerateQRorBarCode value={newQrData} id={id} />
                </QRGeneratorWrapper>
                <div className="success">New QR Code has been uploaded successfully</div>
                <Textarea
                  label="Reason for new QR"
                  placeholder="Enter the reason"
                  rows={2}
                  onChange={({ value }: any) => {
                    const regex = nonEmptyStringRegex;
                    if (regex.test(value)) {
                      setModifyMeta((prev) => ({
                        ...prev,
                        reason: value,
                      }));
                    } else {
                      setModifyMeta((prev) => ({
                        ...prev,
                        reason: '',
                      }));
                    }
                  }}
                />
              </>
            )}
          </>
        ) : (
          <QRGeneratorWrapper>
            {typeof data === 'string' && <GenerateQRorBarCode value={data} id={id} />}
          </QRGeneratorWrapper>
        )}
      </BaseModal>
    </Wrapper>
  );
};

const UploadButton: FC<{
  setModifyMeta: React.Dispatch<React.SetStateAction<QRGeneratorState>>;
}> = ({ setModifyMeta }) => {
  const dispatch = useDispatch();
  const fileRef = useRef<HTMLInputElement | null>(null);

  const onDecode = (data?: string) => {
    if (data) {
      setModifyMeta((prev) => ({
        ...prev,
        newQrData: data,
        success: true,
      }));
    } else {
      setModifyMeta((prev) => ({
        ...prev,
        error: true,
      }));
    }
  };

  const inputChangeHandler = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const imageUrl = URL.createObjectURL(file);
      const codeReader = new BrowserMultiFormatReader();

      codeReader
        .decodeFromImage(undefined, imageUrl)
        .then((result) => {
          onDecode(result.getText());
        })
        .catch(() => {
          onDecode();
        });
    }
  };

  return (
    <>
      <input
        type="file"
        id="file"
        accept={'image/*'}
        ref={fileRef}
        style={{ display: 'none' }}
        onChange={inputChangeHandler}
      />
      {checkPermission(['ontology', 'editObject']) && (
        <Select
          options={[
            {
              label: 'Upload a new one',
              value: 'upload',
            },
            {
              label: 'Scan',
              value: 'scan',
            },
          ]}
          onChange={(option: any) => {
            setModifyMeta({
              isModifying: true,
              error: false,
              success: false,
              newQrData: '',
              reason: '',
            });
            if (option.value === 'upload') {
              fileRef.current?.value && (fileRef.current.value = '');
              fileRef.current?.click();
            } else if (option.value === 'scan') {
              dispatch(
                openOverlayAction({
                  type: OverlayNames.QR_SCANNER,
                  props: {
                    onSuccess: onDecode,
                    hideUploadFromGallery: true,
                    onClose: () => {
                      setModifyMeta((prev) => ({
                        ...prev,
                        isModifying: false,
                      }));
                    },
                  },
                }),
              );
            }
          }}
          placeholder="Select From Options"
          label="Modify QR Code"
        />
      )}
    </>
  );
};
