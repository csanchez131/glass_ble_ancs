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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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
	private List<BluetoothDevice> mList = new ArrayList<BluetoothDevice>();
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
					for (BluetoothDevice dev : mList) {
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
						mList.add(device);
						
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
		
		setContentView(R.layout.searching_ble);
		SliderView mIndeterm = (SliderView) findViewById(R.id.indeterm_slider);
		mIndeterm.startIndeterminate();
		
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
		
		mBluetoothCardScrollView = new CardScrollView(this);
		mBluetoothCardScrollAdapter = new BluetoothCardScrollAdapter();
		mBluetoothCardScrollView.setAdapter(mBluetoothCardScrollAdapter);
		mBluetoothCardScrollView.activate();
		mBluetoothCardScrollView.setOnItemClickListener(mBluetoothClickedHandler);
			        
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
