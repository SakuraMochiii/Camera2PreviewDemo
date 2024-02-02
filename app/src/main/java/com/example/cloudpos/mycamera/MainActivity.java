package com.example.cloudpos.mycamera;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cloudpos.mycamera.R;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";
    Button button;
    Context context;
    private TextView textView;
    CameraManager mCameraManager;
    int cameraCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initParam();
    }

    public void initParam() {
        context = getApplicationContext();
        button = findViewById(R.id.button);
        textView = findViewById(R.id.textView);
        try {
            cameraCount = mCameraManager.getCameraIdList().length;
            Log.d(TAG, "camera count ： =  " + cameraCount);
            for (String s : mCameraManager.getCameraIdList()) {
                Log.d(TAG, "camera list ：" + s);
            }
            textView.setText("camera count：" + cameraCount);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog();
            }
        });
    }


    public void openDialog() {
        View view = getLayoutInflater().inflate(R.layout.half_dialog_view, null);
        final EditText editText = (EditText) view.findViewById(R.id.dialog_edit);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Set Camera")//set title
                .setView(view)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String index = editText.getText().toString();
                        if (("".equals(index)) || (Integer.parseInt(index) > cameraCount || Integer.parseInt(index) < 1)) {
                            Toast.makeText(context, "index value error", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                            Integer index2 = Integer.valueOf(index) - 1;
                            Log.d(TAG, "index.toString() = " + index2);
                            intent.putExtra("cameraId", index2.toString());
                            startActivity(intent);
                        }
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }

}
