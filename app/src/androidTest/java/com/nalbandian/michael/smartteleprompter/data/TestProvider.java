/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.nalbandian.michael.smartteleprompter.data;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import com.nalbandian.michael.smartteleprompter.data.generated.SpeechDatabase;
import com.nalbandian.michael.smartteleprompter.data.generated.values.SpeechesValuesBuilder;

/*
    Note: This is not a complete set of tests of the Sunshine ContentProvider, but it does test
    that at least the basic functionality has been implemented correctly.

    Students: Uncomment the tests in this class as you implement the functionality in your
    ContentProvider to make sure that you've implemented things reasonably correctly.
 */
public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    /*
       This helper function deletes all records from both database tables using the ContentProvider.
       It also queries the ContentProvider to make sure that the database has been successfully
       deleted, so it cannot be used until the Query and Delete functions have been written
       in the ContentProvider.

       Students: Replace the calls to deleteAllRecordsFromDB with this one after you have written
       the delete functionality in the ContentProvider.
     */
    public void deleteAllRecordsFromProvider() {
        mContext.getContentResolver().delete(
                SpeechProvider.Speeches.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                SpeechProvider.Speeches.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Weather table during delete", 0, cursor.getCount());
        cursor.close();
    }

    /*
       This helper function deletes all records from both database tables using the database
       functions only.  This is designed to be used to reset the state of the database until the
       delete functionality is available in the ContentProvider.
     */
    public void deleteAllRecordsFromDB() {
        SQLiteDatabase db = SpeechDatabase.getInstance(mContext).getWritableDatabase();

        db.delete(com.nalbandian.michael.smartteleprompter.data.SpeechProvider.Path.SPEECHES, null, null);
        db.close();
    }

    /*
        Student: Refactor this function to use the deleteAllRecordsFromProvider functionality once
        you have implemented delete functionality there.
     */
    public void deleteAllRecords() {
        deleteAllRecordsFromDB();
    }

    // Since we want each test to start with a clean slate, run deleteAllRecords
    // in setUp (called by the test runner before each test).
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAllRecords();
    }

    /*
        This test checks to make sure that the content provider is registered correctly.
        Students: Uncomment this test to make sure you've correctly registered the WeatherProvider.
     */
    public void testProviderRegistry() {
        PackageManager pm = mContext.getPackageManager();

        // We define the component name based on the package name from the context and the
        // WeatherProvider class.
        ComponentName componentName = new ComponentName(mContext.getPackageName(),
                com.nalbandian.michael.smartteleprompter.data.generated.SpeechProvider.class.getName());
        try {
            // Fetch the provider info using the component name from the PackageManager
            // This throws an exception if the provider isn't registered.
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);

            // Make sure that the registered authority matches the authority from the Contract.
            assertEquals("Error: SpeechProvider registered with authority: " + providerInfo.authority +
                    " instead of authority: " + com.nalbandian.michael.smartteleprompter.data.generated.SpeechProvider.AUTHORITY,
                    providerInfo.authority, com.nalbandian.michael.smartteleprompter.data.generated.SpeechProvider.AUTHORITY);
        } catch (PackageManager.NameNotFoundException e) {
            // I guess the provider isn't registered correctly.
            assertTrue("Error: SpeechProvider not registered at " + mContext.getPackageName(),
                    false);
        }
    }

    /*
        This test doesn't touch the database.  It verifies that the ContentProvider returns
        the correct type for each type of URI that it can handle.
    */
    public void testGetType() {
        // content://com.example.android.sunshine.app/weather/
        String type = mContext.getContentResolver().getType(SpeechProvider.Speeches.CONTENT_URI);
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather
        assertEquals("Error: the SpeechProvider CONTENT_URI should return Content_Types.DIR",
                SpeechProvider.Content_Types.DIR, type);

        long testId = 10L;
        // content://com.example.android.sunshine.app/weather/94074
        type = mContext.getContentResolver().getType(SpeechProvider.Speeches.withId(testId));
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather
        assertEquals("Error: the SpeechProvider.Speeches.withId should return SpeechProvider.Content_Types.ITEM",
                SpeechProvider.Content_Types.ITEM, type);
    }

    /*
        This test uses the database directly to insert and then uses the ContentProvider to
        read out the data.
     */
    public void testBasicSpeechQuery() {
        // insert our test records into the database
        SQLiteDatabase db = SpeechDatabase.getInstance(mContext).getWritableDatabase();

        SpeechesValuesBuilder testValues = new SpeechesValuesBuilder();
        testValues.title("My Unit Test");
        testValues.speech("Four score and seven years ago our founding fathers...");
        long id = db.insert("speeches", null, testValues.values());
        assertTrue("Unable to Insert Speech entry into the Database", id != -1);

        db.close();

        // Test the basic content provider query
        Cursor speechCursor = mContext.getContentResolver().query(
                SpeechProvider.Speeches.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Make sure we get the correct cursor out of the database
        TestUtilities.validateCursor("testBasicSpeechQuery", speechCursor, testValues.values());


        // Test the basic content provider query
        Cursor speechWithIdCursor = mContext.getContentResolver().query(
                SpeechProvider.Speeches.withId(id),
                null,
                null,
                null,
                null
        );

        // Make sure we get the correct cursor out of the database
        TestUtilities.validateCursor("testBasicSpeechQuery", speechWithIdCursor, testValues.values());
    }

    /*
        This test uses the provider to insert and then update the data.
     */
    public void testUpdateLocation() {
        // Create a new map of values, where column names are the keys
        SpeechesValuesBuilder testValues = new SpeechesValuesBuilder();
        testValues.title("My Unit Test");
        testValues.speech("Four score and seven years ago our founding fathers...");
        
        Uri speechUri = mContext.getContentResolver().
                insert(SpeechProvider.Speeches.CONTENT_URI, testValues.values());
        long speechRowId = ContentUris.parseId(speechUri);

        // Verify we got a row back.
        assertTrue(speechRowId != -1);
        Log.d(LOG_TAG, "New row id: " + speechRowId);

        SpeechesValuesBuilder updatedValues = new SpeechesValuesBuilder();
        updatedValues.Id(speechRowId);
        updatedValues.title("Gettysburg Address");
        updatedValues.speech(testValues.values().getAsString(SpeechColumns.SPEECH));

        // Create a cursor with observer to make sure that the content provider is notifying
        // the observers as expected
        Cursor locationCursor = mContext.getContentResolver().query(SpeechProvider.Speeches.CONTENT_URI, null, null, null, null);

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        locationCursor.registerContentObserver(tco);

        int count = mContext.getContentResolver().update(
                SpeechProvider.Speeches.CONTENT_URI, updatedValues.values(), SpeechColumns._ID + "= ?",
                new String[] { Long.toString(speechRowId)});
        assertEquals(count, 1);

        // Test to make sure our observer is called.  If not, we throw an assertion.
        tco.waitForNotificationOrFail();

        locationCursor.unregisterContentObserver(tco);
        locationCursor.close();

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                SpeechProvider.Speeches.CONTENT_URI,
                null,   // projection
                SpeechColumns._ID + " = " + speechRowId,
                null,   // Values for the "where" clause
                null    // sort order
        );

        TestUtilities.validateCursor("testUpdateLocation.  Error validating location entry update.",
                cursor, updatedValues.values());

        cursor.close();
    }

    // Make sure we can still delete after adding/updating stuff
    //
    // Student: Uncomment this test after you have completed writing the delete functionality
    // in your provider.  It relies on insertions with testInsertReadProvider, so insert and
    // query functionality must also be complete before this test can be used.
    public void testDeleteRecords() {

        // Register a content observer for our location delete.
        TestUtilities.TestContentObserver locationObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(SpeechProvider.Speeches.CONTENT_URI, true, locationObserver);

        deleteAllRecordsFromProvider();

        locationObserver.waitForNotificationOrFail();

        mContext.getContentResolver().unregisterContentObserver(locationObserver);
    }

}
