import { QuickFilter } from '#views/Inbox/ListView/types';
import { quickFilterToStatusMap } from '#views/Jobs/ListView/types';
import { Props } from 'react-select';
import { STATUS_COLOR_MAP } from './jobMethods';

export const selectStyles: Props['styles'] = {
  container: (styles) => ({ ...styles, minWidth: '85px' }),
  control: (styles, { isDisabled }) => ({
    ...styles,
    border: '1px solid #ccc',
    borderRadius: 'none',
    boxShadow: 'none',
    cursor: isDisabled ? 'not-allowed' : 'pointer',
    padding: '1.7px',
    minHeight: 'auto',
  }),

  multiValueRemove: (styles, { isDisabled }) => ({
    ...styles,
    display: isDisabled ? 'none' : styles.display,
  }),

  option: (styles, { isFocused, isSelected }) => ({
    ...styles,
    backgroundColor: isSelected || isFocused ? '#f4f4f4' : '#ffffff',
    borderBottom: '1px solid #bababa',
    color: '#000000',
    cursor: 'pointer',
    padding: '10px 16px',
    overflowWrap: 'break-word',
  }),

  menu: (styles) => ({
    ...styles,
    zIndex: 3,
    borderRadius: 'none',
    width: 'max-content',
    minWidth: '100%',
    maxWidth: 'calc(100% + calc(250px - 100%))',
  }),

  menuList: (styles) => ({
    ...styles,
    padding: 0,
  }),

  singleValue: (styles) => ({
    ...styles,
    color: 'hsl(0, 0%, 20%)',
  }),

  groupHeading: (styles) => ({
    ...styles,
    color: 'rgba(0,0,0,0.87)',
    fontSize: '80%',
  }),

  menuPortal: (styles) => ({ ...styles, zIndex: 9999 }),

  indicatorSeparator: () => ({ display: 'none' }),
  indicatorsContainer: (styles) => ({ ...styles, padding: '0' }),
  clearIndicator: (styles) => ({ ...styles, padding: '0' }),
  placeholder: (styles) => ({ ...styles, textWrap: 'nowrap' }),
};

export const quickFilterStyles = (quickFilter?: QuickFilter): Props['styles'] => {
  return {
    ...selectStyles,
    singleValue: (styles, props) => ({
      ...selectStyles.singleValue?.(styles, props),
      backgroundColor: quickFilter
        ? STATUS_COLOR_MAP[quickFilterToStatusMap[quickFilter]]?.backgroundColor
        : '#ffffff',
      color: quickFilter ? STATUS_COLOR_MAP[quickFilterToStatusMap[quickFilter]]?.color : '#161616',
      padding: '2px 8px',
      fontSize: '12px',
    }),
  };
};
