package com.pcompute.zubus.whatsappnumberchecker;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.Tag;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class ContactDeleteService extends IntentService {
    private final String TAG = ContactDeleteService.class.getName();

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ContactDeleteService(String name) {
        super(name);
    }

    public ContactDeleteService() {
        super("test");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("map")) {
            HashMap<String, String> hashMap = (HashMap<String, String>) intent.getSerializableExtra("map");
            for (Map.Entry<String, String> contact : hashMap.entrySet())
                deleteContact(contact.getKey(), contact.getValue());

            stopSelf();
        }
    }

    public void deleteContact(String key, String value) {
        Log.d(TAG, "Key = " + key + " value = " + value);
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(key));
        Cursor cur = getContentResolver().query(contactUri, null, null, null, null);
        try {
            if (cur != null && cur.moveToFirst()) {
                do {
                    if (cur.getString(cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)).equalsIgnoreCase(value)) {
                        String lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                        getContentResolver().delete(uri, null, null);
                        Log.d(TAG, "Contact Deleted");
                    }

                } while (cur.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cur != null)
                cur.close();
        }
    }

}
