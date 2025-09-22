package com.leucine.streem.model.compositekey;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupMemberCompositeKey implements Serializable {
  @Serial
  private static final long serialVersionUID = 879498839833695676L;

  @Column(name = "groups_id", columnDefinition = "bigint")
  private Long groupId;

  @Column(name = "users_id", columnDefinition = "bigint")
  private Long userId;
}
