-- Execute uma vez no banco helpsla já existente para habilitar os dois cronômetros.
USE helpsla;

ALTER TABLE regra_sla ADD COLUMN prazo_aceite_minutos INT UNSIGNED NULL AFTER prazo_minutos;
UPDATE regra_sla SET prazo_aceite_minutos = CASE prioridade
  WHEN 'BAIXA' THEN 1440
  WHEN 'MEDIA' THEN 240
  WHEN 'ALTA' THEN 120
  WHEN 'CRITICA' THEN 30
END
WHERE prazo_aceite_minutos IS NULL AND id > 0;
ALTER TABLE regra_sla MODIFY prazo_aceite_minutos INT UNSIGNED NOT NULL;

ALTER TABLE chamado
  ADD COLUMN prazo_resolucao_minutos INT UNSIGNED NULL AFTER prazo_sla,
  ADD COLUMN prazo_resolucao_sla DATETIME(6) NULL AFTER prazo_resolucao_minutos;

UPDATE chamado c
JOIN regra_sla r ON r.id = c.regra_sla_id
SET c.prazo_resolucao_minutos = r.prazo_minutos,
    c.prazo_sla = DATE_ADD(c.aberto_em, INTERVAL r.prazo_aceite_minutos MINUTE),
    c.prazo_resolucao_sla = CASE
      WHEN c.assumido_em IS NOT NULL THEN DATE_ADD(c.assumido_em, INTERVAL r.prazo_minutos MINUTE)
      ELSE NULL
    END
WHERE c.prazo_resolucao_minutos IS NULL AND c.id > 0;

ALTER TABLE chamado MODIFY prazo_resolucao_minutos INT UNSIGNED NOT NULL;
