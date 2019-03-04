package com.eveningoutpost.dexdrip.utils;

import android.content.*;
import android.os.*;

import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.*;

import java.io.*;
import java.text.*;
import java.util.*;

public class FileUtils {

	public static boolean makeSureDirectoryExists( final String dir ) {
		final File file = new File( dir );
        return file.exists() || file.mkdirs();
	}

	public static String getExternalDir() {

		final String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/xdrip";
		return dir;
	}

	public static String combine( final String path1, final String path2 ) {
		final File file1 = new File( path1 );
		final File file2 = new File( file1, path2 );
		return file2.getPath();
	}
	
	public static void writeToFileWithCurrentDate(String TAG, String file, byte []data) {
		Context context = xdrip.getAppContext();
        
        String dir = context.getFilesDir().getPath();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateandTime = sdf.format(new Date());

        String fileName = dir + '/' + file+ "_" + currentDateandTime + ".dat";
        writeToFile(TAG, fileName, data);
	}
	
    public static void writeToFile(String TAG, String fileName, byte []data) {

        
        UserError.Log.i(TAG, "Writing to file" + fileName);
        try {
            FileOutputStream f = new FileOutputStream(new File(fileName));
            if(data != null) {
                // if no data exists, file will be written with zero length to let the user know what is happening.
                f.write(data);
            }
            f.close();
        }catch (IOException e) {
        	UserError.Log.e(TAG, "Cought exception when trying to write file", e);
        }
    }
}
