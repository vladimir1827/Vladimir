package com.example.axel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private LineChartView lineChartView;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private boolean isRecording = false;
    private FileWriter csvWriter;
    private File tempCsvFile;
    private long recordingStartTime;
    private TextView gyroXText, gyroYText, gyroZText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lineChartView = findViewById(R.id.line_chart_view);
        gyroXText = findViewById(R.id.gyro_x);
        gyroYText = findViewById(R.id.gyro_y);
        gyroZText = findViewById(R.id.gyro_z);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);

            SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
            boolean keepScreenOn = sharedPreferences.getBoolean("KeepScreenOn", false);

            if (keepScreenOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        Button recordButton = findViewById(R.id.record_button);
        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
                recordButton.setText("▶");
            } else {
                startRecording();
                recordButton.setText("■");
            }
        });
    }

    private void startRecording() {
        isRecording = true;
        recordingStartTime = System.currentTimeMillis();
        String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date()) + ".csv";
        tempCsvFile = new File(getCacheDir(), fileName);

        try {
            csvWriter = new FileWriter(tempCsvFile);
            csvWriter.append("time;x;y;z\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "Запись началась", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        isRecording = false;

        try {
            if (csvWriter != null) {
                csvWriter.flush();
                csvWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        showSaveDialog();
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Сохранить запись");

        String defaultFileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());

        EditText input = new EditText(this);
        input.setText(defaultFileName);
        input.setSelection(0, defaultFileName.length());
        input.setHint("Введите название файла");
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String userFileName = input.getText().toString().trim();
            if (userFileName.isEmpty()) {
                userFileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            }
            File finalFile = new File(getCacheDir(), userFileName + ".csv");

            if (tempCsvFile.renameTo(finalFile)) {
                shareSavedFile(finalFile);
            } else {
                Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void shareSavedFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(this, "com.example.axel.provider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Отправить файл через"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0]; // Угловая скорость по оси X
            float y = event.values[1]; // Угловая скорость по оси Y
            float z = event.values[2]; // Угловая скорость по оси Z

            Log.d("GyroscopeData", String.format(Locale.getDefault(), "Raw: x=%.6f, y=%.6f, z=%.6f", x, y, z));

            lineChartView.addDataPoint(x, y, z, 0); // Последний параметр (totalAcceleration) не используется для гироскопа

            gyroXText.setText(String.format("x=%.6f", x));
            gyroYText.setText(String.format(" y=%.6f", y));
            gyroZText.setText(String.format(" z=%.6f", z));

            writeDataToCsv(x, y, z); // Запись данных в CSV
        }
    }

    private void writeDataToCsv(float x, float y, float z) {
        if (csvWriter != null) {
            try {
                double timeSinceStart = (System.currentTimeMillis() - recordingStartTime) / 1000.0;
                csvWriter.append(String.format(Locale.getDefault(), "%.3f;%.6f;%.6f;%.6f\n",
                        timeSinceStart, x, y, z));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}