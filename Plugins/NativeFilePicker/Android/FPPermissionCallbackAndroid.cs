#if UNITY_EDITOR || UNITY_ANDROID
using System.Threading;
using UnityEngine;

namespace NativeFilePickerNamespace
{
	public class FPPermissionCallbackAndroid : AndroidJavaProxy
	{
		private object threadLock;
		public int Result { get; private set; }

		public FPPermissionCallbackAndroid( object threadLock ) : base( "com.yasirkula.unity.NativeFilePickerPermissionReceiver" )
		{
			Result = -1;
			this.threadLock = threadLock;
		}

		[UnityEngine.Scripting.Preserve]
		public void OnPermissionResult( int result )
		{
			Result = result;

			lock( threadLock )
			{
				Monitor.Pulse( threadLock );
			}
		}
	}

	public class FPPermissionCallbackAsyncAndroid : AndroidJavaProxy
	{
		private readonly NativeFilePicker.PermissionCallback callback;
		private readonly FPCallbackHelper callbackHelper;

		public FPPermissionCallbackAsyncAndroid( NativeFilePicker.PermissionCallback callback ) : base( "com.yasirkula.unity.NativeFilePickerPermissionReceiver" )
		{
			this.callback = callback;
			callbackHelper = new GameObject( "FPCallbackHelper" ).AddComponent<FPCallbackHelper>();
		}

		[UnityEngine.Scripting.Preserve]
		public void OnPermissionResult( int result )
		{
			callbackHelper.CallOnMainThread( () => callback( (NativeFilePicker.Permission) result ) );
		}
	}
}
#endif