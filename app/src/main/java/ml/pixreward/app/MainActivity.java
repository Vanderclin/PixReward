package ml.pixreward.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.NumberFormat;
import java.util.Locale;
import ml.pixreward.app.R;
import android.support.design.widget.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements RewardedVideoAdListener {

	private RewardedVideoAd mRewardedVideoAd;
	private AdView mAdView;
    private Button mButtonShowAds;
    private TextView mTextViewShowPoints, mTextViewShowBalance;
    private DatabaseReference mDatabase;
    private Integer amountPoints = 0;
    private CoordinatorLayout mCoordinator;
    private Toolbar mToolbar;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private String name, email, uid;
    private Uri photoUrl;
	private boolean emailVerified;

    private FloatingActionButton mFloatingSignOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(mToolbar);
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);
        mCoordinator = (CoordinatorLayout) findViewById(R.id.root_coordinator);
		registerForContextMenu(mCoordinator);
		// Initialize the Mobile Ads SDK.
		MobileAds.initialize(this, getString(R.string.app_ad_unit_id));
        mAdView = (AdView) findViewById(R.id.ad_view);
        AdRequest adRequest = new AdRequest.Builder()
			.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
			.build();
        mAdView.loadAd(adRequest);


        // Get ID's all Widgets
        mFloatingSignOut = (FloatingActionButton) findViewById(R.id.fab_signout);
        mFloatingSignOut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mAuth.getInstance().signOut();
                    startActivity(new Intent(MainActivity.this, SignInActivity.class));
                }
            });


        mButtonShowAds = (Button) findViewById(R.id.button_show_ads);
        mTextViewShowPoints = (TextView) findViewById(R.id.textview_show_points);
        mTextViewShowBalance = (TextView) findViewById(R.id.textview_show_balance);
        //get current coins from prefs initially
        // mTextViewShowPoints.setText("Coins: " + getCoinsFromPrefs());

        mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    amountPoints = snapshot.child("current_points").getValue(Integer.class);

                    if (amountPoints == 0 || amountPoints == null) {
                        amountPoints = 0;
                    }
                    String replaceValue = amountPoints.toString().replaceAll("[$,.]", "");
                    double doubleValue = Double.parseDouble(replaceValue);
                    String points = Integer.toString(amountPoints);
                    String balance = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format((doubleValue / 100));
                    mTextViewShowPoints.setText(points);
                    mTextViewShowBalance.setText(balance);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("MainActivity", "Failed to read value.", error.toException());
                }
            });

        mButtonShowAds.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadRewardedVideoAd();

                }
            });
    }

    @Override
    public void onRewardedVideoAdLoaded() {
        Toast.makeText(this, "video ad loaded", Toast.LENGTH_SHORT).show();
        //show video when ad is loaded
        if (mRewardedVideoAd.isLoaded()) {
            mRewardedVideoAd.show();
        }

    }

    @Override
    public void onRewardedVideoAdOpened() {
        Toast.makeText(this, "video ad opened", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoStarted() {
        Toast.makeText(this, "video ad started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoAdClosed() {
        Toast.makeText(this, "video ad closed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        Toast.makeText(this, "onRewarded! currency: " + rewardItem.getType() + "  amount: " +
                       rewardItem.getAmount(), Toast.LENGTH_SHORT).show();

        //add the new coins to the saved coins in prefs
        saveCoinsToPrefs(getCoinsFromPrefs() + rewardItem.getAmount());

        amountPoints += rewardItem.getAmount();
        mDatabase.child("points").setValue(amountPoints);

        //set the coins that for user
        // mTextViewShowPoints.setText("Coins: " + getCoinsFromPrefs());
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {
        Toast.makeText(this, "Left application", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {
        Snackbar snackbar = Snackbar.make(mCoordinator, getString(R.string.failed_to_load_the_video), Snackbar.LENGTH_LONG);
        snackbar.setDuration(6000);
        snackbar.show();
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

    private void loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd(getString(R.string.video_ad_unit_id), new AdRequest.Builder().build());

    }

    private void saveCoinsToPrefs(int amount) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("COINS", amount);
        editor.apply();
    }

    private int getCoinsFromPrefs() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        int coins = sharedPref.getInt("COINS", 0);
        return coins;
    }

}
