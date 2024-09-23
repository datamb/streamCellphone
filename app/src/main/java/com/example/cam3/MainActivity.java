package com.example.cam3;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
    private TextView textViewVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonSelectVideo = findViewById(R.id.buttonSelectVideo);
        buttonStartStreaming = findViewById(R.id.buttonStartStreaming);
        textViewVideoPath = findViewById(R.id.textViewVideoPath);

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
        // Caminho do vídeo selecionado. Pode ser ajustado conforme necessário
        //videoPath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/VID-20240903-WA0044.mp4";

        // URL do servidor RTMP (exemplo)
        String rtmpUrl = "rtmp://192.168.0.134/live/stream";

        // Comando FFmpeg para streaming de vídeo
        String ffmpegCommand = String.format("-re -i \"%s\" -c:v libx264 -preset veryfast -f flv \"%s\"", videoPath, rtmpUrl);

        // Executa o comando FFmpeg de forma síncrona
        int rc = FFmpeg.execute(ffmpegCommand);

        // Verifica o código de retorno e loga o resultado
        if (rc == 0) {
            Log.i("FFmpeg", "Transmissão concluída com sucesso.");
        } else {
            Log.e("FFmpeg", "Erro na transmissão. Código de retorno: " + rc);
        }
    }
}

