import { CircularProgress } from '@material-ui/core';
import React, { ReactElement, useEffect, useRef, useState } from 'react';
import versionJson from './version.json';
import { isInDevelopment } from '#utils/constants';

export const AppVersionCheck = ({ children }: { children: ReactElement<any, any> }) => {
  const currentCommit = versionJson.commit;
  const interval = useRef<NodeJS.Timeout | null>(null);
  const [cacheStatus, setCacheStatus] = useState({
    loading: true,
    isLatestVersion: false,
  });

  useEffect(() => {
    if (!isInDevelopment) {
      checkCacheStatus();
    }
    return () => {
      if (interval.current) {
        clearTimeout(interval.current);
      }
    };
  }, []);

  const checkCacheStatus = async () => {
    if (interval.current) {
      clearTimeout(interval.current);
    }
    try {
      const res = await fetch(`/version.json?time=${new Date().getTime()}`);
      const { commit: versionCommit } = await res.json();
      const forceRefresh = currentCommit !== versionCommit;
      if (forceRefresh) {
        setCacheStatus({
          loading: false,
          isLatestVersion: false,
        });
      } else {
        setCacheStatus({
          loading: false,
          isLatestVersion: true,
        });
      }
    } catch (error) {
      setCacheStatus({
        loading: false,
        isLatestVersion: true,
      });
    }
    interval.current = setTimeout(checkCacheStatus, 10000);
  };

  const refreshCacheAndReload = async () => {
    try {
      localStorage.clear();
      if (window?.caches) {
        const { caches } = window;
        const cacheNames = await caches.keys();
        for (const cacheName of cacheNames) {
          caches.delete(cacheName);
        }
        window.location.reload();
      }
    } catch (error) {
      console.error('An error occurred while deleting the cache.', true);
    }
  };

  if (isInDevelopment) {
    return children;
  } else {
    if (cacheStatus.loading) {
      return (
        <div
          style={{
            position: 'fixed',
            width: '100%',
            height: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <CircularProgress />
        </div>
      );
    }

    if (!cacheStatus.loading && !cacheStatus.isLatestVersion) {
      refreshCacheAndReload();
      return null;
    }
    return children;
  }
};
