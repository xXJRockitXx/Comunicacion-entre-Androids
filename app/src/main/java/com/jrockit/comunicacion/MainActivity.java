package com.jrockit.comunicacion;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Button jbtnEnviar, jbtnDesconexion, jbtnArchivo;
    TextView jtvChat, jtvIp;
    EditText jetMensaje, jetIP;

    Socket s;
    ServerSocket ss;
    DataOutputStream dos;
    DataInputStream dis;

    Uri ruta, inicio;
    File archivo;
    FileReader fr;
    FileWriter fw;
    BufferedReader br;
    BufferedWriter bw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Botones
        jbtnEnviar = (Button) findViewById(R.id.xbtnEnviar);
        jbtnDesconexion = (Button) findViewById(R.id.xbtnDesconexion);
        jbtnArchivo = (Button) findViewById(R.id.xbtnArchivo);

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

        // Obtenemos la ruta de la carpeta forensics
        String path = Environment.getExternalStorageDirectory() + "/" + "forensics/";

        // Lo convertimos a un uri que nos servira para abrir esa carpeta por defecto
        inicio = Uri.parse(path);

        // Boton Enviar
        jbtnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Agregamos conversacion al CHAT
                jtvChat.append("\nTú: " + jetMensaje.getText().toString());
                if (jetMensaje.getText().toString().contains(".")) {
                    Toast.makeText(getApplicationContext(), "Archivo Enviado", Toast.LENGTH_SHORT).show();
                } else
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
                jtvChat.setText("CHAT: ");
                jetMensaje.setText("");
                // Limpiamos la casilla de IP
                jetIP.setText("");
            }
        });

        // Boton Archivo
        jbtnArchivo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Que abra directamente la carpeta de foreincs
                openFile(inicio);
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
                            if (mensaje.contains(".")) {
                                Toast.makeText(getApplicationContext(), "Archivo Recibido", Toast.LENGTH_SHORT).show();
                                //jtvChat.append("\nAndroid: " + mensaje);
                            } else {
                                //jtvChat.append("\nAndroid: " + mensaje);
                                Toast.makeText(getApplicationContext(), "Mensaje Recibido", Toast.LENGTH_SHORT).show();
                            }

                            jtvChat.append("\nAndroid: " + mensaje);
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

    // Request code for selecting a PDF document.
    private static final int XML = 2;

    // Seleccionamos el archivo .xml
    private void openFile(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Que sea cualquier tipo de archivo
        intent.setType("*/*");

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, XML);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        //super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == XML && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            ruta = null;
            if (resultData != null) {
                // Guardamos el uri del archivo seleccionado
                ruta = resultData.getData();
                archivo = new File(ruta.getPath());
                jetMensaje.append("" + ruta.getPath());
            }
        }
    }
}