package com.leucine.streem.model;

import com.leucine.streem.model.compositekey.UserGroupMemberCompositeKey;
import com.leucine.streem.model.helper.UserAuditBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_group_members")
public class UserGroupMember extends UserAuditBase implements Serializable {

  @Serial
  private static final long serialVersionUID = -3764401588250496032L;

  @EmbeddedId
  private UserGroupMemberCompositeKey userGroupMemberCompositeKey;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "users_id", insertable = false, updatable = false)
  private User user;

  @Column(name = "users_id", insertable = false, updatable = false)
  private Long usersId;


  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("groupId")
  @JoinColumn(name = "groups_id", insertable = false, updatable = false)
  private UserGroup userGroup;

  public UserGroupMember(User user, UserGroup userGroup) {
    this.user = user;
    this.userGroup = userGroup;
    this.userGroupMemberCompositeKey = new UserGroupMemberCompositeKey(user.getId(), userGroup.getId());
  }
}
