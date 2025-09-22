import { RouteComponentProps, Router } from '@reach/router';
import React, { FC } from 'react';
import OntologyContent from './OntologyContent';
import ObjectTypesContent from './ObjectTypes';
import ObjectsContent from './Objects';
import AddObjectType from './ObjectTypes/Components/index';
import AddQrCodeParser from './QrCodeParser';
import { isFeatureAllowed } from '#services/uiPermissions';

const OntologyView: FC<RouteComponentProps> = () => (
  <Router>
    <OntologyContent path="/*" />
    <AddObjectType path="object-types/add" />
    <AddObjectType path="object-types/edit/:id" />
    <ObjectTypesContent path="object-types/:id" />
    {isFeatureAllowed('createObjectFromQR') && (
      <AddQrCodeParser path="object-types/:objectTypeId/parser/:id" />
    )}
    <ObjectsContent path="object-types/:objectTypeId/objects/:id" />
  </Router>
);

export default OntologyView;
