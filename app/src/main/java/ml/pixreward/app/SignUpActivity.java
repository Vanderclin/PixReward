package ml.pixreward.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    
    private EditText mEditFullName, mEditEmail, mEditPassword;
    private Button buttonSignUp;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        //Get Firebase auth instance
        mAuth = FirebaseAuth.getInstance();

        buttonSignUp = (Button) findViewById(R.id.sign_up_button);
        mEditFullName = (EditText) findViewById(R.id.fullname);
        mEditEmail = (EditText) findViewById(R.id.email);
        mEditPassword = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String email = mEditEmail.getText().toString().trim();
                    String password = mEditPassword.getText().toString().trim();

                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(password)) {
                        Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (password.length() < 6) {
                        Toast.makeText(getApplicationContext(), "Password too short, enter minimum 6 characters!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    progressBar.setVisibility(View.VISIBLE);
                    //create user
                    mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (!task.isSuccessful()) {
                                    Toast.makeText(SignUpActivity.this, getString(R.string.the_email_address_is_already_in_use_by_another_account),
                                                   Toast.LENGTH_LONG).show();
                                } else {

                                    /* Atualiza o nome do usuário */
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    String fullname = mEditFullName.getText().toString();
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullname)
                                        .build();

                                    user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // Log.d(TAG, "User profile updated.");
                                                }
                                            }
                                        });


                                    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
                                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                    DatabaseReference mRef = mDatabase.getReference().child("users").child(currentUser.getUid());

                                    int points = 0;
                                    String device_manufacturer = Build.MANUFACTURER;
                                    String device_model = Build.MODEL;


                                    Map<String, Object> values = new HashMap<>();
                                    values.put("current_points", points);
                                    values.put("current_device", device_manufacturer + " " + device_model);
                                    values.put("current_username", fullname);
                                    mRef.setValue(values);

                                    new android.os.Handler().postDelayed(
                                        new Runnable() {
                                            public void run() {
                                                startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                                finish();
                                            }
                                        },
                                        1500);
                                }
                            }
                        });

                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }
}
