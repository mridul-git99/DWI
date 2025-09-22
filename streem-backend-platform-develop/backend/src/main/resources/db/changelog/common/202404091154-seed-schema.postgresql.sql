-- liquibase formatted sql

--changeset mokshesh:202404091154-seed-schema-1
--comment: merge jaas into dwi

CREATE TABLE public.challenge_questions
(
  id          integer                NOT NULL,
  question    character varying(512) NOT NULL,
  archived    boolean DEFAULT false  NOT NULL,
  created_at  bigint                 NOT NULL,
  created_by  bigint,
  modified_at bigint                 NOT NULL,
  modified_by bigint
);

CREATE TABLE public.client_credentials
(
  id                 character varying(255) NOT NULL,
  client_secret      character varying(255) NOT NULL,
  client_name        character varying(50)  NOT NULL,
  client_description character varying(255),
  archived           boolean DEFAULT false  NOT NULL
);

CREATE TABLE public.client_facility_mapping
(
  client_id     character varying(255) NOT NULL,
  facilities_id bigint                 NOT NULL,
  created_by    bigint                 NOT NULL,
  modified_by   bigint                 NOT NULL,
  created_at    bigint                 NOT NULL,
  modified_at   bigint                 NOT NULL
);

CREATE TABLE public.licenses
(
  id                        bigint                NOT NULL,
  organisations_id          bigint                NOT NULL,
  facilities_id             bigint                NOT NULL,
  product                   character varying(150),
  type                      character varying(150),
  subscription_start_date   date,
  subscription_period       integer,
  subscription_renewal_date date,
  payment_done              boolean,
  intimate_before           integer,
  grace_period              integer,
  workflow                  character varying(150),
  archived                  boolean DEFAULT false NOT NULL,
  created_by                bigint,
  created_at                bigint                NOT NULL,
  modified_by               bigint,
  modified_at               bigint                NOT NULL
);

CREATE TABLE public.organisation_facilities_mapping
(
  facilities_id    bigint NOT NULL,
  organisations_id bigint NOT NULL,
  created_at       bigint NOT NULL,
  created_by       bigint
);

CREATE TABLE public.organisation_services_mapping
(
  organisations_id bigint                 NOT NULL,
  services_id      character varying(255) NOT NULL,
  created_at       bigint                 NOT NULL,
  created_by       bigint,
  fqdn             text                   NOT NULL
);

CREATE TABLE public.organisation_settings
(
  id                                          bigint NOT NULL,
  created_at                                  bigint NOT NULL,
  created_by                                  bigint,
  modified_at                                 bigint NOT NULL,
  modified_by                                 bigint,
  auto_unlock_after                           integer,
  max_failed_login_attempts                   integer,
  organisations_id                            bigint NOT NULL,
  password_reset_token_expiration             integer,
  registration_token_expiration               integer,
  session_idle_timeout                        integer,
  logo_url                                    character varying(255),
  max_failed_additional_verification_attempts integer DEFAULT 3,
  max_failed_challenge_question_attempts      integer DEFAULT 3,
  extras                                      jsonb   DEFAULT '{}'::jsonb,
  feature_flags                               jsonb   DEFAULT '{"metabaseReports": false}'::jsonb
);

CREATE TABLE public.password_history
(
  id         bigint NOT NULL,
  created_at bigint NOT NULL,
  password   text,
  users_id   bigint
);

CREATE TABLE public.password_policies
(
  id                                          bigint NOT NULL,
  created_at                                  bigint NOT NULL,
  created_by                                  bigint,
  modified_at                                 bigint NOT NULL,
  modified_by                                 bigint,
  allow_password_similar_to_username_or_email boolean DEFAULT false,
  maximum_password_age                        integer,
  minimum_lowercase_characters                integer,
  minimum_numeric_characters                  integer,
  minimum_password_history                    integer,
  minimum_password_length                     integer,
  minimum_special_characters                  integer,
  minimum_uppercase_characters                integer,
  organisations_id                            bigint NOT NULL,
  password_expiration                         integer
);

CREATE TABLE public.permissions
(
  id          bigint                NOT NULL,
  created_at  bigint                NOT NULL,
  created_by  bigint,
  modified_at bigint                NOT NULL,
  modified_by bigint,
  archived    boolean DEFAULT false NOT NULL,
  description text,
  method      character varying(15),
  name        character varying(255),
  path        text,
  services_id character varying(255)
);

CREATE TABLE public.role_permissions_mapping
(
  permissions_id bigint NOT NULL,
  roles_id       bigint NOT NULL,
  created_at     bigint NOT NULL,
  created_by     bigint
);


CREATE TABLE public.role_scope_groups_mapping
(
  roles_id        bigint NOT NULL,
  scope_groups_id bigint NOT NULL,
  created_at      bigint NOT NULL,
  created_by      bigint
);

CREATE TABLE public.roles
(
  id          bigint                NOT NULL,
  created_at  bigint                NOT NULL,
  created_by  bigint,
  modified_at bigint                NOT NULL,
  modified_by bigint,
  archived    boolean DEFAULT false NOT NULL,
  name        character varying(90),
  services_id character varying(255)
);

CREATE TABLE public.scope_groups
(
  id          bigint                NOT NULL,
  created_at  bigint                NOT NULL,
  created_by  bigint,
  modified_at bigint                NOT NULL,
  modified_by bigint,
  archived    boolean DEFAULT false NOT NULL,
  name        character varying(90),
  order_tree  integer               NOT NULL,
  services_id character varying(255)
);

CREATE TABLE public.scopes
(
  id              bigint                NOT NULL,
  created_at      bigint                NOT NULL,
  created_by      bigint,
  modified_at     bigint                NOT NULL,
  modified_by     bigint,
  archived        boolean DEFAULT false NOT NULL,
  name            character varying(255),
  order_tree      integer               NOT NULL,
  scope_groups_id bigint                NOT NULL
);

CREATE TABLE public.services
(
  id                  character varying(255) NOT NULL,
  archived            boolean DEFAULT false  NOT NULL,
  created_at          bigint                 NOT NULL,
  created_by          bigint,
  login_path          text,
  modified_at         bigint                 NOT NULL,
  modified_by         bigint,
  name                character varying(45),
  registration_path   text,
  reset_password_path text,
  error_code_range    character varying(45)
);

CREATE TABLE public.tokens
(
  token      character varying(32) NOT NULL,
  expiration bigint,
  type       character varying(32) NOT NULL,
  users_id   bigint
);

CREATE TABLE public.user_audits
(
  id               bigint NOT NULL,
  action           character varying(255),
  details          text,
  organisations_id bigint,
  severity         character varying(255),
  triggered_at     bigint,
  triggered_by     bigint NOT NULL,
  facility_ids     text[]
);

CREATE TABLE public.user_facilities_mapping
(
  facilities_id bigint NOT NULL,
  users_id      bigint NOT NULL,
  created_at    bigint NOT NULL,
  created_by    bigint
);

CREATE TABLE public.user_roles_mapping
(
  roles_id   bigint NOT NULL,
  users_id   bigint NOT NULL,
  created_at bigint NOT NULL,
  created_by bigint
);

ALTER TABLE public.facilities ADD created_by bigint NULL;

ALTER TABLE public.facilities ADD date_format varchar(50) DEFAULT 'MMM dd, yyyy'::character varying NOT NULL;

ALTER TABLE public.facilities ADD date_time_format varchar(50) DEFAULT 'MMM dd, yyyy HH:mm'::character varying NOT NULL;

ALTER TABLE public.facilities ADD modified_by bigint NULL;

ALTER TABLE public.facilities ADD time_format varchar(50) DEFAULT 'HH:mm'::character varying NOT NULL;

ALTER TABLE public.organisations ADD created_by bigint NULL;

ALTER TABLE public.facilities ADD CONSTRAINT fkadf9c50af859446f8157778a FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE public.organisations ADD modified_by bigint NULL;

ALTER TABLE public.facilities ADD CONSTRAINT fk37980a8df4a149ae93c2ff0f FOREIGN KEY (modified_by) REFERENCES users(id);

ALTER TABLE public.organisations ADD is_master bool DEFAULT false NOT NULL;

ALTER TABLE public.users ADD created_by bigint NULL;

ALTER TABLE public.users ADD modified_by bigint NULL;

ALTER TABLE public.users ADD locked_at bigint NULL;

ALTER TABLE public.users ADD department varchar(255) NULL;

ALTER TABLE public.users ADD failed_login_attempts int4 DEFAULT 0 NULL;

ALTER TABLE public.users ADD is_system_user bool DEFAULT false NULL;

ALTER TABLE public.users ADD "password" text NULL;

ALTER TABLE public.users ADD password_updated_at bigint NULL;

ALTER TABLE public.users ADD username varchar(255) NULL;

ALTER TABLE public.users ADD state varchar(50) NULL;

ALTER TABLE public.users ADD failed_additional_verification_attempts int4 DEFAULT 0 NULL;

ALTER TABLE public.users ADD challenge_questions_id int4 NULL;

ALTER TABLE public.users ADD challenge_questions_answer varchar(255) NULL;

ALTER TABLE public.users ADD failed_challenge_question_attempts int4 DEFAULT 0 NULL;

ALTER TABLE public.users ADD "type" varchar(15) DEFAULT 'LOCAL'::character varying NULL;

ALTER TABLE ONLY public.challenge_questions
  ADD CONSTRAINT challenge_questions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.client_credentials
  ADD CONSTRAINT client_credentials_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.client_facility_mapping
  ADD CONSTRAINT client_facility_mapping_pkey PRIMARY KEY (client_id, facilities_id);

ALTER TABLE ONLY public.licenses
  ADD CONSTRAINT licenses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.organisation_facilities_mapping
  ADD CONSTRAINT organisation_facilities_mapping_pkey PRIMARY KEY (facilities_id, organisations_id);

ALTER TABLE ONLY public.organisation_services_mapping
  ADD CONSTRAINT organisation_services_mapping_pkey PRIMARY KEY (organisations_id, services_id);

ALTER TABLE ONLY public.organisation_settings
  ADD CONSTRAINT organisation_settings_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.password_history
  ADD CONSTRAINT password_history_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.password_policies
  ADD CONSTRAINT password_policies_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.permissions
  ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.role_permissions_mapping
  ADD CONSTRAINT role_permissions_mapping_pkey PRIMARY KEY (permissions_id, roles_id);

ALTER TABLE ONLY public.role_scope_groups_mapping
  ADD CONSTRAINT role_scope_groups_mapping_pkey PRIMARY KEY (roles_id, scope_groups_id);

ALTER TABLE ONLY public.roles
  ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.scope_groups
  ADD CONSTRAINT scope_groups_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.scopes
  ADD CONSTRAINT scopes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.services
  ADD CONSTRAINT services_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tokens
  ADD CONSTRAINT tokens_pkey PRIMARY KEY (token);

ALTER TABLE ONLY public.permissions
  ADD CONSTRAINT uk4stge73ntans3a1km7sxgtmwb UNIQUE (name, services_id, archived);

ALTER TABLE ONLY public.users
  ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);

ALTER TABLE ONLY public.organisation_settings
  ADD CONSTRAINT uk_e569e2kfei3fndvp2h698faw6 UNIQUE (organisations_id);

ALTER TABLE ONLY public.services
  ADD CONSTRAINT uk_h4rqgjwnqidx6mvj4i22dxwxe UNIQUE (name);

ALTER TABLE ONLY public.password_policies
  ADD CONSTRAINT uk_i31gwblxtrvf9xw9020ve5hpg UNIQUE (organisations_id);

ALTER TABLE ONLY public.services
  ADD CONSTRAINT uk_ilj0qjmo8ypukam8h1c6d0ib9 UNIQUE (error_code_range);

ALTER TABLE ONLY public.scope_groups
  ADD CONSTRAINT ukoffla5dgtnyn1bckb80v8n5x1 UNIQUE (name, services_id, archived);

ALTER TABLE ONLY public.users
  ADD CONSTRAINT ukps8jn3qjcop4ptb756hvqnad3 UNIQUE (employee_id, organisations_id, archived);

ALTER TABLE ONLY public.users
  ADD CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username);

ALTER TABLE ONLY public.scopes
  ADD CONSTRAINT ukrbhy5v58w1gtysg0h9smgo0no UNIQUE (name, scope_groups_id, archived);

ALTER TABLE ONLY public.roles
  ADD CONSTRAINT uksclfxide2pa64qcuo9wkr5pq UNIQUE (name, services_id, archived);

ALTER TABLE ONLY public.user_audits
  ADD CONSTRAINT user_audits_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.user_facilities_mapping
  ADD CONSTRAINT user_facilities_mapping_pkey PRIMARY KEY (facilities_id, users_id);

ALTER TABLE ONLY public.user_roles_mapping
  ADD CONSTRAINT user_roles_mapping_pkey PRIMARY KEY (roles_id, users_id);

CREATE INDEX id1kny5wcthrgio949vswzjsofu9 ON public.users USING btree (state, archived);

CREATE INDEX idxa8enhqqf54anje6jcvv2ymnh5 ON public.user_audits USING btree (organisations_id);

CREATE INDEX idxb9xt56fyg8luuosihexhda3ke ON public.password_history USING btree (users_id);

CREATE INDEX idxgx5w2mh7k08opon3a4xb1sasg ON public.tokens USING btree (type, token);

CREATE INDEX idxrf922wo0pk10bldoh9si309v8 ON public.user_audits USING btree (organisations_id, triggered_by);

ALTER TABLE ONLY public.client_facility_mapping
  ADD CONSTRAINT client_facility_mapping_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.client_credentials (id);

ALTER TABLE ONLY public.client_facility_mapping
  ADD CONSTRAINT client_facility_mapping_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.client_facility_mapping
  ADD CONSTRAINT client_facility_mapping_facilities_id_fkey FOREIGN KEY (facilities_id) REFERENCES public.facilities (id);

ALTER TABLE ONLY public.client_facility_mapping
  ADD CONSTRAINT client_facility_mapping_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.organisation_facilities_mapping
  ADD CONSTRAINT fk2gl9pgd5pfxlb4i7iit6ofwxj FOREIGN KEY (facilities_id) REFERENCES public.facilities (id);

ALTER TABLE ONLY public.users
  ADD CONSTRAINT fk3aqdj6u2t0by2dj24m141utm FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.role_permissions_mapping
  ADD CONSTRAINT fk3n65q9h4enr99aybq0uvgw4nh FOREIGN KEY (permissions_id) REFERENCES public.permissions (id);

ALTER TABLE ONLY public.organisation_services_mapping
  ADD CONSTRAINT fk5hcuc2h4ncne96wmaxw8qmok1 FOREIGN KEY (services_id) REFERENCES public.services (id);

ALTER TABLE ONLY public.scopes
  ADD CONSTRAINT fk6c6wtx3ljdmyj836tu23qau1b FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.permissions
  ADD CONSTRAINT fk6td375738330a4j5gjbkg1260 FOREIGN KEY (services_id) REFERENCES public.services (id);

ALTER TABLE ONLY public.user_facilities_mapping
  ADD CONSTRAINT fk6tjdtc3xhdpasrv9gdx851e7a FOREIGN KEY (users_id) REFERENCES public.users (id);

ALTER TABLE ONLY public.organisation_services_mapping
  ADD CONSTRAINT fk7411dafrebkafo7uqmifox83t FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.scope_groups
  ADD CONSTRAINT fk8ra8xi8snu51d98gc2xabb74p FOREIGN KEY (services_id) REFERENCES public.services (id);

ALTER TABLE ONLY public.password_policies
  ADD CONSTRAINT fk8wi3r4u8890lmkb4p25ghkr9w FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.permissions
  ADD CONSTRAINT fk969n0rlcq9vy1eitcbvvjfpmd FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.organisation_settings
  ADD CONSTRAINT fk9fpebw9gn54vgfg2tymthuefr FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.password_policies
  ADD CONSTRAINT fk9kecghr4v0emfdu8pvql3g25t FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.user_facilities_mapping
  ADD CONSTRAINT fk9qn8tnlfjlqiypuhes39v215e FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.organisation_settings
  ADD CONSTRAINT fkb3ipeaqo0ljm8qurd50vmmkpj FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.role_scope_groups_mapping
  ADD CONSTRAINT fkbfy11wy2vbi8hqhq6jet86may FOREIGN KEY (roles_id) REFERENCES public.roles (id);

ALTER TABLE ONLY public.roles
  ADD CONSTRAINT fkbxp1330h4b9f7n4h5sr5ul5xy FOREIGN KEY (services_id) REFERENCES public.services (id);

ALTER TABLE ONLY public.password_history
  ADD CONSTRAINT fkcbc78f28bf7a4e3286b1299546792bbb FOREIGN KEY (users_id) REFERENCES public.users (id);

ALTER TABLE ONLY public.organisation_services_mapping
  ADD CONSTRAINT fkcnr64kn9vae0vitedqlbn16ny FOREIGN KEY (organisations_id) REFERENCES public.organisations (id);

ALTER TABLE ONLY public.user_roles_mapping
  ADD CONSTRAINT fkd0c4akn0iryn8umt2w81gp2a8 FOREIGN KEY (roles_id) REFERENCES public.roles (id);

ALTER TABLE ONLY public.scopes
  ADD CONSTRAINT fkdwwb499l4abd3prbqafauwjty FOREIGN KEY (scope_groups_id) REFERENCES public.scope_groups (id);

ALTER TABLE ONLY public.licenses
  ADD CONSTRAINT fkenetfi3frnisc5c30ylvxakmg FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.licenses
  ADD CONSTRAINT fkf2x3f12xs70xmjpiennvk2lnb FOREIGN KEY (facilities_id) REFERENCES public.facilities (id);

ALTER TABLE ONLY public.organisation_settings
  ADD CONSTRAINT fkfbkx8ijpw9bs1kix5pvxjtjpa FOREIGN KEY (organisations_id) REFERENCES public.organisations (id);

ALTER TABLE ONLY public.password_policies
  ADD CONSTRAINT fkh9auym8pmhf3ch4yy12upn367 FOREIGN KEY (organisations_id) REFERENCES public.organisations (id);

ALTER TABLE ONLY public.user_facilities_mapping
  ADD CONSTRAINT fkhqdskp1q4u2ffb61h8a2bln4s FOREIGN KEY (facilities_id) REFERENCES public.facilities (id);

ALTER TABLE ONLY public.users
  ADD CONSTRAINT fkibk1e3kaxy5sfyeekp8hbhnim FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.scopes
  ADD CONSTRAINT fkj2yyceojntnrncaxj1v6n6y0k FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.licenses
  ADD CONSTRAINT fkjf3j5j0qhv6tp11kmjxqr768r FOREIGN KEY (organisations_id) REFERENCES public.organisations (id);

ALTER TABLE ONLY public.facilities
  ADD CONSTRAINT fkjptojq3hke4impn3f21pf3yrq FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.organisation_facilities_mapping
  ADD CONSTRAINT fkjwvivng5u15onxn9insejjt35 FOREIGN KEY (organisations_id) REFERENCES public.organisations (id);

ALTER TABLE ONLY public.role_scope_groups_mapping
  ADD CONSTRAINT fkk2is6m5dna657d3uclany49fi FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.roles
  ADD CONSTRAINT fkkbv9sinbix1n1w3dj8tcor7qe FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.role_permissions_mapping
  ADD CONSTRAINT fkkkvnsag455fp16hldwhdd0oih FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.user_roles_mapping
  ADD CONSTRAINT fkkqduk9rn1l3w105ijhej346hm FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.scope_groups
  ADD CONSTRAINT fklvdjibyg9wmoitx5ocytjg5ib FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.user_roles_mapping
  ADD CONSTRAINT fklw04i55g1ydtct714ydlapc1u FOREIGN KEY (users_id) REFERENCES public.users (id);

ALTER TABLE ONLY public.role_permissions_mapping
  ADD CONSTRAINT fkmaqgtoh5r4r90m8rqyjj24uot FOREIGN KEY (roles_id) REFERENCES public.roles (id);

ALTER TABLE ONLY public.scope_groups
  ADD CONSTRAINT fkmldcj5wwbfmvow0wa2ghkeonl FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.services
  ADD CONSTRAINT fknaje90ybl9ehmwiiomsiyl4nl FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.role_scope_groups_mapping
  ADD CONSTRAINT fkokhjmfg0slwu0h8wnnavlfh12 FOREIGN KEY (scope_groups_id) REFERENCES public.scope_groups (id);

ALTER TABLE ONLY public.roles
  ADD CONSTRAINT fkq6ium4se7bjk3mfbj3qm1gvy FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.licenses
  ADD CONSTRAINT fkqgywisinopig7xdsg02epdy2m FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.organisation_facilities_mapping
  ADD CONSTRAINT fkqu53hfqyey8j5h6l3t4l81edc FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.facilities
  ADD CONSTRAINT fkrrysj47j1e2v6odntbcqq40o FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.services
  ADD CONSTRAINT fksije8l9h0jqqw6dsokn93138s FOREIGN KEY (modified_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.permissions
  ADD CONSTRAINT fkswqlowwcdv4nsgr00xfgdi4ky FOREIGN KEY (created_by) REFERENCES public.users (id);

ALTER TABLE ONLY public.users
  ADD CONSTRAINT fkyec8zrg17g5a49v1p817t9br7 FOREIGN KEY (challenge_questions_id) REFERENCES public.challenge_questions (id);

