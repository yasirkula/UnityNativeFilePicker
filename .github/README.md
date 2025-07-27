# Unity Native File Picker Plugin

**Available on Asset Store:** https://assetstore.unity.com/packages/tools/integration/native-file-picker-for-android-ios-173238

**Forum Thread:** https://forum.unity.com/threads/native-file-picker-for-android-ios-open-source.912890/

**Discord:** https://discord.gg/UJJt549AaV

**[GitHub Sponsors ☕](https://github.com/sponsors/yasirkula)**

This plugin helps you import/export files from/to various document providers on Android & iOS (other platforms aren't supported). On iOS, it uses **UIDocumentPickerViewController** which has the following requirements:

- iOS 8 or later
- an Apple Developer Program account (signing the app with a free account won't work)

**NOTE:** custom file extensions are supported on iOS only.

## INSTALLATION

There are 5 ways to install this plugin:

- import [NativeFilePicker.unitypackage](https://github.com/yasirkula/UnityNativeFilePicker/releases) via *Assets-Import Package*
- clone/[download](https://github.com/yasirkula/UnityNativeFilePicker/archive/master.zip) this repository and move the *Plugins* folder to your Unity project's *Assets* folder
- import it from [Asset Store](https://assetstore.unity.com/packages/tools/integration/ios-native-file-picker-173238)
- *(via Package Manager)* click the + button and install the package from the following git URL:
  - `https://github.com/yasirkula/UnityNativeFilePicker.git`
- *(via [OpenUPM](https://openupm.com))* after installing [openupm-cli](https://github.com/openupm/openupm-cli), run the following command:
  - `openupm add com.yasirkula.nativefilepicker`

### Android Setup

NativeFilePicker no longer requires any manual setup on Android.

### iOS Setup

There are two ways to set up the plugin on iOS:

**a. Automated Setup for iOS**

- set the values of **Auto Setup Frameworks** and **Auto Setup iCloud** to *true* at *Project Settings/yasirkula/Native File Picker*. By default, automated setup for iCloud is disabled. That's because this plugin uses the *iCloud capability* and if another plugin uses other capabilities, these plugins may conflict with each other. Set *Auto Setup iCloud* to true at your own risk
- if your app uses custom file extensions that are unique to your app (e.g. *.mydata*), add them to the *Window-NativeFilePicker Custom Types* asset (it has explanatory tooltips). This step works even if the values of *Auto Setup Frameworks* and *Auto Setup iCloud* are set to *false* (this step is not needed for extensions available in [this list](https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html))

**b. Manual Setup for iOS**

- see: https://github.com/yasirkula/UnityNativeFilePicker/wiki/Manual-Setup-for-iOS

## FAQ

- **How can I fetch the path of the saved file or the original path of the picked file?**

You can't. The abstraction layers used on each platform deliberately don't return raw file paths.

- **Plugin doesn't work in a Windows/Mac/Linux build**

Only Android & iOS platforms are supported. Editor functionality is for preview purposes only and uses Unity's [Editor-only API](https://docs.unity3d.com/ScriptReference/EditorUtility.OpenFilePanelWithFilters.html).

- **Can't import/export files, it says "java.lang.ClassNotFoundException: com.yasirkula.unity.NativeFilePicker" in Logcat**

If you are sure that your plugin is up-to-date, then enable **Custom Proguard File** option from *Player Settings* and add the following line to that file: `-keep class com.yasirkula.unity.* { *; }`

- **ConvertExtensionToFileType("\*") not working**

See: https://forum.unity.com/threads/native-file-picker-for-android-ios-open-source.912890/page-3#post-8987518

- **Nothing happens when I try to import/export files on Android**

Make sure that you've set the **Write Permission** to **External (SDCard)** in *Player Settings*.

- **NativeFilePicker functions return Permission.Denied even though I've set "Write Permission" to "External (SDCard)"**

Declare the `WRITE_EXTERNAL_STORAGE` permission manually in your **Plugins/Android/AndroidManifest.xml** with the `tools:node="replace"` attribute as follows: `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" tools:node="replace"/>`.

## HOW TO

### A. Importing Files

`NativeFilePicker.PickFile( FilePickedCallback callback, params string[] allowedFileTypes )`: prompts the user to pick a file from the available document providers.
- This operation is **asynchronous**! After user picks a file or cancels the operation, the **callback** is called (on main thread). **FilePickedCallback** takes a *string* parameter which stores the path of the picked file, or *null* if nothing is picked
- **allowedFileTypes** determines which file types are accepted. On Android, this is the *MIMEs* and on iOS, this is the *UTIs*. For example:
  - *PNG files:* `image/png` on Android and `public.png` on iOS
  - *Image files (png, jpeg, tiff, etc.):* `image/*` on Android and `public.image` on iOS
  - *PDF files:* `application/pdf` on Android and `com.adobe.pdf` on iOS
  - On Android, see the following list for all available MIMEs (other MIMEs may not be supported on all devices): https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/media/java/android/media/MediaFile.java#102
  - On iOS, see the following list for all available UTIs: https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html
  - Also see the *NativeFilePicker.ConvertExtensionToFileType* function
  - If no file type is provided, all files can be selected

`NativeFilePicker.PickMultipleFiles( MultipleFilesPickedCallback callback, params string[] allowedFileTypes )`: prompts the user to pick one or more files.
- **MultipleFilesPickedCallback** takes a *string[]* parameter which stores the path(s) of the picked file(s), or *null* if nothing is picked
- Picking multiple files is only available on *Android 18+* and *iOS 11+*. Call *CanPickMultipleFiles()* to see if this feature is available

**NOTE:** on iOS, imported files will automatically be deleted by the OS after the application is closed. If you need the files to persist, move them to *Application.persistentDataPath*.

### B. Exporting Files

`NativeFilePicker.ExportFile( string filePath, FilesExportedCallback callback = null )`: prompts the user to export a file to the available document providers.
- This operation is **asynchronous**! After user exports the file or cancels the operation, the **callback** is called (on main thread). **FilesExportedCallback** takes a *bool* parameter which stores whether user has exported the file or cancelled the operation
- Exporting a file is only available on *Android 19+* and *iOS 8+*. Call *CanExportFiles()* to see if this feature is available

`NativeFilePicker.ExportMultipleFiles( string[] filePaths, FilesExportedCallback callback = null )`: prompts the user to export one or more files.
- Exporting multiple files is only available on *Android 21+* and *iOS 11+*. Call *CanExportMultipleFiles()* to see if this feature is available

All of these functions automatically call *NativeFilePicker.RequestPermissionAsync*. More details available below.

### C. Runtime Permissions

Beginning with *6.0 Marshmallow*, Android apps must request runtime permissions before accessing certain services. There are two functions to handle permissions with this plugin:

`bool NativeFilePicker.CheckPermission( bool readPermissionOnly = false )`: checks whether the app has access to the document providers or not.

`void NativeFilePicker.RequestPermissionAsync( PermissionCallback callback, bool readPermissionOnly = false )`: requests permission to access the document providers from the user and returns the result asynchronously. It is recommended to show a brief explanation before asking the permission so that user understands why the permission is needed and doesn't click Deny or worse, "Don't ask again". Note that the PickFile/PickMultipleFiles and ExportFile/ExportMultipleFiles functions call RequestPermissionAsync internally and execute only if the permission is granted.
- **PermissionCallback** takes `NativeFilePicker.Permission permission` parameter

**NativeFilePicker.Permission** is an enum that can take 3 values: 
- **Granted**: we have the permission to access the document providers
- **ShouldAsk**: permission is denied but we can ask the user for permission once again. As long as the user doesn't select "Don't ask again" while denying the permission, ShouldAsk is returned
- **Denied**: we don't have permission and we can't ask the user for permission. In this case, user has to give the permission from Settings. This happens when user selects "Don't ask again" while denying the permission or when user is not allowed to give that permission (parental controls etc.)

`Task<NativeFilePicker.Permission> NativeFilePicker.RequestPermissionAsync( bool readPermissionOnly = false )`: Task-based overload of *RequestPermissionAsync*.

`NativeFilePicker.OpenSettings()`: opens the settings for this app, from where the user can manually grant the *Storage* permission in case current permission state is *Permission.Denied*.

**NOTE:** on iOS, no permissions are needed and thus, these functions will always return *Permission.Granted*.

### C. Other Functions

`bool NativeFilePicker.CanPickMultipleFiles()`: returns *true* if importing/exporting multiple files is supported (*Android 18+* and *iOS 11+*).

`bool NativeFilePicker.CanExportFiles()`: returns *true* if exporting a single file is supported (*Android 19+* and *iOS 8+*).

`bool NativeFilePicker.CanExportMultipleFiles()`: returns *true* if exporting multiple files is supported (*Android 21+* and *iOS 11+*).

`bool NativeFilePicker.IsFilePickerBusy()`: returns *true* if the user is currently importing/exporting files. In that case, another *PickFile*, *PickMultipleFiles*, *ExportFile* or *ExportMultipleFiles* request will simply be ignored.

`string NativeFilePicker.ConvertExtensionToFileType( string extension )`: converts a file extension to its corresponding *MIME* on Android and *UTI* on iOS (don't include the period in extension, i.e. use *png* instead of *.png*). Throws an exception if "\*" or "\*/\*" is passed to *extension* (to allow all file types, don't pass anything to *PickFile* function's *allowedFileTypes* parameter).

## EXAMPLE CODE

The following code has 4 functions:

- if you click the left one-third of the screen, a single PDF file is picked
- if you click the middle one-third of the screen, one or more image and video files are picked
- if you click the right one-third of the screen, a dummy text file is created and then exported

```csharp
private string pdfFileType;

void Start()
{
	pdfFileType = NativeFilePicker.ConvertExtensionToFileType( "pdf" ); // Returns "application/pdf" on Android and "com.adobe.pdf" on iOS
	Debug.Log( "pdf's MIME/UTI is: " + pdfFileType );
}

void Update()
{
	if( Input.GetMouseButtonDown( 0 ) )
	{
		// Don't attempt to import/export files if the file picker is already open
		if( NativeFilePicker.IsFilePickerBusy() )
			return;

		if( Input.mousePosition.x < Screen.width / 3 )
		{
			// Pick a PDF file
			NativeFilePicker.PickFile( ( path ) =>
			{
				if( path == null )
					Debug.Log( "Operation cancelled" );
				else
					Debug.Log( "Picked file: " + path );
			}, new string[] { pdfFileType } );
		}
		else if( Input.mousePosition.x < Screen.width * 2 / 3 )
		{
#if UNITY_ANDROID
			// Use MIMEs on Android
			string[] fileTypes = new string[] { "image/*", "video/*" };
#else
			// Use UTIs on iOS
			string[] fileTypes = new string[] { "public.image", "public.movie" };
#endif

			// Pick image(s) and/or video(s)
			NativeFilePicker.PickMultipleFiles( ( paths ) =>
			{
				if( paths == null )
					Debug.Log( "Operation cancelled" );
				else
				{
					for( int i = 0; i < paths.Length; i++ )
						Debug.Log( "Picked file: " + paths[i] );
				}
			}, fileTypes );
		}
		else
		{
			// Create a dummy text file
			string filePath = Path.Combine( Application.temporaryCachePath, "test.txt" );
			File.WriteAllText( filePath, "Hello world!" );

			// Export the file
			NativeFilePicker.ExportFile( filePath, ( success ) => Debug.Log( "File exported: " + success ) );
		}
	}
}
```
