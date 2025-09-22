import { detectMob, getOrientation } from '#utils/layoutUtils';
import { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { closeOverlayAction, openOverlayAction } from './actions';
import { OverlayNames } from './types';

export type Orientation = 'landscape' | 'portrait';

export const DetectOrientation = () => {
  const dispatch = useDispatch();

  const [orientation, setOrientation] = useState<Orientation>(getOrientation());

  const handleOrientationChange = () => {
    setOrientation(getOrientation());
  };

  useEffect(() => {
    window.addEventListener('orientationchange', handleOrientationChange);
    return () => {
      window.removeEventListener('orientationchange', handleOrientationChange);
    };
  }, []);

  if (orientation === 'landscape' && detectMob()) {
    dispatch(
      openOverlayAction({
        type: OverlayNames.ORIENTATION_MODAL,
      }),
    );
  } else if (orientation === 'portrait' && detectMob()) {
    dispatch(closeOverlayAction(OverlayNames.ORIENTATION_MODAL));
  }

  return null;
};
