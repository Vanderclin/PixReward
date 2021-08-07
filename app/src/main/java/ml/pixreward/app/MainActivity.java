package ml.pixreward.app;

import android.annotation.NonNull;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import ml.pixreward.app.R;
import ml.pixreward.updating.UpdateChecker;
import android.widget.EditText;
import ml.pixreward.image.SmartImageView;

public class MainActivity extends AppCompatActivity implements RewardedVideoAdListener {

	private RewardedVideoAd mRewardedVideoAd;
	private InterstitialAd mInterstitialAd;
	private AdListener mAdListener;
	private AdRequest adRequest;
	private AdView mAdView;
    private TextView mTextViewShowPoints, mTextViewShowBalance;
    private DatabaseReference mDatabase, mDatabaseAdmin, mDatabaseRescue;
    private CoordinatorLayout mCoordinator;
    private Toolbar mToolbar;
    private FirebaseAuth mAuth;
    private String name, email, uid;
    private Uri photoUrl;
	private boolean emailVerified;
    private FloatingActionButton mFloatingChatPlus, mFloatingAdView;
	private Integer rescueValue, amountPoints;
	private CardView mCardViewFreeFire, mCardViewGooglePlay, mCardViewNetflix, mCardViewPix;
	private Button mButtonPointsRoulette;
	private SmartImageView mSmartImageView;
	
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
		// Get Database
		mDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid);
		mDatabaseAdmin = FirebaseDatabase.getInstance().getReference("admin");
		mDatabaseRescue = FirebaseDatabase.getInstance().getReference("rescue");
		// Check Update App
        checkUpdate();
		// Admob
		mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);
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
		mFloatingChatPlus = (FloatingActionButton) findViewById(R.id.fab_chat_plus);
		mTextViewShowPoints = (TextView) findViewById(R.id.textview_show_points);
		mTextViewShowPoints.setSelected(true);
        mTextViewShowBalance = (TextView) findViewById(R.id.textview_show_balance);
		mTextViewShowBalance.setSelected(true);
		mCardViewFreeFire = (CardView) findViewById(R.id.cardViewFreeFire);
		mCardViewGooglePlay = (CardView) findViewById(R.id.cardViewGooglePlay);
		mCardViewNetflix = (CardView) findViewById(R.id.cardViewNetflix);
		mCardViewPix = (CardView) findViewById(R.id.cardViewPix);
		mButtonPointsRoulette = (Button) findViewById(R.id.pointsRoulette);

		registerForContextMenu(mCoordinator);
        mTextViewShowPoints.setSelected(true);
		mTextViewShowBalance.setSelected(true);

		startInterstitial();
		mDatabaseAdmin.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    rescueValue = snapshot.child("rescue_value").getValue(Integer.class);
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
                    amountPoints = snapshot.child("current_points").getValue(Integer.class);
                    String replaceValue = amountPoints.toString().replaceAll("[$,.]", "");
                    double doubleValue = Double.parseDouble(replaceValue);
                    String points = Integer.toString(amountPoints);
                    String balance = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format((doubleValue / 1000));
                    mTextViewShowPoints.setText(points);
                    mTextViewShowBalance.setText(balance);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("MainActivity", "Failed to read value.", error.toException());
                }
            });


		mFloatingChatPlus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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
					if (amountPoints >= rescueValue) {
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
					if (amountPoints >= rescueValue) {
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
					if (amountPoints >= rescueValue) {
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
					if (amountPoints >= rescueValue) {
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
			case R.id.menu_signout:
				mAuth.getInstance().signOut();
				startActivity(new Intent(MainActivity.this, SignInActivity.class));
				finish();
				return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRewardedVideoAdLoaded() {
        if (mRewardedVideoAd.isLoaded()) {
            mRewardedVideoAd.show();
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
        amountPoints += rewardItem.getAmount();
        mDatabase.child("current_points").setValue(amountPoints);
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
        mRewardedVideoAd.loadAd(getString(R.string.id_video), new AdRequest.Builder().build());

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
					mDatabase.child("current_points").setValue(amountPoints - rescueValue);
					Map<String, Object> values = new HashMap<>();
					values.put("current_points", amountPoints);
					values.put("current_email", email);
					values.put("current_username", name);
					values.put("current_type", type);
					mDatabaseRescue.setValue(values);
					mAlert.dismiss();
				}
			});
	}

	/** Called when leaving the activity */
    @Override
    public void onPause() {
		mRewardedVideoAd.pause(this);
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    /** Called when returning to the activity */
    @Override
    public void onResume() {
		mRewardedVideoAd.resume(this);
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    /** Called before the activity is destroyed */
    @Override
    public void onDestroy() {
		mRewardedVideoAd.destroy(this);
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

}
