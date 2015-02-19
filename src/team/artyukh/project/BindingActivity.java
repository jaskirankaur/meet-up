package team.artyukh.project;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import team.artyukh.project.ConnectionService.ServiceBinder;
import team.artyukh.project.messages.server.ChatUpdate;
import team.artyukh.project.messages.server.GroupUpdate;
import team.artyukh.project.messages.server.ImageDownloadUpdate;
import team.artyukh.project.messages.server.InviteUpdate;
import team.artyukh.project.messages.server.LoginUpdate;
import team.artyukh.project.messages.server.MapUpdate;
import team.artyukh.project.messages.server.RegisterUpdate;
import team.artyukh.project.messages.server.SearchUpdate;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public abstract class BindingActivity extends FragmentActivity {
	
	private static final String PREFS_FILE = "team.artyukh.project.PREFS_FILE";
	static SharedPreferences data;
	public static final String PREF_USER_ID = "USER_ID";
	public static final String PREF_LAT = "PREF_LAT";
	public static final String PREF_LON = "PREF_LON";
	public static final String PREF_USERNAME = "USERNAME";
	public static final String PREF_PASSWORD = "PASSWORD";
	public static final String PREF_STATUS = "STATUS";
	public static final String PREF_GROUP = "GROUP";
	public static final String PREF_GROUP_MEMBERS = "GROUP_MEMBERS";
	public static final String PREF_CHAT = "CHAT";
	static final String PREF_PHONE_NUMBER = "PHONE_NUMBER";
	private BroadcastReceiver receiver;
	private ConnectionService connService;
    private boolean serviceBound = false;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = this.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String message = intent.getStringExtra("message");
				processMessage(message);
			}
		};
    }
	
	protected static void setPref(String key, String value){
		data.edit().putString(key, value).apply();
		
	}
	
	public static void setPref(String key, double value){
		data.edit().putFloat(key, (float) value).apply();
	}
	
	public static String getStringPref(String key){
		return data.getString(key, "");
	}
	
	public static double getDoublePref(String key){
		return data.getFloat(key, 0);
	}
	
	public static void removePref(String key){
		data.edit().remove(key).apply();
	}
	
	protected void processMessage(String message){
		try {
			JSONObject msgObj = new JSONObject(message);
			String type = msgObj.getString("type");

			if (type.equals("req")) {
				applyUpdate(new MapUpdate(msgObj));
			} else if (type.equals("search")) {
				applyUpdate(new SearchUpdate(msgObj));
			} else if (type.equals("chat")) {
				applyUpdate(new ChatUpdate(msgObj));
			} else if (type.equals("login")) {
				applyUpdate(new LoginUpdate(msgObj));
			} else if (type.equals("register")) {
				applyUpdate(new RegisterUpdate(msgObj));
			} else if (type.equals("invite")) {
				applyUpdate(new InviteUpdate(msgObj));
			} else if (type.equals("newgroup")) {
				applyUpdate(new GroupUpdate(msgObj));
				Log.i("MEMBERS", msgObj.toString());
			} else if (type.equals("imagedownload")){
				applyUpdate(new ImageDownloadUpdate(msgObj));
			}

		} catch (JSONException e) {
			Log.i("EX_BIND", e.toString());
		}
	}
	
	protected void applyUpdate(LoginUpdate message){
		if(!message.getStatus()){
			Intent intent = new Intent(this, LoginActivity.class);
			startActivity(intent);
		}
	}
	
	protected void applyUpdate(ChatUpdate message) {

	}

	protected void applyUpdate(GroupUpdate message) {

	}

	protected void applyUpdate(RegisterUpdate message) {
		
	}

	protected void applyUpdate(MapUpdate message) {

	}
	
	protected void applyUpdate(ImageDownloadUpdate message){
		ListableAdapter.saveNewBitmap(message.getObjectId(), getBitmap(message.getObjectId()));
	}

	protected void applyUpdate(InviteUpdate message) {
		String host = "";
		final String groupId;
		
		groupId = message.getGroupId();
		host = message.getSender();
		
		AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("WARNING");
		b.setMessage(host + " has invited you to join their group.");
        b.setPositiveButton("Accept",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						joinGroup(groupId);
					}
				});
        b.setNegativeButton("Not now",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

        AlertDialog confirm = b.create();
        confirm.show();
	}
	
	protected void applyUpdate(SearchUpdate message) {

	}
	
	private void joinGroup(String groupId){
		JSONObject joinObj = new JSONObject();
		
		try {
			joinObj.put("type", "updategroup");
			joinObj.put("username", getStringPref(PREF_USERNAME));
			joinObj.put("group", groupId);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		send(joinObj.toString());
	}
	
	public void send(String message){
		if(serviceBound){
			connService.send(message);
    	}
	}
	
	public void send(byte[] image){
		if(serviceBound){
			connService.send(image);
		}
	}
	
	protected void sendPendingInvite(String invite){
		connService.addPendingInvite(invite);
	}
	
	protected Bitmap getBitmap(String objId){
		File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		
		for(String filename : dir.list()){
			Log.i("CHECKING FILE", filename);
			if(filename.startsWith(objId)){
				File image = new File(dir, filename);
				final BitmapFactory.Options options = new BitmapFactory.Options();
				Bitmap bmp = BitmapFactory.decodeFile(image.getAbsolutePath(), options);
				return bmp;
			}
		}
		
		return null;
	}
	
	@Override
    protected void onStart() {
    	super.onStart();
    	Intent intent = new Intent(this, ConnectionService.class);
    	startService(intent);
    	bindService(intent, sConnection, Context.BIND_AUTO_CREATE);
    	LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(ConnectionService.INTENT_MESSAGE));
    }
    
    @Override
    protected void onStop() {
        if(serviceBound){
        	unbindService(sConnection);
        	serviceBound = false;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }
    
    private ServiceConnection sConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			ServiceBinder binder = (ServiceBinder) service;
			connService = binder.getService();
			serviceBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceBound = false;
		}
    	
    };
}