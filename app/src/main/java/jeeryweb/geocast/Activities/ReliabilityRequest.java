package jeeryweb.geocast.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import de.hdodenhof.circleimageview.CircleImageView;
import jeeryweb.geocast.Constants.APIEndPoint;
import jeeryweb.geocast.R;
import jeeryweb.geocast.Utility.Network;
import jeeryweb.geocast.Utility.SharedPrefHandler;

public class ReliabilityRequest extends AppCompatActivity {

    private static Handler handler;
    //TAG for Logging
//    private final String getPPSummary = "https://jeeryweb.000webhostapp.com/ProjectLoc/getSummary.php";
//    private final String sendreliable = "https://jeeryweb.000webhostapp.com/ProjectLoc/setReliability.php";
    private String username, phoneno;
    private TextView User, summary;
    private EditText phno;
    private ImageButton reliableYes;
    private ProgressBar loadingSummaryprogressBar;
    private CircleImageView pp;
    private SharedPrefHandler sharedPrefHandler;
    APIEndPoint apiEndPoint;
    private Network network;
    private String result;
    private ProgressDialog progressDialog;
    //private final String sendMsg = "https://jeeryweb.000webhostapp.com/ProjectLoc/uploadMsg.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reliability_request);
        Intent intent = getIntent();
        username = intent.getStringExtra("Username");

        Log.e("ReliabiltyRequsername", username);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pp = findViewById(R.id.activity_reliabilityreq_image);
        User = findViewById(R.id.activity_reliabilityreq_Username);
        summary = findViewById(R.id.activity_reliabilityreq_summary);
        phno = findViewById(R.id.activity_reliabilityreq_phno);
        reliableYes = findViewById(R.id.activity_reliabilityreq_button);
        loadingSummaryprogressBar = findViewById(R.id.activity_reliabilityreq_progressbar);

        sharedPrefHandler = new SharedPrefHandler(this);


        User.setText(username);

        if (sharedPrefHandler.getPhoneNo() != null && !sharedPrefHandler.getPhoneNo().contentEquals("NA")) {
            Log.e("Happening ", "=" + sharedPrefHandler.getPhoneNo());
            phoneno = sharedPrefHandler.getPhoneNo();
            phno.setVisibility(View.GONE);
        }


        loadingSummaryprogressBar.setVisibility(View.VISIBLE);

        //start new thread to get profile picture and summary
        //do this on a new thread
        new Thread(new Runnable() {
            public void run() {                                                 //THREAD 4.............
                // a potentially  time consuming task
                network = new Network(APIEndPoint.getPPSummary, username, "kkk", "kkk", "kk", "kk", "ksdhfj", null, null, null, null);
                result = network.DoWork();
                if (result != null) {
                    Log.e("get PP abd summary", result);
                    //pass this result to UI thread by writing a message to the UI's handler
                    Message m = Message.obtain();
                    Bundle bundle = new Bundle();
                    bundle.putString("resultPPSummary", result);
                    m.setData(bundle);
                    handler.sendMessage(m);
                }

            }
        }).start();

        handler = new Handler(Looper.getMainLooper()) {
            Object PPSummary, reliableReq;
            String splitter;

            @Override
            public void handleMessage(Message msg) {
                PPSummary = msg.getData().get("resultPPSummary");
                if (PPSummary != null) {
                    splitter = PPSummary.toString();
                    String[] PPSum = splitter.split("<br>");

                    String PPlink = PPSum[0];
                    String Summary = PPSum[1];
                    String responseTime = PPSum[2];

                    if (Double.parseDouble(responseTime) == -1)
                        responseTime = "Not responded to a message yet";
                    else
                        responseTime = responseTime + "s";

                    new setPP(PPlink).execute();

                    //format the summary
                    String[] summarysplitter = Summary.split("\\|");
                    String age = summarysplitter[0];
                    String profession = summarysplitter[1];
                    String gender = summarysplitter[2];
                    // String noConnections = summarysplitter[3];
                    summary.setText("Age: " + age + "\n" + "Profession: " + profession + "\n" + "Gender: " + gender + "\n" + "Avg Response Time: " + responseTime);


                    loadingSummaryprogressBar.setVisibility(View.GONE);

                }

                reliableReq = msg.getData().get("sendRRresult");
                if (reliableReq != null) {
                    progressDialog.dismiss();
                    //extract nearby users
                    splitter = reliableReq.toString();
                    if (splitter.contains("already"))
                        Toast.makeText(getApplicationContext(), "Reliability Req Already sent", Toast.LENGTH_SHORT).show();
                    else if (splitter.contains("error"))
                        Toast.makeText(getApplicationContext(), "Can't send reliability request right now", Toast.LENGTH_SHORT).show();
                    else if (splitter.contains("xyz"))
                        Toast.makeText(getApplicationContext(), "Already a Reliable Connection", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getApplicationContext(), "Reliability request send", Toast.LENGTH_SHORT).show();
                }
            }
        };


        //done-----------
        reliableYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (checkPhoneNumberValid()) {
                    progressDialog = new ProgressDialog(ReliabilityRequest.this);
                    progressDialog.setMessage("Sending  Reliability Request");
                    progressDialog.show();

                    //checkPhoneNumberValid()
                    //start new thraed to send a message notification to the user
                    //get the firebase token of the user
                    //send a req to firebase server to push a message
                    //do this on a new thread
                    Log.e("reliability req", "sending to network");
                    new Thread(new Runnable() {
                        public void run() {                                                 //THREAD 4.............
                            // a potentially  time consuming task
                            network = new Network(APIEndPoint.sendreliable, Home.username, username, phoneno, "kk", "kk", "ksdhfj", null, null, null, null);
                            result = network.DoWork();
                            if (result != null) {
                                Log.e("send reliability req", result);
                                //pass this result to UI thread by writing a message to the UI's handler
                                Message m = Message.obtain();
                                Bundle bundle = new Bundle();
                                bundle.putString("sendRRresult", result);
                                m.setData(bundle);
                                handler.sendMessage(m);
                            }

                        }
                    }).start();

                }

            }
        });

    }


    private boolean checkPhoneNumberValid() {
        boolean valid = true;
        String p = sharedPrefHandler.getPhoneNo();
        if (p == null || p.contentEquals("NA")) {
            p = phno.getText().toString();
            phoneno = p;
        }
        Log.e("phno input", p + "len=" + String.valueOf(p.length()));
        if (p.length() != 10 || p.length() == 0) {
            phno.setError("Invalid phone number");
            valid = false;
        } else {
            phno.setError(null);
        }


        return valid;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class setPP extends AsyncTask<Void, Void, Bitmap> {
        private String name;

        public setPP(String name) {
            this.name = name;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                java.net.URL url = new java.net.URL(name);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }


        protected void onPostExecute(Bitmap b) {
            //show image uploaded
            if (b != null)
                pp.setImageBitmap(b);
            else
                Toast.makeText(getApplicationContext(), "Can't get User's Profile picture", Toast.LENGTH_SHORT).show();
        }
    }
}

