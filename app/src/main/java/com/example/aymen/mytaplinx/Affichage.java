package com.example.aymen.mytaplinx;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.nxp.nfclib.CardType;
import com.nxp.nfclib.KeyType;
import com.nxp.nfclib.NxpNfcLib;
import com.nxp.nfclib.classic.ClassicFactory;
import com.nxp.nfclib.classic.IMFClassic;
import com.nxp.nfclib.classic.IMFClassicEV1;
import com.nxp.nfclib.desfire.DESFireFile;
import com.nxp.nfclib.desfire.EV1ApplicationKeySettings;
import com.nxp.nfclib.desfire.EV1PICCKeySettings;
import com.nxp.nfclib.defaultimpl.KeyData;
import com.nxp.nfclib.desfire.DESFireFactory;
import com.nxp.nfclib.desfire.IDESFireEV1;
import com.nxp.nfclib.exceptions.InvalidResponseLengthException;
import com.nxp.nfclib.exceptions.NxpNfcLibException;
import com.nxp.nfclib.interfaces.IKeyData;
import com.nxp.nfclib.ndef.INdefMessage;
import com.nxp.nfclib.utils.NxpLogUtils;
import com.nxp.nfclib.utils.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import javax.crypto.spec.SecretKeySpec;

import java.security.Key;
import java.util.Arrays;

import static org.spongycastle.asn1.x500.style.RFC4519Style.o;

public class Affichage extends AppCompatActivity {

    private IDESFireEV1 desFireEV1;
    private IKeyData objKEY_2KTDES = null;
    private boolean bWriteAllowed = false;
    CheckBox mCheckToWrite;
    //private TextView tv1 = null;
    private static final String ALIAS_KEY_2KTDES = "key_2ktdes";
    private static final String EXTRA_KEYS_STORED_FLAG = "keys_stored_flag";
    private EditText Nom,Prenom,dateNaissance,Email,Tel,Adress,ID,CIN,solde;
    ImageView iv;
    ImageView imageView;
    String QRcode;
    public final static int WIDTH=500;
    String  encodedImageString;
    byte[] byteArray;
    String id;
    String nom;
    String prenom ;
    String tel;
    String email ;
    String datedenaiisance ;
    String adress ;
    String sol;



    public static final String TAG = MainActivity.class.getSimpleName();

    // The package key you will get from the registration server
    private static String m_strPackageKey = "e4175720fbc73f72f40944e98961d14e";

    // The TapLinX library instance
    private NxpNfcLib   m_libInstance   = null;


    private IDESFireEV1 m_objDESFireEV1 = null;
    private IMFClassic mifareClassic;
    private CardType m_cardType      = CardType.UnknownCard;
    public static final byte[] DEFAULT_KEY_2KTDES =
            {                                                 // Default 2kTDES key
                    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                    (byte)0x00, (byte)0x00, (byte)0x00
            };
    public static final byte[] DEFAULT_KEY_2KTDES_ALT =
            {                                                 // Default 2kTDES key
                    (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                    (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                    (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                    (byte)0xff, (byte)0xff, (byte)0xff
            };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("Affiche Activity", "created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_affichage);
        initializeLibrary();
       // tv1 = (TextView) findViewById(R.id.tvLog1);
        mCheckToWrite = (CheckBox) findViewById(R.id.checkBox1);
        initializeKeys();
        iv=(ImageView)findViewById(R.id.imageView);
        imageView=(ImageView) findViewById(R.id.imageView2);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.tres);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.WEBP, 90, stream);
        //byte[]
        byteArray = stream.toByteArray();
        Log.e("taille origi", "taille" + byteArray.length);
        encodedImageString = Base64.encodeToString(byteArray, Base64.DEFAULT);
        Nom = (EditText) findViewById(R.id.editNom);
        Prenom= (EditText) findViewById(R.id.editPrenom);
        dateNaissance= (EditText) findViewById(R.id.EditOganisation);
        Adress= (EditText) findViewById(R.id.EditAdress);
        ID= (EditText) findViewById(R.id.EditSkype);
        Email= (EditText) findViewById(R.id.EditEmail);
        Tel= (EditText) findViewById(R.id.EditTel);
        solde= (EditText) findViewById(R.id.EditFax);
        Bundle extras = getIntent().getExtras();
        Nom.addTextChangedListener(mTextWatcher);
        Prenom.addTextChangedListener(mTextWatcher);
        Adress.addTextChangedListener(mTextWatcher);
        Tel.addTextChangedListener(mTextWatcher);
        Email.addTextChangedListener(mTextWatcher);
        ID.addTextChangedListener(mTextWatcher);
        solde.addTextChangedListener(mTextWatcher);
        dateNaissance.addTextChangedListener(mTextWatcher);
        checkFieldsForEmptyValues();

        id=ID.getText().toString() ;





        if (extras != null) {
            String value = extras.getString("key");
            QRcode = value;

            //tv1.setText(value);
            //The key argument here must match that used in the other activity
            try {
            String[] parts = value.split("--");
            String part1 = parts[0];
            String part2 = parts[1];
            String part3 = parts[2];
            String part4 = parts[3];
            String part5 = parts[4];
            String part6 = parts[5];
            String part7 = parts[6];
            String part8 = parts[7];
            String part9=parts[8];
                Log.e("done", "goood" + value);
            byte[] decodedString = Base64.decode(part1, Base64.DEFAULT);
                Log.e("byte array", "l" + decodedString[8]);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            iv.setImageBitmap(decodedByte);
                Log.e("debug", "name" + part1);
            Log.e("debug","name"+part2);
            Log.e("debug","name888"+part4);
            //mTextView.setText("name: " + part2+" prenom: "+part3+ " Date de naissance: "+part4+" Email: "+part5+" Tel: "+part6+" Adresse "+part7+ " ID: "+part8+"Solde: "+part9);
            Nom.setText(part2);
            Prenom.setText(part3);
            dateNaissance.setText(part4);
            Email.setText(part5);
            Tel.setText(part6);
            Adress.setText(part7);
            ID.setText(part8);
            solde.setText(part9);

            }catch (Exception e) {
                e.printStackTrace();
                Log.e("desfire:","unformatted");
            }

        }




        mCheckToWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!bWriteAllowed) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                            Affichage.this);
                    alertDialog.setCancelable(false);

                    // Setting Dialog Title
                    alertDialog.setTitle("write?");

                    // Setting Dialog Message
                    alertDialog.setMessage("do u wanna write?");

                    // Setting Icon to Dialog
                    //alertDialog.setIcon(R.drawable.nxp_logo);

                    // Setting Positive "Yes" Button
                    alertDialog.setPositiveButton("Allow",
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog,
                                                    final int which) {

                                    bWriteAllowed = true;

                                }

                            });

                    // Setting Negative "NO" Button
                    alertDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog,
                                                    final int which) {
                                    bWriteAllowed = false;
                                    // Write your code here to invoke NO event
                                    mCheckToWrite.setChecked(false);
                                    dialog.cancel();
                                }
                            });

                    // Showing Alert Message
                    alertDialog.show();

                } else {
                    bWriteAllowed = false;
                }
            }
        });



    }
    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // check Fields For Empty Values
            checkFieldsForEmptyValues();
        }
    };

    void checkFieldsForEmptyValues(){
        //Button b = (Button) findViewById(R.id.buttonWrite);

        String s1 = Nom.getText().toString();
        String s2 = Prenom.getText().toString();
        String s3 = Adress.getText().toString();
        String s4 = dateNaissance.getText().toString();
        String s5 = solde.getText().toString();
        String s6 = Email.getText().toString();
        String s7 = ID.getText().toString();
        String s8 = Tel.getText().toString();

        //if(s1.equals("")|| s2.equals("") ||s4.equals("")||s5.equals("")||s6.equals("")||s7.equals("")||s8.equals("")) {
          //  b.setEnabled(false);
        //} else {
            //b.setEnabled(true);
        //}
    }
    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            Log.e("tests","l    1");
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? getResources().getColor(R.color.black):getResources().getColor(R.color.white);
            }
        }
        Log.e("tests","l    2");
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, 500, 0, 0, w, h);
        return bitmap;
    } /// end of this method


    private void initializeLibrary()
    {
        m_libInstance = NxpNfcLib.getInstance();
        m_libInstance.registerActivity(this, m_strPackageKey);
    }
    @Override
    protected void onResume()
    {
        try {
            m_libInstance.startForeGroundDispatch();



        }catch (IllegalStateException e){
            //setContentView(R.layout.activity_main);
        }

        Log.e("Affiche activity:", "resumed");

        super.onResume();
    }

    @Override
    protected void onPostResume() {
        try {
            m_libInstance.startForeGroundDispatch();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = null;

                    try {
                        bitmap = encodeAsBitmap(ID.getText().toString());
                        Log.e("string","bj"+ ID.getText().toString());

                    } catch (WriterException e) {
                        e.printStackTrace();
                    }
                    final Bitmap finalBitmap = bitmap;
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(finalBitmap);


                            }
                        });
                    }


            });

        }catch (IllegalStateException e){
            //setContentView(R.layout.activity_main);
        }


        super.onPostResume();
    }
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onPause()
    {
        //Intent retour=new Intent(getApplicationContext(),MainActivity.class);
        //startActivity(retour);
        try {
            m_libInstance.stopForeGroundDispatch();


        }catch (IllegalStateException e){
            //setContentView(R.layout.activity_main);
            Intent retour=new Intent(getApplicationContext(),MainActivity.class);
            startActivity(retour);

        }
        Log.e("Affiche activity:", "paused");
        super.onPause();
    }


    @Override
    public void onNewIntent( final Intent intent )
    {
        Log.d( TAG, "onNewIntent");
        cardLogic(intent);
        super.onNewIntent(intent);
    }
    private void cardLogic(final Intent intent) {
        CardType type = CardType.UnknownCard;
        try {
            type = m_libInstance.getCardType(intent);
        } catch (NxpNfcLibException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
        switch (type) {

            case MIFAREClassic: {
                if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    if (tag != null) {
                        mifareClassic = ClassicFactory.getInstance().getClassic(MifareClassic.get(tag));
                        m_cardType = CardType.MIFAREClassic;
                        //classicCardLogic();
                    }
                }
                break;
            }
            case DESFireEV1:
                m_cardType = CardType.DESFireEV1;
                desFireEV1 = DESFireFactory.getInstance().getDESFire(m_libInstance.getCustomModules());
                try {

                    desFireEV1.getReader().connect();
                    desFireEV1.getReader().setTimeout(2000);

                    desfireEV1CardLogic();



                } catch (Throwable t) {
                    t.printStackTrace();
                    //showMessage("Unknown Error Tap Again!", 't');
                }
                break;

        }
    }








    private void initializeKeys() {
        KeyInfoProvider infoProvider = KeyInfoProvider.getInstance(getApplicationContext());

        SharedPreferences sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        boolean keysStoredFlag = sharedPrefs.getBoolean(EXTRA_KEYS_STORED_FLAG, false);
        if (!keysStoredFlag) {
            //Set Key stores the key in persistent storage, this method can be called only once if key for a given alias does not change.
            byte[] ulc24Keys = new byte[24];
            System.arraycopy(SampleAppKeys.KEY_2KTDES_ULC, 0, ulc24Keys, 0, SampleAppKeys.KEY_2KTDES_ULC.length);
            System.arraycopy(SampleAppKeys.KEY_2KTDES_ULC, 0, ulc24Keys, SampleAppKeys.KEY_2KTDES_ULC.length, 8);


            infoProvider.setKey(ALIAS_KEY_2KTDES, SampleAppKeys.EnumKeyType.EnumDESKey, SampleAppKeys.KEY_2KTDES);

            sharedPrefs.edit().putBoolean(EXTRA_KEYS_STORED_FLAG, true).commit();
            //If you want to store a new key after key initialization above, kindly reset the flag EXTRA_KEYS_STORED_FLAG to false in shared preferences.
        }
        objKEY_2KTDES = infoProvider.getKey(ALIAS_KEY_2KTDES, SampleAppKeys.EnumKeyType.EnumDESKey);

    }

    public void desfireEV1CardLogic(){

        byte[] appId = new byte[]{0x07, 0x00, 0x00};
        int fileSize = 4700;
        byte[] data = new byte[]{0x11, 0x11, 0x11, 0x11,
                0x11};
        int timeOut = 2000;
        int fileNo = 0;

        //tv.setText(" ");
        //showImageSnap(R.drawable.desfire_ev1);
        //showMessage("Card Detected : " + desFireEV1.getType().getTagName(), 'n');

        try {
            desFireEV1.getReader().setTimeout(timeOut);

            //showMessage("Version of the Card : " + Utilities.dumpBytes(desFireEV1.getVersion()), 'd');
            //showMessage("Existing Applications Ids : " + Arrays.toString(desFireEV1.getApplicationIDs()), 'd');


            desFireEV1.selectApplication(0);
            Log.e("connected", "desfire");


            if (bWriteAllowed) {
                Log.e("tracing", "debug");
                desFireEV1.authenticate(0, IDESFireEV1.AuthType.Native, KeyType.THREEDES, objKEY_2KTDES);

                desFireEV1.format();
                EV1ApplicationKeySettings.Builder appsetbuilder = new EV1ApplicationKeySettings.Builder();

                EV1ApplicationKeySettings appsettings = appsetbuilder.setAppKeySettingsChangeable(true)
                        .setAppMasterKeyChangeable(true)
                        .setAuthenticationRequiredForApplicationManagement(false)
                        .setAuthenticationRequiredForDirectoryConfigurationData(false)
                        .setKeyTypeOfApplicationKeys(KeyType.TWO_KEY_THREEDES).build();

                desFireEV1.createApplication(appId, appsettings);
                Log.e("mifare desfire ev1", "application created");


                desFireEV1.selectApplication(appId);
                desFireEV1.createFile(fileNo, new DESFireFile.StdDataFileSettings(
                        IDESFireEV1.CommunicationType.Plain, (byte) 0, (byte) 0, (byte) 0, (byte) 0, fileSize));

                desFireEV1.authenticate(0, IDESFireEV1.AuthType.Native, KeyType.TWO_KEY_THREEDES, objKEY_2KTDES);

                String string="hey there, i work and i work and do really alot of working work, you know what ? it works yess it works! :D :D can you do work it up now ? working man ? ";
                nom=Nom.getText().toString();
                prenom =Prenom.getText().toString();
                tel=Tel.getText().toString();
                email =Email.getText().toString();
                datedenaiisance =dateNaissance.getText().toString();
                adress =Adress.getText().toString();
                id=ID.getText().toString();
                sol =solde.getText().toString();
                String write=encodedImageString+"--"+nom+"--"+prenom+"--"+datedenaiisance+"--"+email+"--"+tel+"--"+adress+"--"+id+"--"+sol;
                Log.e("desfire :","collected"+write);
                Log.e("collection2","name"+nom);
                byte[] b = write.getBytes();



                try {
                    Toast.makeText(this,"writing data",Toast.LENGTH_LONG).show();
                    desFireEV1.writeData(0, 0, b);//data
                    Toast.makeText(this,"information updated",Toast.LENGTH_LONG).show();
                    Log.e("tche","hjhjghjghj");



                }
                catch (InvalidResponseLengthException e){
                    //showMessage("too long text", 't');
                    e.printStackTrace();
                }
                // showMessage("Data Read from the card : " + Utilities.dumpBytes(desFireEV1.readData(0, 0, 5)), 'd');
                // showMessage("Free Memory of the Card : " + desFireEV1.getFreeMemory(), 'd');
            }
            else{
                //Intent retour=new Intent(getApplicationContext(),MainActivity.class);
                //startActivity(retour);
                //this.finish();
                Toast.makeText(this,"please allow writing",Toast.LENGTH_LONG).show();
            }

            desFireEV1.getReader().close();

            // Set the custom path where logs will get stored, here we are setting the log folder DESFireLogs under
            // external storage.
            String spath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DESFireLogs";
            NxpLogUtils.setLogFilePath(spath);
            // if you don't call save as below , logs will not be saved.
            NxpLogUtils.save();

        } catch (Exception e) {
            //showMessage("IOException occurred... check LogCat", 't');
            e.printStackTrace();
        }


    }





   /* {

        m_cardType = m_libInstance.getCardType(intent);


        Log.e(TAG,"type is:"+m_cardType+m_cardType.getDescription());
        if( CardType.DESFireEV1 == m_cardType) {
            Log.e(TAG, "DESFireEV1 found");

            m_objDESFireEV1 = DESFireFactory.getInstance()
                    .getDESFire( m_libInstance.getCustomModules() );
            try
            {

                m_objDESFireEV1.getReader().connect();
                // Timeout to prevent exceptions in authenticate
                m_objDESFireEV1.getReader().setTimeout(2000);
                // Select root app
                m_objDESFireEV1.selectApplication(0);
                m_objDESFireEV1.format();
                Log.e("desfire", "data formatted");


                Log.e(TAG, "AID 000000 selected");

                // DEFAULT_KEY_2KTDES is a byte array of 24 zero bytes
                Key key = new SecretKeySpec( DEFAULT_KEY_2KTDES, "DESede" );
                final KeyData keyData = new KeyData();
                keyData.setKey(key);
                Log.e(TAG, "psssss");




                // Authenticate to PICC Master Key
                m_objDESFireEV1.authenticate(0, IDESFireEV1.AuthType.Native,
                        KeyType.TWO_KEY_THREEDES, keyData);
                Log.e(TAG, "DESFireEV1 authenticated");














            }
            catch( Throwable t )
            {
                t.printStackTrace();
            }
        }
    } */

}
