import Inter from '#assets/fonts/inter/inter-variableFont.ttf';
import { CustomRoute, Notification, OverlayContainer } from '#components';
import { MultiTabChecker } from '#components/OverlayContainer/MultiTabChecker';
import '#i18n';
import { configureStore } from '#store';
import { setAuthHeader } from '#utils/axiosClient';
import {
  AuthView,
  FacilitySelectionView,
  HomeView,
  UseCaseSelectionView,
  OpenFileUrl,
} from '#views';
import { SsoView } from '#views/Auth/SsoView';
import { LocationProvider, Router } from '@reach/router';
import { Font } from '@react-pdf/renderer';
import { enableMapSet } from 'immer';
import React, { FC, useEffect, useState } from 'react';
import { Provider } from 'react-redux';
import { PersistGate } from 'redux-persist/integration/react';
import { AppVersionCheck } from './AppVersionCheck';
import GlobalStyles from './styles/GlobalStyles';
import { DetectOrientation } from '#components/OverlayContainer/DetectOrientation';
import { NOTIFICATION_TIMEOUT } from '#utils/constants';
import { QueryParamsProvider } from '#hooks/useQueryParams';
import 'react-datepicker/dist/react-datepicker.css';

Font.register({
  family: 'Inter',
  fonts: [{ src: Inter }],
});

enableMapSet();

const { store, persistor } = configureStore({});
window.store = store;
window.persistor = persistor;

const App: FC = () => {
  const [isLoading, setIsLoading] = useState(true);
  const onBeforeLift = () => {
    const {
      auth: { accessToken },
    } = store.getState();
    if (accessToken) {
      setAuthHeader(accessToken);
    }
    setTimeout(() => {
      setIsLoading(false);
    }, 500);
  };

  useEffect(() => {
    const script = document.createElement('script');

    script.src = 'https://js-eu1.hs-scripts.com/25337116.js';

    script.async = true;

    document.body.appendChild(script);

    return () => {
      document.body.removeChild(script);
    };
  }, []);

  return (
    <AppVersionCheck>
      <Provider store={store}>
        <PersistGate loading={null} persistor={persistor} onBeforeLift={onBeforeLift}>
          <LocationProvider>
            <QueryParamsProvider>
              {!isLoading && (
                <>
                  <MultiTabChecker />
                  <DetectOrientation />
                  <Router style={{ display: 'flex', flex: 1 }} basepath="/">
                    <CustomRoute isProtected={false} as={AuthView} path="auth/*" />
                    <CustomRoute isProtected={false} as={SsoView} path="sso/auth" />
                    <CustomRoute as={FacilitySelectionView} path="facility/selection" />
                    <CustomRoute as={HomeView} path="home" />
                    <CustomRoute as={OpenFileUrl} path="media" />
                    <CustomRoute as={UseCaseSelectionView} path="/*" />
                  </Router>
                  <Notification
                    position="top-right"
                    autoClose={NOTIFICATION_TIMEOUT}
                    hideProgressBar={true}
                    newestOnTop={true}
                    closeOnClick
                    rtl={false}
                    pauseOnFocusLoss
                    draggable
                    pauseOnHover
                  />
                  <OverlayContainer />

                  <GlobalStyles />
                </>
              )}
            </QueryParamsProvider>
          </LocationProvider>
        </PersistGate>
      </Provider>
    </AppVersionCheck>
  );
};

export default App;
