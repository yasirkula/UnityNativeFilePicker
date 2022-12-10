package com.yasirkula.unity;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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
	public static boolean showProgressbar = true; // When enabled, a progressbar will be displayed while selected file(s) are copied (if necessary) to the destination directory

	private final NativeFilePickerResultReceiver resultReceiver;
	private boolean selectMultiple;
	private String savePathDirectory, savePathFilename;

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
			onActivityResult( PICK_FILE_CODE, Activity.RESULT_CANCELED, null );
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

			try
			{
				//  MIUI devices have issues with Intent.createChooser on at least Android 11 (#15 and https://stackoverflow.com/questions/67785661/taking-and-picking-photos-on-poco-x3-with-android-11-does-not-work)
				if( NativeFilePicker.UseDefaultFilePickerApp || ( Build.VERSION.SDK_INT == 30 && NativeFilePickerUtils.IsXiaomiOrMIUI() ) )
					startActivityForResult( intent, PICK_FILE_CODE );
				else
					startActivityForResult( Intent.createChooser( intent, title ), PICK_FILE_CODE );
			}
			catch( ActivityNotFoundException e )
			{
				Toast.makeText( getActivity(), "No apps can perform this action.", Toast.LENGTH_LONG ).show();
				onActivityResult( PICK_FILE_CODE, Activity.RESULT_CANCELED, null );
			}
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

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		if( requestCode != PICK_FILE_CODE )
			return;

		NativeFilePickerPickResultFragment resultFragment = null;

		if( resultReceiver == null )
			Log.d( "Unity", "NativeFilePickerPickFragment.resultReceiver became null!" );
		else if( resultCode != Activity.RESULT_OK || data == null )
		{
			if( !selectMultiple )
				resultReceiver.OnFilePicked( "" );
			else
				resultReceiver.OnMultipleFilesPicked( "" );
		}
		else
		{
			NativeFilePickerPickResultOperation resultOperation = new NativeFilePickerPickResultOperation( getActivity(), resultReceiver, data, selectMultiple, savePathDirectory, savePathFilename );
			if( showProgressbar )
				resultFragment = new NativeFilePickerPickResultFragment( resultOperation );
			else
			{
				resultOperation.execute();
				resultOperation.sendResultToUnity();
			}
		}

		if( resultFragment == null )
			getFragmentManager().beginTransaction().remove( this ).commit();
		else
			getFragmentManager().beginTransaction().remove( this ).add( 0, resultFragment ).commit();
	}
}