import { BaseModal, LoadingContainer } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { fetchObjectType, resetOntology, setActiveObject } from '#views/Ontology/actions';
import ObjectView from '#views/Ontology/Objects/ObjectView';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { getParametersInfo } from '../utils';

const Wrapper = styled.div`
  .modal-body {
    display: flex;
    flex-direction: column;
    font-size: 16px;
    padding: 0 !important;
    max-height: 80dvh;

    form {
      margin: 0;
      max-width: unset;
    }
  }
`;

const getParameterIdsFromConfiguration = (configuration: any) => {
  const parameterIds: string[] = [];
  configuration.forEach((item) => {
    if (item.parameterId) {
      parameterIds.push(item.parameterId);
    }
  });
  return parameterIds;
};

const AutomationActionModal: FC<
  CommonOverlayProps<{
    objectTypeId: string;
    actionType: string;
    onDone: (createObjectData: any) => void;
    setLoadingState: (loading: boolean) => void;
    createObjectAutomationDetail: any;
  }>
> = ({
  closeAllOverlays,
  closeOverlay,
  props: { objectTypeId, onDone, setLoadingState, createObjectAutomationDetail },
}) => {
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(true);
  const [allRefParams, setAllRefParams] = useState({});

  const active = useTypedSelector((state) => state.ontology.objectTypes.active);
  const activeLoading = useTypedSelector((state) => state.ontology.objectTypes.activeLoading);
  const selectedObject = useTypedSelector((state) => state.ontology.objects.active);
  const jobId = useTypedSelector((state) => state.job.id);

  useEffect(() => {
    dispatch(setActiveObject());
    if (objectTypeId) {
      dispatch(fetchObjectType(objectTypeId));
    }

    return () => {
      dispatch(resetOntology(['objectTypes', 'activeLoading']));
    };
  }, []);

  const fetchAutomationParameters = async (parameterIds: string[]) => {
    const data: any = await getParametersInfo(jobId, parameterIds);
    setAllRefParams(data);
    setLoading(false);
  };

  useEffect(() => {
    const configurationParameterIds = getParameterIdsFromConfiguration(
      createObjectAutomationDetail.actionDetails?.configuration || [],
    );
    if (configurationParameterIds?.length > 0) {
      fetchAutomationParameters(configurationParameterIds);
    } else {
      setLoading(false);
    }
  }, [createObjectAutomationDetail]);

  const onCloseModal = () => {
    closeOverlay();
    setLoadingState(false);
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={onCloseModal}
        showFooter={false}
        title="Automation Action"
      >
        <LoadingContainer
          loading={activeLoading || !active || loading}
          component={
            <ObjectView
              label="Automation Action"
              values={{
                goBack: false,
                objectTypeId,
                onCancel: onCloseModal,
                onDone,
                id: selectedObject ? selectedObject.id : 'new',
                readOnly: !!selectedObject,
                createObjectAutomationDetail: createObjectAutomationDetail,
                allRefParams,
              }}
            />
          }
        />
      </BaseModal>
    </Wrapper>
  );
};

export default AutomationActionModal;
