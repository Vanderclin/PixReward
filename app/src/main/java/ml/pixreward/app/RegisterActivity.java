package ml.pixreward.app;

import android.Manifest;
import android.annotation.NonNull;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;
import ml.pixreward.app.RegisterActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText mEditTextUsername, mEditTextPhone;
    private Button mButtonRegister;
    private DatabaseReference mDatabase;
    private String device_id;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;
    private boolean mValue;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registry);
        mDatabase = FirebaseDatabase.getInstance().getReference("users");
        openDialog();
        loadDevice();
        loadIMEI();

        mEditTextUsername = (EditText) findViewById(R.id.edittext_set_username);
        mEditTextPhone = (EditText) findViewById(R.id.edittext_register_phone);
        mButtonRegister = (Button) findViewById(R.id.button_register_user);


        mButtonRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String username = mEditTextUsername.getText().toString();
                    String phone = mEditTextPhone.getText().toString();

                    if (TextUtils.isEmpty(username)) {
                        Toast.makeText(RegisterActivity.this, "Preencha por favor", Toast.LENGTH_SHORT).show();
                    }
                    if (TextUtils.isEmpty(phone)) {
                        Toast.makeText(RegisterActivity.this, "Preencha por favor", Toast.LENGTH_SHORT).show();
                    } else {
                        Map<String, Object> values = new HashMap<>();
                        values.put("current_username", username);
                        values.put("current_phone", phone);
                        values.put("current_device", device_id);
                        mDatabase.child(device_id).setValue(values);
                    }

                }
            });


    }

    private void openDialog() {
        mValue = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("mValue", true);

        if (mValue) {
            AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);
            mAlertDialog.setTitle("Requisito");
            mAlertDialog.setMessage("O registro do usuário e associado ao dispositivo, solicitamos a permissão \"READ_PHONE_STATE\" para obter o IMEI do dispositivo para que o mesmo possa ser registrado com nome e número do usuário.");
            mAlertDialog.setCancelable(false);
            mAlertDialog.setPositiveButton("Solicitar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            mAlertDialog.show();

            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("mValue", true).commit();
        }
    }





    public void loadDevice() {
        mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.child(device_id).exists() == true) {
                        Toast.makeText(RegisterActivity.this, "True", Toast.LENGTH_SHORT).show();
                        // startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    } else {
                        Toast.makeText(RegisterActivity.this, "False", Toast.LENGTH_SHORT).show();
                        // loadIMEI();
                    }

                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("MainActivity", "Failed to read value.", error.toException());
                }
            });
    }

    public void loadIMEI() {
        // Check if the READ_PHONE_STATE permission is already available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            // READ_PHONE_STATE permission has not been granted.
            requestReadPhoneStatePermission();
        } else {
            // READ_PHONE_STATE permission is already been granted.
            doPermissionGrantedStuffs();
        }
    }



    /**
     * Requests the READ_PHONE_STATE permission.
     * If the permission has been denied previously, a dialog will Prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private void requestReadPhoneStatePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                                Manifest.permission.READ_PHONE_STATE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            new AlertDialog.Builder(RegisterActivity.this)
                .setTitle("Permission Request")
                .setMessage(getString(R.string.permission_read_phone_state_rationale))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //re-request
                        ActivityCompat.requestPermissions(RegisterActivity.this,
                                                          new String[]{Manifest.permission.READ_PHONE_STATE},
                                                          MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
                    }
                })
                .show();
        } else {
            // READ_PHONE_STATE permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},
                                              MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE) {
            // Received permission result for READ_PHONE_STATE permission.est.");
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // READ_PHONE_STATE permission has been granted, proceed with displaying IMEI Number
                //alertAlert(getString(R.string.permision_available_read_phone_state));
                doPermissionGrantedStuffs();
            } else {
                alertAlert(getString(R.string.permissions_not_granted_read_phone_state));
            }
        }
    }

    private void alertAlert(String msg) {
        new AlertDialog.Builder(RegisterActivity.this)
            .setTitle("Permission Request")
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // do somthing here
                }
            })
            .show();
    }


    public void doPermissionGrantedStuffs() {
        //Have an  object of TelephonyManager
        TelephonyManager mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Get IMEI Number of Phone  //////////////// for this example i only need the IMEI
        device_id = mTelephonyManager.getDeviceId();
    }
}
