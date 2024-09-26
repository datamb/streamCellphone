package com.example.cam3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.ExecuteCallback;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private Button buttonStartStreaming, buttonStopStreaming;
    private boolean isStreaming = false;
    private String rtmpUrl = "rtmp://192.168.0.134/live/stream"; // Seu URL RTMP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
        buttonStartStreaming = findViewById(R.id.buttonStartStreaming);
        buttonStopStreaming = findViewById(R.id.buttonStopStreaming);

        buttonStopStreaming.setEnabled(false);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            setupCamera();
        }

        buttonStartStreaming.setOnClickListener(v -> startStreaming());

        buttonStopStreaming.setOnClickListener(v -> stopStreaming());
    }

    private boolean isHardwareLevelSupported(CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        return requiredLevel <= deviceLevel;
    }

    private void setupCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[1];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

            if (!isHardwareLevelSupported(characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)) {
                Toast.makeText(this, "O dispositivo não suporta todos os recursos da API Camera2", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void startCameraPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    try {
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "Configuração da câmera falhou", Toast.LENGTH_SHORT).show();
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startStreaming() {
        if (isStreaming) {
            return;
        }

        isStreaming = true;
        buttonStopStreaming.setEnabled(true);

        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            Surface cameraSurface = new Surface(surfaceTexture);

            // Configura MediaCodec para codificação em H.264
            MediaCodec mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat format = MediaFormat.createVideoFormat("video/avc", 640, 480);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 1250000); // Bitrate
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30); // Frame rate
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // Intervalo de keyframes (1 segundo)

            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface inputSurface = mediaCodec.createInputSurface(); // Superfície para os frames codificados
            mediaCodec.start();

            // Use Camera2 API para capturar os frames e enviá-los ao inputSurface
            startCameraPreview(inputSurface);  // Passar inputSurface para o preview da câmera

            // Agora inicia a thread para codificar e enviar os dados para o servidor RTMP via FFmpeg
            new Thread(() -> {
                // Comando FFmpeg para pegar dados do MediaCodec e transmitir
                //String ffmpegCommand = String.format(
                 //       "-f lavfi -i anullsrc -f rawvideo -vcodec h264 -pix_fmt yuv420p -s 640x480 -r 30 -i - -c:v libx264 -preset veryfast -f mpegts %s",
                 //       rtmpUrl
                //);

                String[] ffmpegCommand = {
                        "-re",
                        "-f", "rawvideo",
                        "-vcodec", "h264",
                        "-pix_fmt", "nv21",
                        "-s", "640x480",
                        "-r", "30",
                        "-i", "-",
                        "-c:v", "libx264",
                        "-preset", "ultrafast",
                        "-b:v", "500k",
                        "-f","mpegts",
                        "rtmp://192.168.0.134/live/stream"
                };

                FFmpeg.executeAsync(ffmpegCommand, new ExecuteCallback() {
                    @Override
                    public void apply(long executionId, int returnCode) {
                        if (returnCode == 0) {
                            Log.i("FFmpeg", "Transmissão concluída com sucesso.");
                        } else {
                            Log.e("FFmpeg", "Erro durante a transmissão. Código: " + returnCode);
                        }
                        isStreaming = false;
                    }
                });
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startCameraPreview(Surface inputSurface) {
        // Usar Camera2 API para iniciar o preview da câmera na superfície do MediaCodec
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(inputSurface); // Adiciona o Surface de input do MediaCodec

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, inputSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    try {
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "Configuração da câmera falhou", Toast.LENGTH_SHORT).show();
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopStreaming() {
        if (!isStreaming) {
            return;
        }

        FFmpeg.cancel();
        isStreaming = false;
        buttonStopStreaming.setEnabled(false);
        Log.i("FFmpeg", "Transmissão interrompida.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Toast.makeText(this, "Permissão de câmera negada", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
