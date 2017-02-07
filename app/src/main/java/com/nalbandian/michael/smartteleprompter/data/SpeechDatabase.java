package com.nalbandian.michael.smartteleprompter.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by nalbandianm on 2/2/2017.
 */

@Database(version = SpeechDatabase.VERSION)
public final class SpeechDatabase {
    public static final int VERSION = 1;

    @Table(SpeechColumns.class) public static final String SPEECHES = "speeches";
}
