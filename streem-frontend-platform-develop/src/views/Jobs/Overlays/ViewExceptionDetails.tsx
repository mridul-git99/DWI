import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC } from 'react';
import { ApproverView } from '#views/Job/components/Task/Parameters/Exceptions/ApproverView';
import { getExceptionParameter } from '#utils/parameterUtils';

type TViewExceptionDetailsProps = {
  parameter: any;
  jobId: string;
};

const ViewExceptionDetails: FC<CommonOverlayProps<TViewExceptionDetailsProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { parameter, jobId },
}) => {
  if (!parameter) {
    return null;
  }

  return (
    <BaseModal
      closeAllModals={closeAllOverlays}
      closeModal={closeOverlay}
      title={'View Exception Details'}
      showFooter={false}
    >
      {parameter.validations.map((validation: any) => {
        const _parameter = getExceptionParameter(parameter, validation.ruleId);

        if (!_parameter.response.exception) {
          return null;
        }

        return (
          <ApproverView
            key={_parameter.id}
            parameter={_parameter}
            isReadOnly={true}
            jobId={jobId}
          />
        );
      })}
    </BaseModal>
  );
};

export default ViewExceptionDetails;
