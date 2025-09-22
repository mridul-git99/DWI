import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { ImageOutlined } from '@material-ui/icons';
import React, { FC } from 'react';
import { useDispatch } from 'react-redux';
import { ParameterProps } from '../Parameter';
import { Wrapper } from './styles';
import { ImageAuth } from '#components';

const MaterialParameter: FC<ParameterProps> = ({ parameter }) => {
  const dispatch = useDispatch();

  return (
    <Wrapper>
      <ol className="list-container" data-id={parameter.id} data-type={parameter.type}>
        {parameter.data.map((el, index) => (
          <li className="list-item" key={index}>
            {el.link ? (
              <div>
                <ImageAuth
                  key={el.id}
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
    </Wrapper>
  );
};

export default MaterialParameter;
