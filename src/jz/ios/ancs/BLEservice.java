package jz.ios.ancs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import jz.ancs.parse.ANCSGattCallback;
import jz.ancs.parse.ANCSGattCallback.StateListener;
import jz.ancs.parse.ANCSParser;
import jz.ancs.parse.IOSNotification;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

public class BLEservice extends Service implements ANCSParser.onIOSNotification{
	private static final String LIVE_CARD_TAG = "LiveCardDemo";

    private LiveCard mLiveCard;
    private RemoteViews mLiveCardView;
    private AudioManager audio;
	
	private static final String TAG="BLEservice]]";
	private final IBinder mBinder = new MyBinder();
	private ANCSParser mANCSHandler;
	private ANCSGattCallback mANCScb;
	BluetoothGatt mBluetoothGatt;
	BroadcastReceiver mBtOnOffReceiver;
	boolean mAuto;
	String addr;
    public class MyBinder extends Binder {
    	BLEservice getService() {
            // Return this instance  so clients can call public methods
            return BLEservice.this;
        }
    }
    @SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
			switch (msg.what) {
			case 11:	//bt off, stopSelf()
				stopSelf();
				startActivityMsg();
				break;
			}
    	}
    };
    // when bt off,  show a Message to notify user that ble need re_connect
    private void startActivityMsg(){
    	Intent i = new Intent(this,Notice.class);
    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(i);
    }
    
	@Override
	public void onCreate() {
		super.onCreate();
		mANCSHandler = ANCSParser.getDefault(this);
		mANCScb = new ANCSGattCallback(this, mANCSHandler);
		mBtOnOffReceiver = new BroadcastReceiver() {
			public void onReceive(Context arg0, Intent i) {
				// action must be bt on/off .
				int state = i.getIntExtra(BluetoothAdapter.EXTRA_STATE,
						BluetoothAdapter.ERROR);
				if (state == BluetoothAdapter.STATE_OFF) {
					Devices.log("bluetooth OFF !");
					mHandler.sendEmptyMessageDelayed(11, 500);
				}
			}
		};
		IntentFilter filter= new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);// bt on/off
		registerReceiver(mBtOnOffReceiver, filter);
		Devices.log(TAG+"onCreate()");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			mAuto = intent.getBooleanExtra("auto", true);
			addr = intent.getStringExtra("addr");
		}
		
		// get audio manager
		audio = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		
		// set up live card
		if (mLiveCard == null) {
			
			// Get an instance of a live card
	        mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
	
	        // Inflate a layout into a remote view
	        mLiveCardView = new RemoteViews(getPackageName(), R.layout.notif_card);
	        
	        // Set default values
	        mLiveCardView.setTextViewText(R.id.notif_subject, "title");
	        mLiveCardView.setTextViewText(R.id.notif_message, "message");
	        mLiveCardView.setTextViewText(R.id.notif_time, "date");
	        
	        // Set up the live card's action with a pending intent
            // to show a menu when tapped
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(
                this, 0, menuIntent, 0));
	        
            // Always call setViews() to update the live card's RemoteViews.
            mLiveCard.setViews(mLiveCardView);
            
	        // Publish the live card
            mLiveCard.publish(PublishMode.REVEAL);
        
		}
		
		Devices.log(TAG+"onStartCommand() flags="+flags+",stardId="+startId);
		return startId;
	}

	@Override
	public void onDestroy() {
		Devices.log(TAG+" onDestroy()");
		mANCScb.stop();
		unregisterReceiver(mBtOnOffReceiver);
		Editor e =getSharedPreferences(Devices.PREFS_NAME, 0).edit();
		e.putInt(Devices.BleStateKey, ANCSGattCallback.BleDisconnect);
		e.commit();

		if (mLiveCard != null && mLiveCard.isPublished()) {
			mLiveCard.unpublish();
	        mLiveCard = null;
	    }
		
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent i) {
		Devices.log(TAG+" onBind()thread id ="+android.os.Process.myTid());
		return mBinder;
	}

	//** when ios notification changed
	@Override
	public void onIOSNotificationAdd(IOSNotification noti) {
		Devices.log("Notification");
		Devices.log(noti.title);
		Devices.log(noti.message);
		
		mLiveCardView.setTextViewText(R.id.notif_subject, noti.title);
        mLiveCardView.setTextViewText(R.id.notif_message, noti.message);
        
        if (noti.date != null && !noti.date.isEmpty())
        {
        	Date date = null;
    		SimpleDateFormat old_date_format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    		SimpleDateFormat new_date_format = new SimpleDateFormat("hh:mm a");
    		try {
    			date = old_date_format.parse(noti.date);
    		} catch (ParseException e) {
    			e.printStackTrace();
    		}
        	mLiveCardView.setTextViewText(R.id.notif_time, new_date_format.format(date));
        }
        
        // Always call setViews() to update the live card's RemoteViews.
        mLiveCard.setViews(mLiveCardView);
        
        // Play sound effect
        audio.playSoundEffect(Sounds.SUCCESS);
	}

	@Override
	public void onIOSNotificationRemove(int uid) {
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(uid);
	}
	
	//** public method , for client to call
	public void startBleConnect(String addr, boolean auto) {
		Devices.log(TAG+" startBleConnect() iPhone addr = "+addr);
		mAuto = auto;
		this.addr = addr;
		BluetoothDevice dev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(addr);
		mANCSHandler.listenIOSNotification(this);
		mBluetoothGatt = dev.connectGatt(this, auto, mANCScb);
		mANCScb.setBluetoothGatt(mBluetoothGatt);
		mANCScb.setStateStart();
	}

	public void registerStateChanged(StateListener sl) {
		if (null != sl)
			mANCScb.addStateListen(sl);
	}
	public void connect(){
		if (!mAuto)
			mBluetoothGatt.connect();
	}
	public String getStateDes(){
		return mANCScb.getState();
	}
}
