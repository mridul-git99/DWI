import { RefObject, useEffect, useMemo } from 'react';
import { throttle, noop, DebouncedFunc } from 'lodash';

export type useThrottledOnScrollProps = {
  scrollTarget: RefObject<HTMLDivElement>;
  callback?: () => void;
  delay?: number;
};

const useThrottledOnScroll = ({
  scrollTarget,
  callback = noop,
  delay = 50,
}: useThrottledOnScrollProps) => {
  const throttledCallback = useMemo(
    () => (callback ? throttle(callback, delay) : noop),
    [callback, delay],
  );

  useEffect(() => {
    if (throttledCallback === noop) {
      return undefined;
    }
    if (scrollTarget?.current) {
      scrollTarget.current?.addEventListener('scroll', throttledCallback, {
        passive: true,
      });
    }

    return () => {
      if (scrollTarget?.current) {
        scrollTarget.current.removeEventListener('scroll', throttledCallback);
        (throttledCallback as DebouncedFunc<any>).cancel();
      }
    };
  }, [throttledCallback]);
};

export default useThrottledOnScroll;
