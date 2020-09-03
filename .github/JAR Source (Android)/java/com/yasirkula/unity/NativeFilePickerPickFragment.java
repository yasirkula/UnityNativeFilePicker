package com.yasirkula.unity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/* COMMENTS ON INTENT TYPES

ACTION_GET_CONTENT
+ Pick single file on all APIs
+ Pick multiple files on API >= 18
+ Has Dropbox support
- May not work with multiple mime types
- Doesn't work with custom file extensions

ACTION_OPEN_DOCUMENT
+ Pick single file on API >= 19
+ Pick multiple files on API >= 19
+ Works with multiple mime types
- Doesn't have Dropbox support
- Doesn't work with custom file extensions

ACTION_CREATE_DOCUMENT
+ Export single file on API >= 19

ACTION_OPEN_DOCUMENT_TREE
+ Export multiple files on API >= 21

 */

public class NativeFilePickerPickFragment extends Fragment
{
	private static final int PICK_FILE_CODE = 121455;

	public static final String SELECT_MULTIPLE_ID = "NFPP_MULTIPLE";
	public static final String SAVE_PATH_ID = "NFPP_SAVE_PATH";
	public static final String MIMES_ID = "NFPP_MIME";
	public static final String TITLE_ID = "NFPP_TITLE";

	private static final int PICKER_MODE_DEFAULT = 0;
	private static final int PICKER_MODE_GET_CONTENT = 1;
	private static final int PICKER_MODE_OPEN_DOCUMENT = 2;

	public static int pickerMode = PICKER_MODE_DEFAULT;

	public static boolean tryPreserveFilenames = true; // When enabled, app's cache will fill more quickly since most of the picked files will have a unique filename (less chance of overwriting old files)

	private final NativeFilePickerResultReceiver resultReceiver;
	private boolean selectMultiple;
	private String savePathDirectory, savePathFilename;

	private ArrayList<String> savedFiles;

	private static String secondaryStoragePath = null;

	public NativeFilePickerPickFragment()
	{
		resultReceiver = null;
	}

	public NativeFilePickerPickFragment( final NativeFilePickerResultReceiver resultReceiver )
	{
		this.resultReceiver = resultReceiver;
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		if( resultReceiver == null )
			getFragmentManager().beginTransaction().remove( this ).commit();
		else
		{
			ArrayList<String> mimes = getArguments().getStringArrayList( MIMES_ID );
			String title = getArguments().getString( TITLE_ID );
			selectMultiple = getArguments().getBoolean( SELECT_MULTIPLE_ID );
			String savePath = getArguments().getString( SAVE_PATH_ID );

			int pathSeparator = savePath.lastIndexOf( '/' );
			savePathFilename = pathSeparator >= 0 ? savePath.substring( pathSeparator + 1 ) : savePath;
			savePathDirectory = pathSeparator > 0 ? savePath.substring( 0, pathSeparator ) : getActivity().getCacheDir().getAbsolutePath();

			Intent intent;
			if( mimes.size() <= 1 )
			{
				if( pickerMode != PICKER_MODE_OPEN_DOCUMENT || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT )
					intent = new Intent( Intent.ACTION_GET_CONTENT );
				else
					intent = new Intent( Intent.ACTION_OPEN_DOCUMENT );
			}
			else
			{
				if( pickerMode == PICKER_MODE_GET_CONTENT || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT )
					intent = new Intent( Intent.ACTION_GET_CONTENT );
				else
					intent = new Intent( Intent.ACTION_OPEN_DOCUMENT );

				if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
				{
					String[] mimetypes = new String[mimes.size()];
					for( int i = 0; i < mimes.size(); i++ )
						mimetypes[i] = mimes.get( i );

					intent.putExtra( Intent.EXTRA_MIME_TYPES, mimetypes );
				}
			}

			intent.setType( getCombinedMimeType( mimes ) );
			intent.addCategory( Intent.CATEGORY_OPENABLE );
			intent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );

			if( selectMultiple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 )
				intent.putExtra( Intent.EXTRA_ALLOW_MULTIPLE, true );

			if( title != null && title.length() > 0 )
				intent.putExtra( Intent.EXTRA_TITLE, title );

			startActivityForResult( Intent.createChooser( intent, title ), PICK_FILE_CODE );
		}
	}

	private String getCombinedMimeType( ArrayList<String> mimes )
	{
		if( mimes.size() == 0 )
			return "*/*";
		if( mimes.size() == 1 )
			return mimes.get( 0 );

		String mimeType = null;
		String mimeSubtype = null;
		for( int i = 0; i < mimes.size(); i++ )
		{
			String mime = mimes.get( i );
			if( mime == null || mime.length() == 0 )
				return "*/*";

			int mimeDivider = mime.indexOf( '/' );
			if( mimeDivider <= 0 || mimeDivider == mime.length() - 1 )
				return "*/*";

			String thisMimeType = mime.substring( 0, mimeDivider );
			String thisMimeSubtype = mime.substring( mimeDivider + 1 );

			if( mimeType == null )
				mimeType = thisMimeType;
			else if( !mimeType.equals( thisMimeType ) )
				return "*/*";

			if( mimeSubtype == null )
				mimeSubtype = thisMimeSubtype;
			else if( !mimeSubtype.equals( thisMimeSubtype ) )
				mimeSubtype = "*";
		}

		return mimeType + "/" + mimeSubtype;
	}

	// Credit: https://stackoverflow.com/a/47023265/2373034
	@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR2 )
	private void fetchPathsOfMultipleMedia( ArrayList<String> result, Intent data )
	{
		if( data.getClipData() != null )
		{
			int count = data.getClipData().getItemCount();
			for( int i = 0; i < count; i++ )
				result.add( getPathFromURI( data.getClipData().getItemAt( i ).getUri() ) );
		}
		else if( data.getData() != null )
			result.add( getPathFromURI( data.getData() ) );
	}

	private String getPathFromURI( Uri uri )
	{
		if( uri == null )
			return null;

		Log.d( "Unity", "Selected media uri: " + uri.toString() );

		String path = getPathFromURIInternal( uri );
		if( path != null && path.length() > 0 )
		{
			// Check if file is accessible
			FileInputStream inputStream = null;
			try
			{
				inputStream = new FileInputStream( new File( path ) );
				inputStream.read();

				return path;
			}
			catch( Exception e )
			{
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

		// Either file path couldn't be determined or Android 10 restricts our access to the raw filesystem,
		// copy the file to an accessible temporary location
		return copyToTempFile( uri );
	}

	// Credit: https://stackoverflow.com/a/36714242/2373034
	private String getPathFromURIInternal( Uri uri )
	{
		// Android 10 restricts our access to the raw filesystem, file must be copied to an accessible temporary location
		if( android.os.Build.VERSION.SDK_INT >= 29 && !Environment.isExternalStorageLegacy() )
			return null;

		String selection = null;
		String[] selectionArgs = null;

		try
		{
			if( Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri( getActivity().getApplicationContext(), uri ) )
			{
				if( "com.android.externalstorage.documents".equals( uri.getAuthority() ) )
				{
					final String docId = DocumentsContract.getDocumentId( uri );
					final String[] split = docId.split( ":" );

					if( "primary".equalsIgnoreCase( split[0] ) )
						return Environment.getExternalStorageDirectory() + File.separator + split[1];
					else if( "raw".equalsIgnoreCase( split[0] ) ) // https://stackoverflow.com/a/51874578/2373034
						return split[1];

					return getSecondaryStoragePathFor( split[1] );
				}
				else if( "com.android.providers.downloads.documents".equals( uri.getAuthority() ) )
				{
					final String id = DocumentsContract.getDocumentId( uri );
					if( id.startsWith( "raw:" ) ) // https://stackoverflow.com/a/51874578/2373034
						return id.substring( 4 );

					uri = ContentUris.withAppendedId( Uri.parse( "content://downloads/public_downloads" ), Long.valueOf( id ) );
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
					cursor = getActivity().getContentResolver().query( uri, projection, selection, selectionArgs, null );
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

	private String getSecondaryStoragePathFor( String localPath )
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

	private String copyToTempFile( Uri uri )
	{
		// Credit: https://developer.android.com/training/secure-file-sharing/retrieve-info.html#RetrieveFileInfo
		ContentResolver resolver = getActivity().getContentResolver();
		Cursor returnCursor = null;
		String filename = null;

		try
		{
			returnCursor = resolver.query( uri, null, null, null, null );
			if( returnCursor != null && returnCursor.moveToFirst() )
				filename = returnCursor.getString( returnCursor.getColumnIndex( OpenableColumns.DISPLAY_NAME ) );
		}
		catch( Exception e )
		{
			Log.e( "Unity", "Exception:", e );
		}
		finally
		{
			if( returnCursor != null )
				returnCursor.close();
		}

		if( filename == null || filename.length() < 3 )
			filename = "temp";

		String extension = null;
		String mime = resolver.getType( uri );
		if( mime != null )
		{
			String mimeExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType( mime );
			if( mimeExtension != null && mimeExtension.length() > 0 )
				extension = "." + mimeExtension;
		}

		if( extension == null )
		{
			int filenameExtensionIndex = filename.lastIndexOf( '.' );
			if( filenameExtensionIndex > 0 && filenameExtensionIndex < filename.length() - 1 )
				extension = filename.substring( filenameExtensionIndex );
			else
				extension = ".tmp";
		}

		if( !tryPreserveFilenames )
			filename = savePathFilename;
		else if( filename.endsWith( extension ) )
			filename = filename.substring( 0, filename.length() - extension.length() );

		try
		{
			InputStream input = resolver.openInputStream( uri );
			if( input == null )
				return null;

			String fullName = filename + extension;
			if( savedFiles != null )
			{
				int n = 1;
				for( int i = 0; i < savedFiles.size(); i++ )
				{
					if( savedFiles.get( i ).equals( fullName ) )
					{
						n++;
						fullName = filename + n + extension;
						i = -1;
					}
				}
			}

			File tempFile = new File( savePathDirectory, fullName );
			OutputStream output = null;
			try
			{
				output = new FileOutputStream( tempFile, false );

				byte[] buf = new byte[4096];
				int len;
				while( ( len = input.read( buf ) ) > 0 )
				{
					output.write( buf, 0, len );
				}

				if( selectMultiple )
				{
					if( savedFiles == null )
						savedFiles = new ArrayList<String>();

					savedFiles.add( fullName );
				}

				return tempFile.getAbsolutePath();
			}
			finally
			{
				if( output != null )
					output.close();

				input.close();
			}
		}
		catch( Exception e )
		{
			Log.e( "Unity", "Exception:", e );
		}

		return null;
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		if( requestCode != PICK_FILE_CODE )
			return;

		if( !selectMultiple )
		{
			String result;
			if( resultCode != Activity.RESULT_OK || data == null )
				result = "";
			else
			{
				result = getPathFromURI( data.getData() );
				if( result == null )
					result = "";
			}

			if( result.length() > 0 && !( new File( result ).exists() ) )
				result = "";

			if( resultReceiver != null )
				resultReceiver.OnFilePicked( result );
		}
		else
		{
			ArrayList<String> result = new ArrayList<String>();
			if( resultCode == Activity.RESULT_OK && data != null )
				fetchPathsOfMultipleMedia( result, data );

			for( int i = result.size() - 1; i >= 0; i-- )
			{
				if( result.get( i ) == null || result.get( i ).length() == 0 || !( new File( result.get( i ) ).exists() ) )
					result.remove( i );
			}

			String resultCombined = "";
			for( int i = 0; i < result.size(); i++ )
			{
				if( i == 0 )
					resultCombined += result.get( i );
				else
					resultCombined += ">" + result.get( i );
			}

			if( resultReceiver != null )
				resultReceiver.OnMultipleFilesPicked( resultCombined );
		}

		getFragmentManager().beginTransaction().remove( this ).commit();
	}
}