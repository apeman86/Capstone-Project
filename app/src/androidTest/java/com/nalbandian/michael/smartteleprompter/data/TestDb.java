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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.nalbandian.michael.smartteleprompter.data.generated.SpeechDatabase;
import com.nalbandian.michael.smartteleprompter.data.generated.values.SpeechesValuesBuilder;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase("speechDatabase.db");
    }

    /*
        This function gets called before each test is executed to delete the database.  This makes
        sure that we always have a clean test.
     */
    public void setUp() {
        deleteTheDatabase();
    }

    /*
        Test the creation of the DB
     */
    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add("speeches");

        mContext.deleteDatabase("speechDatabase.db");
        SQLiteDatabase db = SpeechDatabase.getInstance(this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: The database was created without the speeches entry tables",
                tableNameHashSet.isEmpty());


        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(speeches)",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> speechesColumnHashSet = new HashSet<String>();
        speechesColumnHashSet.add(SpeechColumns._ID);
        speechesColumnHashSet.add(SpeechColumns.TITLE);
        speechesColumnHashSet.add(SpeechColumns.SPEECH);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            speechesColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                speechesColumnHashSet.isEmpty());
        // Finally, close the cursor and database
        assertFalse("Error: More than one record returned from location query", c.moveToNext());
        c.close();
        db.close();
        assertFalse("Error: db did not close correctly", db.isOpen());
    }

    /**
     * Test that data can be inserted in to and queried from the Speeches table.
     **/
    public void testSpeechesTable() {
        //Get writable DB
        SQLiteDatabase db = SpeechDatabase.getInstance(this.mContext).getWritableDatabase();

        SpeechesValuesBuilder testValues = new SpeechesValuesBuilder();
        testValues.title("My Unit Test");
        testValues.speech("Four score and seven years ago our founding fathers...");
        long id = db.insert("speeches", null, testValues.values());
        assertTrue(id != -1);
        // Query the database and receive a Cursor back
        Cursor cursor = db.query("speeches", null, null, null, null, null, null);
        // Move the cursor to a valid database row
        assertTrue("Error: No Records returned from speeches query", cursor.moveToFirst());
        // Validate data in resulting Cursor with the original ContentValues
        // (you can use the validateCurrentRecord function in TestUtilities to validate the
        // query if you like)
        TestUtilities.validateCurrentRecord("Error: Location Query Validation Failed",
                cursor, testValues.values());
        // Finally, close the cursor and database
        assertFalse("Error: More than one record returned from speeches query", cursor.moveToNext());
        cursor.close();
        db.close();
        assertFalse("Error: db did not close correctly", db.isOpen());
    }

}
