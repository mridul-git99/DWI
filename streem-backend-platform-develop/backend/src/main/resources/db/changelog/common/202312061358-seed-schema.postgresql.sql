-- liquibase formatted sql

--changeset siba:202312061358-seed-schema-1
create table if not exists parameter_rules
(
  id         bigserial
    primary key,
  rules_id   varchar(255)          not null,
  operator   varchar(255)          not null,
  input      text[]                not null,
  visibility boolean default false not null
);

--changeset siba:202312061358-seed-schema-2
create table if not exists parameter_rule_mapping
(
  parameter_rules_id       bigint not null
    references parameter_rules
      on delete cascade,
  impacted_parameters_id   bigint not null
    references parameters,
  triggering_parameters_id bigint not null
    references parameters
);

alter table parameter_rule_mapping
  add primary key (parameter_rules_id,impacted_parameters_id, triggering_parameters_id);



