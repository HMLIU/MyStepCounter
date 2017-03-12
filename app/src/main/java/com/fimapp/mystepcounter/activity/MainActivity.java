package com.fimapp.mystepcounter.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.fimapp.mystepcounter.R;
import com.fimapp.mystepcounter.util.StepCounter;

public class MainActivity extends AppCompatActivity {

    private TextView titleLabel;
    private TextView stepCountText;
    private TextView stepLengthText;
    private TextView angelText;
    private TextView coordText;

    StepCounter stepCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titleLabel = (TextView) findViewById(R.id.titleLabel);
        titleLabel.setText("计步器 Demo");

        stepCountText = (TextView) findViewById(R.id.stepCountText);
        stepCountText.setText("0 步");

        stepLengthText = (TextView) findViewById(R.id.stepLengthText);
        stepLengthText.setText("0.00 cm");

        angelText = (TextView) findViewById(R.id.angelText);
        angelText.setText("0 °");

        coordText = (TextView) findViewById(R.id.coordText);
        coordText.setText("X:0.00  Y:0.00");

        //初始化计步器
        stepCounter = new StepCounter(MainActivity.this, stepCountText, stepLengthText, angelText, coordText);
    }

    public void startStep(View view) {
        //开始计步
        stepCounter.startCountStep();
    }

    public void stopStep(View view) {
        //停止计步
        stepCounter.stopCountStep();
    }
}
