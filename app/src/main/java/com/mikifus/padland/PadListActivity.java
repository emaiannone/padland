/*
 * Copyleft PadLand
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
package com.mikifus.padland;

import android.app.LoaderManager;
import android.content.ClipData;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import com.mikifus.padland.Dialog.NewPadGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This activity displays a list of previously checked documents.
 * Here documents can be deleted via Intent.
 * It handles as well the sharing intent to the app.
 *
 * @author mikifus
 * @since 0.1
 */
public class PadListActivity extends PadLandDataActivity
        implements ActionMode.Callback, LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "PadListActivity";

    /**
     * mActionMode defines behaviour of the action-bar
     */
    protected ActionMode mActionMode;

    /**
     * expandableListView
     */
    private ExpandableListView expandableListView;

    /**
     * Adapter to play with the expandableListView
     */
    private PadListAdapter adapter = null;

    /**
     * Override onCreate
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Layout
        super.onCreate(savedInstanceState);

        // Intent
        this._actionFromIntent();

        setContentView(R.layout.activity_padlist);

        // Intent
        this._textFromIntent();

        // Loader
        this.initLoader(this);

        // Init list view
        this._initListView();
    }

    /**
     * If there is a share intent this function gets the extra text
     * and copies it into clipboard
     */
    private void _textFromIntent() {
        String extra_text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        if (extra_text != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(extra_text);
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", extra_text);
                clipboard.setPrimaryClip(clip);
            }

            Toast.makeText(this, getString(R.string.activity_padlist_implicitintent_text_copied), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * If there is an intent with "action", here it is processed
     * For now there is only the delete action.
     */
    private void _actionFromIntent() {
        String action = getIntent().getStringExtra("action");
        ArrayList<String> pad_id_list = getIntent().getStringArrayListExtra("pad_id");
        if (action != null && pad_id_list.size() > 0) {
            Log.d("DELETE_PAD_INTENT", "list: " + pad_id_list.toString());
            switch (action) {
                case "delete":
                    for(int i = 0 ; i < pad_id_list.size(); i++)
                    {
                        Log.d("DELETE_PAD_INTENT", "action: " + action + " list_get: " + pad_id_list.get(i));
                        boolean result = padlandDb.deletePad(Long.parseLong(pad_id_list.get(i)));
                        if (result) {
                            Toast.makeText(this, getString(R.string.padlist_document_deleted), Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
            }
        }
//        if( adapter != null ) {
//            adapter.notifyDataSetChanged();
//        }
    }

    /**
     * When the list is empty a message with a button is shown.
     * This handles the button onClick.
     *
     * @param view
     */
    public void onNewPadClick(View view) {
        Intent newPadIntent = new Intent(this, NewPadActivity.class);
        startActivity(newPadIntent);
    }

    /**
     * When the list is empty a message with a button is shown.
     * This handles the button onClick.
     *
     * @param view
     */
    public void onNewPadgroupClick(View view) {
        showNewPadgroupDialog();
    }

    private void showNewPadgroupDialog() {
        FragmentManager fm = getSupportFragmentManager();
        NewPadGroup dialog = new NewPadGroup();
        dialog.show(fm, "dialog_new_padgroup");
    }

    /**
     * Makes an empty ListView and returns it.
     *
     * @return ListView
     */
    private void _initListView() {
        expandableListView = (ExpandableListView) findViewById(R.id.listView);
        expandableListView.setTextFilterEnabled(true);
        expandableListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        expandableListView.setEmptyView(findViewById(android.R.id.empty));

        // Set the data
        setAdapter();

        // events
        this._setListViewEvents();
    }

    /**
     * This function adds events listeners for a ListView object to provide usage of the ActionBar
     */
    private void _setListViewEvents() {
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Log.d(TAG, "child click: " + id);

                if (mActionMode != null) {
//                    startActionMode(view, position, id);
                    int position = ExpandableListView.getPackedPositionChild(id);
                    Log.d(TAG, "Childclick: position: " + position + " childPosition:" + childPosition + " waschecked?" + expandableListView.isItemChecked(position));

                    int final_position = childPosition + 1;
                    expandableListView.setItemChecked(final_position, (!expandableListView.isItemChecked(final_position)));
                    if (expandableListView.getCheckedItemCount() == 0) {
                        mActionMode.finish();
                    }
                    return false;
                }


                Intent padViewIntent = new Intent(PadListActivity.this, PadInfoActivity.class);
                padViewIntent.putExtra("pad_id", id);

                startActivity(padViewIntent);

                return true;
            }
        });

        expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //  convert the input flat list position to a packed position
                long packedPosition = expandableListView.getExpandableListPosition(position);

                int itemType        = ExpandableListView.getPackedPositionType(packedPosition);
                int groupPosition   = ExpandableListView.getPackedPositionGroup(packedPosition);
                int childPosition   = ExpandableListView.getPackedPositionChild(packedPosition);

                Log.d(TAG, "Longclick: item: "+itemType+" childpos:" + childPosition + " pos:" + position + " id:" + id);

                //  GROUP-item clicked
                if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    long groupId = adapter.getGroupId(groupPosition);
                    if( groupId > 0 ) {
                        menu_delete_group(groupId);
                        expandableListView.setSelectedGroup(groupPosition);
                        expandableListView.setItemChecked(position, true);
                        view.setSelected(true);
                        return true;
                    }
                    //  ...
//                        onGroupLongClick(groupPosition);
//                    return false;
                }
                //  CHILD-item clicked
                else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {

                    long childId = adapter.getChildId(groupPosition, childPosition);

                    startActionMode(view, position, childId);
                    expandableListView.setSelectedChild(groupPosition, childPosition, true);
                    expandableListView.setItemChecked(position, true);
                    view.setSelected(true);

                    // Return true as we are handling the event.
                    return true;
                }

                return false;
            }
        });
    }

    /**
     * Check an item and set is as selected.
     *
     * @param view
     * @param position
     * @param id
     */
    public void startActionMode(View view, int position, long id) {
        Log.d(TAG, "SELECTION NEW: pos:" + String.valueOf(position) + " id:" + String.valueOf(id));
//
        if (mActionMode == null) {
//            // Start the CAB using the ActionMode.Callback defined above
            PadListActivity.this.startActionMode(PadListActivity.this);
        }
    }

    /**
     * Gets an adapter for the expandableListView with the contents from the database
     *
     * @return
     */
    private void setAdapter()
    {
        ArrayList<HashMap<String, ArrayList>> group_data = getGroupsForAdapter();
        HashMap<Long, ArrayList<String>> padlist_data = _getPadListData();

        adapter = new PadListAdapter(this, group_data, padlist_data);

        // Bind to adapter.
        expandableListView.setAdapter(adapter);
    }

    private HashMap<Long, ArrayList<String>> _getPadListData()
    {
        Uri padlist_uri = Uri.parse(getString(R.string.request_padlist));
        Cursor cursor = getContentResolver()
                .query(padlist_uri,
                        new String[]{PadContentProvider._ID, PadContentProvider.NAME, PadContentProvider.URL},
                        null,
                        null,
                        PadContentProvider.LAST_USED_DATE + " ASC");

        HashMap<Long, ArrayList<String>> result = new HashMap<>();

        if (cursor == null || cursor.getCount() == 0) {
            return result;
        }

        HashMap<Long, ArrayList<String>> pad_data = new HashMap<>();

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            String url = cursor.getString(2);

            ArrayList<String> pad_strings = new ArrayList<String>();
            pad_strings.add(name);
            pad_strings.add(url);

            pad_data.put(id, pad_strings);

            // do something
            cursor.moveToNext();
        }
        cursor.close();

        return pad_data;
    }


    /**
     * Data loader initial event
     *
     * @param id
     * @param args
     * @return
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {PadContentProvider._ID, PadContentProvider.NAME, PadContentProvider.URL};
        CursorLoader cursorLoader = new CursorLoader(this, PadContentProvider.PADLIST_CONTENT_URI, projection, null, null, null);
        return cursorLoader;
    }

    /**
     * Data loader finish event
     *
     * @param loader
     * @param data
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        setAdapter();
    }

    /**
     * Data loader event
     *
     * @param loader
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // data is not available anymore, delete reference
        setAdapter();
    }

    /**
     * Called when the action mode is created; startActionMode() was called
     *
     * @param mode
     * @param menu
     * @return boolean
     */
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.rowselection, menu);

        mActionMode = mode;

        return true;
    }

    /**
     * Called each time the action mode is shown. Always called after onCreateActionMode, but
     * may be called multiple times if the mode is invalidated.
     *
     * @param mode
     * @param menu
     * @return boolean
     */
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false; // Return false if nothing is done
    }

    /**
     * Called when the user selects a contextual menu item
     *
     * @param mode
     * @param item
     * @return
     */
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuitem_group:
                menu_group(getCheckedItemIds(), getGroupsForAdapter());
                // Action picked, so close the CAB
                mode.finish();
                return true;
            case R.id.menuitem_delete:
                AskDelete(getCheckedItemIds());
                // Action picked, so close the CAB
                mode.finish();
                return true;
            case R.id.menuitem_share:
                menu_share(getCheckedItemIds());
                // Action picked, so close the CAB
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private ArrayList<String> getCheckedItemIds()
    {
        ArrayList<String> selectedItems = new ArrayList<>();
        HashMap<Long, ArrayList<String>> padlist_data = _getPadListData();

        SparseBooleanArray positions = expandableListView.getCheckedItemPositions();
        Log.d(TAG, "selectedItemsPositions: " + positions);
        for (int i = 0; i < positions.size(); ++i)
        {
            int position = positions.keyAt(i);
            if ( positions.valueAt(i) ) {
                long packed_position = expandableListView.getExpandableListPosition(position);
                int group = expandableListView.getPackedPositionGroup(packed_position);
                int child = expandableListView.getPackedPositionChild(packed_position);
                Log.d(TAG, "selectedItemsPositions: g: " + group + "c: " + child);
                if( child == -1 )
                {
                    continue;
                }
                selectedItems.add( String.valueOf(adapter.getChildId(group, child)) );
            }
        }
        Log.d(TAG, "selectedItemsIds: " + selectedItems.toString());

        return selectedItems;
    }

    /**
     * Called when the user exits the action mode
     *
     * @param mode
     */
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        uncheckAllItems();
    }

    private void uncheckAllItems() {
        if( expandableListView == null ) {
            return;
        }
        SparseBooleanArray checked = expandableListView.getCheckedItemPositions();
        for (int i = 0; i < checked.size(); i++) {
            // Item position in adapter
            int position = checked.keyAt(i);
            // Add sport if it is checked i.e.) == TRUE!
            if (checked.valueAt(i)) {
                expandableListView.setItemChecked(position, false);
            }
        }
    }

    /**
     * backbutton event
     */
    public void onBackPressed() {
        onDestroyActionMode(mActionMode);
        super.onBackPressed();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu, R.menu.pad_list);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public ArrayList<HashMap<String,ArrayList>> getGroupsForAdapter() {
        ArrayList<HashMap<String, ArrayList>> group_data = new ArrayList<>();
        HashMap<String, ArrayList> header = new HashMap<>();
        Log.d(TAG, "Requesting groups data");
        HashMap<Long, ArrayList<String>> padgroups_data = padlandDb._getPadgroupsData();

        Iterator iterator = padgroups_data.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            ArrayList<String> groupdata = (ArrayList<String>) pair.getValue();
            HashMap<String, ArrayList> group = new HashMap<>();
            group.put(groupdata.get(0), new ArrayList());
            group_data.add(group);
        }

        HashMap<Long, ArrayList<String>> padlist_data = _getPadListData();
        ArrayList<Long> pad_id_list = new ArrayList<>();

        iterator = padlist_data.keySet().iterator();
        while(iterator.hasNext())
        {
            long next = (long) iterator.next();
            pad_id_list.add(next);
        }
        header.put("Unclassified", pad_id_list);
        group_data.add(header);

        return group_data;
    }

    public void notifyDataSetChanged() {
        if( expandableListView != null ) {
            setAdapter();
//            ((BaseExpandableListAdapter) expandableListView.getExpandableListAdapter()).notifyDataSetChanged();
        }
    }
}