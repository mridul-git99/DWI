-- liquibase formatted sql

--changeset Peesa:202312181518-seed-schema-1
--comment add job annotation table
CREATE TABLE IF NOT EXISTS job_annotations
(
  id                    BIGINT,
  remarks               TEXT,
  jobs_id                BIGINT        NOT NULL,
  created_at            BIGINT        NOT NULL,
  created_by            BIGINT        NOT NULL,
  modified_by           BIGINT        NOT NULL,
  modified_at           BIGINT        NOT NULL,
  CONSTRAINT job_annotations_pkey PRIMARY KEY (id)
);

--changeset Peesa:202312181518-seed-schema-2
--comment add job annotation media mapping table
CREATE TABLE IF NOT EXISTS job_annotation_media_mapping
(
  medias_id          BIGINT NOT NULL,
  job_annotations_id BIGINT NOT NULL,
  jobs_id             BIGINT NOT NULL,
  archived           BOOLEAN DEFAULT FALSE,
  created_at         BIGINT NOT NULL,
  modified_at        BIGINT NOT NULL,
  created_by         BIGINT NOT NULL,
  modified_by        BIGINT NOT NULL,
  CONSTRAINT job_annotation_media_mapping_pkey PRIMARY KEY (medias_id, job_annotations_id)
);


ALTER TABLE job_annotation_media_mapping
  ADD CONSTRAINT fkb6c5fe8a21d8405790e43adfd FOREIGN KEY (medias_id) REFERENCES medias(id);
ALTER TABLE job_annotation_media_mapping
  ADD CONSTRAINT fkb6c5fe8a21d8405790e43bp8 FOREIGN KEY (job_annotations_id) REFERENCES job_annotations(id);
ALTER TABLE job_annotation_media_mapping
  ADD CONSTRAINT fkb6c5fe8a21d8405790e43cq9 FOREIGN KEY (jobs_id) REFERENCES jobs(id);
