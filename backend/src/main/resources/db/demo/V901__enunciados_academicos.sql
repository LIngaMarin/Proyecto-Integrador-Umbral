-- Ajusta exclusivamente datos ficticios demo sin alterar las migraciones que ya se ejecutaron.
UPDATE preguntas SET enunciado=SUBSTRING(enunciado,7) WHERE enunciado LIKE 'DEMO: %';
