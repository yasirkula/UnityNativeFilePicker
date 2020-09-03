using System;
using System.IO;
using UnityEngine;
#if !UNITY_EDITOR && ( UNITY_ANDROID || UNITY_IOS )
using NativeFilePickerNamespace;
#endif

public static class NativeFilePicker
{
	public delegate void FilePickedCallback( string path );
	public delegate void MultipleFilesPickedCallback( string[] paths );
	public delegate void FilesExportedCallback( bool success );

	public enum Permission { Denied = 0, Granted = 1, ShouldAsk = 2 };

	#region Platform Specific Elements
#if !UNITY_EDITOR && UNITY_ANDROID
	private static AndroidJavaClass m_ajc = null;
	private static AndroidJavaClass AJC
	{
		get
		{
			if( m_ajc == null )
				m_ajc = new AndroidJavaClass( "com.yasirkula.unity.NativeFilePicker" );

			return m_ajc;
		}
	}

	private static AndroidJavaObject m_context = null;
	private static AndroidJavaObject Context
	{
		get
		{
			if( m_context == null )
			{
				using( AndroidJavaObject unityClass = new AndroidJavaClass( "com.unity3d.player.UnityPlayer" ) )
				{
					m_context = unityClass.GetStatic<AndroidJavaObject>( "currentActivity" );
				}
			}

			return m_context;
		}
	}
#elif !UNITY_EDITOR && UNITY_IOS
	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern int _NativeFilePicker_CanPickMultipleFiles();

	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern string _NativeFilePicker_ConvertExtensionToUTI( string extension );

	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern void _NativeFilePicker_PickFile( string[] UTIs, int UTIsCount );

	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern void _NativeFilePicker_PickMultipleFiles( string[] UTIs, int UTIsCount );

	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern void _NativeFilePicker_ExportFiles( string[] files, int filesCount );
#endif
	#endregion

#if !UNITY_EDITOR && UNITY_ANDROID
	private static string m_selectedFilePath = null;
	private static string SelectedFilePath
	{
		get
		{
			if( m_selectedFilePath == null )
			{
				m_selectedFilePath = Path.Combine( Application.temporaryCachePath, "pickedFile" );
				Directory.CreateDirectory( Application.temporaryCachePath );
			}

			return m_selectedFilePath;
		}
	}
#endif

	#region Runtime Permissions
	public static Permission CheckPermission( bool readPermissionOnly = false )
	{
#if !UNITY_EDITOR && UNITY_ANDROID
		Permission result = (Permission) AJC.CallStatic<int>( "CheckPermission", Context, readPermissionOnly );
		if( result == Permission.Denied && (Permission) PlayerPrefs.GetInt( "NativeFilePickerPermission", (int) Permission.ShouldAsk ) == Permission.ShouldAsk )
			result = Permission.ShouldAsk;

		return result;
#else
		return Permission.Granted;
#endif
	}

	public static Permission RequestPermission( bool readPermissionOnly = false )
	{
#if !UNITY_EDITOR && UNITY_ANDROID
		object threadLock = new object();
		lock( threadLock )
		{
			FPPermissionCallbackAndroid nativeCallback = new FPPermissionCallbackAndroid( threadLock );

			AJC.CallStatic( "RequestPermission", Context, nativeCallback, readPermissionOnly, PlayerPrefs.GetInt( "NativeFilePickerPermission", (int) Permission.ShouldAsk ) );

			if( nativeCallback.Result == -1 )
				System.Threading.Monitor.Wait( threadLock );

			if( (Permission) nativeCallback.Result != Permission.ShouldAsk && PlayerPrefs.GetInt( "NativeFilePickerPermission", -1 ) != nativeCallback.Result )
			{
				PlayerPrefs.SetInt( "NativeFilePickerPermission", nativeCallback.Result );
				PlayerPrefs.Save();
			}

			return (Permission) nativeCallback.Result;
		}
#else
		return Permission.Granted;
#endif
	}

	public static void OpenSettings()
	{
#if !UNITY_EDITOR && UNITY_ANDROID
		AJC.CallStatic( "OpenSettings", Context );
#endif
	}
	#endregion

	#region Helper Functions
	public static bool CanPickMultipleFiles()
	{
#if !UNITY_EDITOR && UNITY_ANDROID
		return AJC.CallStatic<bool>( "CanPickMultipleFiles" );
#elif !UNITY_EDITOR && UNITY_IOS
		return _NativeFilePicker_CanPickMultipleFiles() == 1;
#else
		return false;
#endif
	}

	public static bool CanExportFiles()
	{
#if !UNITY_EDITOR && UNITY_ANDROID
		return AJC.CallStatic<bool>( "CanExportFiles" );
#elif !UNITY_EDITOR && UNITY_IOS
		return true;
#else
		return false;
#endif
	}

	public static bool CanExportMultipleFiles()
	{
#if !UNITY_EDITOR && UNITY_ANDROID
		return AJC.CallStatic<bool>( "CanExportMultipleFiles" );
#elif !UNITY_EDITOR && UNITY_IOS
		return _NativeFilePicker_CanPickMultipleFiles() == 1;
#else
		return false;
#endif
	}

	public static bool IsFilePickerBusy()
	{
#if !UNITY_EDITOR && UNITY_IOS
		return FPResultCallbackiOS.IsBusy;
#else
		return false;
#endif
	}

	public static string ConvertExtensionToFileType( string extension )
	{
		if( string.IsNullOrEmpty( extension ) )
			return null;

#if !UNITY_EDITOR && UNITY_ANDROID
		return AJC.CallStatic<string>( "GetMimeTypeFromExtension", extension.ToLowerInvariant() );
#elif !UNITY_EDITOR && UNITY_IOS
		return _NativeFilePicker_ConvertExtensionToUTI( extension.ToLowerInvariant() );
#else
		return extension;
#endif
	}
	#endregion

	#region Import Functions
	public static Permission PickFile( FilePickedCallback callback, string[] allowedFileTypes )
	{
		if( allowedFileTypes == null || allowedFileTypes.Length == 0 )
			throw new ArgumentException( "Parameter 'allowedFileTypes' is null or empty!" );

		Permission result = RequestPermission( true );
		if( result == Permission.Granted && !IsFilePickerBusy() )
		{
#if !UNITY_EDITOR && UNITY_ANDROID
			AJC.CallStatic( "PickFiles", Context, new FPResultCallbackAndroid( callback, null, null ), false, SelectedFilePath, allowedFileTypes, "" );
#elif !UNITY_EDITOR && UNITY_IOS
			FPResultCallbackiOS.Initialize( callback, null, null );
			_NativeFilePicker_PickFile( allowedFileTypes, allowedFileTypes.Length );
#else
			if( callback != null )
				callback( null );
#endif
		}

		return result;
	}

	public static Permission PickMultipleFiles( MultipleFilesPickedCallback callback, string[] allowedFileTypes )
	{
		if( allowedFileTypes == null || allowedFileTypes.Length == 0 )
			throw new ArgumentException( "Parameter 'allowedFileTypes' is null or empty!" );

		Permission result = RequestPermission( true );
		if( result == Permission.Granted && !IsFilePickerBusy() )
		{
			if( CanPickMultipleFiles() )
			{
#if !UNITY_EDITOR && UNITY_ANDROID
				AJC.CallStatic( "PickFiles", Context, new FPResultCallbackAndroid( null, callback, null ), true, SelectedFilePath, allowedFileTypes, "" );
#elif !UNITY_EDITOR && UNITY_IOS
				FPResultCallbackiOS.Initialize( null, callback, null );
				_NativeFilePicker_PickMultipleFiles( allowedFileTypes, allowedFileTypes.Length );
#endif
			}
			else if( callback != null )
				callback( null );
		}

		return result;
	}
	#endregion

	#region Export Functions
	public static Permission ExportFile( string filePath, FilesExportedCallback callback = null )
	{
		if( string.IsNullOrEmpty( filePath ) )
			throw new ArgumentException( "Parameter 'filePath' is null or empty!" );

		Permission result = RequestPermission( false );
		if( result == Permission.Granted && !IsFilePickerBusy() )
		{
			if( CanExportFiles() )
			{
#if !UNITY_EDITOR && UNITY_ANDROID
				AJC.CallStatic( "ExportFiles", Context, new FPResultCallbackAndroid( null, null, callback ), new string[1] { filePath }, 1 );
#elif !UNITY_EDITOR && UNITY_IOS
				FPResultCallbackiOS.Initialize( null, null, callback );
				_NativeFilePicker_ExportFiles( new string[1] { filePath }, 1 );
#endif
			}
			else if( callback != null )
				callback( false );
		}

		return result;
	}

	public static Permission ExportMultipleFiles( string[] filePaths, FilesExportedCallback callback = null )
	{
		if( filePaths == null || filePaths.Length == 0 )
			throw new ArgumentException( "Parameter 'filePaths' is null or empty!" );

		Permission result = RequestPermission( false );
		if( result == Permission.Granted && !IsFilePickerBusy() )
		{
			if( CanExportMultipleFiles() )
			{
#if !UNITY_EDITOR && UNITY_ANDROID
				AJC.CallStatic( "ExportFiles", Context, new FPResultCallbackAndroid( null, null, callback ), filePaths, filePaths.Length );
#elif !UNITY_EDITOR && UNITY_IOS
				FPResultCallbackiOS.Initialize( null, null, callback );
				_NativeFilePicker_ExportFiles( filePaths, filePaths.Length );
#endif
			}
			else if( callback != null )
				callback( false );
		}

		return result;
	}
	#endregion
}