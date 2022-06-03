package com.jrockit.comunicacion;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Button jbtnEnviar, jbtnDesconexion;
    TextView jtvChat, jtvIp;
    EditText jetMensaje, jetIP;

    Socket s;
    ServerSocket ss;
    DataOutputStream dos;
    DataInputStream dis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Botones
        jbtnEnviar = (Button) findViewById(R.id.xbtnEnviar);
        jbtnDesconexion = (Button) findViewById(R.id.xbtnDesconexion);

        // TextView
        jtvChat = (TextView) findViewById(R.id.xtvChat);
        jtvIp = (TextView) findViewById(R.id.xtvIp);

        // EditText
        jetMensaje = (EditText) findViewById(R.id.xetMensaje);
        jetIP = (EditText) findViewById(R.id.xetIp);

        // Mostramos nuestra la IP
        jtvIp.append("Mi IP: " + obtenerIp());

        // Hilo para el servidor
        Thread myThread = new Thread(new MiServidor());
        myThread.start();

        // Boton Enviar
        jbtnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Agregamos conversacion al CHAT
                jtvChat.append("\nTú: " + jetMensaje.getText().toString());
                Toast.makeText(getApplicationContext(), "Mensaje Enviado", Toast.LENGTH_SHORT).show();

                BackgroundTask b = new BackgroundTask();
                b.execute(jetIP.getText().toString(), jetMensaje.getText().toString());

                // Limpiamos casilla de mensaje
                jetMensaje.setText("");
            }
        });

        // Boton desconexion
        jbtnDesconexion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    // Cerramos
                    dos.close();
                    dis.close();
                    ss.close();
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Informamos Desconexion
                Toast.makeText(getApplicationContext(), "Desconectado...", Toast.LENGTH_SHORT).show();
                // Limpiamos casilla de Chat
                jetMensaje.setText("CHAT: ");
                // Limpiamos la casilla de IP
                jetIP.setText("");
            }
        });
    }

    // Clase para el envio de mensajes
    class BackgroundTask extends AsyncTask<String, Void, String> {
        String ip, mensaje;

        @Override
        protected String doInBackground(String... strings) {
            // Obtenemos la IP ingresada
            ip = strings[0];
            // Obtenemos el mensaje ingresado
            mensaje = strings[1];

            try {
                s = new Socket(ip, 5432);
                dos = new DataOutputStream(s.getOutputStream());
                // Enviamos el mensaje
                dos.writeUTF(mensaje);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    // Clase para iniciar servidor y recibir mensajes
    class MiServidor implements Runnable {
        //ServerSocket ss;
        Socket miSocket;
        //DataInputStream dis;
        String mensaje;
        Handler handler = new Handler();

        @Override
        public void run() {
            try {
                ss = new ServerSocket(5432);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Esperando conexión...", Toast.LENGTH_SHORT).show();
                    }
                });

                while (true) {
                    s = ss.accept();
                    dis = new DataInputStream(s.getInputStream());
                    mensaje = dis.readUTF();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Agregamos conversacion al CHAT
                            jtvChat.append("\nAndroid: " + mensaje);
                            Toast.makeText(getApplicationContext(), "Mensaje Recibido", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Obtenemos la IP del dispositivo para mostrarla y que sea más facil su visualización
    public static String obtenerIp() {
        List<InetAddress> addrs;
        String address = "";
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        address = addr.getHostAddress().toUpperCase(new Locale("es", "MX"));
                    }
                }
            }
        } catch (Exception e) {
            Log.w("F", "Ex obteniendo valor de la IP" + e.getMessage());
        }
        return address;
    }
}