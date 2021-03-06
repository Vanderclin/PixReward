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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import ml.pixreward.app.R;

public class SignUpActivity extends AppCompatActivity {

    private EditText mEditFullName, mEditEmail, mEditPixKey, mEditPassword;
    private Button buttonSignUp;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

	private String fullname, email, pixkey, password;
	private RadioGroup mRadioGroup;
    private RadioButton mRadioButton;
	private int mRadioID, mGenreResult;
	
	
	private DatabaseReference mDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        //Get Firebase auth instance
        mAuth = FirebaseAuth.getInstance();

        mEditFullName = (EditText) findViewById(R.id.fullname);
        mEditEmail = (EditText) findViewById(R.id.email);
		mEditPixKey = (EditText) findViewById(R.id.pixkey);
        mEditPassword = (EditText) findViewById(R.id.password);
		mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
		buttonSignUp = (Button) findViewById(R.id.sign_up_button);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

					fullname = mEditFullName.getText().toString();
					email = mEditEmail.getText().toString().toString();
					pixkey = mEditPixKey.getText().toString();
					password = mEditPassword.getText().toString().trim();

					mDatabase = FirebaseDatabase.getInstance().getReference("users");

					if (TextUtils.isEmpty(fullname)) {
						Toast.makeText(getApplicationContext(), getString(R.string.enter_full_name), Toast.LENGTH_SHORT).show();
						return;
					}
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(getApplicationContext(), getString(R.string.enter_the_email), Toast.LENGTH_SHORT).show();
                        return;
                    }
					if (TextUtils.isEmpty(pixkey)) {
						Toast.makeText(getApplicationContext(), getString(R.string.enter_pix_key), Toast.LENGTH_SHORT).show();
						return;
					}
                    if (TextUtils.isEmpty(password)) {
                        Toast.makeText(getApplicationContext(), getString(R.string.enter_password), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (password.length() < 8) {
                        Toast.makeText(getApplicationContext(), getString(R.string.minimum_password), Toast.LENGTH_LONG).show();
                        return;
                    }
					
					mRadioID = mRadioGroup.getCheckedRadioButtonId();
					mRadioButton = (RadioButton) findViewById(mRadioID);
					if (mRadioGroup.getCheckedRadioButtonId() == -1) {
						Toast.makeText(SignUpActivity.this, getString(R.string.choose_a_genre), Toast.LENGTH_LONG).show();
						return;
					}
					if (mRadioButton.getText() == getString(R.string.woman)) {
						mGenreResult = 0;
					}
					if (mRadioButton.getText() == getString(R.string.man)) {
						mGenreResult = 1;
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

                                    /* Atualiza o nome do usu??rio */
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

                                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                    String device_manufacturer = Build.MANUFACTURER;
                                    String device_model = Build.MODEL;

									String versionName;
									try {
										versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
									} catch (Exception e) {
										versionName = getString(R.string.version_number_unknown);
									}
                                    Map<String, Object> values = new HashMap<>();
                                    values.put("current_points", 0);
									values.put("current_email", email);
									values.put("current_pix", pixkey);
									values.put("current_lock", false);
									values.put("current_genre", mGenreResult);
                                    values.put("current_device", device_manufacturer + " " + device_model);
                                    values.put("current_username", fullname);
									values.put("current_app_version", versionName);
                                    mDatabase.child(currentUser.getUid()).setValue(values);

                                    new android.os.Handler().postDelayed(
                                        new Runnable() {
                                            public void run() {
                                                finish();
												overridePendingTransition(R.anim.fadeout, R.anim.fadein);
												startActivity(new Intent(SignUpActivity.this, MainActivity.class));
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
