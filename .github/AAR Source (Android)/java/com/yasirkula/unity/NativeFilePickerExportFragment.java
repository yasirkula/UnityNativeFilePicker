package com.yasirkula.unity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;

public class NativeFilePickerExportFragment extends Fragment
{
	private static final int EXPORT_FILE_CODE = 625441;

	public static final String FILES_ID = "NFPE_FILES";

	private final NativeFilePickerResultReceiver resultReceiver;
	private ArrayList<String> files;

	public NativeFilePickerExportFragment()
	{
		resultReceiver = null;
	}

	public NativeFilePickerExportFragment( final NativeFilePickerResultReceiver resultReceiver )
	{
		this.resultReceiver = resultReceiver;
	}

	@Override
	@TargetApi( Build.VERSION_CODES.KITKAT )
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		if( resultReceiver == null )
			onActivityResult( EXPORT_FILE_CODE, Activity.RESULT_CANCELED, null );
		else
		{
			files = getArguments().getStringArrayList( FILES_ID );

			Intent intent;
			if( files.size() == 1 || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP )
			{
				// Exporting multiple files isn't supported on this Android version
				for( int i = files.size() - 1; i >= 1; i-- )
					files.remove( i );

				intent = new Intent( Intent.ACTION_CREATE_DOCUMENT );
				intent.setType( GetMimeTypeFromFile( files.get( 0 ) ) );
				intent.putExtra( Intent.EXTRA_TITLE, new File( files.get( 0 ) ).getName() );
				intent.addCategory( Intent.CATEGORY_OPENABLE );
			}
			else
			{
				// We won't set MIME type here, we will set it for each file separately inside ExportMultipleFilesToDirectory
				intent = new Intent( Intent.ACTION_OPEN_DOCUMENT_TREE );
				intent.putExtra( "android.content.extra.SHOW_ADVANCED", true );
				intent.putExtra( "android.content.extra.FANCY", true );
				intent.putExtra( "android.content.extra.SHOW_FILESIZE", true );
			}

			intent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION );

			try
			{
				//  MIUI devices have issues with Intent.createChooser on at least Android 11 (#15 and https://stackoverflow.com/questions/67785661/taking-and-picking-photos-on-poco-x3-with-android-11-does-not-work)
				if( NativeFilePicker.UseDefaultFilePickerApp || ( Build.VERSION.SDK_INT == 30 && NativeFilePickerUtils.IsXiaomiOrMIUI() ) )
					startActivityForResult( intent, EXPORT_FILE_CODE );
				else
					startActivityForResult( Intent.createChooser( intent, "" ), EXPORT_FILE_CODE );
			}
			catch( ActivityNotFoundException e )
			{
				Toast.makeText( getActivity(), "No apps can perform this action.", Toast.LENGTH_LONG ).show();
				onActivityResult( EXPORT_FILE_CODE, Activity.RESULT_CANCELED, null );
			}
		}
	}

	private String GetMimeTypeFromFile( String file )
	{
		int extensionStart = file.lastIndexOf( '.' );
		if( extensionStart < 0 || extensionStart == file.length() - 1 )
			return "application/octet-stream";

		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension( file.substring( extensionStart + 1 ).toLowerCase( Locale.ENGLISH ) );
		return ( mimeType == null || mimeType.length() == 0 ) ? "application/octet-stream" : mimeType;
	}

	@TargetApi( Build.VERSION_CODES.LOLLIPOP )
	private boolean ExportMultipleFilesToDirectory( ArrayList<String> files, Uri directoryUri )
	{
		NativeFilePickerSAFEntry directory = NativeFilePickerSAFEntry.fromTreeUri( getActivity(), directoryUri );
		if( directory == null )
		{
			Log.e( "Unity", "Couldn't access export directory: " + directoryUri.toString() );
			return false;
		}

		boolean result = true;
		for( int i = 0; i < files.size(); i++ )
		{
			File file = new File( files.get( i ) );
			if( !file.exists() )
			{
				Log.e( "Unity", "Can't export " + files.get( i ) + ", file doesn't exist!" );
				continue;
			}

			NativeFilePickerSAFEntry safFile = directory.createFile( GetMimeTypeFromFile( files.get( i ) ), file.getName() );
			if( safFile == null )
			{
				Log.e( "Unity", "Couldn't create file inside directory: " + directoryUri.toString() + "/" + file.getName() );
				result = false;
			}

			try
			{
				result &= WriteFileToStream( file, getActivity().getContentResolver().openOutputStream( safFile.getUri() ) );
			}
			catch( Exception e )
			{
				Log.e( "Unity", "Exception:", e );
				result = false;
			}
		}

		return result;
	}

	private boolean WriteFileToStream( File file, OutputStream out )
	{
		try
		{
			InputStream in = new FileInputStream( file );
			try
			{
				byte[] buf = new byte[1024];
				int len;
				while( ( len = in.read( buf ) ) > 0 )
					out.write( buf, 0, len );
			}
			finally
			{
				try
				{
					in.close();
				}
				catch( Exception e )
				{
					Log.e( "Unity", "Exception:", e );
				}
			}
		}
		catch( Exception e )
		{
			Log.e( "Unity", "Exception:", e );
			return false;
		}
		finally
		{
			try
			{
				out.close();
			}
			catch( Exception e )
			{
				Log.e( "Unity", "Exception:", e );
			}
		}

		return true;
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		if( requestCode != EXPORT_FILE_CODE )
			return;

		boolean result = false;
		if( files == null || files.size() == 0 )
			Log.e( "Unity", "Fragment data got reset while exporting files!" );
		else if( resultCode != Activity.RESULT_OK || data == null || data.getData() == null )
			Log.d( "Unity", "Export operation cancelled" );
		else if( files.size() == 1 )
		{
			File file = new File( files.get( 0 ) );
			if( !file.exists() )
				Log.e( "Unity", "Can't export " + files.get( 0 ) + ", file doesn't exist!" );
			else
			{
				try
				{
					result = WriteFileToStream( file, getActivity().getContentResolver().openOutputStream( data.getData() ) );
				}
				catch( Exception e )
				{
					Log.e( "Unity", "Exception:", e );
				}
			}
		}
		else
			result = ExportMultipleFilesToDirectory( files, data.getData() );

		if( resultReceiver != null )
			resultReceiver.OnFilesExported( result );

		getFragmentManager().beginTransaction().remove( this ).commit();
	}
}