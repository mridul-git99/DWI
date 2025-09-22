import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { apiTaskDependencies } from '#utils/apiUrls';
import { request } from '#utils/request';
import React, { FC, useEffect, useState } from 'react';
import styled from 'styled-components';

export interface ViewTaskDependencyModalProps {
  taskId: string;
  taskName: string;
  hasPrerequisites: boolean;
}

const Wrapper = styled.div.attrs({})`
  .modal {
    width: 480px !important;

    .modal-body {
      padding: 24px !important;
      overflow: auto;
      font-size: 14px;
      color: #525252;
      display: flex;
      flex-direction: column;

      .title {
        margin-bottom: 8px;
      }

      .stage {
        padding: 8px 0px;
      }

      .task {
        padding: 8px 8px 8px 24px;
      }

      p {
        margin: 0;
      }

      span {
        font-weight: 700;
      }
    }

    .modal-footer {
      flex-direction: row-reverse !important;
    }
  }
`;

const ViewTaskDependency: FC<CommonOverlayProps<ViewTaskDependencyModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { taskId, taskName, hasPrerequisites },
}) => {
  const [selectedTasks, setSelectedTasks] = useState([]);

  const fetchDependencies = async () => {
    const { data } = await request('GET', apiTaskDependencies(taskId, 'details'));
    if (data) {
      setSelectedTasks(data.stages);
    }
  };

  useEffect(() => {
    if (hasPrerequisites) fetchDependencies();
  }, []);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title="View Dependencies"
        closeModal={closeOverlay}
        showPrimary={false}
        secondaryText="Close"
        onSecondary={closeOverlay}
      >
        {hasPrerequisites && selectedTasks.length > 0 ? (
          <>
            <p className="title">
              Tasks that need to be executed before <span>{taskName}</span>
            </p>
            {selectedTasks.map((stage: any) => (
              <div key={stage.id}>
                <p className="stage">
                  Stage {stage.orderTree} : {stage.name}
                </p>
                {stage.tasks.map((task: any) => (
                  <p key={task.id} className="task">
                    Task {stage.orderTree}.{task.orderTree} : {task.name}
                  </p>
                ))}
              </div>
            ))}
          </>
        ) : (
          <div>
            <p>
              <span>{taskName}</span> has no dependencies.
            </p>
          </div>
        )}
      </BaseModal>
    </Wrapper>
  );
};

export default ViewTaskDependency;
