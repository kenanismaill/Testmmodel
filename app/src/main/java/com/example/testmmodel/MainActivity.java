package com.example.testmmodel;

import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    Interpreter tflite;
    Button bt;
    private TextToSpeech mTts;
    ImageView imageView;
    /////////// my definations/////////

    private final int Number_Of_Sample = 400;
    private static List Ax;
    private static List Ay;
    private static List Az;
    private static List Gx;
    private static List Gy;
    private static List Gz;
    private static List processed_input ;

    private SensorManager mSensorManager;
    private Sensor mlinearAcc;
    private Sensor Gyro;
    private TextView activtyTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Ax = new ArrayList();
        Ay = new ArrayList();
        Az = new ArrayList();
        Gx = new ArrayList();
        Gy = new ArrayList();
        Gz = new ArrayList();

        processed_input  = new ArrayList();

        activtyTextView = findViewById(R.id.ActivityTV);
        imageView=findViewById(R.id.imageView);

        mSensorManager = (SensorManager) getSystemService(getApplicationContext().SENSOR_SERVICE);
        mlinearAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Gyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception ex){
            ex.printStackTrace();
        }
        mTts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status==1)
                {
                    mTts.setLanguage(Locale.UK);
                }
            }
        });

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        float ax_mean= (float) -0.01573707;
        float ay_mean= (float) -0.01251529;
        float az_mean= (float) -0.04038151;
        float gx_mean= (float) 0.00760838;
        float gy_mean= (float) -0.00161912;
        float gz_mean= (float) -0.00161912;


//        event.values[0]=((event.values[0]-ax_mean)/400);
        switch (event.sensor.getType()) {

            case Sensor.TYPE_LINEAR_ACCELERATION:
                Ax.add(event.values[0]);
                Ay.add(event.values[1]);
                Az.add(event.values[2]);
//                Log.d("ax", String.valueOf(event.values[0]));
                break;
            case Sensor.TYPE_GYROSCOPE:
                Gx.add(event.values[0]);
                Gy.add(event.values[1]);
                Gz.add(event.values[2]);
                break;
        }
        ActivityRecognation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mlinearAcc,
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, Gyro,
                SensorManager.SENSOR_DELAY_GAME);
    }


    private void ActivityRecognation(){


        if (Ax.size() >= Number_Of_Sample && Ay.size() >= Number_Of_Sample && Az.size() >= Number_Of_Sample &&
                Gx.size() >= Number_Of_Sample && Gy.size() >= Number_Of_Sample && Gz.size() >= Number_Of_Sample) {
//            Log.d("Results", "result");


            int i = 0;
            while (i < Number_Of_Sample) {

                processed_input .add(Ax.get(i));
                processed_input .add(Ay.get(i));
                processed_input .add(Az.get(i));
                processed_input .add(Gx.get(i));
                processed_input .add(Gy.get(i));
                processed_input .add(Gz.get(i));
                i++;
            }
            float [] myarray = toFloatArray(processed_input );
            float [][][][] inputarray= new float[1][400][6][1];
            for(int k=0;k<400;k++)
            {
                for (int j=0;j<6;j++)
                {
                    inputarray[0][k][j][0]=myarray[6*k+j];
                }
            }
            float [][] outputarray= new float [1][2];
            tflite.run(inputarray,outputarray);
            float [] result=outputarray[0];
//            Log.d("result0", String.valueOf(result[0]));
//            Log.d("result1", String.valueOf(result[1]));
//            Log.d("result", String.valueOf(result));
//            Log.d("result", String.valueOf(result));
//
//
            if (round(result[0],2)>round(result[1],2))
            {
                activtyTextView.setText("sitting");
                mTts.speak((String) activtyTextView.getText(),TextToSpeech.QUEUE_FLUSH,null);
                imageView.setImageResource(R.drawable.sitting);
            }
            else if (round(result[1],2)>round(result[0],2))
            {
                activtyTextView.setText(" standing");
                mTts.speak((String) activtyTextView.getText(),TextToSpeech.QUEUE_FLUSH,null);
                imageView.setImageResource(R.drawable.standingg);
            }

            // Clear all the values
            Ax.clear();
            Ay.clear();
            Az.clear();
            Gx.clear();
            Gy.clear();
            Gz.clear();
            processed_input .clear();

        }
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for(int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
    private MappedByteBuffer loadModelFile() throws IOException {

        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
}
