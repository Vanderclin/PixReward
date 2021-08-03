package ml.pixreward.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import ml.pixreward.app.MessageActivity;
import ml.pixreward.app.R;

public class MessageActivity extends AppCompatActivity implements RewardedVideoAdListener {

    private EditText mEditText;
    private ImageButton mImageButton;
    private ListView mListView;
	private Message msg;
    private List<Message> MessageList;
    private DatabaseReference mDatabase, mDatabaseMessage;
	
	private FirebaseAuth mAuth;
    private String name, email, uid;
    private Uri photoUrl;
	private boolean emailVerified;
	
	private MessageAdapter mAdapter;
	
	private RewardedVideoAd mRewardedVideoAd;
	private InterstitialAd mInterstitialAd;
	private AdListener mAdListener;
	private AdRequest adRequest;
	private AdView mAdView;
	
	private Integer amountPoints;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
		mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            name = mAuth.getCurrentUser().getDisplayName();
            email = mAuth.getCurrentUser().getEmail();
            uid = mAuth.getCurrentUser().getUid();
            photoUrl = mAuth.getCurrentUser().getPhotoUrl();
            emailVerified = mAuth.getCurrentUser().isEmailVerified();

        } else {
            startActivity(new Intent(MessageActivity.this, SignInActivity.class));
            finishAffinity();
		}
		mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(MessageActivity.this);
		MobileAds.initialize(this, getString(R.string.id_app));
		adRequest = new AdRequest.Builder()
			.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
			.build();
		mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.id_intersticial));
        mInterstitialAd.setAdListener(mAdListener);
        mInterstitialAd.loadAd(adRequest);
		
		mDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid);
        mDatabaseMessage = FirebaseDatabase.getInstance().getReference("message");
        mEditText = (EditText) findViewById(R.id.message_in);
        mListView = (ListView) findViewById(R.id.message_view);
        mImageButton = (ImageButton) findViewById(R.id.button_send);
        MessageList = new ArrayList<>();
		
		mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    amountPoints = snapshot.child("current_points").getValue(Integer.class);
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("MainActivity", "Failed to read value.", error.toException());
                }
            });
		
        mImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					String message = mEditText.getText().toString();

					Date today = Calendar.getInstance().getTime();
					SimpleDateFormat formatter = new SimpleDateFormat("hh:mm - dd/MM/yyyy");
					String time = formatter.format(today);
					if (!TextUtils.isEmpty(message)) {
						String id = mDatabaseMessage.push().getKey();
						Message msg = new Message(id, name, message, time, uid);
						mDatabaseMessage.child(id).setValue(msg);
						mEditText.setText("");
						loadRewardedVideoAd();
					} else {
						Toast.makeText(MessageActivity.this, "Type a message", Toast.LENGTH_LONG).show();
					}
				}
			});
			

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
					msg = MessageList.get(i);
					if (uid.equals(msg.getUID())) {
						DialogDelete(msg.getUID(), msg.getNAME(), msg.getID());
					} else {
						Toast.makeText(MessageActivity.this, "Você não pode apagar esta mensagem", Toast.LENGTH_LONG).show();
					}
					return true;
				}
			});
			
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
		if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
            mInterstitialAd.loadAd(adRequest);
        }
	}

	@Override
	public void onRewarded(RewardItem rewardItem) {
		amountPoints += rewardItem.getAmount();
        mDatabase.child("current_points").setValue(amountPoints);
	}

	@Override
	public void onRewardedVideoAdLeftApplication() {
	}

	@Override
	public void onRewardedVideoAdFailedToLoad(int p1) {
		if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
            mInterstitialAd.loadAd(adRequest);
        }
	}
	
	private void loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd(getString(R.string.id_video_two), new AdRequest.Builder().build());

    }

    @Override
    protected void onStart() {
        super.onStart();
        mDatabaseMessage.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {
					MessageList.clear();
					for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
						msg = postSnapshot.getValue(Message.class);
						MessageList.add(msg);
					}
					mAdapter = new MessageAdapter(MessageActivity.this, MessageList);
					mListView.setAdapter(mAdapter);
					mListView.post(new Runnable() {
							@Override
							public void run() {
								mListView.setSelection(mAdapter.getCount() - 1);
							}
						});
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
				}
			});
    }
	
    private void DialogDelete(String uid, String name, final String id) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_delete, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);

        final Button buttonDelete = dialogView.findViewById(R.id.button_delete);
        final Button buttonCancel = dialogView.findViewById(R.id.button_cancel);

        final TextView messageAlert = dialogView.findViewById(R.id.message_alert);

        messageAlert.setText(getString(R.string.delete_message));
        final AlertDialog b = dialogBuilder.create();
        b.show();


        buttonCancel.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					b.dismiss();
				}
			});

        buttonDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {

					deleteMessage(id);
					b.dismiss();
				}
			});
    }
	
	
    private boolean deleteMessage(String id) {
        mDatabaseMessage.child(id).removeValue();
        Toast.makeText(getApplicationContext(), getString(R.string.message_deleted), Toast.LENGTH_LONG).show();

        return true;
    }
}
