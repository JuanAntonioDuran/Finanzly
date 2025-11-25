package com.example.finanzly.services;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.example.finanzly.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    // Se llama cuando se genera un token nuevo
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Aquí envías el token a tu servidor para asociarlo al usuario
        sendTokenToServer(token);
    }

    // Se llama cuando llega un mensaje
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String mensaje = null;

        // Mensaje enviado en "data"
        if (remoteMessage.getData().size() > 0) {
            mensaje = remoteMessage.getData().get("message");
        }

        // Mensaje enviado en "notification"
        if (remoteMessage.getNotification() != null) {
            mensaje = remoteMessage.getNotification().getBody();
        }

        if (mensaje != null) {
            mostrarNotificacion(mensaje);
        }
    }

    private void mostrarNotificacion(String mensaje) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "default_channel";

        // Crear canal (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Notificaciones",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Construir y mostrar la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Nuevo movimiento")
                .setContentText(mensaje)
                .setSmallIcon(R.drawable.ic_notification) // tu icono de notificación
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        NotificationManagerCompat.from(this).notify(0, builder.build());
    }

    private void sendTokenToServer(String token) {
        // TODO: Aquí haces la petición a tu backend para guardar el token del usuario
    }
}
