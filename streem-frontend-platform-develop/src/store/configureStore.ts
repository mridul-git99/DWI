import { Overlay, OverlayContainerAction } from '#components/OverlayContainer/types';
import { authInitialState } from '#views/Auth/reducer';
import { AuthAction } from '#views/Auth/types';
import { AnyAction, applyMiddleware, compose, createStore, Middleware } from 'redux';
import { createTransform, persistReducer, persistStore } from 'redux-persist';
import storage from 'redux-persist/lib/storage';
import createSagaMiddleware from 'redux-saga';
import { rootReducer } from './rootReducer';
import { rootSaga } from './rootSaga';
import { RootState } from './types';
import { isInDevelopment } from '#utils/constants';

const isPageReloaded = () => {
  if (isInDevelopment) {
    return true;
  } else {
    if (location?.pathname.includes('/sso')) {
      return true;
    } else if (window.performance.getEntriesByType) {
      return window.performance.getEntriesByType('navigation')[0].type === 'reload';
    }
  }
};

const sagaMiddleware = createSagaMiddleware();
const persistConfig = {
  key: 'root',
  storage,
  whitelist: ['auth', 'auditLogFilters', 'facilityWiseConstants'],
  transforms: [
    createTransform(
      (inboundState: RootState, key) => {
        switch (key) {
          case 'auth':
            return {
              ...inboundState,
              fetchingUseCaseList: true,
              error: undefined,
              email: undefined,
              token: undefined,
            };
          default:
            return inboundState;
        }
      },
      (outboundState: RootState, key) => {
        const pageReloaded = isPageReloaded();
        const keepPersistedData = localStorage.getItem('keepPersistedData');
        switch (key) {
          case 'auth':
            return pageReloaded || keepPersistedData ? outboundState : authInitialState;
          default:
            return outboundState;
        }
      },
    ),
  ],
};

export const persistedReducer = persistReducer(persistConfig, rootReducer);

let onIdleRequests: AnyAction[] = [];
let previousIdle = true;

const handleOnIdle: Middleware = (store) => (next) => (action: AnyAction) => {
  const {
    auth: { isIdle, isLoggedIn },
  } = store.getState();

  if (isIdle) {
    if (
      ![
        AuthAction.TRIGGER_PERSIST,
        AuthAction.RE_LOGIN,
        AuthAction.LOGOUT,
        AuthAction.LOGOUT_SUCCESS,
        OverlayContainerAction.OPEN_OVERLAY,
        OverlayContainerAction.CLOSE_ALL_OVERLAY,
      ].includes(action.type) &&
      !action['@@redux-saga/SAGA_ACTION']
    ) {
      previousIdle = isIdle;
      onIdleRequests.push(action);
      return false;
    }
  } else {
    if (previousIdle && isLoggedIn) {
      previousIdle = false;
      onIdleRequests.forEach((action) => store.dispatch(action));
      onIdleRequests = [];
    }
  }

  previousIdle = isIdle;
  next(action);
};

export const configureStore = (
  initialState: Partial<RootState>,
  additionalMiddleware: Middleware[] = [],
) => {
  const middlewares = [handleOnIdle, sagaMiddleware];

  const composeEnhancers =
    typeof window === 'object' && (window as any).__REDUX_DEVTOOLS_EXTENSION_COMPOSE__
      ? (window as any).__REDUX_DEVTOOLS_EXTENSION_COMPOSE__({
          actionSanitizer: (action: AnyAction) =>
            action.type === '@@overlay/Container/OPEN_OVERLAY'
              ? {
                  ...action,
                  payload: { ...action.payload, popOverAnchorEl: '<<EVENT>>' },
                }
              : action,
          stateSanitizer: (state: RootState) =>
            state.overlayContainer
              ? {
                  ...state,
                  overlayContainer: state.overlayContainer.currentOverlays.map(
                    (overlay: Overlay) => ({
                      ...overlay,
                      popOverAnchorEl: '<<EVENT>>',
                    }),
                  ),
                }
              : state,
        })
      : compose;

  const store = createStore(
    persistedReducer,
    initialState,
    composeEnhancers(applyMiddleware(...additionalMiddleware, ...middlewares)),
  );

  const persistor = persistStore(store);

  sagaMiddleware.run(rootSaga);

  return { store, persistor };
};
