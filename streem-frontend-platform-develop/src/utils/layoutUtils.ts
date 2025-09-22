import { Orientation } from '#components/OverlayContainer/DetectOrientation';

export const detectMob = () => {
  const toMatch = [
    /Android/i,
    /webOS/i,
    /iPhone/i,
    /iPad/i,
    /iPod/i,
    /BlackBerry/i,
    /Windows Phone/i,
  ];

  return toMatch.some((toMatchItem) => {
    return navigator.userAgent.match(toMatchItem);
  });
};

export const getOrientation = (): Orientation => {
  if (window.screen.orientation) {
    const screenOrientation = window.screen.orientation.type;
    if (screenOrientation.startsWith('landscape') || screenOrientation === 'landscape-primary') {
      return 'landscape';
    } else if (
      screenOrientation.startsWith('portrait') ||
      screenOrientation === 'portrait-primary'
    ) {
      return 'portrait';
    }
  }
  // Fallback for browsers that don't support screen.orientation API
  return window.matchMedia('(orientation: landscape)').matches ? 'landscape' : 'portrait';
};
