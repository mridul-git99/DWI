import { Checkbox } from '#components';
import { Selections } from '#types';
import { jobActions } from '#views/Job/jobStore';
import { Close } from '@material-ui/icons';
import { get } from 'lodash';
import React, { FC, useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { ParameterProps } from '../Parameter';
import { Wrapper } from './styles';

const ChecklistParameter: FC<ParameterProps> = ({ parameter }) => {
  const metaInfo = useRef<{
    shouldCallApi?: boolean;
  }>({});
  const dispatch = useDispatch();

  const [selectedOptions, setSelectedOptions] = useState({});

  useEffect(() => {
    if (parameter?.response?.choices) {
      setSelectedOptions(parameter?.response?.choices);
    } else {
      const checklistOptions = parameter?.data?.reduce((acc, option) => {
        acc[option.id] = Selections.NOT_SELECTED;
        return acc;
      }, {});
      setSelectedOptions(checklistOptions);
    }
  }, [parameter?.response?.audit?.modifiedAt]);

  useEffect(() => {
    if (metaInfo.current?.shouldCallApi) {
      metaInfo.current.shouldCallApi = false;
      if (selectedOptions) {
        const data = parameter.data.map((d: any) => {
          return {
            ...d,
            state: get(selectedOptions, d.id, Selections.NOT_SELECTED),
          };
        });

        dispatch(
          jobActions.executeParameter({
            parameter: {
              ...parameter,
              data,
            },
          }),
        );
      }
    }
  }, [selectedOptions]);

  const handleExecution = (id: string, choice: Selections) => {
    metaInfo.current.shouldCallApi = true;
    setSelectedOptions((prevChoices) => ({
      ...prevChoices,
      [id]: choice,
    }));
  };

  return (
    <Wrapper>
      <ul className="list-container" data-id={parameter.id} data-type={parameter.type}>
        {parameter.data.map((el, index) => {
          const isItemSelected = get(selectedOptions, el.id) === Selections.SELECTED;

          return (
            <li key={index} className="list-item">
              <div
                className="item-content"
                onClick={(e) => {
                  e.stopPropagation();
                  e.preventDefault();
                  handleExecution(
                    el.id,
                    isItemSelected ? Selections.NOT_SELECTED : Selections.SELECTED,
                  );
                }}
              >
                <Checkbox checked={isItemSelected} label={el.name} />
              </div>

              <Close className="icon" />
            </li>
          );
        })}
      </ul>
    </Wrapper>
  );
};

export default ChecklistParameter;
