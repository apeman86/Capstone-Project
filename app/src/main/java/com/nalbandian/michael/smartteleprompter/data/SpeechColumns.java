package com.nalbandian.michael.smartteleprompter.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

import static net.simonvt.schematic.annotation.DataType.Type.INTEGER;
import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Created by nalbandianm on 2/2/2017.
 */

public interface SpeechColumns {
    @DataType(INTEGER) @PrimaryKey @AutoIncrement
    String _ID = "_id";
    @DataType(TEXT) @NotNull
    String TITLE = "title";
    @DataType(TEXT) @NotNull
    String SPEECH = "speech";
}
