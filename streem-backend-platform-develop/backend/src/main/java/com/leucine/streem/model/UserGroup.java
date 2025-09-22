package com.leucine.streem.model;

import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_groups")
public class UserGroup extends UserAuditIdentifiableBase {

  @Serial
  private static final long serialVersionUID = -7554929371446176851L;

  @Column(columnDefinition = "varchar", nullable = false)
  private String name;

  @Column(columnDefinition = "text", nullable = false)
  private String description;

  @Column(columnDefinition = "boolean", nullable = false)
  private boolean active;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(columnDefinition = "bigint", name = "facility_id", referencedColumnName = "id")
  private Facility facility;

  @Column(name = "facility_id", insertable = false, updatable = false)
  private Long facilityId;

  @OneToMany(mappedBy = "userGroup", fetch = FetchType.LAZY)
  private List<UserGroupMember> userGroupMembers = new ArrayList<>();
}
