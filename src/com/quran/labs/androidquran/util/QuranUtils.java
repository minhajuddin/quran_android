package com.quran.labs.androidquran.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.quran.labs.androidquran.common.AyahItem;

public class QuranUtils {

	public static boolean failedToWrite = false;
	public static String IMG_HOST = "http://downthestreetfromyou.com/androidquran/";
	private static String QURAN_BASE = File.separator + "quran_android"
			+ File.separator;
	private static String DATABASE_DIRECTORY = "databases";
	private static int BUFF_SIZE = 1024;
	public static final String PACKAGE_NAME = "com.quran.labs.androidquran";

	public static boolean debugRmDir(String dir, boolean deleteDirectory) {
		File directory = new File(dir);
		if (directory.isDirectory()) {
			String[] children = directory.list();
			for (String s : children) {
				if (!debugRmDir(dir + File.separator + s, true))
					return false;
			}
		}

		return deleteDirectory ? directory.delete() : true;
	}

	public static void debugLsDir(String dir) {
		File directory = new File(dir);
		Log.d("quran_dbg", directory.getAbsolutePath());

		if (directory.isDirectory()) {
			String[] children = directory.list();
			for (String s : children)
				debugLsDir(dir + File.separator + s);
		}
	}

	public static boolean haveAllImages() {
		String state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_MOUNTED)) {
			File dir = new File(getQuranDirectory() + File.separator);
			if (dir.isDirectory()) {
				int files = dir.list().length;
				if (files == 605)
					return true;
			} else
				QuranUtils.makeQuranDirectory();
		}
		return false;
	}
	
	public static boolean isSDCardMounted() {
		String state = Environment.getExternalStorageState();
		return state.equals(Environment.MEDIA_MOUNTED);
	}
	
	public static Bitmap getImageFromSD(String filename){
		String location = getQuranDirectory();
		if (location == null)
			return null;
		return BitmapFactory.decodeFile(location + File.separator + filename);
	}

	public static boolean writeNoMediaFile() {
		File f = new File(getQuranDirectory() + "/.nomedia");
		if (f.exists()) {
			return true;
		}

		try {
			return f.createNewFile();
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean makeQuranDirectory() {
		String path = getQuranDirectory();
		if (path == null)
			return false;

		File directory = new File(path);
		if (directory.exists() && directory.isDirectory()) {
			return writeNoMediaFile();
		} else if (directory.mkdirs()) {
			return writeNoMediaFile();
		} else
			return false;
	}

	public static boolean makeQuranDatabaseDirectory() {
		String path = getQuranDatabaseDirectory();
		if (path == null)
			return false;

		File directory = new File(path);
		if (directory.exists() && directory.isDirectory()) {
			return true;
		} else if (directory.mkdirs()) {
			return true;
		} else
			return false;
	}

	public static boolean makeAudioDirectory(int readerId, int suraId) {
		String path = getAudioDirectory();
		if (path == null)
			return false;

		File directory = new File(path + File.separator + readerId
				+ File.separator + suraId);
		if (directory.exists() && directory.isDirectory()) {
			return true;
		} else if (directory.mkdirs()) {
			return true;
		} else
			return false;
	}

	public static boolean getTranslation(String fileUrl, String fileName) {
		String urlString = fileUrl;
		InputStream is;
		try {
			URL url = new URL(urlString);
			is = (InputStream) url.getContent();
		} catch (Exception e) {
			return false;
		}

		if (failedToWrite)
			return false;

		String path = getQuranDatabaseDirectory();
		if (path != null) {
			path += File.separator + fileName;

			if (!QuranUtils.makeQuranDatabaseDirectory()) {
				failedToWrite = true;
				return false;
			}

			try {
				saveStream(is, path);
				return true;
			} catch (Exception e) {
				Log.d("quran_utils", e.toString());
				return false;
			}
		} else
			return false;
	}

	public static Bitmap getImageFromWeb(String filename) {
		String urlString = IMG_HOST + "width"
				+ QuranScreenInfo.getInstance().getWidthParam() + "/"
				+ filename;
		Log.d("quran_utils", "want to download: " + urlString);

		InputStream is;
		try {
			URL url = new URL(urlString);
			is = (InputStream) url.getContent();
		} catch (Exception e) {
			return null;
		}

		if (failedToWrite)
			return BitmapFactory.decodeStream(is);

		String path = getQuranDirectory();
		if (path != null) {
			path += File.separator + filename;

			if (!QuranUtils.makeQuranDirectory()) {
				failedToWrite = true;
				return BitmapFactory.decodeStream(is);
			}

			boolean readPhase = false;
			try {
				readPhase = true;
				saveStream(is, path);

				return QuranUtils.getImageFromSD(filename);
			} catch (Exception e) {
				Log.d("quran_utils", e.toString());
				if (readPhase == false)
					return BitmapFactory.decodeStream(is);
				failedToWrite = true;
				return QuranUtils.getImageFromWeb(filename);
			}
		} else
			return BitmapFactory.decodeStream(is);
	}

	private static void saveStream(InputStream is, String savePath)
			throws IOException {
		FileOutputStream output = new FileOutputStream(savePath);
		int readlen;

		byte[] buf = new byte[BUFF_SIZE];
		while ((readlen = is.read(buf)) > 0)
			output.write(buf, 0, readlen);
		output.close();
		is.close();
	}

	public static String getQuranBaseDirectory() {
		String state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_MOUNTED))
			return Environment.getExternalStorageDirectory() + QURAN_BASE;
		else
			return null;
	}

	public static String getQuranDatabaseDirectory() {
		String base = getQuranBaseDirectory();
		return (base == null) ? null : base + DATABASE_DIRECTORY;
	}

	public static String getQuranDirectory() {
		String base = getQuranBaseDirectory();
		QuranScreenInfo qsi = QuranScreenInfo.getInstance();
		if (qsi == null)
			return null;
		return (base == null) ? null : base + "width" + qsi.getWidthParam();
	}

	public static String getZipFileUrl() {
		String url = IMG_HOST;
		QuranScreenInfo qsi = QuranScreenInfo.getInstance();
		if (qsi == null)
			return null;
		url += "images" + qsi.getWidthParam() + ".zip";
		return url;
	}
	
	public static String getAyaPositionFileUrl() {
		String url = IMG_HOST;
		QuranScreenInfo qsi = QuranScreenInfo.getInstance();
		if (qsi == null)
			return null;
		url += "databases/ayahinfo" + qsi.getWidthParamNoUnderScore() + ".db.zip";
		return url;
	}

	public static boolean hasTranslation(String fileName) {
		String path = getQuranDatabaseDirectory();
		if (path != null) {
			path += File.separator + fileName;
			return new File(path).exists();
		}
		return false;
	}

	public static boolean removeTranslation(String fileName) {
		String path = getQuranDatabaseDirectory();
		if (path != null) {
			path += File.separator + fileName;
			File f = new File(path);
			return f.delete();
		}
		return false;
	}

	/* Quran Audio Methods */

	public static String getAyahAudioPath(AyahItem ayahItem) {
		return getAyahAudioPath(ayahItem.getSoura(), ayahItem.getAyah(), ayahItem.getQuranReaderId());

	}

	public static String getSuraAudioPath(int quranReaderId, int sura){
		return getAudioDirectory() + File.separator + quranReaderId
		+ File.separator + sura ;
	}
	
	public static int isSouraAudioFound(int quranReaderId, int sura){
		File f = new File(getSuraAudioPath(quranReaderId, sura));
		if(f.exists() && f.isDirectory()){
			String[] filesNames = f.list(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String filename) {
					try{
						if(filename.endsWith(QuranAudioLibrary.AUDIO_EXTENSION)){
							filename = filename.substring(0, filename.indexOf(QuranAudioLibrary.AUDIO_EXTENSION));
							Integer.parseInt(filename);
							return true;
						}return false;						
					}catch (Exception e) {
						return false;
					}
				}
			});
			if(filesNames != null)
				return filesNames.length;
		}		
		return -1;
		
	}
	public static String getSuraImagePath(int sura){
		return getAyahImagesDirectory() + File.separator + sura;
	}
	public static String getAyahImagePath(AyahItem ayahItem) {
		return getAyahImagePath(ayahItem.getSoura(), ayahItem
				.getAyah());
	}

	public static String getAyahAudioPath(int sura, int ayah, int quranReaderId) {
		// always get basmala from el fate7a
		if(ayah == 0){
			ayah = 1;
			sura = 1;
		}
		return getAudioDirectory() + File.separator + quranReaderId
				+ File.separator + sura + File.separator + ayah
				+ QuranAudioLibrary.AUDIO_EXTENSION;
	}

	public static String getAyahImagePath(int sura, int ayah) {
		return getAyahImagesDirectory() + File.separator + sura
				+ File.separator + ayah + QuranAudioLibrary.IMAGE_EXTENSION;
	}

	public static String getAudioDirectory() {
		File f = null;
		File externalPath = Environment.getExternalStorageDirectory();
		String path = "/Android/data/" + PACKAGE_NAME + "/files/audio/";
		f = new File(externalPath.getAbsolutePath() + path);
		return f.getAbsolutePath();
	}
	
	public static String getAyahImagesDirectory() {
		File f = null;
		File externalPath = Environment.getExternalStorageDirectory();
		String path = "/Android/data/" + PACKAGE_NAME + "/files/ayat/";
		f = new File(externalPath.getAbsolutePath() + path);
		return f.getAbsolutePath();
	}
}
