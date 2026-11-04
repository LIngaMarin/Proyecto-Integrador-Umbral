package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.security.PasswordAcademico
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.net.http.*
import java.security.MessageDigest
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import tools.jackson.databind.ObjectMapper

/** HTTP real y base aislada: nunca usa ni escribe contraseñas de producción. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AutenticacionHttpTests {
    @LocalServerPort var port:Int=0
    @Autowired lateinit var jdbc:JdbcTemplate
    @Autowired lateinit var json:ObjectMapper
    @Autowired lateinit var passwords:PasswordAcademico
    @Autowired lateinit var service:AutenticacionService
    companion object {
        @JvmStatic @DynamicPropertySource fun config(r:DynamicPropertyRegistry){
            r.add("spring.datasource.url"){"jdbc:h2:mem:auth-web;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"}
            r.add("umbral.jwt.secret"){UUID.randomUUID().toString()+UUID.randomUUID()}
        }
    }
    private fun request(method:String,path:String,body:String?=null,token:String?=null):HttpResponse<String>{
        val b=HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path")).header("Content-Type","application/json")
        token?.let{b.header("Authorization","Bearer $it")}
        return HttpClient.newHttpClient().send(b.method(method,body?.let{HttpRequest.BodyPublishers.ofString(it)}?:HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString())
    }
    private fun login(user:String,password:String)=request("POST","/api/auth/login",json.writeValueAsString(mapOf("usuario" to user,"password" to password)))
    private fun hash(password:String):String {
        val sha=MessageDigest.getInstance("SHA-256").digest(password.toByteArray()).joinToString(""){"%02x".format(it)}
        return "bcrypt_sha256$"+BCrypt.hashpw(sha,BCrypt.gensalt(10))
    }

    @Test fun `acceso con usuario y correo emite JWT valido sin revelar contrasena`() {
        val password=UUID.randomUUID().toString()
        jdbc.update("UPDATE auth_user SET password=? WHERE id=2",hash(password))
        for(user in listOf("alumno.demo","ALUMNO@EXAMPLE.INVALID")){
            val response=login(user,password)
            assertEquals(200,response.statusCode(),response.body())
            assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"))
            assertFalse(response.body().contains(password))
            val token=json.readTree(response.body()).get("accessToken").asString()
            val cuenta=request("GET","/api/cuenta",token=token)
            assertEquals(200,cuenta.statusCode());assertTrue(cuenta.body().contains("ALUMNO"))
            assertEquals(403,request("GET","/api/docente/clases",token=token).statusCode())
        }
        assertEquals(401,login("alumno.demo","incorrecta").statusCode())
        assertEquals(401,login("no-existe","incorrecta").statusCode())
        assertEquals(401,login("' OR 1=1 --","incorrecta").statusCode())
        assertEquals(400,login(" ",password).statusCode())
        assertEquals(400,login("alumno.demo","x".repeat(129)).statusCode())
    }

    @Test fun `usuario inactivo y rol no autorizado no pueden iniciar sesion`() {
        val password=UUID.randomUUID().toString()
        jdbc.update("UPDATE auth_user SET username='inactivo',password=?,is_active=false WHERE id=4",hash(password))
        assertEquals(401,login("inactivo",password).statusCode())
        jdbc.update("UPDATE auth_user SET is_active=true WHERE id=4")
        jdbc.update("UPDATE adm_perfil SET rol='ADMIN' WHERE user_id=4")
        assertEquals(401,login("inactivo",password).statusCode())
    }

    @Test fun `hashes BCrypt y PBKDF2 se verifican sin admitir texto plano ni formatos invalidos`() {
        val password=UUID.randomUUID().toString()+"á"
        val bcrypt=hash(password)
        assertTrue(passwords.coincide(password,bcrypt));assertFalse(passwords.coincide(password+"x",bcrypt))
        val salt=UUID.randomUUID().toString()
        val bytes=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(),salt.toByteArray(),10000,256)).encoded
        val pbkdf="pbkdf2_sha256$10000$$salt$"+Base64.getEncoder().encodeToString(bytes)
        assertTrue(passwords.coincide(password,pbkdf));assertFalse(passwords.coincide(password+"x",pbkdf))
        for(invalido in listOf(password,"!inutilizable","bcrypt_sha256\$corrupto","pbkdf2_sha256\$0\$$salt\$mal"))assertFalse(passwords.coincide(password,invalido))
        val demo=jdbc.queryForList("SELECT password FROM auth_user WHERE id IN(1,3)")
        assertTrue(demo.all{it.values.first().toString().startsWith("bcrypt_sha256$")})
    }

    @Test fun `limite de intentos bloquea fuerza bruta sin otorgar acceso`() {
        val ip="prueba-"+UUID.randomUUID()
        repeat(20){assertEquals(401,assertThrows(ResponseStatusException::class.java){service.iniciar("no-existe","invalida",ip)}.statusCode.value())}
        assertEquals(429,assertThrows(ResponseStatusException::class.java){service.iniciar("no-existe","invalida",ip)}.statusCode.value())
    }
}
