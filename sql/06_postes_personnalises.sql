-- A appliquer apres 04 sur une base existante. Conserve les rattachements.
\set ON_ERROR_STOP on
BEGIN;
ALTER TABLE poste ALTER COLUMN section_id DROP NOT NULL;
COMMIT;
