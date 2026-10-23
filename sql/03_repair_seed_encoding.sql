-- Executer avec psql : corrige uniquement les valeurs du jeu de reference
-- importees comme Windows-1252 au lieu de UTF-8. Conserve les identifiants.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;

CREATE FUNCTION pg_temp.repair_seed_encoding(value text) RETURNS text
LANGUAGE plpgsql AS $$
BEGIN
  RETURN convert_from(convert_to(value, 'WIN1252'), 'UTF8');
EXCEPTION
  WHEN character_not_in_repertoire OR untranslatable_character THEN
    RETURN value;
END;
$$;

UPDATE niveau_diplome SET niveau = pg_temp.repair_seed_encoding(niveau)
WHERE niveau LIKE '%Ã%' OR niveau LIKE '%Â%';
UPDATE diplome_filiere SET filiere = pg_temp.repair_seed_encoding(filiere)
WHERE filiere LIKE '%Ã%' OR filiere LIKE '%Â%';
UPDATE diplome_option SET option_nom = pg_temp.repair_seed_encoding(option_nom)
WHERE option_nom LIKE '%Ã%' OR option_nom LIKE '%Â%';
UPDATE direction SET nom = pg_temp.repair_seed_encoding(nom)
WHERE nom LIKE '%Ã%' OR nom LIKE '%Â%';
UPDATE departement SET nom = pg_temp.repair_seed_encoding(nom)
WHERE nom LIKE '%Ã%' OR nom LIKE '%Â%';
UPDATE service SET nom = pg_temp.repair_seed_encoding(nom)
WHERE nom LIKE '%Ã%' OR nom LIKE '%Â%';
UPDATE section SET nom = pg_temp.repair_seed_encoding(nom)
WHERE nom LIKE '%Ã%' OR nom LIKE '%Â%';
UPDATE poste SET intitule = pg_temp.repair_seed_encoding(intitule),
                 description = pg_temp.repair_seed_encoding(description)
WHERE intitule LIKE '%Ã%' OR intitule LIKE '%Â%'
   OR description LIKE '%Ã%' OR description LIKE '%Â%';

COMMIT;

SELECT niveau FROM niveau_diplome ORDER BY id;
SELECT filiere FROM diplome_filiere ORDER BY id;
SELECT option_nom FROM diplome_option ORDER BY id;
