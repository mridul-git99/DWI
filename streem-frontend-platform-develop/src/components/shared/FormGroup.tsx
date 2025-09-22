import { Role, Select, TextInput, Textarea } from '#components';
import { InputTypes } from '#utils/globalTypes';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Radio, { RadioProps } from '@material-ui/core/Radio';
import RadioGroup from '@material-ui/core/RadioGroup';
import { makeStyles } from '@material-ui/core/styles';
import CheckCircle from '@material-ui/icons/CheckCircle';
import React, { forwardRef } from 'react';
import styled from 'styled-components';
import { RoleProps } from './Role';

type FormInput = {
  type: InputTypes;
  props: Record<any, any> & { id: string };
};

export type FormGroupProps = {
  inputs: FormInput[];
} & React.HTMLProps<HTMLDivElement>;

const Wrapper = styled.div.attrs({
  className: 'form-group',
})`
  display: flex;
  flex-direction: column;
  padding: 24px 16px;

  > div {
    margin-bottom: 24px;

    :last-child {
      margin-bottom: unset;
    }
  }

  .error-container {
    display: flex;
    flex-wrap: wrap;
    flex-direction: column;
    padding: 0px 2px;
    margin-top: -12px;

    > div {
      display: flex;
      flex: 1;
      font-size: 12px;
      letter-spacing: 0.32px;
      line-height: 16px;
      align-items: center;
      margin-top: 10px;

      :first-child {
        margin-top: 0px;
      }

      .indicator {
        font-size: 20px;
        line-height: 16px;
        margin-right: 9px;
        color: #5aa700;
      }
    }
  }

  .MuiFormControlLabel-label {
    font-family: inherit;
    font-size: 14px;
    font-weight: bold;
    line-height: 1.14;
    letter-spacing: 0.16px;
    color: #333333;
  }

  .radio-desc {
    font-size: 14px;
    line-height: 1.14;
    letter-spacing: 0.16px;
    color: #666666;
    margin: 0 0 24px 24px;
  }

  .optional-badge {
    color: #999999;
    font-size: 12px;
    margin-left: 4px;
  }
`;

const useStyles = makeStyles({
  root: {
    '&:hover': {
      backgroundColor: 'transparent',
    },
  },
  icon: {
    borderRadius: '50%',
    width: 16,
    height: 16,
    boxShadow: 'inset 0 0 0 1px rgba(16,22,26,.2), inset 0 -1px 0 rgba(16,22,26,.1)',
    backgroundColor: '#f5f8fa',
    backgroundImage: 'linear-gradient(180deg,hsla(0,0%,100%,.8),hsla(0,0%,100%,0))',
    '$root.Mui-focusVisible &': {
      outline: '2px auto rgba(19,124,189,.6)',
      outlineOffset: 2,
    },
    'input:hover ~ &': {
      backgroundColor: '#ebf1f5',
    },
    'input:disabled ~ &': {
      boxShadow: 'none',
      background: 'rgba(206,217,224,.5)',
    },
  },
  checkedIcon: {
    borderRadius: '50%',
    width: 16,
    height: 16,
    boxShadow: 'inset 0 0 0 1px rgba(16,22,26,.2), inset 0 -1px 0 rgba(16,22,26,.1)',
    backgroundColor: '#1d84ff',
    backgroundImage: 'linear-gradient(180deg,hsla(0,0%,100%,.1),hsla(0,0%,100%,0))',
    '&:before': {
      display: 'block',
      width: 16,
      height: 16,
      backgroundImage: 'radial-gradient(#fff,#fff 28%,transparent 32%)',
      content: '""',
    },
    '$root.Mui-focusVisible &': {
      outline: '2px auto rgba(19,124,189,.6)',
      outlineOffset: 2,
    },
    'input:hover ~ &': {
      backgroundColor: '#1d84ff',
    },
  },
});

export function StyledRadio(props: RadioProps) {
  const classes = useStyles();

  return (
    <Radio
      className={classes.root}
      disableRipple
      color="default"
      checkedIcon={<span className={classes.checkedIcon} />}
      icon={<span className={classes.icon} />}
      {...props}
    />
  );
}

export const FormGroup = forwardRef(({ inputs, ...rest }: FormGroupProps, ref) => {
  return (
    <Wrapper {...rest} ref={ref}>
      {inputs.map(({ type, props }: FormInput) => {
        const key = `${type}_${props.id}`;
        switch (type) {
          case InputTypes.ERROR_CONTAINER:
            return (
              <div key={key} className="error-container">
                {Object.keys(props?.messages).map(
                  (item): JSX.Element => (
                    <div key={`${item}`}>
                      {props?.errorsTypes && (
                        <CheckCircle
                          className="indicator"
                          style={props.errorsTypes?.includes(item) ? { color: '#bababa' } : {}}
                        />
                      )}
                      {props?.messages?.[item]}
                    </div>
                  ),
                )}
              </div>
            );
          case InputTypes.PASSWORD:
          case InputTypes.SINGLE_LINE:
          case InputTypes.NUMBER:
          case InputTypes.DATE:
          case InputTypes.TIME:
          case InputTypes.DATE_TIME:
            return <TextInput key={key} type={type} {...props} />;
          case InputTypes.MULTI_LINE:
            return <Textarea key={key} {...props} />;
          case InputTypes.SINGLE_SELECT:
          case InputTypes.MULTI_SELECT:
            return <Select key={key} isMulti={type === InputTypes.MULTI_SELECT} {...props} />;
          case InputTypes.ROLE:
            return <Role key={key} {...(props as RoleProps)} />;
          case InputTypes.RADIO:
            return (
              <RadioGroup key={props.groupProps.id} {...props.groupProps}>
                {props.items.map((item: any) => (
                  <>
                    <FormControlLabel control={<StyledRadio />} {...item} />
                    {item?.desc && <span className="radio-desc">{item.desc}</span>}
                  </>
                ))}
              </RadioGroup>
            );
          default:
            return null;
        }
      })}
    </Wrapper>
  );
});
