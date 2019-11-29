package a.w.heartratepolaralert;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.UUID;

import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarHrData;

public class MainActivity extends AppCompatActivity {
	PolarBleApi api;

	private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 100;
	private boolean connected = false;
	Button connectBtn;
	BluetoothAdapter mBluetoothAdapter;
	EditText heartbeatalertet, phonenumberet, messagetosendet;
	private String phoneNo, message;
	private boolean smsSent;
	private int SMS_INTERVAL = 60;
	private int timer = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
		}

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter != null){
			if (!mBluetoothAdapter.isEnabled()) {
				new AlertDialog.Builder(this)
								.setTitle("Bluetooth")
								.setMessage("Turn on bluetooth?")
								.setPositiveButton(android.R.string.yes, (dialog, which) -> mBluetoothAdapter.enable())
								.setNegativeButton(android.R.string.no, (dialog, which) -> {})
								.show();
			}
		}


		connectBtn = findViewById(R.id.connect_btn);
		heartbeatalertet = findViewById(R.id.heartbeatalert);
		phonenumberet = findViewById(R.id.phonenumber);
		messagetosendet = findViewById(R.id.messagetosend);

		PolarBleApi api = PolarBleApiDefaultImpl.defaultImplementation(this, PolarBleApi.FEATURE_HR);
		api.setApiCallback(new PolarBleApiCallback() {
			@Override
			public void blePowerStateChanged(boolean powered) {
				super.blePowerStateChanged(powered);
				Log.d("MyApp","BLE power: " + powered);
			}

			@Override
			public void deviceConnected(PolarDeviceInfo polarDeviceInfo) {
				super.deviceConnected(polarDeviceInfo);
				connected = true;
				connectBtn.setText("Disconnect");
				connectBtn.setBackground(getResources().getDrawable(R.drawable.bg_btn_red));
				Toast.makeText(MainActivity.this, "Connected to device", Toast.LENGTH_SHORT).show();
				heartbeatalertet.setEnabled(false);
				phonenumberet.setEnabled(false);
				messagetosendet.setEnabled(false);
			}

			@Override
			public void deviceConnecting(PolarDeviceInfo polarDeviceInfo) {
				super.deviceConnecting(polarDeviceInfo);
				Toast.makeText(MainActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void deviceDisconnected(PolarDeviceInfo polarDeviceInfo) {
				super.deviceDisconnected(polarDeviceInfo);
				connected = false;
				connectBtn.setText("Connect");
				connectBtn.setBackground(getResources().getDrawable(R.drawable.bg_btn_green));
				Toast.makeText(MainActivity.this, "Disconnected from device", Toast.LENGTH_SHORT).show();
				heartbeatalertet.setEnabled(true);
				phonenumberet.setEnabled(true);
				messagetosendet.setEnabled(true);
			}

			@Override
			public void ecgFeatureReady(String identifier) {
				super.ecgFeatureReady(identifier);
			}

			@Override
			public void accelerometerFeatureReady(String identifier) {
				super.accelerometerFeatureReady(identifier);
			}

			@Override
			public void ppgFeatureReady(String identifier) {
				super.ppgFeatureReady(identifier);
			}

			@Override
			public void ppiFeatureReady(String identifier) {
				super.ppiFeatureReady(identifier);
			}

			@Override
			public void biozFeatureReady(String identifier) {
				super.biozFeatureReady(identifier);
			}

			@Override
			public void hrFeatureReady(String identifier) {
				super.hrFeatureReady(identifier);
				Log.d("MyApp","HR READY: " + identifier);
				Toast.makeText(MainActivity.this, "Heartrate ready to get from device", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void disInformationReceived(String identifier, UUID uuid, String value) {
				super.disInformationReceived(identifier, uuid, value);
			}

			@Override
			public void batteryLevelReceived(String identifier, int level) {
				super.batteryLevelReceived(identifier, level);
			}

			@Override
			public void hrNotificationReceived(String identifier, PolarHrData data) {
				super.hrNotificationReceived(identifier, data);
				TextView currentHR = findViewById(R.id.currenthr);
				Log.e("Still counting", String.valueOf(data.hr));
				currentHR.setText(data.hr + "BPM");
				if(data.hr >= Integer.parseInt(heartbeatalertet.getText().toString()) && !smsSent){
					Log.e("Sending", "SMS");
					sendSMSMessage();
					smsSent = true;
				} else if(smsSent){
					++timer;
					if(timer > SMS_INTERVAL){
						smsSent = false;
						timer = 0;
					}
				}
			}

			@Override
			public void polarFtpFeatureReady(String identifier) {
				super.polarFtpFeatureReady(identifier);
			}
		});

		connectBtn.setOnClickListener(v->{
			if(!connected){
				try {
					mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
					if(mBluetoothAdapter != null){
						if (!mBluetoothAdapter.isEnabled()) {
							new AlertDialog.Builder(this)
											.setTitle("Bluetooth")
											.setMessage("Turn on bluetooth?")
											.setPositiveButton(android.R.string.yes, (dialog, which) -> mBluetoothAdapter.enable())
											.setNegativeButton(android.R.string.no, (dialog, which) -> {})
											.show();
						}
					}
					Toast.makeText(MainActivity.this, "Attempting to connect", Toast.LENGTH_SHORT).show();
					api.connectToDevice("173D1821");
				} catch (PolarInvalidArgument polarInvalidArgument) {
					polarInvalidArgument.printStackTrace();
				}
			} else {
				try {
					api.disconnectFromDevice("173D1821");
				} catch (PolarInvalidArgument polarInvalidArgument) {
					polarInvalidArgument.printStackTrace();
				}
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
		if(api != null)
			api.backgroundEntered();
	}

	@Override
	public void onResume() {
		super.onResume();
		if(api != null)
			api.foregroundEntered();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(api != null)
			api.shutDown();
	}

	private void sendSMSMessage() {
		phoneNo = phonenumberet.getText().toString();
		message = messagetosendet.getText().toString();

		if (ContextCompat.checkSelfPermission(this,
						Manifest.permission.SEND_SMS)
						!= PackageManager.PERMISSION_GRANTED) {
			if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
							Manifest.permission.SEND_SMS)) {
				ActivityCompat.requestPermissions(this,
								new String[]{Manifest.permission.SEND_SMS},
								MY_PERMISSIONS_REQUEST_SEND_SMS);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_SEND_SMS: {
				if (grantResults.length > 0
								&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					SmsManager smsManager = SmsManager.getDefault();
					smsManager.sendTextMessage(phoneNo, null, message, null, null);
					Toast.makeText(getApplicationContext(), "SMS sent.",
									Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(getApplicationContext(),
									"SMS failed, please try again.", Toast.LENGTH_LONG).show();
				}
			}
		}

	}
}
