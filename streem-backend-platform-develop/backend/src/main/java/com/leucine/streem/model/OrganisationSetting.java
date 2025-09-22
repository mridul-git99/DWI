package com.leucine.streem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leucine.streem.constant.TableName;
import com.leucine.streem.model.helper.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = TableName.ORGANISATION_SETTINGS)
public class OrganisationSetting extends BaseEntity implements Serializable {
  private static final long serialVersionUID = -5186926456733049279L;

  @Column(name = "logo_url", columnDefinition = "varchar", length = 255)
  private String logoUrl;

  // In Minutes
  @Column(name = "session_idle_timeout", columnDefinition = "int")
  private Integer sessionIdleTimeout = 10;

  // In Minutes
  @Column(name = "registration_token_expiration", columnDefinition = "int")
  private Integer registrationTokenExpiration = 60;

  // In Minutes
  @Column(name = "password_reset_token_expiration", columnDefinition = "int")
  private Integer passwordResetTokenExpiration = 60;

  @Column(name = "max_failed_login_attempts", columnDefinition = "int")
  private Integer maxFailedLoginAttempts = 3;

  @Column(name = "max_failed_additional_verification_attempts", columnDefinition = "int")
  private Integer maxFailedAdditionalVerificationAttempts = 3;

  @Column(name = "max_failed_challenge_question_attempts", columnDefinition = "int")
  private Integer maxFailedChallengeQuestionAttempts = 3;

  // In Minutes
  @Column(name = "auto_unlock_after", columnDefinition = "int")
  private Integer autoUnlockAfter = 15;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organisations_id", nullable = false)
  private Organisation organisation;

  @Column(name = "organisations_id", columnDefinition = "bigint", updatable = false, insertable = false)
  private Long organisationId;

  @JsonIgnore
  @Column(columnDefinition = "bigint", updatable = false, nullable = false)
  private Long createdAt;

  @JsonIgnore
  @Column(columnDefinition = "bigint", nullable = false)
  private Long modifiedAt;
}
