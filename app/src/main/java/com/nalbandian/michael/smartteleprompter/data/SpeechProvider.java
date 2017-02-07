package com.nalbandian.michael.smartteleprompter.data;

import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;

/**
 * Created by nalbandianm on 2/2/2017.
 */

@ContentProvider(authority = SpeechProvider.AUTHORITY, database = SpeechDatabase.class)
public final class SpeechProvider {
    public static final String AUTHORITY = "com.nalbandian.michael.smartteleprompter";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://"+AUTHORITY);

    interface Path {
        String SPEECHES = "speeches";
    }
    interface Content_Types{
        String DIR = "vnd.android.cursor.dir/speeches";
        String ITEM = "vnd.android.cursor.item/speeches";
    }

    private static Uri buildUri(String ... paths){
        Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
        for (String path : paths){
            builder.appendPath(path);
        }
        return builder.build();
    }
    @TableEndpoint(table = SpeechDatabase.SPEECHES) public static class Speeches {

        @ContentUri(
                path = Path.SPEECHES,
                type = Content_Types.DIR,
                defaultSort = SpeechColumns.TITLE + " ASC")
        public static final Uri CONTENT_URI = buildUri(Path.SPEECHES);

        @InexactContentUri(
                name = "SPEECH_ID",
                path = Path.SPEECHES + "/#",
                type = Content_Types.ITEM,
                whereColumn = SpeechColumns._ID,
                pathSegment = 1)
        public static Uri withId(long id) {
            return buildUri(Path.SPEECHES, String.valueOf(id));
        }
    }
}
