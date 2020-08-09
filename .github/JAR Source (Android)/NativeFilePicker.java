package com.yasirkula.unity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.MimeTypeMap;

import java.util.ArrayList;

public class NativeFilePicker
{
	public static void PickFiles( Context context, final NativeFilePickerResultReceiver resultReceiver, final boolean selectMultiple, final String savePath, final String[] mimes, final String title )
	{
		if( CheckPermission( context, true ) != 1 )
		{
			if( !selectMultiple )
				resultReceiver.OnFilePicked( "" );
			else
				resultReceiver.OnMultipleFilesPicked( "" );

			return;
		}

		ArrayList<String> mimesList = new ArrayList<String>( mimes.length );
		for( int i = 0; i < mimes.length; i++ )
			mimesList.add( mimes[i] );

		Bundle bundle = new Bundle();
		bundle.putBoolean( NativeFilePickerPickFragment.SELECT_MULTIPLE_ID, selectMultiple );
		bundle.putString( NativeFilePickerPickFragment.SAVE_PATH_ID, savePath );
		bundle.putStringArrayList( NativeFilePickerPickFragment.MIMES_ID, mimesList );
		bundle.putString( NativeFilePickerPickFragment.TITLE_ID, title );

		final Fragment request = new NativeFilePickerPickFragment( resultReceiver );
		request.setArguments( bundle );

		( (Activity) context ).getFragmentManager().beginTransaction().add( 0, request ).commit();
	}

	public static void ExportFiles( Context context, final NativeFilePickerResultReceiver resultReceiver, final String[] files, final int dummyParameter ) // Having an array as last parameter can cause Unity to crash
	{
		if( CheckPermission( context, false ) != 1 )
		{
			resultReceiver.OnFilesExported( false );
			return;
		}

		ArrayList<String> filesList = new ArrayList<String>( files.length );
		for( int i = 0; i < files.length; i++ )
			filesList.add( files[i] );

		Bundle bundle = new Bundle();
		bundle.putStringArrayList( NativeFilePickerExportFragment.FILES_ID, filesList );

		final Fragment request = new NativeFilePickerExportFragment( resultReceiver );
		request.setArguments( bundle );

		( (Activity) context ).getFragmentManager().beginTransaction().add( 0, request ).commit();
	}

	@TargetApi( Build.VERSION_CODES.M )
	public static int CheckPermission( Context context, final boolean readPermissionOnly )
	{
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.M )
			return 1;

		if( context.checkSelfPermission( Manifest.permission.READ_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED )
		{
			if( readPermissionOnly || context.checkSelfPermission( Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED )
				return 1;
		}

		return 0;
	}

	// Credit: https://github.com/Over17/UnityAndroidPermissions/blob/0dca33e40628f1f279decb67d901fd444b409cd7/src/UnityAndroidPermissions/src/main/java/com/unity3d/plugin/UnityAndroidPermissions.java
	public static void RequestPermission( Context context, final NativeFilePickerPermissionReceiver permissionReceiver, final boolean readPermissionOnly, final int lastCheckResult )
	{
		if( CheckPermission( context, readPermissionOnly ) == 1 )
		{
			permissionReceiver.OnPermissionResult( 1 );
			return;
		}

		if( lastCheckResult == 0 ) // If user clicked "Don't ask again" before, don't bother asking them again
		{
			permissionReceiver.OnPermissionResult( 0 );
			return;
		}

		Bundle bundle = new Bundle();
		bundle.putBoolean( NativeFilePickerPermissionFragment.READ_PERMISSION_ONLY, readPermissionOnly );

		final Fragment request = new NativeFilePickerPermissionFragment( permissionReceiver );
		request.setArguments( bundle );

		( (Activity) context ).getFragmentManager().beginTransaction().add( 0, request ).commit();
	}

	// Credit: https://stackoverflow.com/a/35456817/2373034
	public static void OpenSettings( Context context )
	{
		Uri uri = Uri.fromParts( "package", context.getPackageName(), null );

		Intent intent = new Intent();
		intent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
		intent.setData( uri );

		context.startActivity( intent );
	}

	public static String GetMimeTypeFromExtension( String extension )
	{
		String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension( extension );
		return mime != null ? mime : "";
	}

	public static boolean CanPickMultipleFiles()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
	}

	public static boolean CanExportFiles()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	}

	public static boolean CanExportMultipleFiles()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}
}