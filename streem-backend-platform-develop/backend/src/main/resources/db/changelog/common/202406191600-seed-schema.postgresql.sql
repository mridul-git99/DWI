-- liquibase formatted sql

-- changeset nishant:202406181906-seed-schema-1
-- comment: Drop the existing foreign key constraint and Add the new foreign key constraint with ON DELETE CASCADE

ALTER TABLE variation_media_mapping
DROP CONSTRAINT IF EXISTS variation_media_mapping_variations_id_fkey;

ALTER TABLE variation_media_mapping
ADD CONSTRAINT variation_media_mapping_variations_id_fkey FOREIGN KEY (variations_id) REFERENCES variations(id) ON DELETE CASCADE;
