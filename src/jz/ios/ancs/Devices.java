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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;
import com.google.glass.widget.SliderView;


public class Devices extends Activity {
	public static final String TAG = "ble";
	
	public static final String PREFS_NAME = "MyPrefsFile";
	public static final String BleStateKey="ble_state";
	public static final String BleAddrKey="ble_addr";
	public static final String BleAutoKey="ble_auto_connect";
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mLEscaning = false;
	private List<BluetoothDevice> mBluetoothList = new ArrayList<BluetoothDevice>();
	private List<Card> bluetoothCards = new ArrayList<Card>();
    private CardScrollView mBluetoothCardScrollView;
    private BluetoothCardScrollAdapter mBluetoothCardScrollAdapter;
    private boolean bluetoothCardScrollViewVisible = false;
	
	private LeScanCallback mLEScanCallback = new LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					boolean found = false;
					for (BluetoothDevice dev : mBluetoothList) {
						// verify if this is a new BLE device
						if (dev.getAddress().equals(device.getAddress())) {
							found = true;
							break;
						}
					}
					
					// if new BLE device
					if (!found) {
						
						// show BLE cards
						if (!bluetoothCardScrollViewVisible)
						{
							setContentView(mBluetoothCardScrollView);
							bluetoothCardScrollViewVisible = true;
						}
						
						// add to list of BLE devices
						mBluetoothList.add(device);
						
						// add device to BLE cards
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
		
		// check if BLE is supported, all Glass should have it
		PackageManager pm = getPackageManager();
		boolean support = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
		if (!support) {
			Toast.makeText(this, R.string.error_ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		// put up searching for BLE screen
		setContentView(R.layout.searching_ble);
		SliderView mIndeterm = (SliderView) findViewById(R.id.indeterm_slider);
		mIndeterm.startIndeterminate();
		
		// initialize BluetoothManager, get adapter, ensure enabled
		BluetoothManager mgr = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mgr.getAdapter();
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, 1);
		}
		
		// clear list of BLE devices
		mBluetoothList.clear();
		
		// set up scroll view of Bluetooth device cards
		mBluetoothCardScrollView = new CardScrollView(this);
		mBluetoothCardScrollAdapter = new BluetoothCardScrollAdapter();
		mBluetoothCardScrollView.setAdapter(mBluetoothCardScrollAdapter);
		mBluetoothCardScrollView.activate();
		mBluetoothCardScrollView.setOnItemClickListener(mBluetoothClickedHandler);
		
		// start scanning BLE
		scan(true);
	}
	
	void scan(final boolean enable) {
		if (enable) {
			mLEscaning = true;
			mBluetoothAdapter.startLeScan(mLEScanCallback);
			log("BLE scanning started...");
		} else {
			if (mLEscaning) {
				mLEscaning = false;
				mBluetoothAdapter.stopLeScan(mLEScanCallback);
				log("BLE scanning stopped");
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		scan(false);
		super.onDestroy();
	}
	
	@Override
	protected void onPause() {
		scan(false);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		scan(true);
		super.onResume();
	}
	
	// when clicked on a bluetooth device card
	private OnItemClickListener mBluetoothClickedHandler = new OnItemClickListener() {
	    public void onItemClick(AdapterView parent, View v, int position, long id)
	    {
	    	// stop scanning
			scan(false);
	    	
			// get BLE device
	    	BluetoothDevice dev = mBluetoothList.get(position);

	    	// load connect class
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
            return mBluetoothList.indexOf(item);
        }

        @Override
        public int getCount() {
            return mBluetoothList.size();
        }

        @Override
        public Object getItem(int position) {
            return mBluetoothList.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	BluetoothDevice item = mBluetoothList.get(position);
        	View view = convertView;
			if (view == null) // no view to re-use, create new
				view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.ble_device, parent, false);
			
			 ((TextView)view.findViewById(R.id.device_name)).setText(item.getName());
			 ((TextView)view.findViewById(R.id.device_addr)).setText(item.getAddress());
			
        	return  view;
        }
    }
}
