import { actionSpreader } from '#store/helpers';
import { OverlayContainerAction, Overlay, OverlayNames } from './types';

export const openOverlayAction = (params: Overlay) =>
  actionSpreader(OverlayContainerAction.OPEN_OVERLAY, {
    type: params.type,
    props: params.props,
    popOverAnchorEl: params.popOverAnchorEl,
  });

export const closeOverlayAction = (type: OverlayNames) =>
  actionSpreader(OverlayContainerAction.CLOSE_OVERLAY, { type });

export const updatePropsAction = (type: OverlayNames, props: Overlay['props']) =>
  actionSpreader(OverlayContainerAction.UPDATE_PROPS, { type, props });

export const closeAllOverlayAction = () => actionSpreader(OverlayContainerAction.CLOSE_ALL_OVERLAY);
