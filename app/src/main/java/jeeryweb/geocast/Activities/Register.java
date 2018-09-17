package jeeryweb.geocast.Activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jeeryweb.geocast.Constants.APIEndPoint;
import jeeryweb.geocast.R;
import jeeryweb.geocast.Utility.ImeiExtractor;
import jeeryweb.geocast.Utility.Network;
import jeeryweb.geocast.Utility.PPUpload;
import jeeryweb.geocast.Utility.SharedPrefHandler;

public class Register extends AppCompatActivity {


//Attributes*************************************************************

//    private final String reg ="https://jeeryweb.000webhostapp.com/ProjectLoc/register.php";
//    private final String url_uplaod = "https://jeeryweb.000webhostapp.com/ProjectLoc/saveProPic.php";
    private final String TAG=getClass().getSimpleName()+" LoginActivity";

    //objects
    APIEndPoint apiEndPoint;
    Network network;
    ImeiExtractor imeiExtractor;
    Context c;
    SharedPrefHandler session;
    Handler handler;

    //widgets
    EditText reguser , regpass1,regpass2 , bioAge , bioOccupation , phone , imeiShow;
    RadioGroup bioGender;
    Button register ;
    TextView loginLink;
    RadioButton bioGendervalue;
    ProgressDialog progressDialog , progressDialogUplaod;

    String bio, fcmtoken, ph;
    String imeiNumber;
    ImageView imageView;
    ImageButton selectImage;
    Bitmap image2;


    String passcode;

    private static final int RESULT_SELECT_IMAGE = 1;

    public String uss,pas,result, phonevalue=null , timestamp ,picname=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //setting up status bar
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        session = new SharedPrefHandler(this);
        imeiExtractor = new ImeiExtractor(this);
        c = this;

        //setting up widgets
        reguser = findViewById(R.id.activity_register_user);
        regpass1 = findViewById(R.id.activity_register_pass1);
        regpass2 = findViewById(R.id.activity_register_pass2);
        register = findViewById(R.id.activity_register_regbtn);
        loginLink = findViewById(R.id.activity_register_link_login);
        imeiShow = findViewById(R.id.activity_reg_imei);

        bioAge = findViewById(R.id.activity_register_bioage);
        bioOccupation = findViewById(R.id.activity_register_biooccupation);
        bioGender = findViewById(R.id.activity_register_biogender);

        phone = findViewById(R.id.activity_register_phonenumber);

        imageView = findViewById(R.id.activity_register_pp_imageView);
        selectImage = findViewById(R.id.activity_register_ppselectImage);
        int selectedId = bioGender.getCheckedRadioButtonId();
        bioGendervalue = findViewById(selectedId);

        //ask for permissions
        //getImei_askForPermission(Manifest.permission.READ_PHONE_STATE, 0x1);

        //get imei number and save in shared preference
        imeiNumber = imeiExtractor.getPhoneIEMINumber();
        imeiShow.setText(imeiNumber);
        session.saveIMEI(imeiNumber);


        // Toast.makeText(c, getPhoneIEMINumber(), Toast.LENGTH_LONG).show();
        register.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                signup();
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), Login.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //call the function to select image from album
                selectImage();
            }
        });

        handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                result = msg.getData().get("result").toString();



                // Starting nextActivity
                if (result.contains("valid")) {

                    session.createLoginSession(uss, pas);
                    if (phone.getText().toString().contentEquals(""))
                        ph = "NA";
                    else
                        ph = phone.getText().toString();
                    session.saveBio(bioOccupation.getText().toString(), bioGendervalue.getText().toString(), bioAge.getText().toString(), ph);
                    showAlertBoxForPasscode();

                }
                else{
                    Toast.makeText(c, result, Toast.LENGTH_LONG).show();
                }

            }
        };

    }


    public void signup() {
        Log.e(TAG, "Signup");

        if (!validate()) {
            return;
        }


        progressDialog = new ProgressDialog(Register.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        //Toast.makeText(this, iemiNumber, Toast.LENGTH_SHORT).show();

        uss = reguser.getText().toString();
        pas = regpass1.getText().toString();


        bio = bioAge.getText().toString() + '|' + bioOccupation.getText().toString() + '|' + bioGendervalue.getText().toString();
        String reEnterPassword = regpass2.getText().toString();
        fcmtoken = session.getFcmToken();
        phonevalue = phone.getText().toString();

        //save all info to show on profile page
        session.saveBio(bioOccupation.getText().toString(),bioGendervalue.getText().toString(),bioAge.getText().toString(),phonevalue);

        passcode = generatePassCode(uss, pas);
        Log.d(TAG, "Data "+uss+pas+bio+imeiNumber);
        new Thread(new Runnable() {
            public void run() {
                // a potentially  time consuming task
                network = new Network(APIEndPoint.reg, uss, pas, passcode, "00.00", "00.00", fcmtoken, imeiNumber, bio, phonevalue, picname);
                result=network.DoWork();
                Log.e("result",result);
                progressDialog.dismiss();
                if(result!=null) {
                    //pass this result to UI thread by writing a message to the UI's handler
                    Message m=Message.obtain();
                    Bundle bundle=new Bundle();
                    bundle.putString("result",result);
                    m.setData(bundle);
                    handler.sendMessage(m);
                }

            }
        }).start();

    }

    public boolean validate() {
        boolean valid = true;

        String email = reguser.getText().toString();
        String password = regpass1.getText().toString();
        String passwordre= regpass2.getText().toString();
        String occupation = bioOccupation.getText().toString();
        String gender = bioGendervalue.getText().toString();
        String age = bioAge.getText().toString();

        String p = phone.getText().toString();
//        if(p.contentEquals(""))
//            Log.e("p=","yes 1");
//        if(p.contentEquals(" "))
//            Log.e("p=","yes 2");


        Log.e("Register ph no = ", "junk" + p);

        if(imeiNumber ==null){
            imeiNumber = imeiExtractor.getPhoneIEMINumber();
        }

        if (email.isEmpty() || email.length() < 4 || email.length() > 20) {
            reguser.setError("enter a valid username between 4 to 20 characters");
            valid = false;
        } else {
            reguser.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            regpass1.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            regpass1.setError(null);
        }

        if (passwordre.isEmpty() || passwordre.length() < 4 || passwordre.length() > 10 || !(passwordre.equals(password))) {
            regpass2.setError("Password Do not match");
            valid = false;
        } else {
            regpass2.setError(null);
        }

        if (age.isEmpty() || age.length() > 3) {
            bioAge.setError("enter a valid Age");
            valid = false;
        } else {
            bioAge.setError(null);
        }

        if (occupation.isEmpty() || occupation.length() < 2) {
            bioOccupation.setError("enter a valid Profession");
            valid = false;
        } else {
            bioOccupation.setError(null);
        }



        if (p.length()!=10 && p.length()!=0) {
            phone.setError("Invalid phone number");
            valid = false;
        } else {
            phone.setError(null);
        }

        return valid;
    }

    private String generatePassCode(String us, String ps) {

        int l = 20;
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        String passcode = new String();
        for (int i = 0; i < l; i++) {
            sb.append(rand.nextInt(9) + 1);
        }
        Log.e("Register", "passcoe is " + sb.toString());
        return sb.toString();
    }

    public void showAlertBoxForPasscode() {
        AlertDialog alertDialog = new AlertDialog.Builder(
                Register.this).create();

        // Setting Dialog Title
        alertDialog.setTitle("Passcode generated " + passcode);
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        // Setting Dialog Message
        alertDialog.setMessage("Please copy the generated passcode. This can be used when you forget your password or want to migrate your app to new device");

        // Setting Icon to Dialog
        alertDialog.setIcon(R.drawable.ic_launcher);

        // Setting OK Button
        alertDialog.setButton("Copy", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Write your code here to execute after dialog closed
                setClipboard(getApplicationContext(), passcode);
                Toast.makeText(getApplicationContext(), "Passcode copied", Toast.LENGTH_LONG).show();
                Intent i = new Intent(c, Home.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("firstTime", true);
                i.putExtras(bundle);
                startActivity(i);
                finish();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    private void setClipboard(Context context, String text) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    public void selectImage(){
        //open album to select image
        Intent gallaryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(gallaryIntent, RESULT_SELECT_IMAGE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_SELECT_IMAGE && resultCode == RESULT_OK && data != null){
            //set the selected image to image variable
            Uri image = data.getData();
            imageView.setImageURI(image);

            //get the current timeStamp and strore that in the time Variable
            Long tsLong = System.currentTimeMillis() / 1000;
            timestamp = tsLong.toString();

            progressDialogUplaod = new ProgressDialog(Register.this);
            progressDialogUplaod.setIndeterminate(true);
            progressDialogUplaod.setMessage("Uploading Image");
            progressDialogUplaod.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialogUplaod.show();

            //get image in bitmap format
            image2 = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            //execute the async task and upload the image to server
            picname = "IMG_"+timestamp;
            new Upload(image2,picname).execute();
        }
    }

    private class Upload extends AsyncTask<Void,Void,String> {
        private Bitmap image;
        private String name;

        public Upload(Bitmap image,String name){
            this.image = image;
            this.name = name;
        }

        @Override
        protected String doInBackground(Void... params) {

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //compress the image to jpg format
            image.compress(Bitmap.CompressFormat.JPEG,30,byteArrayOutputStream);
            /*
            * encode image to base64 so that it can be picked by saveImage.php file
            * */
            String encodeImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);

            //generate hashMap to store encodedImage and the name
            HashMap<String,String> detail = new HashMap<>();
            detail.put("name", name);
            detail.put("image", encodeImage);

            try{
                //convert this HashMap to encodedUrl to send to php file
                String dataToSend = hashMapToUrl(detail);
                //make a Http request and send data to saveImage.php file
                String response = PPUpload.post(APIEndPoint.url_uplaod, dataToSend);

                //return the response
                return response;

            }catch (Exception e){
                e.printStackTrace();
                Log.e(TAG,"ERROR  "+e);
                return null;
            }
        }

        public String saveProfilePicture(Bitmap bitmap) {
            ContextWrapper cw = new ContextWrapper(getApplicationContext());
            // path to /data/data/yourapp/app_data/imageDir
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            // Create imageDir
            File mypath = new File(directory, "profile.jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mypath);
                // Use the compress method on the BitMap object to write image to the OutputStream
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            session.savePPpath(directory.getAbsolutePath());
            return directory.getAbsolutePath();

        }



        protected void onPostExecute(String s){
            //show image uploaded
            progressDialogUplaod.dismiss();
            if (s != null) {
                Toast.makeText(getApplicationContext(), "Image Uploaded " + s, Toast.LENGTH_SHORT).show();
                saveProfilePicture(image2);
            }
            else
                Toast.makeText(getApplicationContext(),"Image Uploaded Failed",Toast.LENGTH_SHORT).show();
        }
    }

    private String hashMapToUrl(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }
}
