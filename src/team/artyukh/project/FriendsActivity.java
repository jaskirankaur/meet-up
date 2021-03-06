package team.artyukh.project;

import team.artyukh.project.lists.IListable;
import team.artyukh.project.lists.ListableFragment;
import team.artyukh.project.messages.client.RemoveFriendRequest;
import team.artyukh.project.messages.client.ViewFriendsRequest;
import team.artyukh.project.messages.server.FriendIdUpdate;
import team.artyukh.project.messages.server.ViewFriendsUpdate;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.TextView;

public class FriendsActivity extends BindingActivity {
	
	private TextView noFriends;
	private Button removeFriend;
	private ListableFragment mainFrag;
	private boolean showingFriends = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friends);
		
		noFriends = (TextView) findViewById(R.id.tvNoFriends);
		removeFriend = (Button) findViewById(R.id.btnRemoveFriend);
		
		mainFrag = new ListableFragment(FriendsActivity.this);
    	FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container_friends, mainFrag).commit();
        mainFrag.makeSelectable();
        
        getSupportFragmentManager().beginTransaction().hide(mainFrag).commit();
        noFriends.setVisibility(View.INVISIBLE);
        removeFriend.setVisibility(View.INVISIBLE);
	}
	
	@Override
	protected void onServiceConnected(){
		send((new ViewFriendsRequest()).toString());
	}
	
	@Override
	protected void applyUpdate(ViewFriendsUpdate message){
		if(message.getFriends().size() > 0){
			mainFrag.setAdapter(new ListableAdapter(FriendsActivity.this, message.getFriends(), true));
			getSupportFragmentManager().beginTransaction().show(mainFrag).commit();
	        noFriends.setVisibility(View.INVISIBLE);
	        removeFriend.setVisibility(View.VISIBLE);
		}
		else{
			getSupportFragmentManager().beginTransaction().hide(mainFrag).commit();
	        noFriends.setVisibility(View.VISIBLE);
	        removeFriend.setVisibility(View.INVISIBLE);
		}
	}
	
	@Override
	protected void applyUpdate(FriendIdUpdate message){
		send((new ViewFriendsRequest()).toString());
	}
	
	public void removeFriend(View v){
		int position = mainFrag.getSelectedItemPosition();
		
		if(position >= 0){
			IListable friend = mainFrag.getAdapter().getItem(position);
			send((new RemoveFriendRequest(friend.getId())).toString());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.friends, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
