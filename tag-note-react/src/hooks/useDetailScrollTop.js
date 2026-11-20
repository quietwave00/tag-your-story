import { useLayoutEffect } from 'react';

export default function useDetailScrollTop(detailKey) {
  useLayoutEffect(() => {
    window.scrollTo(0, 0);
  }, [detailKey]);
}
