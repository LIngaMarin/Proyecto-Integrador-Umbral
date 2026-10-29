package com.umbral.umbral_backend.service

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import com.zaxxer.hikari.HikariDataSource

/** Usa una base/esquema nuevo; nunca limpia ni borra las tablas del proyecto. */
class MigracionEvolutivaTests {
    @Test fun `migraciones conservan datos anteriores y reconstruyen historial`() {
        val url=System.getProperty("spring.datasource.url","jdbc:h2:mem:evolucion;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
        val user=System.getProperty("spring.datasource.username","sa")
        val pass=System.getProperty("spring.datasource.password","")
        val schema="evolucion_${System.nanoTime().toString().takeLast(10)}"
        // Igual que Spring Boot: mantiene conexiones físicas durante migración y validación.
        HikariDataSource().apply { jdbcUrl=url;username=user;password=pass;maximumPoolSize=2;minimumIdle=1 }.use { pool ->
        fun flyway(target: String)=Flyway.configure().dataSource(pool).schemas(schema).defaultSchema(schema)
            .locations("classpath:db/migration").target(target).load()
        flyway("6").migrate()
        pool.connection.use { conn ->
            conn.schema=schema
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("""INSERT INTO preguntas(id,enunciado,tema,dificultad,respuesta_correcta,estado,docente_id,fecha_creacion)
                    VALUES(101,'Pregunta conservada','Algebra','FACIL','4','APROBADA',1,TIMESTAMP '2026-10-19 08:00:00')""")
                stmt.executeUpdate("""INSERT INTO grupos_dudas(id,clave,docente_id,seccion_id,tema,pregunta_id)
                    VALUES(101,'clave-anterior',1,1,'Algebra',101)""")
                stmt.executeUpdate("""INSERT INTO respuestas_grupo(id,grupo_id,docente_id,texto,fecha)
                    VALUES(101,101,1,'Respuesta conservada',TIMESTAMP '2026-10-20 10:00:00')""")
                stmt.executeUpdate("""INSERT INTO dudas(id,grupo_id,alumno_id,texto,estado,fecha,respuesta_id)
                    VALUES(101,101,2,'Duda conservada','PENDIENTE',TIMESTAMP '2026-10-19 09:00:00',101)""")
            }
        }
        flyway("8").migrate()
        pool.connection.use { conn ->
            conn.schema=schema
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT texto,respuesta_id FROM dudas WHERE id=101").use { rs ->
                    assertTrue(rs.next());assertEquals("Duda conservada",rs.getString(1));assertEquals(101,rs.getLong(2))
                }
                stmt.executeQuery("SELECT evento,fecha FROM historial_dudas WHERE duda_id=101 ORDER BY fecha,id").use { rs ->
                    assertTrue(rs.next());assertEquals("REGISTRADA",rs.getString(1));assertEquals("2026-10-19T09:00",rs.getTimestamp(2).toLocalDateTime().toString())
                    assertTrue(rs.next());assertEquals("RESPONDIDA",rs.getString(1));assertEquals("2026-10-20T10:00",rs.getTimestamp(2).toLocalDateTime().toString())
                    assertFalse(rs.next())
                }
                stmt.executeQuery("SELECT enunciado FROM preguntas WHERE id=101").use { rs ->
                    assertTrue(rs.next());assertEquals("Pregunta conservada",rs.getString(1))
                }
            }
        }
        }
    }
}
