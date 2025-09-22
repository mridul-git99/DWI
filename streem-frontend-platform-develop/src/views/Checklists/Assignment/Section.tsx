import { Checkbox, AssigneeList } from '#components';
import { ArrowDropDown, ArrowRight } from '@material-ui/icons';
import React, { Dispatch, FC, useState } from 'react';
import { Stage } from '#PrototypeComposer/Stages/types';
import { AssignmentSectionWrapper } from '#views/Jobs/Assignment/Section';
import checkPermission from '#services/uiPermissions';

type Props = {
  stage: Stage;
  sectionState: Record<string, boolean>;
  localDispatch: Dispatch<any>;
  isFirst: boolean;
  isAllTaskSelected: boolean;
  isNoTaskSelected: boolean;
  assignedData: Record<string, any>;
};

const Section: FC<Props> = ({
  stage,
  sectionState = {},
  localDispatch,
  isFirst,
  isAllTaskSelected,
  isNoTaskSelected,
  assignedData,
}) => {
  const [isOpen, toggleIsOpen] = useState(isFirst);
  let isAllTaskAssigned = true;
  let isNoTaskAssigned = true;
  const isEditingAllowed = checkPermission(['trainedUsers', 'edit']);

  stage.tasks.forEach((task) => {
    if (assignedData?.[task.id]) {
      isNoTaskAssigned = false;
    } else {
      isAllTaskAssigned = false;
    }
  });

  return (
    <AssignmentSectionWrapper>
      <div className="section-header">
        <div className="icon-wrapper" onClick={() => toggleIsOpen((val) => !val)}>
          {isOpen ? (
            <ArrowDropDown className="icon toggle-section" />
          ) : (
            <ArrowRight className="icon toggle-section" />
          )}
        </div>
        {isEditingAllowed ? (
          <Checkbox
            {...(isAllTaskSelected
              ? { checked: true, partial: false }
              : isNoTaskSelected
              ? { checked: false, partial: false }
              : { checked: false, partial: true })}
            label={
              <div>
                <span style={{ fontWeight: 'bold' }}>Stage {stage.orderTree}</span> {stage.name}
              </div>
            }
            onClick={() => {
              localDispatch({
                type: 'SET_TASK_SELECTED_STATE',
                payload: {
                  stageId: stage.id,
                  taskIds: stage.tasks.map((task) => task.id),
                  states: stage.tasks.map(() => (isAllTaskSelected ? false : isNoTaskSelected)),
                },
              });
            }}
          />
        ) : (
          <div style={{ marginRight: 'auto' }}>
            <span style={{ fontWeight: 'bold' }}>Stage {stage.orderTree}</span> {stage.name}
          </div>
        )}

        <div
          className={`pill ${
            isAllTaskAssigned ? 'assigned' : isNoTaskAssigned ? 'unassigned' : 'partial'
          }`}
        >
          {isAllTaskAssigned ? 'Assigned' : isNoTaskAssigned ? 'Unassigned' : 'Partial Assigned'}
        </div>
      </div>
      {isOpen && (
        <div className="section-body">
          {stage.tasks.map((task) => {
            return (
              <div className="section-body-item" key={task.id}>
                {isEditingAllowed ? (
                  <Checkbox
                    checked={sectionState[task.id] ?? false}
                    label={`Task ${stage.orderTree}.${task.orderTree} : ${task.name}`}
                    onClick={() => {
                      localDispatch({
                        type: 'SET_TASK_SELECTED_STATE',
                        payload: {
                          stageId: stage.id,
                          taskIds: [task.id],
                          states: [!sectionState[task.id]],
                        },
                      });
                    }}
                  />
                ) : (
                  <span>
                    Task {stage.orderTree}.{task.orderTree} : {task.name}
                  </span>
                )}
                {!!assignedData?.[task.id]?.users?.length && (
                  <AssigneeList users={assignedData[task.id].users} />
                )}
                {!!assignedData?.[task.id]?.userGroups?.length && (
                  <AssigneeList users={assignedData[task.id].userGroups} isGroup={true} />
                )}

                {Object.keys(assignedData).length === 0 ||
                !assignedData.hasOwnProperty(task.id) ||
                (assignedData?.[task.id]?.users?.length === 0 &&
                  assignedData?.[task.id]?.userGroups?.length === 0) ? (
                  <div className="pill unassigned">Unassigned</div>
                ) : null}
              </div>
            );
          })}
        </div>
      )}
    </AssignmentSectionWrapper>
  );
};

export default Section;
