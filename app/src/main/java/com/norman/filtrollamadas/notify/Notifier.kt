package com.norman.filtrollamadas.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.norman.filtrollamadas.R
import com.norman.filtrollamadas.ui.MainActivity

object Notifier {
    private const val CHANNEL = "desviadas"

    fun createChannel(ctx: Context) {
        val channel = NotificationChannel(CHANNEL, "Llamadas desviadas", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Aviso cada vez que Filtro desvía una llamada no identificada"
        }
        ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private const val SUMMARY_ID = 1001

    /**
     * Una sola notificación que se actualiza con el total del día;
     * solo suena la primera vez (luego se actualiza en silencio).
     */
    @SuppressLint("MissingPermission")
    fun diverted(ctx: Context, countToday: Int, number: String, reason: String, destination: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(
                if (countToday <= 1) "Llamada desviada a $destination"
                else "$countToday llamadas desviadas hoy a $destination"
            )
            .setContentText("Última: $number · $reason")
            .setNumber(countToday)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(SUMMARY_ID, notification)
    }
}
