package ir.mbf5923.searchbar.searchbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import ir.mbf5923.searchbar.searchbar.adapter.AutoSuggestAdapter;


public class mbfsearchbar extends LinearLayout {
    private AutoSuggestAdapter autoSuggestAdapter;
    private Listener listener;
    private Context mContext;
    private ImageView imgmag;
    private AutoCompleteTextView actvsearch;
    private static final int TRIGGER_AUTO_COMPLETE = 100;
    private static final long AUTO_COMPLETE_DELAY = 300;
    private Handler handler;
    private List<String> cid = new ArrayList<String>();
    private boolean isopen = false;
    private String serviceurl = "";
    private View root;
    private boolean showkeyboard = false;
    private int closeimage, textcolor, searchbutton, hinttextcolor, underlinecolor;
    private String hinttext = "", text = "", direction = "";

    public void setUrl(String serviceurl) {
        this.serviceurl = serviceurl;
    }

    public mbfsearchbar(Context context) {
        super(context);
        this.mContext = context;
        this.closeimage = R.attr.closebutton;
        init(context, null);
        Log.e("attr", "no");
    }


    public mbfsearchbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        TypedArray theAttrs = context.obtainStyledAttributes(attrs, R.styleable.mbfsearchbar);
        closeimage = theAttrs.getResourceId(R.styleable.mbfsearchbar_closebutton, android.R.drawable.ic_delete);
        searchbutton = theAttrs.getResourceId(R.styleable.mbfsearchbar_searchbutton, android.R.drawable.ic_menu_search);
        textcolor = theAttrs.getColor(R.styleable.mbfsearchbar_textcolor, Color.parseColor("#FF2343C2"));
        hinttextcolor = theAttrs.getColor(R.styleable.mbfsearchbar_hinttextcolor, Color.parseColor("#bfbfbf"));
        underlinecolor = theAttrs.getColor(R.styleable.mbfsearchbar_underlinecolor, Color.parseColor("#bfbfbf"));
        hinttext = theAttrs.getString(R.styleable.mbfsearchbar_hinttext);
        text = theAttrs.getString(R.styleable.mbfsearchbar_text);
        direction = theAttrs.getString(R.styleable.mbfsearchbar_direction);
        theAttrs.recycle();
        init(context, attrs);

        Log.e("attr", "yes");
    }

    private void init(Context context, AttributeSet attrs) {

        root = inflate(context, R.layout.searchbar, this);


        actvsearch = root.findViewById(R.id.actvsearch);
        actvsearch.setTextColor(textcolor);
        actvsearch.setHint(hinttext);
        actvsearch.setHintTextColor(hinttextcolor);
        actvsearch.setText(text);
        ColorStateList colorStateList = ColorStateList.valueOf(underlinecolor);
        ViewCompat.setBackgroundTintList(actvsearch, colorStateList);
        imgmag = root.findViewById(R.id.imgmag);

        imgmag.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isopen) {
                    actvsearch.setText("");
                    isopen = false;
                    actvsearch.setVisibility(GONE);
                    imgmag.setImageResource(searchbutton);
                    InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        Log.e("close", "keyboard");
                        imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
                    }
                } else {

                    isopen = true;
                    actvsearch.setVisibility(VISIBLE);
                    imgmag.setImageResource(closeimage);
                    actvsearch.requestFocus();
                    InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {

                        imm.showSoftInput(actvsearch, 0);
                    }

                }

            }
        });

        initComponents();


    }

    private void initComponents() {

        actvsearch.setThreshold(2);
        autoSuggestAdapter = new AutoSuggestAdapter(mContext,
                android.R.layout.simple_dropdown_item_1line);
        actvsearch.setAdapter(autoSuggestAdapter);


        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == TRIGGER_AUTO_COMPLETE) {
                    if (!TextUtils.isEmpty(actvsearch.getText())) {
                        makeApiCall(actvsearch.getText().toString());
                    }
                }
                return false;
            }
        });


        actvsearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {

                    imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
                }
                if (listener != null) {
                    listener.onItemClickListener(position, id);
                }
            }
        });

        actvsearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int
                    count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                if (handler != null) {
                    handler.removeMessages(TRIGGER_AUTO_COMPLETE);
                    handler.sendEmptyMessageDelayed(TRIGGER_AUTO_COMPLETE,
                            AUTO_COMPLETE_DELAY);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });

        actvsearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    //run query to the server
                    isopen = false;
                    actvsearch.setVisibility(GONE);
                    imgmag.setImageResource(searchbutton);
                    InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        Log.e("close", "keyboard");
                        imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
                    }


                }
                return false;
            }
        });
    }


    public interface Listener {
        public void onItemClickListener(int position, long id);
    }


    public void setListener(Listener listener) {
        this.listener = listener;
    }


    private void makeApiCall(String s) {
        String strreturn = "";

        new ServiceHandler().execute(serviceurl, s);


    }

    public class ServiceHandler extends AsyncTask<String, Void, String> {
        protected void onPreExecute() {
        }

        protected String doInBackground(String... arg0) {
            try {

                URL url = new URL(arg0[0]); // here is your URL path

                JSONObject postDataParams = new JSONObject();
                postDataParams.put("string", arg0[1]);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in = new BufferedReader(new
                            InputStreamReader(
                            conn.getInputStream()));

                    StringBuffer sb = new StringBuffer("");
                    String line = "";

                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                        break;
                    }

                    in.close();
                    return sb.toString();

                } else {
                    return new String("false : " + responseCode);
                }
            } catch (Exception e) {
                return new String("Exception: " + e.getMessage());
            }

        }

        @Override
        protected void onPostExecute(String result) {

            List<String> stringList = new ArrayList<>();
            cid.clear();
            try {
                JSONObject responseObject = new JSONObject(result);
                JSONArray array = responseObject.getJSONArray("results");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject row = array.getJSONObject(i);
                    stringList.add(row.getString("text"));
                    cid.add(row.getString("id"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            autoSuggestAdapter.setData(stringList);
            autoSuggestAdapter.notifyDataSetChanged();
        }


        public String getPostDataString(JSONObject params) throws Exception {

            StringBuilder result = new StringBuilder();
            boolean first = true;

            Iterator<String> itr = params.keys();

            while (itr.hasNext()) {

                String key = itr.next();
                Object value = params.get(key);

                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(key, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(value.toString(), "UTF-8"));

            }
            return result.toString();
        }
    }
}
