package com.pcompute.zubus.whatsappnumberchecker;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.alexbykov.nopermission.PermissionHelper;

public class MainActivity extends AppCompatActivity {

    private EditText number;
    private TextView textView, version;
    private Button btnSearch;
    private PermissionHelper helper;
    private String displayName = "whatsappTest";
    private ProgressDialog dialog = null;
    private HashMap<String, String> addedNumbers = new HashMap<>();
    private long contactId;
    private int check = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        number = findViewById(R.id.editText);
        version = findViewById(R.id.version);
        textView = findViewById(R.id.tv_hasWhatsapp);
        btnSearch = findViewById(R.id.btn_search);
        try {
            //Setting app's version in the text field
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = pInfo.versionName;
            version.setText("WhatsApp Number Checker V-" + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //search button when clicked
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Checking for android version for runtime permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //Checking for contacts permission
                    if (checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        dotheDeed();
                    } else {
                        //Requesting runtime permission
                        helper = new PermissionHelper(MainActivity.this);
                        helper.check(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                                .onSuccess(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Permission Granted!! Please press the button again to execute your request!", Toast.LENGTH_LONG).show();
                                    }
                                }).

                                onDenied(new Runnable() {
                                    @Override
                                    public void run() {
                                        showAlertDialog("Permission's are needed to access contacts and check if the number have whatsapp", false);
                                    }
                                }).

                                onNeverAskAgain(new Runnable() {
                                    @Override
                                    public void run() {
                                        showAlertDialog("You need to give contacts permission from the contacts section of the app settings in the phone settings", true);
                                    }
                                }).

                                run();
                    }
                } else {
                    dotheDeed();
                }


            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                helper = new PermissionHelper(MainActivity.this);
                helper.check(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                        .onSuccess(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Permission Granted!!", Toast.LENGTH_SHORT).show();
                            }
                        }).

                        onDenied(new Runnable() {
                            @Override
                            public void run() {
                                showAlertDialog("Permission's are needed to access contacts and check if the number have whatsapp", false);
                            }
                        }).

                        onNeverAskAgain(new Runnable() {
                            @Override
                            public void run() {
                                showAlertDialog("You need to give contacts permission from the contacts section of the app settings in the phone settings", true);
                            }
                        }).

                        run();
            }
        }
    }

    private void dotheDeed() {
        String n = number.getText().toString();
        if (n.isEmpty()) {
            number.setError("Please enter a number");
        } else {
            if (dialog == null) {
                dialog = new ProgressDialog(MainActivity.this);
                dialog.setMessage("Checking number");
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }
            textView.setText("");
            check = 0;
            for (int i = 0; i <= 3; i++) {
                //Adding + searching number
                addNumber(n, i);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        helper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showAlertDialog(String message, final boolean takeToSettings) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (takeToSettings) {
                    helper.startApplicationSettingsActivity();
                } else
                    dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void setTextViewText(final boolean hasWhatsapp, final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (hasWhatsapp)
                    textView.setText(number.getText().toString() + ":  Number have whatsapp");
                else
                    textView.setText(number.getText().toString() + ":  Number doesn't have whatsapp");

                if (i == 3)
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                        dialog = null;
                    }
                check++;
            }
        });

    }


    private void addNumber(final String phoneNumber, final int i) {


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                //Inserting contact
                boolean contactAdded = insertContactother();


                //force syncing Whatsapp Service to sync the contact
                Bundle bundle = new Bundle();
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                Account whatsappAccount = null;
                for (Account account : AccountManager.get(getApplicationContext()).getAccounts()) {
                    if (account.type.equalsIgnoreCase("com.whatsapp")) {
                        whatsappAccount = account;
                    }
                }
                if (whatsappAccount == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "There isnt any whatsapp account in the device", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                final String Authority = "com.android.contacts";
                ContentResolver.requestSync(whatsappAccount, Authority, bundle);
                final Account finalWhatsappAccount = whatsappAccount;
                do {
                    //waiting for the sync servie to complete
                    if (!ContentResolver.isSyncPending(finalWhatsappAccount, Authority) && !ContentResolver.isSyncActive(finalWhatsappAccount, Authority)) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //setting text whether number have whatsapp or not
                                setTextViewText(fetchWhatsAppContacts(), i);
                            }
                        }, 10000);
                        break;
                    }

                } while (true);
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private boolean insertContactother() {
        String DisplayName = displayName + System.currentTimeMillis();
        String MobileNumber = number.getText().toString().trim();
        if (addedNumbers.containsKey(MobileNumber))
            return false;
        //Uri addContactsUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        // Below uri can avoid java.lang.UnsupportedOperationException: URI: content://com.android.contacts/data/phones error.
        Uri addContactsUri = ContactsContract.Data.CONTENT_URI;


        // Add an empty contact and get the generated id.
        contactId = getRawContactId();

        // Add contact name data.
        insertContactDisplayName(addContactsUri, contactId, DisplayName);

        // Add contact phone data.
        insertContactPhoneNumber(addContactsUri, contactId, MobileNumber);
        addedNumbers.put(MobileNumber, DisplayName);
        return true;
    }

    private long getRawContactId() {
        // Inser an empty contact.
        ContentValues contentValues = new ContentValues();
        Uri rawContactUri = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, contentValues);
        // Get the newly created contact raw id.
        long ret = ContentUris.parseId(rawContactUri);
        return ret;
    }

    private void insertContactDisplayName(Uri addContactsUri, long rawContactId, String
            displayName) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);

        // Each contact must has an mime type to avoid java.lang.IllegalArgumentException: mimetype is required error.
        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);

        // Put contact display name value.
        contentValues.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, displayName);

        getContentResolver().insert(addContactsUri, contentValues);

    }

    private void insertContactPhoneNumber(Uri addContactsUri, long rawContactId, String
            phoneNumber) {
        // Create a ContentValues object.
        ContentValues contentValues = new ContentValues();

        // Each contact must has an id to avoid java.lang.IllegalArgumentException: raw_contact_id is required error.
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);

        // Each contact must has an mime type to avoid java.lang.IllegalArgumentException: mimetype is required error.
        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

        // Put phone number value.
        contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);

        int phoneContactType = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;

        // Put phone type value.
        contentValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, phoneContactType);

        // Insert new contact data into phone contact list.
        getContentResolver().insert(addContactsUri, contentValues);

    }


    private boolean fetchWhatsAppContacts() {
        boolean numberFound = false;
        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();

        final String[] projection = {
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                "account_type",
                ContactsContract.Data.DATA3,
        };
        final String selection = ContactsContract.Data.MIMETYPE + " =? and account_type=?";
//        final String selection = ContactsContract.Data.CONTACT_ID + " =? and account_type=?";
//        final String[] selectionArgs = {
//                String.valueOf(contactId),
//                "com.whatsapp"};
        final String[] selectionArgs = {
                "vnd.android.cursor.item/vnd.com.whatsapp.profile",
                "com.whatsapp"
        };
        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);
        while (c.moveToNext()) {
            String id = c.getString(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
            String wnumber = c.getString(c.getColumnIndex(ContactsContract.Data.DATA3));
            String name = "";
            Cursor mCursor = getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                    ContactsContract.Contacts._ID + " =?",
                    new String[]{id},
                    null);
            while (mCursor.moveToNext()) {
                name = mCursor.getString(mCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            }
            mCursor.close();
            wnumber = wnumber.replace("Message", "");
            wnumber = wnumber.replace(" ", "");
            wnumber = wnumber.replace("-", "");
            Log.d("whatsapp", wnumber.trim());

            if (wnumber.trim().equals(number.getText().toString().trim())) {
                numberFound = true;
                Log.d("found", wnumber.trim());
                break;
            }
        }
        Log.v("WhatsApp", "Total WhatsApp Contacts: " + c.getCount());
        c.close();
        return numberFound;
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d("WhatsappChecker", "OnPause()");
        Intent intent = new Intent(getApplicationContext(), ContactDeleteService.class);
        intent.putExtra("map", addedNumbers);
        startService(intent);
        super.onPause();
    }
}
