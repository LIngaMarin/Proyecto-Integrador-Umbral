package com.umbral.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.umbral.app.MainActivity
import com.umbral.app.R

class UmbralFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val tipoNotificacion = data["tipo"] ?: "GENERAL"
        val titulo = remoteMessage.notification?.title ?: data["titulo"] ?: "Umbral"
        val cuerpo = remoteMessage.notification?.body ?: data["cuerpo"] ?: ""

        when (tipoNotificacion) {
            "DUDA_DOCENTE" -> {
                val dudaId = data["dudaId"]?.toLongOrNull() ?: 0L
                mostrarNotificacionDudaDocente(titulo, cuerpo, dudaId)
            }
            "NUEVA_PREGUNTA" -> {
                val preguntaId = data["preguntaId"]?.toLongOrNull() ?: 0L
                val tema = data["tema"] ?: "General"
                mostrarNotificacionNuevaPregunta(titulo, cuerpo, preguntaId, tema)
            }
            else -> {
                mostrarNotificacionGeneral(titulo, cuerpo)
            }
        }
    }

    private fun mostrarNotificacionDudaDocente(titulo: String, cuerpo: String, dudaId: Long) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SCREEN", "EXPLICACION_IA")
            putExtra("DUDA_ID", dudaId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            dudaId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        crearCanalNotificacion(
            CHANNEL_DUDAS_DOCENTE_ID,
            "Dudas de Alumnos",
            "Notificaciones cuando un alumno envía una duda con foto"
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_DUDAS_DOCENTE_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(dudaId.toInt().coerceAtLeast(100), notification)
    }

    private fun mostrarNotificacionNuevaPregunta(titulo: String, cuerpo: String, preguntaId: Long, tema: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SCREEN", "PRATICA_TEMAS")
            putExtra("PREGUNTA_ID", preguntaId)
            putExtra("TEMA", tema)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            preguntaId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        crearCanalNotificacion(
            CHANNEL_PREGUNTAS_ID,
            "Nuevas Preguntas de Práctica",
            "Notificaciones al publicarse nuevas preguntas de práctica"
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_PREGUNTAS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(preguntaId.toInt().coerceAtLeast(200), notification)
    }

    private fun mostrarNotificacionGeneral(titulo: String, cuerpo: String) {
        crearCanalNotificacion(
            CHANNEL_GENERAL_ID,
            "Notificaciones Generales",
            "Avisos y mensajes generales de la aplicación"
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_GENERAL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun crearCanalNotificacion(channelId: String, nombre: String, descripcion: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, nombre, importance).apply {
                this.description = descripcion
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_DUDAS_DOCENTE_ID = "channel_dudas_docente"
        const val CHANNEL_PREGUNTAS_ID = "channel_preguntas_practica"
        const val CHANNEL_GENERAL_ID = "channel_general"

        fun suscribirATema(tema: String) {
            val topicLimpio = tema.lowercase().replace(" ", "_").replace("[^a-z0-9_]".toRegex(), "")
            FirebaseMessaging.getInstance().subscribeToTopic("tema_$topicLimpio")
        }

        fun desuscribirDeTema(tema: String) {
            val topicLimpio = tema.lowercase().replace(" ", "_").replace("[^a-z0-9_]".toRegex(), "")
            FirebaseMessaging.getInstance().unsubscribeFromTopic("tema_$topicLimpio")
        }
    }
}
