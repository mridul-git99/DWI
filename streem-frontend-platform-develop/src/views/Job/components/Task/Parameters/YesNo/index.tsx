import { closeOverlayAction, openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { Selections } from '#types';
import { jobActions } from '#views/Job/jobStore';
import React, { FC, useCallback, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { ParameterProps } from '../Parameter';
import { Wrapper } from './styles';
import { Textarea } from '#components';

const YesNoParameter: FC<ParameterProps> = ({
  parameter,
  isCorrectingError,
  setCorrectedParameterValues,
}) => {
  const dispatch = useDispatch();

  const getSelectedIdByChoices = useCallback((data: any[]) => {
    return data
      ? Object.entries(data).reduce(
          (acc, [choiceId, choiceValue]) => (choiceValue === Selections.SELECTED ? choiceId : acc),
          '',
        )
      : undefined;
  }, []);

  const [selectedId, setSelectedId] = useState(getSelectedIdByChoices(parameter.response.choices));

  const dispatchActions = (data: any, reason: string = '') => {
    if (isCorrectingError) {
      if (setCorrectedParameterValues) {
        const newChoice = data.reduce((acc, el) => {
          acc[el.id] = el.state;
          return acc;
        }, {});
        setCorrectedParameterValues((prev) => ({ ...prev, newChoice }));
        setSelectedId(Object.keys(newChoice).find((key) => newChoice[key] === 'SELECTED'));
      }
    } else {
      dispatch(
        jobActions.executeParameter({
          parameter: {
            ...parameter,
            data,
          },
          reason: reason,
        }),
      );
    }
  };

  useEffect(() => {
    setSelectedId(getSelectedIdByChoices(parameter.response.choices));
  }, [parameter.response.audit.modifiedAt]);

  return (
    <Wrapper data-id={parameter.id} data-type={parameter.type}>
      <div className="buttons-container">
        {[...parameter.data]
          .sort((a, b) => (a.type > b.type ? -1 : 1))
          .map((el, index) => {
            const isSelected = selectedId === el.id;
            const isDisabled = isCorrectingError && !setCorrectedParameterValues;
            const classNames = isSelected
              ? isDisabled
                ? 'disabled filled'
                : 'filled'
              : isDisabled
              ? 'disabled'
              : '';

            return (
              <div key={index} className="button-item">
                <button
                  className={classNames}
                  onClick={() => {
                    const data = parameter.data.map((d: any) => ({
                      ...d,
                      state: d.id === el.id ? Selections.SELECTED : Selections.NOT_SELECTED,
                    }));
                    if (el.type === 'no') {
                      if (isCorrectingError) {
                        dispatchActions(data);
                      } else {
                        dispatch(
                          openOverlayAction({
                            type: OverlayNames.REASON_MODAL,
                            props: {
                              modalTitle: 'State your Reason',
                              modalDesc: 'You need to submit a reason for No to proceed',
                              onSubmitHandler: (reason: string) => {
                                const data = parameter.data.map((d: any) => ({
                                  ...d,
                                  state:
                                    d.id === el.id ? Selections.SELECTED : Selections.NOT_SELECTED,
                                }));
                                dispatchActions(data, reason);
                                dispatch(closeOverlayAction(OverlayNames.REASON_MODAL));
                              },
                            },
                          }),
                        );
                      }
                    } else {
                      dispatchActions(data);
                    }
                  }}
                >
                  {el.name}
                </button>
              </div>
            );
          })}
      </div>
      {parameter.response?.reason && (
        <div className="decline-reason">
          <Textarea label="Reason" value={parameter.response.reason} rows={4} disabled={true} />
        </div>
      )}
    </Wrapper>
  );
};

export default YesNoParameter;
