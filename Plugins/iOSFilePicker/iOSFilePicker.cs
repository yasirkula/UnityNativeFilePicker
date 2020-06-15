using System;
#if !UNITY_EDITOR && UNITY_IOS
using iOSFilePickerNamespace;
#endif

public static class iOSFilePicker
{
	public delegate void FilePickedCallback( string path );
	public delegate void MultipleFilesPickedCallback( string[] paths );
	public delegate void FilesExportedCallback( bool success );

	#region Platform Specific Elements
#if !UNITY_EDITOR && UNITY_IOS
	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern int _iOSFilePicker_CanPickMultipleFiles();

	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern string _iOSFilePicker_ConvertExtensionToUTI( string extension );

	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern void _iOSFilePicker_PickFile( string[] UTIs, int UTIsCount );

	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern void _iOSFilePicker_PickMultipleFiles( string[] UTIs, int UTIsCount );

	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern void _iOSFilePicker_ExportFiles( string[] files, int filesCount );
#endif
	#endregion

	#region Load Functions
	public static bool CanPickMultipleFiles()
	{
#if !UNITY_EDITOR && UNITY_IOS
		return _iOSFilePicker_CanPickMultipleFiles() == 1;
#else
		return false;
#endif
	}

	public static bool IsFilePickerBusy()
	{
#if !UNITY_EDITOR && UNITY_IOS
		return iOSFilePickerCallback.IsBusy;
#else
		return false;
#endif
	}

	public static string ConvertExtensionToUTI( string extension )
	{
#if !UNITY_EDITOR && UNITY_IOS
		return _iOSFilePicker_ConvertExtensionToUTI( extension );
#else
		return extension;
#endif
	}

	public static void PickFile( FilePickedCallback callback, string[] allowedUTIs )
	{
		if( allowedUTIs == null || allowedUTIs.Length == 0 )
			throw new ArgumentException( "Parameter 'allowedUTIs' is null or empty!" );

		if( IsFilePickerBusy() )
			return;

#if !UNITY_EDITOR && UNITY_IOS
		iOSFilePickerCallback.Initialize( callback, null, null );
		_iOSFilePicker_PickFile( allowedUTIs, allowedUTIs.Length );
#else
		if( callback != null )
			callback( null );
#endif
	}

	public static void PickMultipleFiles( MultipleFilesPickedCallback callback, string[] allowedUTIs )
	{
		if( allowedUTIs == null || allowedUTIs.Length == 0 )
			throw new ArgumentException( "Parameter 'allowedUTIs' is null or empty!" );

		if( IsFilePickerBusy() )
			return;

		if( CanPickMultipleFiles() )
		{
#if !UNITY_EDITOR && UNITY_IOS
			iOSFilePickerCallback.Initialize( null, callback, null );
			_iOSFilePicker_PickMultipleFiles( allowedUTIs, allowedUTIs.Length );
#endif
		}
		else if( callback != null )
			callback( null );
	}

	public static void ExportFiles( string[] filePaths, FilesExportedCallback callback = null )
	{
		if( filePaths == null || filePaths.Length == 0 )
			throw new ArgumentException( "Parameter 'filePaths' is null or empty!" );

		if( IsFilePickerBusy() )
			return;

#if !UNITY_EDITOR && UNITY_IOS
		iOSFilePickerCallback.Initialize( null, null, callback );
		_iOSFilePicker_ExportFiles( filePaths, filePaths.Length );
#else
		if( callback != null )
			callback( false );
#endif
	}
	#endregion
}