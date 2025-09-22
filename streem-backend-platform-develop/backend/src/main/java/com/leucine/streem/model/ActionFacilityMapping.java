package com.leucine.streem.model;

import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.compositekey.ActionFacilityCompositeKey;
import com.leucine.streem.model.helper.UserAuditBase;
import com.leucine.streem.model.helper.UserAuditIdentifiableBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.ACTION_FACILITY_MAPPING)
public class ActionFacilityMapping extends UserAuditBase implements Serializable {
  @EmbeddedId
  private ActionFacilityCompositeKey actionFacilityCompositeKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "actions_id", nullable = false, updatable = false, insertable = false)
  private Action action;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "facilities_id", nullable = false, updatable = false, insertable = false)
  private Facility facility;




  public ActionFacilityMapping(Action action, Facility facility) {
    this.action = action;
    this.facility = facility;
    this.actionFacilityCompositeKey = new ActionFacilityCompositeKey(action.getId(), facility.getId());
  }
}
