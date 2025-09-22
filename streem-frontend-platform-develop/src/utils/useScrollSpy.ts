import { useState, useCallback, RefObject, MutableRefObject } from 'react';
import useThrottledOnScroll from './useThrottledOnScroll';

const useScrollSpy = ({
  itemsRef,
  scrollTarget,
  contentTarget,
}: {
  itemsRef: MutableRefObject<HTMLDivElement[]>;
  scrollTarget: RefObject<HTMLDivElement>;
  contentTarget: RefObject<HTMLDivElement>;
}) => {
  const [activeState, setActiveState] = useState<number | null>(0);

  const findActiveIndex = useCallback(() => {
    if (scrollTarget?.current && contentTarget?.current && itemsRef?.current) {
      let active = 0;
      for (let i = itemsRef.current.length - 1; i >= 0; i -= 1) {
        const item = itemsRef.current[i];

        // Diving by 12 helps in calculating active index of elements having offsetTop small then Scroll Target clientHeight.
        if (
          item?.offsetTop <
          scrollTarget.current.scrollTop + contentTarget.current.clientHeight / 12
        ) {
          active = i;
          break;
        }
      }

      if (activeState !== active) {
        setActiveState(active);
      }
    }
  }, [activeState]);

  useThrottledOnScroll({
    callback: itemsRef.current.length > 0 ? findActiveIndex : undefined,
    scrollTarget,
  });

  return activeState;
};

export default useScrollSpy;
