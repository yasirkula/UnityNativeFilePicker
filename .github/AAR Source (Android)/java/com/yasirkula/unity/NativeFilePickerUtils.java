package com.yasirkula.unity;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class NativeFilePickerUtils
{
	private static String secondaryStoragePath = null;
	private static int isXiaomiOrMIUI = 0; // 1: true, -1: false

	public static boolean IsXiaomiOrMIUI()
	{
		if( isXiaomiOrMIUI > 0 )
			return true;
		else if( isXiaomiOrMIUI < 0 )
			return false;

		if( "xiaomi".equalsIgnoreCase( android.os.Build.MANUFACTURER ) )
		{
			isXiaomiOrMIUI = 1;
			return true;
		}

		// Check if device is using MIUI
		// Credit: https://gist.github.com/Muyangmin/e8ec1002c930d8df3df46b306d03315d
		String line;
		BufferedReader inputStream = null;
		try
		{
			java.lang.Process process = Runtime.getRuntime().exec( "getprop ro.miui.ui.version.name" );
			inputStream = new BufferedReader( new InputStreamReader( process.getInputStream() ), 1024 );
			line = inputStream.readLine();

			if( line != null && line.length() > 0 )
			{
				isXiaomiOrMIUI = 1;
				return true;
			}
			else
			{
				isXiaomiOrMIUI = -1;
				return false;
			}
		}
		catch( Exception e )
		{
			isXiaomiOrMIUI = -1;
			return false;
		}
		finally
		{
			if( inputStream != null )
			{
				try
				{
					inputStream.close();
				}
				catch( Exception e )
				{
				}
			}
		}
	}

	// Credit: https://stackoverflow.com/a/36714242/2373034
	public static String GetPathFromURI( Context context, Uri uri )
	{
		if( uri == null )
			return null;

		String selection = null;
		String[] selectionArgs = null;

		try
		{
			if( Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri( context.getApplicationContext(), uri ) )
			{
				if( "com.android.externalstorage.documents".equals( uri.getAuthority() ) )
				{
					final String docId = DocumentsContract.getDocumentId( uri );
					final String[] split = docId.split( ":" );

					if( "primary".equalsIgnoreCase( split[0] ) )
						return Environment.getExternalStorageDirectory() + File.separator + split[1];
					else if( "raw".equalsIgnoreCase( split[0] ) ) // https://stackoverflow.com/a/51874578/2373034
						return split[1];

					return GetSecondaryStoragePathFor( split[1] );
				}
				else if( "com.android.providers.downloads.documents".equals( uri.getAuthority() ) )
				{
					final String id = DocumentsContract.getDocumentId( uri );
					if( id.startsWith( "raw:" ) ) // https://stackoverflow.com/a/51874578/2373034
						return id.substring( 4 );
					else if( id.indexOf( ':' ) < 0 ) // Don't attempt to parse stuff like "msf:NUMBER" (newer Android versions)
						uri = ContentUris.withAppendedId( Uri.parse( "content://downloads/public_downloads" ), Long.parseLong( id ) );
					else
						return null;
				}
				else if( "com.android.providers.media.documents".equals( uri.getAuthority() ) )
				{
					final String docId = DocumentsContract.getDocumentId( uri );
					final String[] split = docId.split( ":" );
					final String type = split[0];
					if( "image".equals( type ) )
						uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
					else if( "video".equals( type ) )
						uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
					else if( "audio".equals( type ) )
						uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
					else if( "raw".equals( type ) ) // https://stackoverflow.com/a/51874578/2373034
						return split[1];

					selection = "_id=?";
					selectionArgs = new String[] { split[1] };
				}
			}

			if( "content".equalsIgnoreCase( uri.getScheme() ) )
			{
				String[] projection = { MediaStore.Images.Media.DATA };
				Cursor cursor = null;

				try
				{
					cursor = context.getContentResolver().query( uri, projection, selection, selectionArgs, null );
					if( cursor != null )
					{
						int column_index = cursor.getColumnIndexOrThrow( MediaStore.Images.Media.DATA );
						if( cursor.moveToFirst() )
						{
							String columnValue = cursor.getString( column_index );
							if( columnValue != null && columnValue.length() > 0 )
								return columnValue;
						}
					}
				}
				catch( Exception e )
				{
				}
				finally
				{
					if( cursor != null )
						cursor.close();
				}
			}
			else if( "file".equalsIgnoreCase( uri.getScheme() ) )
				return uri.getPath();

			// File path couldn't be determined
			return null;
		}
		catch( Exception e )
		{
			Log.e( "Unity", "Exception:", e );
			return null;
		}
	}

	private static String GetSecondaryStoragePathFor( String localPath )
	{
		if( secondaryStoragePath == null )
		{
			String primaryPath = Environment.getExternalStorageDirectory().getAbsolutePath();

			// Try paths saved at system environments
			// Credit: https://stackoverflow.com/a/32088396/2373034
			String strSDCardPath = System.getenv( "SECONDARY_STORAGE" );
			if( strSDCardPath == null || strSDCardPath.length() == 0 )
				strSDCardPath = System.getenv( "EXTERNAL_SDCARD_STORAGE" );

			if( strSDCardPath != null && strSDCardPath.length() > 0 )
			{
				if( !strSDCardPath.contains( ":" ) )
					strSDCardPath += ":";

				String[] externalPaths = strSDCardPath.split( ":" );
				for( int i = 0; i < externalPaths.length; i++ )
				{
					String path = externalPaths[i];
					if( path != null && path.length() > 0 )
					{
						File file = new File( path );
						if( file.exists() && file.isDirectory() && file.canRead() && !file.getAbsolutePath().equalsIgnoreCase( primaryPath ) )
						{
							String absolutePath = file.getAbsolutePath() + File.separator + localPath;
							if( new File( absolutePath ).exists() )
							{
								secondaryStoragePath = file.getAbsolutePath();
								return absolutePath;
							}
						}
					}
				}
			}

			// Try most common possible paths
			// Credit: https://gist.github.com/PauloLuan/4bcecc086095bce28e22
			String[] possibleRoots = new String[] { "/storage", "/mnt", "/storage/removable",
					"/removable", "/data", "/mnt/media_rw", "/mnt/sdcard0" };
			for( String root : possibleRoots )
			{
				try
				{
					File fileList[] = new File( root ).listFiles();
					for( File file : fileList )
					{
						if( file.exists() && file.isDirectory() && file.canRead() && !file.getAbsolutePath().equalsIgnoreCase( primaryPath ) )
						{
							String absolutePath = file.getAbsolutePath() + File.separator + localPath;
							if( new File( absolutePath ).exists() )
							{
								secondaryStoragePath = file.getAbsolutePath();
								return absolutePath;
							}
						}
					}
				}
				catch( Exception e )
				{
				}
			}

			secondaryStoragePath = "_NulL_";
		}
		else if( !secondaryStoragePath.equals( "_NulL_" ) )
			return secondaryStoragePath + File.separator + localPath;

		return null;
	}
}