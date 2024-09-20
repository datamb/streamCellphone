package com.example.cam3;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;

public class MainActivity extends AppCompatActivity {

    private Button startStreamButton;
    private boolean isStreaming = false;
    private String rtmpUrl = "rtmp://192.168.0.134/live/stream";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SurfaceView cameraView = findViewById(R.id.camera_view);
        startStreamButton = findViewById(R.id.start_stream_button);

        startStreamButton.setOnClickListener(v -> {
            if (isStreaming) {
                stopStream();
            } else {
                startStream();
            }
        });
    }

    private void startStream() {
        // Configurar captura da câmera com SurfaceView aqui
        // ...

        // Comando FFmpeg para transmitir o vídeo para o servidor RTMP
        String ffmpegCommand = "-f lavfi -i anullsrc -f android_camera -video_size 640x480 -r 30 -i 0:0 -c:v libx264 -pix_fmt yuv420p -f flv " + rtmpUrl;

        FFmpegSession session = FFmpegKit.executeAsync(ffmpegCommand, sessionState -> {
            if (sessionState.getReturnCode().isValueSuccess()) {
                // Transmissão iniciada com sucesso
            } else {
                // Erro ao iniciar a transmissão
            }
        });

        isStreaming = true;
        startStreamButton.setText("Stop Stream");
    }

    private void stopStream() {
        FFmpegKit.cancel();
        isStreaming = false;
        startStreamButton.setText("Start Stream");
    }
}
