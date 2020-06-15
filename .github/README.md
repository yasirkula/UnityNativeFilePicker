# Unity iOS File Picker Plugin

**Forum Thread:** https://forum.unity.com/threads/native-file-picker-for-ios-using-uidocumentpickerviewcontroller-open-source.912890/

**[Support the Developer ☕](https://yasirkula.itch.io/unity3d)**

This plugin helps you import files from *iCloud* and other document providers, or export files to these providers on iOS. It uses **UIDocumentPickerViewController** which has the following requirements:

- iOS 8 or later
- an Apple Developer Program account (signing the app with a free account won't work)

## INSTALLATION

There are 4 ways to install this plugin:

- import [iOSFilePicker.unitypackage](https://github.com/yasirkula/UnityiOSFilePicker/releases) via *Assets-Import Package*
- clone/[download](https://github.com/yasirkula/UnityiOSFilePicker/archive/master.zip) this repository and move the *Plugins* folder to your Unity project's *Assets* folder
- *(via Package Manager)* add the following line to *Packages/manifest.json*:
  - `"com.yasirkula.iosfilepicker": "https://github.com/yasirkula/UnityiOSFilePicker.git",`
- *(via [OpenUPM](https://openupm.com))* after installing [openupm-cli](https://github.com/openupm/openupm-cli), run the following command:
  - `openupm add com.yasirkula.iosfilepicker`

### Setup

There are two ways to set up the plugin on iOS:

**a. Automated Setup for iOS**

- set the value of **ENABLED** to *true* in *iOSFilePickerPostProcessBuild.cs*. By default, automated setup is disabled. That's because this plugin uses the *iCloud capability* and if another plugin uses other capabilities, these plugins will likely conflict with each other. Set this value to true at your own risk
- if your app uses custom file extensions that are unique to your app (e.g. *.mydata*), add them to the *Plugins/iOSFilePicker/CustomTypes* asset (it has explanatory tooltips). This step works even if the value of *ENABLED* is set to *false* (this step is not needed for extensions available in [this list](https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html))

**b. Manual Setup for iOS**

- see: https://github.com/yasirkula/UnityiOSFilePicker/wiki/Manual-Setup-for-iOS

## HOW TO

### A. Importing Files

`void iOSFilePicker.PickFile( FilePickedCallback callback, string[] allowedUTIs )`: prompts the user to pick a file from *iCloud* or other available document providers.
- This operation is **asynchronous**! After user picks a file or cancels the operation, the **callback** is called (on main thread). **FilePickedCallback** takes a *string* parameter which stores the path of the picked file, or *null* if nothing is picked
- **allowedUTIs** determines which file types are accepted, for example:
  - *public.png*: PNG files
  - *public.image*: image files in general (png, jpeg, tiff and etc.)
  - *com.adobe.pdf*: PDF files
  - see the following list for all available UTIs: https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html
  - also see the *iOSFilePicker.ConvertExtensionToUTI* function

`void iOSFilePicker.PickMultipleFiles( MultipleFilesPickedCallback callback, string[] allowedUTIs )`: prompts the user to pick one or more files. **MultipleFilesPickedCallback** takes a *string[]* parameter which stores the path(s) of the picked file(s), or *null* if nothing is picked. Picking multiple files is only available on *iOS 11* and later. Call *CanPickMultipleFiles()* to see if this feature is available.

**NOTE:** imported files will automatically be deleted by the OS after the application is closed. If you need the files to persist, move them to *Application.persistentDataPath*.

### B. Exporting Files

`void iOSFilePicker.ExportFiles( string[] filePaths, FilesExportedCallback callback = null )`: prompts the user to export one or more files to *iCloud* or other available document providers.
- This operation is **asynchronous**! After user exports the file(s) or cancels the operation, the **callback** is called (on main thread). **FilesExportedCallback** takes a *bool* parameter which stores whether user has exported the files or cancelled the operation
- If *CanPickMultipleFiles()* returns *false*, only the first file will be exported

### C. Other Functions

`bool iOSFilePicker.CanPickMultipleFiles()`: returns *true* if importing/exporting multiple files is supported (available on *iOS 11* and later).

`bool iOSFilePicker.IsFilePickerBusy()`: returns *true* if the user is currently importing/exporting files. In that case, another *PickFile*, *PickMultipleFiles* or *ExportFiles* request will simply be ignored.

`string iOSFilePicker.ConvertExtensionToUTI( string extension )`: converts a file extension to its corresponding *UTI* (don't include the period in extension, i.e. use *png* instead of *.png*).

## EXAMPLE CODE

The following code has three functions:

- if you click the left one-third of the screen, a single PDF file is picked
- if you click the middle one-third of the screen, one or more JPEG/PNG files are picked
- if you click the right one-third of the screen, a dummy text file is created and then exported

```csharp
private string pdfUTI;

void Start()
{
	pdfUTI = iOSFilePicker.ConvertExtensionToUTI( "pdf" ); // Returns com.adobe.pdf
	Debug.Log( "pdf's UTI is: " + pdfUTI );
}

void Update()
{
	if( Input.GetMouseButtonDown( 0 ) )
	{
		// Don't attempt to import/export files if the file picker is already open
		if( iOSFilePicker.IsFilePickerBusy() )
			return;

		if( Input.mousePosition.x < Screen.width / 3 )
		{
			// Pick a PDF file
			iOSFilePicker.PickFile( ( path ) =>
			{
				if( path == null )
					Debug.Log( "Operation cancelled" );
				else
					Debug.Log( "Picked file: " + path );
			}, new string[] { pdfUTI } );
		}
		else if( Input.mousePosition.x < Screen.width * 2 / 3 )
		{
			// Pick JPEG and/or PNG file(s)
			iOSFilePicker.PickMultipleFiles( ( paths ) =>
			{
				if( paths == null )
					Debug.Log( "Operation cancelled" );
				else
				{
					for( int i = 0; i < paths.Length; i++ )
						Debug.Log( "Picked file: " + paths[i] );
				}
			}, new string[] { "public.jpeg", "public.png" } );
		}
		else
		{
			// Create a dummy text file
			string filePath = Path.Combine( Application.temporaryCachePath, "test.txt" );
			File.WriteAllText( filePath, "Hello world!" );

			// Export the file
			iOSFilePicker.ExportFiles( new string[] { filePath }, ( success ) => Debug.Log( "File(s) exported: " + success ) );
		}
	}
}
```
