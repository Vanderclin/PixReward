package ml.pixreward.app;

import android.animation.ValueAnimator;
import android.annotation.NonNull;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import ml.pixreward.app.MainActivity;
import ml.pixreward.app.MessageActivity;
import ml.pixreward.app.R;
import ml.pixreward.image.SmartImageView;
import ml.pixreward.updating.UpdateChecker;

public class MainActivity extends AppCompatActivity implements RewardedVideoAdListener {

	private RewardedVideoAd mRewardedVideoHome;
	private InterstitialAd mInterstitialAd;
	private AdListener mAdListener;
	private AdRequest adRequest;
	private AdView mAdView;
    private TextView mTextViewShowPoints, mTextViewShowBalance;
    private DatabaseReference mDatabase, mDatabaseAdmin, mDatabaseRescue, mDatabaseRanking;
    private CoordinatorLayout mCoordinator;
    private Toolbar mToolbar;
    private FirebaseAuth mAuth;
    private String name, email, uid;
    private Uri photoUrl;
	private boolean emailVerified;
    private FloatingActionButton mFloatingAdView;

	private CardView mCardViewFreeFire, mCardViewGooglePlay, mCardViewNetflix, mCardViewPix;
	private Button mButtonPointsRoulette;
	private SmartImageView mSmartImageView;


	private ImageView mImageChatPlus;

	// Rescue
	private String rescueCode;

	private Integer rescueValue;
	// private Integer amountPoints;
	private Integer rescueWithdraw;


	private TextView displayName, displayEmail;
	private ImageView displayPicture;

	// Profile
	private String currentAppVersion;
	private String currentDevice;
	private String currentEmail;
	private String currentPix;
	private Boolean currentLock;
	private Integer currentPoints, currentGenre;
	private String currentUsername;

	private boolean mValue;
	final private String welcome = "file:///android_asset/html/welcome.html";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(mToolbar);
		// Get Auth
		mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            name = mAuth.getCurrentUser().getDisplayName();
            email = mAuth.getCurrentUser().getEmail();
            uid = mAuth.getCurrentUser().getUid();
            photoUrl = mAuth.getCurrentUser().getPhotoUrl();
            emailVerified = mAuth.getCurrentUser().isEmailVerified();

        } else {
            startActivity(new Intent(MainActivity.this, SignInActivity.class));
            finishAffinity();
		}
		
		openDialog();
		// Get Database
		mDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid);
		mDatabaseAdmin = FirebaseDatabase.getInstance().getReference("admin");
		mDatabaseRanking = FirebaseDatabase.getInstance().getReference("ranking");
		mDatabaseRescue = FirebaseDatabase.getInstance().getReference("rescue");

		// Check Update App
        checkUpdate();
		// Admob
		mRewardedVideoHome = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoHome.setRewardedVideoAdListener(this);
		MobileAds.initialize(this, getString(R.string.id_app));
        mAdView = (AdView) findViewById(R.id.ad_view);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        mAdView.loadAd(adRequest);
		mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.id_intersticial));
        mInterstitialAd.setAdListener(mAdListener);
        mInterstitialAd.loadAd(adRequest);
		// Get ID's
		mCoordinator = (CoordinatorLayout) findViewById(R.id.root_coordinator);
		mSmartImageView = (SmartImageView) findViewById(R.id.displayThumbnail);
		mFloatingAdView = (FloatingActionButton) findViewById(R.id.fab_ad_view);
		mImageChatPlus = (ImageView) findViewById(R.id.img_chat_plus);
		mTextViewShowPoints = (TextView) findViewById(R.id.textview_show_points);
		mTextViewShowPoints.setSelected(true);
        mTextViewShowBalance = (TextView) findViewById(R.id.textview_show_balance);
		mTextViewShowBalance.setSelected(true);

		displayPicture = (ImageView) findViewById(R.id.display_profile_picture);
		displayName = (TextView) findViewById(R.id.display_profile_name);
		displayEmail = (TextView) findViewById(R.id.display_profile_email);
		displayName.setText(name);
		displayEmail.setText(email);



		mCardViewFreeFire = (CardView) findViewById(R.id.cardViewFreeFire);
		mCardViewGooglePlay = (CardView) findViewById(R.id.cardViewGooglePlay);
		mCardViewNetflix = (CardView) findViewById(R.id.cardViewNetflix);
		mCardViewPix = (CardView) findViewById(R.id.cardViewPix);
		mButtonPointsRoulette = (Button) findViewById(R.id.pointsRoulette);
		registerForContextMenu(mCoordinator);



        mTextViewShowPoints.setSelected(true);
		mTextViewShowBalance.setSelected(true);

		startInterstitial();
		// Admin
		mDatabaseAdmin.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    rescueValue = snapshot.child("rescue_value").getValue(Integer.class);
					rescueCode = snapshot.child("rescue_code").getValue(String.class);
					rescueWithdraw = snapshot.child("rescue_withdraw").getValue(Integer.class);

					String photo_url = snapshot.child("photo_url").getValue(String.class);
					mSmartImageView.setImageUrl(photo_url);


                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("MainActivity", "Failed to read value.", error.toException());
                }
            });


        mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
					currentGenre = snapshot.child("current_genre").getValue(Integer.class);
                    currentPoints = snapshot.child("current_points").getValue(Integer.class);
					currentLock = snapshot.child("current_lock").getValue(Boolean.class);
					if (currentLock == true) {
						mFloatingAdView.setVisibility(View.GONE);
						mButtonPointsRoulette.setVisibility(View.GONE);
					} else {
						mFloatingAdView.setVisibility(View.VISIBLE);
						mButtonPointsRoulette.setVisibility(View.VISIBLE);
					}
					
					
					setImageProfile(currentGenre);
					
					ValueAnimator animator = ValueAnimator.ofInt(0, currentPoints);
					animator.setDuration(6000);
					animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
							public void onAnimationUpdate(ValueAnimator animation) {
								String replaceValue = animation.getAnimatedValue().toString().replaceAll("[$,.]", "");
								double doubleValue = Double.parseDouble(replaceValue);
								String points = animation.getAnimatedValue().toString();
								String balance = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format((doubleValue / 1000));
								mTextViewShowPoints.setText(points);
								mTextViewShowBalance.setText(balance);
							}
						});
					animator.start();
				}

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("MainActivity", "Failed to read value.", error.toException());
                }
            });
			
		

		mImageChatPlus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
					finish();
					overridePendingTransition(R.anim.fadeout, R.anim.fadein);
					startActivity(new Intent(MainActivity.this, MessageActivity.class));
                }
            });
			
        mFloatingAdView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadRewardedVideoAd();

                }
            });
		mCardViewFreeFire.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentPoints >= rescueWithdraw) {
						String type = "Free Fire";
						String description = "O resgate do cartão presente para Free Fire pode levar até 3 dias úteis";
						openDialog(type, description);
					} else {
						Toast.makeText(MainActivity.this, getString(R.string.insufficient_funds), Toast.LENGTH_LONG).show();
					}
				}
			});
		mCardViewGooglePlay.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentPoints >= rescueWithdraw) {
						String type = "Google Play";
						String description = "O resgate do cartão presente para Google Play pode levar até 3 dias úteis";
						openDialog(type, description);
					} else {
						Toast.makeText(MainActivity.this, getString(R.string.insufficient_funds), Toast.LENGTH_LONG).show();
					}
				}
			});
		mCardViewNetflix.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentPoints >= rescueWithdraw) {
						String type = "Netflix";
						String description = "O resgate do cartão presente para Netflix pode levar até 3 dias úteis";
						openDialog(type, description);
					} else {
						Toast.makeText(MainActivity.this, getString(R.string.insufficient_funds), Toast.LENGTH_LONG).show();
					}
				}
			});

		mCardViewPix.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentPoints >= rescueWithdraw) {
						String type = "Pix";
						String description = "O resgate do Pix pode levar até 24 horas, até a confirmação dos dados do usuário.";
						openDialog(type, description);
					} else {
						Toast.makeText(MainActivity.this, getString(R.string.insufficient_funds), Toast.LENGTH_LONG).show();
					}
				}
			});

		mButtonPointsRoulette.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
					overridePendingTransition(R.anim.fadeout, R.anim.fadein);
					startActivity(new Intent(MainActivity.this, RouletteActivity.class));
				}
			});


    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
				startActivity(new Intent(MainActivity.this, AboutActivity.class));
				return true;

			case R.id.menu_rescue_code:
				rescueCode();
				return true;


			case R.id.menu_signout:
				mAuth.getInstance().signOut();
				finish();
				overridePendingTransition(R.anim.fadeout, R.anim.fadein);
				startActivity(new Intent(MainActivity.this, SignInActivity.class));
				return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRewardedVideoAdLoaded() {
        if (mRewardedVideoHome.isLoaded()) {
            mRewardedVideoHome.show();
        }

    }

    @Override
    public void onRewardedVideoAdOpened() {
    }

    @Override
    public void onRewardedVideoStarted() {
    }

    @Override
    public void onRewardedVideoAdClosed() {
		startInterstitial();
    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        currentPoints += rewardItem.getAmount();
        mDatabase.child("current_points").setValue(currentPoints);
		StringBuffer esrever = new StringBuffer(uid).reverse();
		String diu = String.valueOf(esrever);
		mDatabaseRanking.child(diu).child("order").setValue(-currentPoints);
		mDatabaseRanking.child(diu).child("points").setValue(currentPoints);
		mDatabaseRanking.child(diu).child("name").setValue(name);

		Vibrator mVibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
		mVibrator.vibrate(100);
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {
		Snackbar snackbar = Snackbar.make(mCoordinator, getString(R.string.try_again_later), Snackbar.LENGTH_LONG);
		snackbar.setDuration(6000);
		snackbar.show();
		startInterstitial();

    }

	private void loadRewardedVideoAd() {
        mRewardedVideoHome.loadAd(getString(R.string.id_video_home), new AdRequest.Builder().build());

    }

	private void startInterstitial() {
		if (mInterstitialAd.isLoaded()) {
			mInterstitialAd.show();
			mInterstitialAd.loadAd(adRequest);
		}
	}

	private void checkUpdate() {
        UpdateChecker.checkForDialog(MainActivity.this);
        UpdateChecker.checkForNotification(MainActivity.this);
		String versionName;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (Exception e) {
			versionName = getString(R.string.version_number_unknown);
		}
		mDatabase.child("current_app_version").setValue(versionName);
	}

	private void openDialog(final String type, final String description) {
		AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_rescue, null);
		mAlertDialog.setView(dialogView);
		final TextView rescueType = dialogView.findViewById(R.id.rescueType);
		final TextView rescueDescription = dialogView.findViewById(R.id.rescueDescription);
        final Button mButtonCancel = dialogView.findViewById(R.id.dialogBescueButtonCancel);
		final Button mButtonRescue = dialogView.findViewById(R.id.dialogRescueButtonRescue);
		final AlertDialog mAlert = mAlertDialog.create();
		rescueType.setText(type);
		rescueDescription.setText(description);
		mAlert.setCancelable(false);
		mAlert.show();
		mButtonCancel.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					mAlert.dismiss();
				}
			});
		mButtonRescue.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					mDatabase.child("current_points").setValue(currentPoints - rescueWithdraw);
					Map<String, Object> values = new HashMap<>();
					values.put("current_points", currentPoints);
					values.put("current_email", email);
					values.put("current_username", name);
					values.put("current_type", type);
					mDatabaseRescue.child(uid).setValue(values);
					mAlert.dismiss();
				}
			});
	}

	private void rescueCode() {
		AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_referral_code, null);
		mAlertDialog.setView(dialogView);
		final EditText mRescueCode = dialogView.findViewById(R.id.editTextRescueCode);

		final Button mButtonCancel = dialogView.findViewById(R.id.buttonRescueCodeCancel);
		final Button mButtonRescue = dialogView.findViewById(R.id.buttonRescueCodeCheck);

		final AlertDialog mAlert = mAlertDialog.create();
		mAlert.setCancelable(false);
		mAlert.show();
		mButtonCancel.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					mAlert.dismiss();
				}
			});


		mButtonRescue.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {

					String code = mRescueCode.getText().toString();

					if (TextUtils.isEmpty(code)) {
						Toast.makeText(MainActivity.this, "Insira o código", Toast.LENGTH_LONG).show();
					}
					if (code.equals(rescueCode)) {
						Toast.makeText(MainActivity.this, "Código igual", Toast.LENGTH_LONG).show();
						currentPoints += rescueValue;
						mDatabase.child("current_points").setValue(currentPoints);
						String empty = "								";
						mDatabaseAdmin.child("rescue_code").setValue(empty);
						mDatabaseAdmin.child("rescue_value").setValue(0);
						congratulationDialog(rescueValue);
						mAlert.dismiss();
					} else {
						Toast.makeText(MainActivity.this, "Código diferente", Toast.LENGTH_LONG).show();
					}
				}
			});
	}


	private void congratulationDialog(Integer rescuePoints) {
		AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_congratulation, null);
		mAlertDialog.setView(dialogView);
		TextView mTextViewMessage = dialogView.findViewById(R.id.congratulationValue);
		LinearLayout mLinearLayout = dialogView.findViewById(R.id.congratulationLinearLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) mLinearLayout.getBackground();
        animationDrawable.setEnterFadeDuration(1000);
        animationDrawable.setExitFadeDuration(2000);
        animationDrawable.start();
		mTextViewMessage.setText(getString(R.string.you_win, rescuePoints));
		mAlertDialog.setCancelable(true);
		mAlertDialog.show();
	}






	/** Called when leaving the activity */
    @Override
    public void onPause() {
		mRewardedVideoHome.pause(this);
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    /** Called when returning to the activity */
    @Override
    public void onResume() {
		mRewardedVideoHome.resume(this);
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    /** Called before the activity is destroyed */
    @Override
    public void onDestroy() {
		mRewardedVideoHome.destroy(this);
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

	private void setImageProfile(Integer genre) {
        switch (genre) {
            case 0:
                displayPicture.setImageDrawable(getResources().getDrawable(R.drawable.avatar_female));
                return;
            case 1:
                displayPicture.setImageDrawable(getResources().getDrawable(R.drawable.avatar_male));
                return;
            case 2:
                displayPicture.setImageDrawable(getResources().getDrawable(R.drawable.avatar_master));
                return;
        }
    }
	
	private void openDialog()
	{
        mValue = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("mValue", true);

        if (mValue)
		{
            AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);
			WebView mWebViewDialog = new WebView(this);
			mWebViewDialog.loadUrl(welcome);
			mWebViewDialog.setWebViewClient(new WebViewClient() {
					@Override
					public boolean shouldOverrideUrlLoading(WebView view, String url)
					{
						view.loadUrl(url);

						return true;
					}
				});

            mAlertDialog.setView(mWebViewDialog);
			mAlertDialog.setCancelable(false);
            mAlertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				});
            mAlertDialog.show();

            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("mValue", false).commit();
        }
    }
}
