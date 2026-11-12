-- Solo demo/test: completa el contrato auth_user de Django sin tocar PostgreSQL de producción.
-- Se guardan únicamente hashes BCrypt con prehash SHA-256 compatible con Django.
ALTER TABLE auth_user ADD COLUMN username VARCHAR(150);
ALTER TABLE auth_user ADD COLUMN password VARCHAR(255);
CREATE UNIQUE INDEX idx_demo_username ON auth_user(username);
UPDATE auth_user SET username='docente.demo', password='bcrypt_sha256$$2a$12$dQneMINJc2LNVbP3CMPR7u1CCA.ofGAHqMTbRmbMulqwrgAL08zbm' WHERE id=1;
UPDATE auth_user SET username='alumno.demo', password='bcrypt_sha256$$2a$12$rtul.zcGPh33DKYWodkN4uO0Bt5WvwLlKbpJ63vkKP/1xklkxcupu' WHERE id=2;
UPDATE auth_user SET username='apoderado.demo', password='bcrypt_sha256$$2a$12$VWbiHWT8fapLKtsW.V7AdOQIf8dv69iZPBTPOsQRSdF13t3dtz4x2' WHERE id=3;
