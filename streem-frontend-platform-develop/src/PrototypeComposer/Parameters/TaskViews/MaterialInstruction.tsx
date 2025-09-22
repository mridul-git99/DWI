import { ImageAuth } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import { ImageOutlined } from '@material-ui/icons';
import React, { FC } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const MaterialInstructionTaskViewWrapper = styled.div`
  .list {
    &-container {
      margin: 0;
      padding: 0;
      counter-reset: item;
      list-style-type: none;
    }

    &-item {
      list-style-position: inside;
      margin-bottom: 8px;
      display: flex;
      align-items: center;
      border-bottom: 1px solid #f4f4f4;

      :last-of-type {
        margin-bottom: 0;
        border-bottom: none;
      }

      &::before {
        color: #000000;
        content: counter(item) ' ';
        counter-increment: item;
        font-size: 14px;
        margin-right: 12px;
      }

      &-image {
        align-items: center;
        background-color: #f4f4f4;
        cursor: pointer;
        display: flex;
        height: 40px;
        justify-content: center;
        margin-right: 12px;
        width: 40px;
      }

      > input[type='text'] {
        flex: 1;
      }

      &-quantity {
        align-items: center;
        display: flex;
        margin-left: 12px;

        > .icon.disabled {
          cursor: not-allowed;
        }

        .quantity {
          line-height: 1.15;
          padding: 13px 4px;
        }
      }

      .name {
        flex: 1;
      }

      > .icon {
        margin-left: 12px;
      }
    }
  }
`;

const MaterialInstructionTaskView: FC<Pick<ParameterProps, 'parameter'>> = ({ parameter }) => {
  const dispatch = useDispatch();
  return (
    <MaterialInstructionTaskViewWrapper>
      <ol className="list-container">
        {parameter.data.map((el: any, index: number) => (
          <li className="list-item" key={index}>
            {el.link ? (
              <div>
                <ImageAuth
                  src={el.link}
                  className="list-item-image"
                  onClick={() =>
                    dispatch(
                      openOverlayAction({
                        type: OverlayNames.TASK_MEDIA,
                        props: { mediaDetails: el, disableDescInput: true },
                      }),
                    )
                  }
                />
              </div>
            ) : (
              <div className="list-item-image">
                <ImageOutlined className="icon" />
              </div>
            )}

            <span className="name">{el.name}</span>

            <div className="list-item-quantity">
              <span className="quantity">
                {el.quantity === 0 ? null : el.quantity.toString().padStart(2, '0')}
              </span>
            </div>
          </li>
        ))}
      </ol>
    </MaterialInstructionTaskViewWrapper>
  );
};

export default MaterialInstructionTaskView;
