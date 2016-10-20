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
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {

    private IDESFireEV1 desFireEV1;
    private IKeyData objKEY_2KTDES = null;
    private boolean bWriteAllowed = false;
    //CheckBox mCheckToWrite;
    //private TextView tv = null;
    private static final String ALIAS_KEY_2KTDES = "key_2ktdes";
    private static final String EXTRA_KEYS_STORED_FLAG = "keys_stored_flag";
    ImageView iv;
    String  encodedImageString;
    byte[] byteArray;


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
        Log.e("Main Activity", "created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeLibrary();
        //tv = (TextView) findViewById(R.id.tvLog);
        //mCheckToWrite = (CheckBox) findViewById(R.id.checkBox);
        initializeKeys();
        iv=(ImageView)findViewById(R.id.imageView);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.tres);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.WEBP, 90, stream);
        //byte[]
        byteArray = stream.toByteArray();
        Log.e("taille origi", "taille" + byteArray.length);
        encodedImageString = Base64.encodeToString(byteArray, Base64.DEFAULT);







    }

    private void initializeLibrary()
    {
        m_libInstance = NxpNfcLib.getInstance();
        m_libInstance.registerActivity(this, m_strPackageKey);
    }
    @Override
    protected void onResume()

    {
        Log.e("Main Activity", "resumed");
        try {
            m_libInstance.startForeGroundDispatch();
            Log.e("Main Activity", "resumed2");

        }catch (IllegalStateException e){
            //
            //this.finish();
            Log.e("main activity:", "paused1");
            //Intent actu=new Intent(getApplicationContext(),MainActivity.class);
            //startActivity(actu);
            this.finish();

        }



        super.onResume();
    }

    @Override
    protected void onPostResume() {
        try {
            m_libInstance.startForeGroundDispatch();

        }catch (IllegalStateException e){
            //setContentView(R.layout.activity_main);
        }

        super.onPostResume();
    }

///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onPause()
    {
        Log.e("Main activity:", "paused");
        //m_libInstance.stopForeGroundDispatch();
        //setContentView(R.layout.activity_main);

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
                    //Intent passage = new Intent(MainActivity.this,Affichage.class);
                   // startActivity(passage);


                } catch (Throwable t) {
                    t.printStackTrace();
                    showMessage("Unknown Error Tap Again!", 't');
                }
                break;

        }
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
        showMessage("Card Detected : " + desFireEV1.getType().getTagName(), 'n');

        try {
            desFireEV1.getReader().setTimeout(timeOut);

            //showMessage("Version of the Card : " + Utilities.dumpBytes(desFireEV1.getVersion()), 'd');
            //showMessage("Existing Applications Ids : " + Arrays.toString(desFireEV1.getApplicationIDs()), 'd');


            desFireEV1.selectApplication(0);
            Log.e("connected", "desfire");




            /* Do the following only if write checkbox is selected */

            Log.e("tracing", "debug");

            desFireEV1.authenticate(0, IDESFireEV1.AuthType.Native, KeyType.THREEDES, objKEY_2KTDES);

            //desFireEV1.format();
            // EV1ApplicationKeySettings.Builder appsetbuilder = new EV1ApplicationKeySettings.Builder();

            //EV1ApplicationKeySettings appsettings = appsetbuilder.setAppKeySettingsChangeable(true)
            //     .setAppMasterKeyChangeable(true)
            //   .setAuthenticationRequiredForApplicationManagement(false)
            //   .setAuthenticationRequiredForDirectoryConfigurationData(false)
            //   .setKeyTypeOfApplicationKeys(KeyType.TWO_KEY_THREEDES).build();

            //desFireEV1.createApplication(appId, appsettings);
            Log.e("mifare desfire ev1", "application created");


            desFireEV1.selectApplication(appId);
            //desFireEV1.createFile(fileNo, new DESFireFile.StdDataFileSettings(
            //   IDESFireEV1.CommunicationType.Plain, (byte) 0, (byte) 0, (byte) 0, (byte) 0, fileSize));

            desFireEV1.authenticate(0, IDESFireEV1.AuthType.Native, KeyType.TWO_KEY_THREEDES, objKEY_2KTDES);

            String string="hey there, i work and i work and do really alot of working work, you know what ? it works yess it works! :D :D can you do work it up now ? working man ? ";
            byte[] b = string.getBytes();
            byte[] read;
            //read=desFireEV1.readData(0, 0, 0);
            //Log.e("readdata:", "read" + read);


            try {
                //desFireEV1.writeData(0, 0, b);//data
                Log.e("tche","hjhjghjghj");
                read=desFireEV1.readData(0, 0, 0);
                Log.e("readdata:", "read" + read);
                String s = new String(read);
                Log.e("Text Decrypted : ", "string" + s);
                //tv.setText(s);
                Intent i = new Intent(getApplicationContext(), Affichage.class);

                i.putExtra("key",s);
                startActivity(i);




            }
            catch (InvalidResponseLengthException e){
                showMessage("too long text", 't');
                e.printStackTrace();
            }
            // showMessage("Data Read from the card : " + Utilities.dumpBytes(desFireEV1.readData(0, 0, 5)), 'd');
            // showMessage("Free Memory of the Card : " + desFireEV1.getFreeMemory(), 'd');


            desFireEV1.getReader().close();

            // Set the custom path where logs will get stored, here we are setting the log folder DESFireLogs under
            // external storage.
            String spath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DESFireLogs";
            NxpLogUtils.setLogFilePath(spath);
            // if you don't call save as below , logs will not be saved.
            NxpLogUtils.save();

        } catch (Exception e) {
            showMessage("get your card close to thr phone please", 't');
            e.printStackTrace();
        }


    }

/*
    public void desfireEV1CardLogic(){

        byte[] appId = new byte[]{0x05, 0x00, 0x00};
        int fileSize = 4700;
        byte[] data = new byte[]{0x11, 0x11, 0x11, 0x11,
                0x11};
        int timeOut = 2000;
        int fileNo = 0;

        tv.setText(" ");
        //showImageSnap(R.drawable.desfire_ev1);
        showMessage("Card Detected : " + desFireEV1.getType().getTagName(), 'n');

        try {
            desFireEV1.getReader().setTimeout(timeOut);

            //showMessage("Version of the Card : " + Utilities.dumpBytes(desFireEV1.getVersion()), 'd');
            //showMessage("Existing Applications Ids : " + Arrays.toString(desFireEV1.getApplicationIDs()), 'd');


            desFireEV1.selectApplication(0);
            Log.e("connected", "desfire");




              Do the following only if write checkbox is selected
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
                byte[] b = string.getBytes();
                byte[] read;
                //read=desFireEV1.readData(0, 0, 0);
                //Log.e("readdata:", "read" + read);


                try {
                    desFireEV1.writeData(0, 0, b);//data
                    Log.e("tche","hjhjghjghj");
                    /*read=desFireEV1.readData(0, 0, 0);
                    Log.e("readdata:", "read" + read);
                    String s = new String(read);
                   Log.e("Text Decrypted : " ,"string" +s);
                    Intent i = new Intent(getApplicationContext(), Affichage.class);

                    i.putExtra("key","value");
                    startActivity(i);


                }
                catch (InvalidResponseLengthException e){
                    showMessage("too long text", 't');
                    e.printStackTrace();
                }
                // showMessage("Data Read from the card : " + Utilities.dumpBytes(desFireEV1.readData(0, 0, 5)), 'd');
                // showMessage("Free Memory of the Card : " + desFireEV1.getFreeMemory(), 'd');
            }

            desFireEV1.getReader().close();

            // Set the custom path where logs will get stored, here we are setting the log folder DESFireLogs under
            // external storage.
            String spath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DESFireLogs";
            NxpLogUtils.setLogFilePath(spath);
            // if you don't call save as below , logs will not be saved.
            NxpLogUtils.save();

        } catch (Exception e) {
            showMessage("IOException occurred... check LogCat", 't');
            e.printStackTrace();
        }


    }
        */




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





    protected void showMessage(final String str, final char where) {

        switch (where) {

            case 't':
                Toast.makeText(MainActivity.this, "\n" + str, Toast.LENGTH_SHORT)
                        .show();
                break;
            case 'l':
                NxpLogUtils.i(TAG, "\n" + str);
                break;
            case 'd':
               // tv.setText(tv.getText() + "\n-----------------------------------\n" + str);
                break;
            case 'a':
                Toast.makeText(MainActivity.this, "\n" + str, Toast.LENGTH_SHORT)
                        .show();
                NxpLogUtils.i(TAG, "\n" + str);
                //tv.setText(tv.getText() + "\n-----------------------------------\n" + str);
                break;
            case 'n':
                NxpLogUtils.i(TAG, "Dump Data: " + str);
               // tv.setText(tv.getText() + "\n-----------------------------------\n" + str);
                break;
            default:
                break;
        }
        return;
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
