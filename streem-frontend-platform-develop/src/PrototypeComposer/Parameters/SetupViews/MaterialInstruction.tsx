import ImageUploadIcon from '#assets/svg/ImageUpload';
import { AddNewItem, ImageAuth, ImageUploadButton, TextInput } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { MediaDetails } from '#PrototypeComposer/Tasks/types';
import { ArrowDropDown, ArrowDropUp, Close } from '@material-ui/icons';
import { debounce, pick } from 'lodash';
import React, { FC } from 'react';
import { useFormContext } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { v4 as uuidv4 } from 'uuid';

const MaterialWrapper = styled.div<{
  isReadOnly: boolean;
}>`
  display: flex;
  flex-direction: column;

  .material-list {
    margin: 16px 0 0 0;
    padding: 0;

    &-item {
      display: flex;
      align-items: center;
      margin-top: 8px;
      gap: 12px;

      .image-wrapper {
        align-items: center;
        background-color: #f4f4f4;
        cursor: pointer;
        display: flex;
        height: max-content;
        justify-content: center;

        .upload-image {
          padding: 8px;
          .icon {
            font-size: 24px;
          }
        }

        .image {
          height: 40px;
          width: 40px;
        }
      }

      .quantity-control {
        align-items: center;
        background-color: #f4f4f4;
        display: flex;
        height: max-content;
        pointer-events: ${({ isReadOnly }) => (isReadOnly ? 'none' : 'auto')};

        span {
          border: 1px solid transparent;
          border-bottom-color: #bababa;
          padding: 10px;
          text-align: center;
          width: 50px;
        }
      }

      #remove-item {
        cursor: pointer;
        font-size: 16px;
      }
    }
  }
`;

const MaterialInstruction: FC<{
  isReadOnly: boolean;
}> = ({ isReadOnly }) => {
  const dispatch = useDispatch();
  const { watch, setValue, setError, trigger } = useFormContext();
  const data = watch('data', []);

  const openMediaModal = (mediaDetails: any, _: any, index: number) => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.TASK_MEDIA,
        props: {
          mediaDetails,
          disableNameInput: true,
          isParameter: true,
          execute: (_data: MediaDetails) => {
            updateInstructionData(
              pick(_data, [
                'mediaId',
                'filename',
                'originalFilename',
                'link',
                'type',
                'description',
              ]),
              index,
            );
          },
        },
      }),
    );
  };

  const validateData = (updatedData: any) => {
    const keysToValidate = [
      'name',
      'mediaId',
      'filename',
      'originalFilename',
      'link',
      'type',
      'quantity',
    ];
    let isValid = false;
    isValid =
      updatedData.length > 0 &&
      updatedData.every((item) => keysToValidate.every((key) => item?.[key]));

    if (!isValid) {
      setError('data', {
        message: 'All fields are mandatory',
      });
    } else {
      trigger();
    }
  };

  const updateInstructionData = (value: any, index: number) => {
    const updated = [...data];
    updated[index] = { ...updated[index], ...value };
    setValue('data', updated, {
      shouldDirty: true,
    });

    validateData(updated);
  };

  return (
    <MaterialWrapper isReadOnly={isReadOnly}>
      <ul className="material-list">
        {data.map((item: any, index: number) => (
          <li className="material-list-item" key={item.id}>
            <div className="image-wrapper">
              {item.link ? (
                <div>
                  <ImageAuth
                    key={item.id}
                    src={item.link}
                    className="image"
                    onClick={() => {
                      openMediaModal(
                        {
                          filename: item.filename,
                          link: item.link,
                          type: item.type,
                          name: item.name,
                          description: item.description,
                        },
                        item,
                        index,
                      );
                    }}
                  />
                </div>
              ) : (
                <ImageUploadButton
                  icon={ImageUploadIcon}
                  acceptedTypes={['image/*', '.png', '.jpg', '.jpeg']}
                  onUploadSuccess={(fileData) => {
                    openMediaModal(
                      {
                        ...fileData,
                        name: item.name || '',
                        description: item.description || '',
                      },
                      item,
                      index,
                    );
                  }}
                  onUploadError={(error) =>
                    console.error('error came in file upload for material item :: ', error)
                  }
                />
              )}
            </div>
            <TextInput
              defaultValue={item.name}
              onChange={debounce(({ value }) => updateInstructionData({ name: value }, index), 500)}
              disabled={isReadOnly}
            />
            <div className="quantity-control">
              <ArrowDropUp
                className="icon"
                onClick={() => {
                  updateInstructionData({ quantity: item.quantity + 1 }, index);
                }}
              />
              <span>{!item?.quantity ? 'Any' : item.quantity.toString().padStart(2, '0')}</span>
              <ArrowDropDown
                className="icon"
                onClick={() => {
                  if (item.quantity > 0) {
                    updateInstructionData({ quantity: item.quantity - 1 }, index);
                  }
                }}
              />
            </div>
            {!isReadOnly && (
              <Close
                id="remove-item"
                onClick={() => {
                  const updatedData = data.filter((_, i) => i !== index);
                  setValue('data', updatedData, {
                    shouldDirty: true,
                  });
                  validateData(updatedData);
                }}
              />
            )}
          </li>
        ))}
      </ul>
      {!isReadOnly && (
        <AddNewItem
          onClick={() => {
            const updatedData = [...data, { id: uuidv4(), name: '', quantity: 0 }];
            setValue('data', updatedData, {
              shouldDirty: true,
            });
            validateData(updatedData);
          }}
        />
      )}
    </MaterialWrapper>
  );
};

export default MaterialInstruction;
