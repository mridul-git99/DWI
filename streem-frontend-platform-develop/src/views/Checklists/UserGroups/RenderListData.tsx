import React from 'react';
import { ListItem } from '../TrainedUser/ListItem';

export const RenderListData = ({
  section,
  trainedUserList = [],
  trainedGroupList,
  usersList = [],
  groupList = [],
  handleClickHandler,
  fields = [],
}: any) => {
  const mappedList =
    section === 'users' ? [...trainedUserList, ...usersList] : [...trainedGroupList, ...groupList];

  if (!mappedList?.length)
    return (
      <div className="no-data-found">No {section === 'users' ? 'Users' : 'User Groups'} Found</div>
    );

  return (
    <div>
      {mappedList
        ?.filter(
          (data) =>
            !fields.some(
              (_data) =>
                (_data.userGroupId || _data.userId || _data.id) ===
                (data.userGroupId || data.userId || data.id),
            ),
        )
        ?.map((data) => {
          return (
            <ListItem
              key={data.userGroupId || data.id || data.userId}
              user={data}
              onClick={() => handleClickHandler(data)}
              isGroup={section === 'userGroups'}
            />
          );
        })}
    </div>
  );
};
