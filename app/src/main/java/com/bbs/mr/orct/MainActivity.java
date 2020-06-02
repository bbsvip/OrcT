package com.bbs.mr.orct;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    ImageView imgV;
    Button btnTake, btnLoad, btnSave;
    EditText edtName, edtClass, edtId, edtSub, edtPoint;
    Bitmap selectedBitmap;
    String url = "http://192.168.1.88:8081/server/InsertData.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgV = findViewById(R.id.imgV);
        btnTake = findViewById(R.id.btnTake);
        btnLoad = findViewById(R.id.btnLoad);
        btnSave = findViewById(R.id.btnSave);
        edtName = findViewById(R.id.edtName);
        edtClass = findViewById(R.id.edtClass);
        edtId = findViewById(R.id.edtId);
        edtSub = findViewById(R.id.edtSub);
        edtPoint = findViewById(R.id.edtPoint);
        btnTake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capturePicture();
            }
        });
        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePicture();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSV();
            }
        });
    }
    void ClearEdt(){
        edtName.setText("");
        edtClass.setText("");
        edtId.setText("");
        edtSub.setText("");
        edtPoint.setText("");
        imgV.setImageBitmap(null);
    }
    private void UpdateSV(){

        RequestQueue requestQueue = Volley.newRequestQueue( this );
        StringRequest request = new StringRequest( Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.toString().equals( "Success" )) {
                            Toast.makeText(MainActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                            ClearEdt();
                        }else{
                            Toast.makeText(MainActivity.this, "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d( "ERRO_CANLEDARWORK", error.toString() );
                    }
                } ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> prams = new HashMap<>();
                prams.put( "Diem", edtPoint.getText().toString() );
                prams.put( "TenSV", edtName.getText().toString() );
                prams.put( "MaSV", edtId.getText().toString() );
                return prams;
            }
        };
        requestQueue.add( request );
    }

    private void choosePicture() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, 200);
    }

    private void capturePicture() {
        Intent cInt = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cInt, 100);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100 && resultCode == RESULT_OK) {
            selectedBitmap = (Bitmap) data.getExtras().get("data");
            imgV.setImageBitmap(selectedBitmap);
            if (selectedBitmap != null) runTextReco(selectedBitmap);
        } else if (requestCode == 200 && resultCode == RESULT_OK) {
            try {
                Uri imageUri = data.getData();
                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imgV.setImageBitmap(selectedBitmap);
                if (selectedBitmap != null) runTextReco(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runTextReco(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();
                                if (blocks.size() == 0) {
                                    Toast.makeText(MainActivity.this, "Không nhận diện được hình ảnh!", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                for (int i = 0; i < blocks.size(); i++) {
                                    List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
                                    for (int j = 0; j < lines.size(); j++) {
                                        /*Log.i("-", j+"");
                                        Log.i("--------", lines.get(j).getText());*/
                                        String [] output = lines.get(j).getText().split(":");
                                        String outputText = output[0].toLowerCase().replaceAll("\\s+","");
                                        String outputText2 = output[1].trim();
                                        if (outputText.equals("hovaten")) edtName.setText(outputText2);
                                        else if (outputText.equals("lop")) edtClass.setText(outputText2);
                                        else if (outputText.equals("masinhvien")) edtId.setText(outputText2);
                                        else if (outputText.equals("monhoc")) edtSub.setText(outputText2);
                                        else if (outputText.equals("diem")) edtPoint.setText(outputText2);
                                        else
                                            Toast.makeText(MainActivity.this, "Dữ liệu nhận dạng không phù hợp!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
    }

    BroadcastReceiver checkInternet = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
            if (connectivityManager.getActiveNetworkInfo() != null) {
            } else {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        turnOnInternet();
                    }
                }, 1000);

            }
        }
    };

    private void turnOnInternet() {


        Button yes, no;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_network);
        yes = dialog.findViewById(R.id.btnYesCheckNet);
        no = dialog.findViewById(R.id.btnNoCheckNet);
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                wifi.setWifiEnabled(true);
                dialog.dismiss();
            }
        });
        dialog.show();
    }
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(checkInternet, filter);
    }
    protected void onPause() {
        super.onPause();
        unregisterReceiver(checkInternet);
    }
}
