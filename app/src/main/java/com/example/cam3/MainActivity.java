package com.example.cam3;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.arthenica.mobileffmpeg.FFmpeg;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_VIDEO = 1;
    private String videoPath;
    private Button buttonSelectVideo;
    private Button buttonStartStreaming;
    private Button buttonStopStreaming;
    private TextView textViewVideoPath;
    private boolean isStreaming = false;
    private Handler handler;
    private Thread streamThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonSelectVideo = findViewById(R.id.buttonSelectVideo);
        buttonStartStreaming = findViewById(R.id.buttonStartStreaming);
        buttonStopStreaming = findViewById(R.id.buttonStopStreaming);
        textViewVideoPath = findViewById(R.id.textViewVideoPath);

        handler = new Handler();

        buttonSelectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectVideo();
            }
        });

        buttonStartStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStreaming();
            }
        });

        buttonStopStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopStreaming();
            }
        });
    }

    private void selectVideo() {
        // Abre a galeria para escolher o vídeo
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        startActivityForResult(intent, REQUEST_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VIDEO && resultCode == RESULT_OK && data != null) {
            // Obtém o caminho real do vídeo selecionado
            Uri selectedVideoUri = data.getData();
            videoPath = getRealPathFromURI(selectedVideoUri);
            textViewVideoPath.setText(videoPath);
            buttonStartStreaming.setEnabled(true); // Habilita o botão de transmissão após o vídeo ser selecionado
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        // Obtém o caminho absoluto do arquivo de vídeo
        String[] proj = {MediaStore.Video.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private void startStreaming() {
        if (isStreaming) {
            return;
        }

        isStreaming = true;

        streamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isStreaming) {
                    // Caminho do vídeo selecionado
                    String rtmpUrl = "rtmp://192.168.0.134/live/stream";
                    String ffmpegCommand = String.format("-re -i \"%s\" -c:v libx264 -preset veryfast -f flv \"%s\"", videoPath, rtmpUrl);

                    // Executa o comando FFmpeg de forma síncrona
                    int rc = FFmpeg.execute(ffmpegCommand);

                    // Verifica o código de retorno
                    if (rc == 0) {
                        Log.i("FFmpeg", "Transmissão em loop.");
                    } else {
                        Log.e("FFmpeg", "Erro na transmissão. Código de retorno: " + rc);
                        break;
                    }

                    // Pequena pausa antes de reiniciar o loop
                    try {
                        Thread.sleep(1000); // 1 segundo de pausa antes de iniciar novamente
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        streamThread.start();
    }

    private void stopStreaming() {
        // Para a transmissão
        isStreaming = false;
        if (streamThread != null && streamThread.isAlive()) {
            streamThread.interrupt();
        }
        Log.i("FFmpeg", "Transmissão interrompida.");
    }
}
