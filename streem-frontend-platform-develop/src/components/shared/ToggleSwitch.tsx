import React, { FC } from 'react';
import Switch, { ReactSwitchProps } from 'react-switch';
import styled from 'styled-components';

const Wrapper = styled.div.attrs({
  className: 'toggle-switch',
})`
  align-items: center;
  display: flex;

  .label {
    color: #000000;
    font-size: 14px;
    margin-left: 8px;
  }
`;

type Props = {
  checkedIcon?: false | JSX.Element;
  height?: number;
  offColor?: string;
  offHandleColor?: string;
  offLabel: string;
  onColor?: string;
  onHandleColor?: string;
  onLabel: string;
  onChange: (isChecked: boolean) => void;
  uncheckedIcon?: false | JSX.Element;
  checked?: boolean;
  width?: number;
} & ReactSwitchProps;

const ToggleSwitch: FC<Props> = ({
  checkedIcon = undefined,
  height = 16,
  offColor = '#dadada',
  offHandleColor = '#ffffff',
  offLabel,
  onColor = '#1d84ff',
  onHandleColor = '#ffffff',
  onLabel,
  onChange,
  checked = false,
  width = 32,
  ...rest
}) => {
  return (
    <Wrapper>
      <Switch
        activeBoxShadow=""
        checked={checked}
        checkedIcon={typeof checkedIcon === 'undefined' ? undefined : checkedIcon}
        handleDiameter={height - 6}
        height={height}
        offColor={offColor}
        offHandleColor={offHandleColor}
        onColor={onColor}
        onHandleColor={onHandleColor}
        onChange={() => {
          onChange(!checked);
        }}
        uncheckedIcon={false}
        width={width}
        {...rest}
      />
      <label className="label">{checked ? onLabel : offLabel}</label>
    </Wrapper>
  );
};

export default ToggleSwitch;
