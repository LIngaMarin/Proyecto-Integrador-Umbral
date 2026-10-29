-- Exclusivo de demo: base H2 efimera. No se carga con el perfil normal.
INSERT INTO preguntas (enunciado, tema, dificultad, respuesta_correcta, estado, docente_id, fecha_creacion)
VALUES ('DEMO: Resolver 2x + 6 = 14', 'Algebra', 'FACIL', '4', 'APROBADA', 1, CURRENT_TIMESTAMP);
INSERT INTO preguntas (enunciado, tema, dificultad, respuesta_correcta, estado, docente_id, fecha_creacion)
VALUES ('DEMO: Resolver 3x + 9 = 21', 'Algebra', 'INTERMEDIO', '4', 'APROBADA', 1, CURRENT_TIMESTAMP);
INSERT INTO preguntas (enunciado, tema, dificultad, respuesta_correcta, estado, docente_id, fecha_creacion)
VALUES ('DEMO: Pregunta pendiente 4x + 4 = 20', 'Algebra', 'FACIL', '4', 'PENDIENTE', 1, CURRENT_TIMESTAMP);

INSERT INTO pregunta_opciones (pregunta_id, opciones)
SELECT id, '2' FROM preguntas;
INSERT INTO pregunta_opciones (pregunta_id, opciones)
SELECT id, '4' FROM preguntas;
INSERT INTO pregunta_opciones (pregunta_id, opciones)
SELECT id, '6' FROM preguntas;
INSERT INTO pregunta_opciones (pregunta_id, opciones)
SELECT id, '8' FROM preguntas;

INSERT INTO resultados_simulacro (simulacro_id, alumno_id, seccion_id, puntaje, temas_incorrectos_json)
VALUES (1, 1, 1, 12.0, '["Algebra","Geometria"]');
INSERT INTO resultados_simulacro (simulacro_id, alumno_id, seccion_id, puntaje, temas_incorrectos_json)
VALUES (1, 2, 1, 14.0, '["Algebra"]');
