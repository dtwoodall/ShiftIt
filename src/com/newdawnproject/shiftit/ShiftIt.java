package com.newdawnproject.shiftit;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.newdawnproject.shiftit.ShiftView.ShiftThread;

public class ShiftIt extends Activity {
	private ShiftView mo_surface;
	private ShiftThread mo_thread;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        mo_surface = (ShiftView) findViewById(R.id.surface);
        TextView myText = (TextView) findViewById(R.id.text);
        ImageButton myButton = (ImageButton) findViewById(R.id.button);
        mo_surface.setTextView(myText);
        mo_surface.setButton(myButton);
        mo_thread = mo_surface.getThread();
        
        if(savedInstanceState!=null){
        	mo_thread.restoreState(savedInstanceState);
        }
    }
    
    public void onConfigurationChanged(Configuration newConfig){
    	super.onConfigurationChanged(newConfig);
    	return;
    }
    
    public void onSaveInstanceState(Bundle outState){
    	super.onSaveInstanceState(outState);
    	mo_thread.saveState(outState);
    }
}