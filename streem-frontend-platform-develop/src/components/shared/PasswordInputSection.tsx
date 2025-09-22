import { TextInput } from '#components';
import { InputTypes } from '#utils/globalTypes';
import { VisibilityOutlined } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useCallback, useState } from 'react';

interface IPasswordInputSectionProps {
  handlePasswordChange: (value: string) => void;
}

const PasswordInputSection: FC<IPasswordInputSectionProps> = ({ handlePasswordChange }) => {
  const [isPasswordInputType, setIsPasswordInputType] = useState(true);

  const PasswordAfterIcon = useCallback(
    () => (
      <VisibilityOutlined
        onClick={() => setIsPasswordInputType((prevState) => !prevState)}
        style={{ color: isPasswordInputType ? '#999' : '#1d84ff' }}
      />
    ),
    [isPasswordInputType, setIsPasswordInputType],
  );

  return (
    <>
      <TextInput
        type={isPasswordInputType ? InputTypes.PASSWORD : InputTypes.SINGLE_LINE}
        placeholder="Enter your Password"
        id="password"
        name="password"
        afterElementWithoutError={true}
        AfterElement={PasswordAfterIcon}
        onChange={debounce(({ value }) => {
          handlePasswordChange(value);
        }, 500)}
        autoComplete="new-password"
      />
    </>
  );
};

export default PasswordInputSection;
