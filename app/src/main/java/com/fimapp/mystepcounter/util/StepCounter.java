package com.fimapp.mystepcounter.util;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.content.Context;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.TextView;

import com.fimapp.mystepcounter.activity.MainActivity;

/**
 * Created by Fima on 2017/3/11.
 */

public class StepCounter implements SensorEventListener {

    SensorManager sensorManager;
    Context context;
    Sensor sensorAccelerometer, sensorMagnetometer;

    TextView textview_stepCountText, textview_stepLengthText, textview_angelText, textview_coordText;

    int maLength = 5, stepState = 0, stepCount = 0;
    long lastTimeAcc, curTimeAcc, lastTimeMag, curTimeMag;

    float[] accValues = new float[3];
    float[] magValues = new float[3];
    float[] values = new float[3];
    float[] R = new float[9];
    float[] I = new float[9];
    float accModule = 0, maResult = 0;
    float maxVal = 0f, minVal = 0f, stepLength = 0f;

    //计步参数，可以根据情况设置调整
    static int stepCountDelaySecond = 0;
    static float accThreshold = 0.65f, co_k_wein = 45f, alpha = 0.25f;

    int degreeDisplay, sensorCounter;
    float offset, degree;
    DecimalFormat decimalF = new DecimalFormat("#.00");
    static float[] curCoordsOfStep = {137, 642};
    static ArrayList<CoordPoint> points = new ArrayList<CoordPoint>();

    //构造函数 初始化
    public StepCounter(Context context, TextView textview_stepCountText, TextView textview_stepLengthText,
                       TextView textview_angelText, TextView textview_coordText) {
        this.context = context;
        this.textview_stepCountText = textview_stepCountText;
        this.textview_stepLengthText = textview_stepLengthText;
        this.textview_angelText = textview_angelText;
        this.textview_coordText = textview_coordText;

        loadSystemService();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            curTimeAcc = System.currentTimeMillis();
            if (curTimeAcc - lastTimeAcc > 40) {
                getStepAccInfo(event.values.clone());
                lastTimeAcc = curTimeAcc;
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            curTimeMag = System.currentTimeMillis();
            if (curTimeMag - lastTimeMag > 40) {
                getAzimuthDegree(event.values.clone());
                lastTimeMag = curTimeMag;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //初始化传感器
    public void loadSystemService() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    //开始计步
    public void startCountStep() {
        lastTimeAcc = System.currentTimeMillis();
        lastTimeMag = System.currentTimeMillis();
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    //停止计步
    public void stopCountStep() {
        sensorManager.unregisterListener(this);
    }

    //计步算法
    private void getStepAccInfo(float[] accClone) {
        accValues = accClone;
        accModule = (float) (Math.sqrt(Math.pow(accValues[0], 2) + Math.pow(accValues[1], 2) + Math.pow(accValues[2], 2)) - 9.794);
        maResult = MovingAverage.movingAverage(accModule, maLength);

        if (stepState == 0 && maResult > accThreshold) {
            stepState = 1;
        }
        if (stepState == 1 && maResult > maxVal) { //步伐波峰
            maxVal = maResult;
        }
        if (stepState == 1 && maResult <= 0) {
            stepState = 2;
        }
        if (stepState == 2 && maResult < minVal) { //步伐波谷
            minVal = maResult;
        }
        if (stepState == 2 && maResult >= 0) {
            stepCount ++;
            getStepLengthAndCoordinate();
            recordTrajectory(curCoordsOfStep.clone());
            maxVal = minVal = stepState = 0;
        }

        stepViewShow();
    }

    private void stepViewShow() {
        textview_stepCountText.setText(stepCount + " 步");
        textview_stepLengthText.setText(decimalF.format(stepLength) + " cm");
        textview_coordText.setText("X: " + decimalF.format(curCoordsOfStep[0]) + " Y: "
                + decimalF.format(curCoordsOfStep[1]));
    }

    //计算步长
    private void getStepLengthAndCoordinate() {
        stepLength = (float)(co_k_wein * Math.pow(maxVal - minVal,1.0/4));
        double delta_x = Math.cos(Math.toRadians(degreeDisplay)) * stepLength;
        double delta_y = Math.sin(Math.toRadians(degreeDisplay)) * stepLength;
        curCoordsOfStep[0] += delta_x;
        curCoordsOfStep[1] += delta_y;
    }

    private void recordTrajectory(float[] clone) {
        points.add(new CoordPoint(clone[0], clone[1]));
    }

    private void getAzimuthDegree(float[] MagClone) {
        magValues = lowPassFilter(MagClone, magValues);
        if (accValues == null || magValues == null) return;
        boolean sucess = SensorManager.getRotationMatrix(R, I, accValues, magValues);
        if (sucess) {
            SensorManager.getOrientation(R, values);
            degree = (int)(Math.toDegrees(values[0]) + 360) % 360;
            degree = ((int)(degree + 2)) / 5 * 5;
            if (offset == 0) {
                degreeDisplay = (int) degree;
            } else {
                degreeDisplay = roomDirection(degree, offset);
            }

            stepDegreeViewShow();
        }
    }

    private void stepDegreeViewShow() {
        textview_angelText.setText(degreeDisplay + " °");
    }

    private int roomDirection(float myDegree, float myOffset) {
        int tmp = (int)(myDegree - myOffset);
        if(tmp < 0) tmp += 360;
        else if(tmp >= 360) tmp -= 360;
        return tmp;
    }

    private float[] lowPassFilter(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }

}
