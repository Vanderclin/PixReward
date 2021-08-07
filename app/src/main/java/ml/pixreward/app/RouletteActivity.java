package ml.pixreward.app;


import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
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
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;
import ml.pixreward.app.R;
import ml.pixreward.app.RouletteActivity;
import android.support.design.widget.FloatingActionButton;

public class RouletteActivity extends AppCompatActivity implements Animation.AnimationListener, RewardedVideoAdListener {

    private boolean mButtonRotation = true;
    private int intNumber = 8, rouletteResult;
    private long lngDegrees = 0;
    private ImageView imageRoulette;
	private FloatingActionButton mFloatingRouletteStart;

	private FirebaseAuth mAuth;
    private String name, email, uid;
    private Uri photoUrl;
	private boolean emailVerified;
	private DatabaseReference mDatabase;
	private Integer amountPoints = 0;
	private TextView mTextViewRoulettePoints, mTextViewRouletteBalance;
	private RewardedVideoAd mRewardedVideoAd;
	private CoordinatorLayout mCoordinator;
	
	private AdView mAdView;
	private int snackbarDuration = 5000;
	
	private String urlFile = "file:///android_asset/roulette_game.mp3";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roulette);
		mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            name = mAuth.getCurrentUser().getDisplayName();
            email = mAuth.getCurrentUser().getEmail();
            uid = mAuth.getCurrentUser().getUid();
            photoUrl = mAuth.getCurrentUser().getPhotoUrl();
            emailVerified = mAuth.getCurrentUser().isEmailVerified();

        } else {
            startActivity(new Intent(RouletteActivity.this, SignInActivity.class));
            finishAffinity();
		}
		
		

		mDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid);
		mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);

		MobileAds.initialize(this, getString(R.string.id_app));
        mAdView = (AdView) findViewById(R.id.ad_view);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        mAdView.loadAd(adRequest);


		mCoordinator = (CoordinatorLayout) findViewById(R.id.root_coordinator_roulette);
		registerForContextMenu(mCoordinator);

		mTextViewRoulettePoints = (TextView) findViewById(R.id.displayPoints);
		mTextViewRouletteBalance = (TextView) findViewById(R.id.displayBalance);
        imageRoulette = (ImageView)findViewById(R.id.imageRoulette);
		imageRoulette.setImageDrawable(getResources().getDrawable(R.drawable.roulette_8));
		mFloatingRouletteStart = (FloatingActionButton) findViewById(R.id.buttonRouletteStart);
		mFloatingRouletteStart.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mButtonRotation) {
						int ran = new Random().nextInt(360) + 3600;
						RotateAnimation rotateAnimation = new RotateAnimation((float) lngDegrees, (float) (lngDegrees + ((long)ran)), 1, 0.5f, 1, 0.5f);
						lngDegrees = (lngDegrees + ((long)ran)) % 360;
						rotateAnimation.setDuration((long)ran);
						rotateAnimation.setFillAfter(true);
						rotateAnimation.setInterpolator(new DecelerateInterpolator());
						rotateAnimation.setAnimationListener(RouletteActivity.this);
						imageRoulette.setAnimation(rotateAnimation);
						imageRoulette.startAnimation(rotateAnimation);

					}
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
					mTextViewRoulettePoints.setText(points);
					mTextViewRouletteBalance.setText(balance);
					
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("MainActivity", "Failed to read value.", error.toException());
                }
            });



    }

    @Override
    public void onAnimationStart(Animation animation) {
        this.mButtonRotation = false;
		mFloatingRouletteStart.setEnabled(false);
		MediaPlayer mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(urlFile));
		} catch (SecurityException e) {} catch (IllegalArgumentException e) {} catch (IOException e) {} catch (IllegalStateException e) {}
		try {
			mediaPlayer.prepare();
		} catch (IOException e) {} catch (IllegalStateException e) {}
		mediaPlayer.start();
		
		
		
		
		
    }

    @Override
    public void onAnimationEnd(Animation animation) {
		rouletteResult = ((int)(((double)this.intNumber) - Math.floor(((double)this.lngDegrees) / (360.0d / ((double)this.intNumber)))));
		setRouletteResult(rouletteResult);
		if (mRewardedVideoAd.isLoaded()) {
			mButtonRotation = false;
			mFloatingRouletteStart.setEnabled(false);
        } else {
			mButtonRotation = true;
			mFloatingRouletteStart.setEnabled(true);
		}
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

	private void setRouletteResult(int result) {
        switch (result) {
            case 1:
				loadRewardedVideoAd();
                return;
            case 2:
                loadRewardedVideoAd();
                return;
            case 3:
                loadRewardedVideoAd();
                return;
            case 4:
                loadRewardedVideoAd();
                return;
            case 5:
                loadRewardedVideoAd();
                return;
            case 6:
                loadRewardedVideoAd();
                return;
            case 7:
                loadRewardedVideoAd();
                return;
            case 8:
                loadRewardedVideoAd();
                return;
        }
    }

	@Override
	public void onRewardedVideoAdLoaded() {
		if (mRewardedVideoAd.isLoaded()) {
			Snackbar snackbar = Snackbar.make(mCoordinator, getString(R.string.you_will_win, rouletteResult), Snackbar.LENGTH_LONG);
			snackbar.setDuration(snackbarDuration);
			snackbar.show();
			
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						//Do something after 7000ms
						mRewardedVideoAd.show();
					}
				}, 7000);
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
		mButtonRotation = true;
		mFloatingRouletteStart.setEnabled(true);
		Snackbar snackbar = Snackbar.make(mCoordinator, getString(R.string.you_win, rouletteResult), Snackbar.LENGTH_LONG);
		snackbar.setDuration(snackbarDuration);
		snackbar.show();
	}

	@Override
	public void onRewarded(RewardItem p1) {
		mDatabase.child("current_points").setValue(amountPoints += rouletteResult);
	}

	@Override
	public void onRewardedVideoAdLeftApplication() {
	}
	
	@Override
	public void onRewardedVideoAdFailedToLoad(int p1) {
		mButtonRotation = true;
		Snackbar snackbar = Snackbar.make(mCoordinator, getString(R.string.try_again_later), Snackbar.LENGTH_LONG);
		snackbar.setDuration(snackbarDuration);
		snackbar.show();
	}

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
        mRewardedVideoAd.loadAd(getString(R.string.id_video_roulette), new AdRequest.Builder().build());
    }
	
	
	
	

}
