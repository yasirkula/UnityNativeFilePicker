#if !UNITY_EDITOR && UNITY_IOS
using UnityEngine;

namespace NativeFilePickerNamespace
{
	public class FPResultCallbackiOS : MonoBehaviour
	{
		private static FPResultCallbackiOS instance;

		private NativeFilePicker.FilePickedCallback pickCallback;
		private NativeFilePicker.MultipleFilesPickedCallback pickCallbackMultiple;
		private NativeFilePicker.FilesExportedCallback exportCallback;

		private float nextBusyCheckTime;

		public static bool IsBusy { get; private set; }

		[System.Runtime.InteropServices.DllImport( "__Internal" )]
		private static extern int _NativeFilePicker_IsFilePickerBusy();

		public static void Initialize( NativeFilePicker.FilePickedCallback pickCallback, NativeFilePicker.MultipleFilesPickedCallback pickCallbackMultiple, NativeFilePicker.FilesExportedCallback exportCallback )
		{
			if( IsBusy )
				return;

			if( instance == null )
			{
				instance = new GameObject( "FPResultCallbackiOS" ).AddComponent<FPResultCallbackiOS>();
				DontDestroyOnLoad( instance.gameObject );
			}

			instance.pickCallback = pickCallback;
			instance.pickCallbackMultiple = pickCallbackMultiple;
			instance.exportCallback = exportCallback;

			instance.nextBusyCheckTime = Time.realtimeSinceStartup + 1f;
			IsBusy = true;
		}

		private void Update()
		{
			if( IsBusy )
			{
				if( Time.realtimeSinceStartup >= nextBusyCheckTime )
				{
					nextBusyCheckTime = Time.realtimeSinceStartup + 1f;

					if( _NativeFilePicker_IsFilePickerBusy() == 0 )
						OnOperationCancelled( null );
				}
			}
		}

		public void OnFilePicked( string path )
		{
			IsBusy = false;

			if( string.IsNullOrEmpty( path ) )
				path = null;

			if( pickCallback != null )
			{
				pickCallback( path );
				pickCallback = null;
			}
		}

		public void OnMultipleFilesPicked( string paths )
		{
			IsBusy = false;

			string[] _paths = SplitPaths( paths );
			if( _paths != null && _paths.Length == 0 )
				_paths = null;

			if( pickCallbackMultiple != null )
			{
				pickCallbackMultiple( _paths );
				pickCallbackMultiple = null;
			}
		}

		public void OnFilesExported( string message )
		{
			IsBusy = false;

			if( exportCallback != null )
			{
				exportCallback( message == "1" );
				exportCallback = null;
			}
		}

		public void OnOperationCancelled( string message )
		{
			IsBusy = false;

			if( pickCallback != null )
			{
				pickCallback( null );
				pickCallback = null;
			}

			if( pickCallbackMultiple != null )
			{
				pickCallbackMultiple( null );
				pickCallbackMultiple = null;
			}

			if( exportCallback != null )
			{
				exportCallback( false );
				exportCallback = null;
			}
		}

		private string[] SplitPaths( string paths )
		{
			string[] result = null;
			if( !string.IsNullOrEmpty( paths ) )
			{
				string[] pathsSplit = paths.Split( '>' );

				int validPathCount = 0;
				for( int i = 0; i < pathsSplit.Length; i++ )
				{
					if( !string.IsNullOrEmpty( pathsSplit[i] ) )
						validPathCount++;
				}

				if( validPathCount == 0 )
					pathsSplit = new string[0];
				else if( validPathCount != pathsSplit.Length )
				{
					string[] validPaths = new string[validPathCount];
					for( int i = 0, j = 0; i < pathsSplit.Length; i++ )
					{
						if( !string.IsNullOrEmpty( pathsSplit[i] ) )
							validPaths[j++] = pathsSplit[i];
					}

					pathsSplit = validPaths;
				}

				result = pathsSplit;
			}

			return result;
		}
	}
}
#endif