/*
Copyright (c) 2013 Dattas Moonchaser

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dattasmoon.pebble.plugin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class IgnorePreference extends DialogPreference {
    JSONArrayAdapter arrayAdapter;
    ListView lvIgnore;
    EditText etMatch;
    CheckBox chkRawRegex;
    Spinner spnApplications;
    public IgnorePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_ignore);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }
    @Override
    protected void onBindDialogView(View view) {


        Button btnAdd = (Button)view.findViewById(R.id.btnAdd);
        etMatch = (EditText)view.findViewById(R.id.etMatch);
        chkRawRegex = (CheckBox)view.findViewById(R.id.chkRawRegex);
        spnApplications = (Spinner)view.findViewById(R.id.spnApplications);
        lvIgnore = (ListView) view.findViewById(R.id.lvIgnore);
        lvIgnore.setAdapter(arrayAdapter);
        lvIgnore.setEmptyView(view.findViewById(android.R.id.empty));
        new LoadAppsTask().execute();
        lvIgnore.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
                AdapterView.AdapterContextMenuInfo contextInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
                int position = contextInfo.position;
                long id = contextInfo.id;
                // the child view who's info we're viewing (should be equal to v)
                View v = contextInfo.targetView;
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());


                final int arrayPosition = (Integer) v.getTag();
                final String text = ((TextView)v.findViewById(R.id.tvItem)).getText().toString();
                builder.setMessage(getContext().getResources().getString(R.string.confirm_delete) + " '" + text + "' ?")
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                JSONArray temp = new JSONArray();
                                for(int i = 0; i < arrayAdapter.getJSONArray().length(); i++){
                                    if(i == arrayPosition){
                                        continue;
                                    }
                                    try {
                                        temp.put(arrayAdapter.getJSONArray().getJSONObject(i));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                arrayAdapter.setJSONArray(temp);

                                arrayAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                builder.create().show();
            }
        });
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject item = new JSONObject();
                try {
                    item.put("match", etMatch.getText().toString());
                    item.put("raw", chkRawRegex.isChecked());
                    item.put("app", spnApplications.getSelectedView().getTag().toString());
                    if(Constants.IS_LOGGABLE){
                        Log.i(Constants.LOG_TAG, "Item is: " + item.toString());
                    }
                    arrayAdapter.getJSONArray().put(item);
                    etMatch.setText("");
                    arrayAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        super.onBindDialogView(view);

    }
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String tempValue;
        JSONArray mCurrentValue;
        if (restoreValue) {
            if (defaultValue == null) {
                tempValue = getPersistedString("[]");
            } else {
                tempValue = getPersistedString(defaultValue.toString());
            }
        } else {
            tempValue = defaultValue.toString();
        }
        try{
            mCurrentValue = new JSONArray(tempValue);
        } catch (JSONException e){
            mCurrentValue = new JSONArray();
        }
        arrayAdapter = new JSONArrayAdapter(getContext(), mCurrentValue);
    }
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            String tempValue = arrayAdapter.getJSONArray().toString();
            if(Constants.IS_LOGGABLE){
                Log.i(Constants.LOG_TAG, "Setting preference to be: " + tempValue);
            }
            persistString(tempValue);
        }
    }
    private class LoadAppsTask extends AsyncTask<Void, Integer, List<PackageInfo>> {

        @Override
        protected List<PackageInfo> doInBackground(Void... unused) {
            final List<PackageInfo> pkgAppsList = getContext().getPackageManager().getInstalledPackages(0);

            PackageComparator comparer = new PackageComparator();
            Collections.sort(pkgAppsList, comparer);
            pkgAppsList.add(0, null);
            return pkgAppsList;
        }

        @Override
        protected void onPostExecute(List<PackageInfo> pkgAppsList) {
            packageAdapter adapter = new packageAdapter(getContext(), pkgAppsList
                    .toArray(new PackageInfo[pkgAppsList.size()]));
            //adapter.setDropDownViewResource(R.layout.list_convert_item);
            spnApplications.setAdapter(adapter);

        }
    }

    private class packageAdapter extends ArrayAdapter<PackageInfo> {
        private final Context       context;
        private final PackageInfo[] packages;

        public packageAdapter(Context context, PackageInfo[] packages) {
            super(context, R.layout.list_convert_item, packages);
            this.context = context;
            this.packages = packages;
        }
        @Override
        public View getDropDownView(int position, View convertView,ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent) {
            return getCustomView(position, rowView, parent);
        }
        private View getCustomView(int position, View rowView, ViewGroup parent){
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.list_convert_item, parent, false);

            }

            PackageInfo info = packages[position];

            TextView tvText = (TextView)rowView.findViewById(R.id.tvItem);
            if(info == null){
                tvText.setText("Any");
                rowView.setTag("-1");
            } else {
                tvText.setText(info.applicationInfo.loadLabel(getContext().getPackageManager()).toString() + " ("+info.packageName+")");
                rowView.setTag(info.packageName);
            }

            return rowView;
        }
    }

    public class JSONArrayAdapter extends BaseAdapter {

        private ViewGroup group;
        private JSONArray items;
        private Context context;

        public JSONArrayAdapter(Context context, JSONArray items){
            super();
            this.items = items;
            this.context = context;
        }

        public JSONArray getJSONArray(){
            return items;
        }
        public void setJSONArray(JSONArray items){
            this.items = items;
        }

        public int getCount() {
            return items.length();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent)  {
            View view = convertView;
            group = parent;

            if (view == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.list_convert_item, null);
            }

            String itemText = "";

            try {
                JSONObject jsonObject = items.getJSONObject(position);
                view.setTag(position);


                if(jsonObject.getBoolean("raw")){
                    itemText = "(Regex) ";
                }
                String pkg = jsonObject.getString("app");
                if(pkg.equals("-1")){
                    itemText += "[Any] ";
                } else {
                    itemText += "[" + pkg + "] ";
                }
                itemText += jsonObject.getString("match");
            } catch (JSONException e) {

            }

            if (itemText != null){

                TextView name = (TextView) view.findViewById(R.id.tvItem);

                if (name != null)
                    name.setText(itemText);
            }


            return view;
        }
    }
    public class PackageComparator implements Comparator<PackageInfo> {

        @Override
        public int compare(PackageInfo leftPackage, PackageInfo rightPackage) {

            String leftName = leftPackage.applicationInfo.loadLabel(getContext().getPackageManager()).toString();
            String rightName = rightPackage.applicationInfo.loadLabel(getContext().getPackageManager()).toString();

            return leftName.compareToIgnoreCase(rightName);
        }
    }
}
