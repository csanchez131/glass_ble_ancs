package jz.ios.ancs;

import java.util.ArrayList;
import java.util.List;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import jz.ancs.parse.ANCSGattCallback;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class Devices extends Activity {
	public static final String TAG = "ble";
	
	public static final String PREFS_NAME = "MyPrefsFile";
	public static final String BleStateKey="ble_state";
	public static final String BleAddrKey="ble_addr";
	public static final String BleAutoKey="ble_auto_connect";
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mLEscaning = false;
	private List<BluetoothDevice> mList = new ArrayList<BluetoothDevice>();
	private List<Card> bluetoothCards = new ArrayList<Card>();
    private CardScrollView mBluetoothCardScrollView;
    private BluetoothCardScrollAdapter mBluetoothCardScrollAdapter;
	
	private LeScanCallback mLEScanCallback = new LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					boolean found = false;
					for (BluetoothDevice dev : mList) {
						if (dev.getAddress().equals(device.getAddress())) {
							found = true;
							break;
						}
					}
					if (!found) {
						mList.add(device);
						
						Card card = new Card(getApplicationContext());
						card.setText(device.getName());
						card.setFootnote(device.getAddress());
						bluetoothCards.add(card);
						
						mBluetoothCardScrollAdapter.notifyDataSetChanged();
					}
				}
			});

		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Card card = new Card(getApplicationContext());
		card.setText("Discovering Bluetooth");
		setContentView(card.getView());
		
		PackageManager pm = getPackageManager();
		boolean support = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
//		log(" BLE support: "+ support);
		if (!support) {
			Toast.makeText(this, "Ã¦Â­Â¤Ã¨Â®Â¾Ã¥Â¤â€¡Ã¤Â¸ï¿½Ã¦â€�Â¯Ã¦Å’ï¿½ BLE", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		BluetoothManager mgr = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mgr.getAdapter();
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, 1);
		}
		mList.clear();
		
		/*SharedPreferences sp=this.getSharedPreferences(PREFS_NAME, 0);
		int ble_state=sp.getInt(BleStateKey, 0);
		log("read ble state : "+ble_state);
		if(ANCSGattCallback.BleDisconnect != ble_state){
			boolean auto = sp.getBoolean(BleAutoKey, true);
			String addr = sp.getString(BleAddrKey, "");
			Intent intent = new Intent(this,  BLEConnect.class);
			intent.putExtra("addr", addr);
			intent.putExtra("auto", auto);
			intent.putExtra("state", ble_state);
			startActivity(intent);
			finish();
			return;
		}*/
		
		mBluetoothCardScrollView = new CardScrollView(this);
		mBluetoothCardScrollAdapter = new BluetoothCardScrollAdapter();
		mBluetoothCardScrollView.setAdapter(mBluetoothCardScrollAdapter);
		mBluetoothCardScrollView.activate();
		
		mBluetoothCardScrollView.setOnItemClickListener(mBluetoothClickedHandler);
		
	    setContentView(mBluetoothCardScrollView);
	        
		scan(true);
	}
	
	void scan(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			log("BLE scanning started...");
			mLEscaning = true;
			mBluetoothAdapter.startLeScan(mLEScanCallback);
		} else {
			if (mLEscaning) {
				mBluetoothAdapter.stopLeScan(mLEScanCallback);
				mLEscaning = false;
				log("BLE scanning stopped");
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		scan(false);
		super.onDestroy();
	}
	
	private OnItemClickListener mBluetoothClickedHandler = new OnItemClickListener() {
	    public void onItemClick(AdapterView parent, View v, int position, long id)
	    {
			scan(false);
	    	
	    	BluetoothDevice dev = mList.get(position);

			Intent intent = new Intent(getApplicationContext(),  BLEConnect.class);
			intent.putExtra("addr", dev.getAddress());
			intent.putExtra("auto", false);
			startActivity(intent);
			finish();
	    }
	};

	static void log(String s){
		Log.d(TAG, "[BLE_ancs] " + s);
	} 
	static void logw(String s){
		Log.w(TAG,  s);
	}
	static void loge(String s){
		Log.e(TAG,  s);
	}
	
	private class BluetoothCardScrollAdapter extends CardScrollAdapter {

        @Override
        public int getPosition(Object item) {
            return bluetoothCards.indexOf(item);
        }

        @Override
        public int getCount() {
            return bluetoothCards.size();
        }

        @Override
        public Object getItem(int position) {
            return bluetoothCards.get(position);
        }

        /**
         * Returns the amount of view types.
         */
        @Override
        public int getViewTypeCount() {
            return Card.getViewTypeCount();
        }

        /**
         * Returns the view type of this card so the system can figure out
         * if it can be recycled.
         */
        @Override
        public int getItemViewType(int position){
            return bluetoothCards.get(position).getItemViewType();
        }

        @Override
        public View getView(int position, View convertView,
                ViewGroup parent) {
            return  bluetoothCards.get(position).getView(convertView, parent);
        }
    }
}
