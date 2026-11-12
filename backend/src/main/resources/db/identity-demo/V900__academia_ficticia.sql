-- Solo test/demo: contrato mínimo del esquema Django. Nunca cargar en PostgreSQL normal.
CREATE TABLE auth_user (id BIGINT PRIMARY KEY, email VARCHAR(254) NOT NULL, is_active BOOLEAN NOT NULL);
CREATE TABLE adm_perfil (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES auth_user(id), rol VARCHAR(20) NOT NULL);
CREATE TABLE adm_seccion (id BIGINT PRIMARY KEY, curso_id BIGINT NOT NULL, nombre VARCHAR(50), activa BOOLEAN NOT NULL);
CREATE TABLE adm_horario (id BIGINT PRIMARY KEY, seccion_id BIGINT REFERENCES adm_seccion(id), docente_id BIGINT REFERENCES adm_perfil(id), activa BOOLEAN NOT NULL);
CREATE TABLE adm_matricula (id BIGINT PRIMARY KEY, alumno_id BIGINT REFERENCES adm_perfil(id), seccion_id BIGINT REFERENCES adm_seccion(id), activa BOOLEAN NOT NULL);
CREATE TABLE adm_apoderado (id BIGINT PRIMARY KEY, perfil_id BIGINT REFERENCES adm_perfil(id));
CREATE TABLE adm_apoderado_hijos (id BIGINT PRIMARY KEY, apoderado_id BIGINT REFERENCES adm_apoderado(id), perfil_id BIGINT REFERENCES adm_perfil(id));
CREATE TABLE adm_simulacro (id BIGINT PRIMARY KEY, curso_id BIGINT, nombre VARCHAR(150), fecha DATE, activa BOOLEAN NOT NULL);
INSERT INTO auth_user VALUES (1,'docente@example.invalid',true),(2,'alumno@example.invalid',true),(3,'apoderado@example.invalid',true),(4,'otro-alumno@example.invalid',true),(5,'otro-docente@example.invalid',true);
INSERT INTO adm_perfil VALUES (1,1,'DOCENTE'),(2,2,'ALUMNO'),(3,3,'APODERADO'),(4,4,'ALUMNO'),(5,5,'DOCENTE');
INSERT INTO adm_seccion VALUES (1,1,'Demo A',true),(2,2,'Demo B',true);
INSERT INTO adm_horario VALUES (1,1,1,true),(2,2,5,true);
INSERT INTO adm_matricula VALUES (1,2,1,true),(2,4,2,true);
INSERT INTO adm_apoderado VALUES (1,3);
INSERT INTO adm_apoderado_hijos VALUES (1,1,2);
INSERT INTO adm_simulacro VALUES (1,1,'Simulacro demo',DATE '2026-11-01',true),(2,2,'Otro curso',DATE '2026-11-02',true),(3,1,'Cancelado',DATE '2026-11-03',false);
