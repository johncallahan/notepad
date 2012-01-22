/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.demo.notepad3;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Notepadv3 extends ListActivity {
    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;

    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int SEND_ID = Menu.FIRST + 2;
    private static final int WIPE_ID = Menu.FIRST + 3;
    private static final int PREFS_ID = Menu.FIRST + 4;
    private static final int SENDIT_ID = Menu.FIRST + 5;

    private NotesDbAdapter mDbHelper;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_list);
        mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
    }

    private void fillData() {
        Cursor notesCursor = mDbHelper.fetchAllNotes();
        startManagingCursor(notesCursor);

        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{NotesDbAdapter.KEY_BODYLENGTH,NotesDbAdapter.KEY_TITLE};

        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.text0,R.id.text1};

        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter notes = 
            new SimpleCursorAdapter(this, R.layout.notes_row, notesCursor, from, to);
        setListAdapter(notes);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_insert);
        menu.add(0, SEND_ID, 0, R.string.menu_send);
        menu.add(0, WIPE_ID, 0, R.string.menu_wipe);
        menu.add(0, PREFS_ID, 0, R.string.menu_prefs);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case INSERT_ID:
                createNote();
                return true;
            case SEND_ID:
            	sendNotes();
            	return true;
            case WIPE_ID:
            	wipeNotes();
            	return true;
            case PREFS_ID:
            	Intent prefs_intent = new Intent(this,MyPreferences.class);
            	startActivity(prefs_intent);
            	return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
        menu.add(0, SENDIT_ID, 0, R.string.menu_sendit);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case DELETE_ID:
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                mDbHelper.deleteNote(info.id);
                fillData();
                return true;
            case SENDIT_ID:
            	AdapterContextMenuInfo minfo = (AdapterContextMenuInfo) item.getMenuInfo();
            	sendNote(minfo.id);
            	return true;
        }
        return super.onContextItemSelected(item);
    }

    private void createNote() {
        Intent i = new Intent(this, NoteEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
    
    private void sendNotes() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String email = prefs.getString("email", "");
        final String rtmimport = prefs.getString("rtmimport", "");
        final String subject = prefs.getString("emailsubject", getResources().getString(R.string.email_subject));
    	sendMail(subject, email, rtmimport, getNotes());
    }
    
    private void sendNote(long id) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String email = prefs.getString("email", "");
        final String rtmemail = prefs.getString("rtmemail", "");
        sendMail(getTitle(id), email, rtmemail, getNote(id));
    }
    
    private void sendMail(String subject,String email, String rtm, String body) {
    	Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
    	
		String aEmailList[] = { email, rtm };

		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);

		emailIntent.setType("plain/text");
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);

		startActivity(emailIntent);
    }
    
    private void wipeNotes() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage("Wipe ALL notes?")
    	       .setCancelable(false)
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   mDbHelper.deleteAll();
    	        	   fillData();
    	           }
    	       })
    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
    }
    
    private String getTitle(long id) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	final String priority = prefs.getString("default_priority", getResources().getString(R.string.default_priority));
    	final String duedate = prefs.getString("default_duedate", getResources().getString(R.string.default_duedate));
    	StringBuffer strBuf = new StringBuffer();
    	Cursor cur = mDbHelper.fetchNote(id);
    	cur.moveToFirst();
    	strBuf.append(cur.getString(cur.getColumnIndexOrThrow(NotesDbAdapter.KEY_TITLE)));
    	strBuf.append(" ");
    	strBuf.append(priority);
    	strBuf.append(" ");
    	strBuf.append(duedate);
    	cur.close();
        return strBuf.toString();
    }
    
    private String getNote(long id) {
    	StringBuffer strBuf = new StringBuffer();
    	Cursor cur = mDbHelper.fetchNote(id);
    	cur.moveToFirst();
    	if(cur.getString(cur.getColumnIndexOrThrow(NotesDbAdapter.KEY_BODY)).length() > 0) {
        	strBuf.append(cur.getString(cur.getColumnIndexOrThrow(NotesDbAdapter.KEY_BODY)));
            strBuf.append("\n");
        }
    	strBuf.append("\n-end-\n");
    	cur.close();
        return strBuf.toString();
    }
    
    private String getNotes() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	final String priority = prefs.getString("default_priority", getResources().getString(R.string.default_priority));
    	final String duedate = prefs.getString("default_duedate", getResources().getString(R.string.default_duedate));
    	Cursor cur = mDbHelper.fetchAllNotes();
    	StringBuffer strBuf = new StringBuffer();
    	cur.moveToFirst();
        while (cur.isAfterLast() == false) {
            strBuf.append(cur.getString(cur.getColumnIndexOrThrow(NotesDbAdapter.KEY_TITLE)));
            strBuf.append(" ");
            strBuf.append(priority);
            strBuf.append(" ");
            strBuf.append(duedate);
            strBuf.append("\n");
       	    cur.moveToNext();
        }
        strBuf.append("\n-end-\n");
        cur.close();
        return strBuf.toString();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, NoteEdit.class);
        i.putExtra(NotesDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();
    }
}
