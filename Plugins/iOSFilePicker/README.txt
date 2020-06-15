= iOS File Picker =

Online documentation & example code available at: https://github.com/yasirkula/UnityiOSFilePicker
E-mail: yasirkula@gmail.com


1. ABOUT
This plugin helps you import files from iCloud and other document providers, or export files to these providers on iOS. It uses UIDocumentPickerViewController which has the following requirements:

- iOS 8 or later
- an Apple Developer Program account (signing the app with a free account won't work)


2. HOW TO
There are two ways to set up the plugin on iOS:

a. Automated Setup for iOS
- set the value of ENABLED to true in iOSFilePickerPostProcessBuild.cs. By default, automated setup is disabled. That's because this plugin uses the iCloud capability and if another plugin uses other capabilities, these plugins will likely conflict with each other. Set this value to true at your own risk
- if your app uses custom file extensions that are unique to your app (e.g. .mydata), add them to the Plugins/iOSFilePicker/CustomTypes asset (it has explanatory tooltips). This step works even if the value of ENABLED is set to false (this step is not needed for extensions available in this list: https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html)

b. Manual Setup for iOS
- after building the Unity project, open the Xcode project
- add MobileCoreServices.framework to Link Binary With Libraries list in Build Phases
- enable iCloud in Capabilities and make sure that at least one of its Services is active
- if your app uses custom file extensions that are unique to your app (e.g. .mydata), add them to the Exported UTIs or Imported UTIs lists in Info (about custom UTIs: https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_declare/understand_utis_declare.html) (this step is not needed for extensions available in this list: https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html)


3. SCRIPTING API
Please see the online documentation for a more in-depth documentation of the Scripting API: https://github.com/yasirkula/UnityiOSFilePicker

delegate void FilePickedCallback( string path );
delegate void MultipleFilesPickedCallback( string[] paths );
delegate void FilesExportedCallback( bool success );

//// Importing Files ////

// This operation is asynchronous! After user picks a file or cancels the operation, the callback is called (on main thread)
// FilePickedCallback takes a string parameter which stores the path of the picked file, or null if nothing is picked
// MultipleFilesPickedCallback takes a string[] parameter which stores the path(s) of the picked file(s), or null if nothing is picked
// allowedUTIs determines which file types are accepted, for example:
//   public.png: PNG files
//   public.image: image files in general (png, jpeg, tiff and etc.)
//   com.adobe.pdf: PDF files
//   see the following list for all available UTIs: https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html
//   also see the iOSFilePicker.ConvertExtensionToUTI function
void iOSFilePicker.PickFile( FilePickedCallback callback, string[] allowedUTIs );
void iOSFilePicker.PickMultipleFiles( MultipleFilesPickedCallback callback, string[] allowedUTIs );

// NOTE: imported files will automatically be deleted by the OS after the application is closed. If you need the files to persist, move them to Application.persistentDataPath


//// Exporting Files ////

// This operation is asynchronous! After user exports the file(s) or cancels the operation, the callback is called (on main thread)
// FilesExportedCallback takes a bool parameter which stores whether user has exported the files or cancelled the operation
// If CanPickMultipleFiles() returns false, only the first file will be exported
void iOSFilePicker.ExportFiles( string[] filePaths, FilesExportedCallback callback = null );


//// Other Functions ////

// Returns true if importing/exporting multiple files is supported (available on iOS 11 and later)
bool iOSFilePicker.CanPickMultipleFiles();

// Returns true if the user is currently importing/exporting files. In that case, another PickFile, PickMultipleFiles or ExportFiles request will simply be ignored
bool iOSFilePicker.IsFilePickerBusy();

// Converts a file extension to its corresponding UTI (don't include the period in extension, i.e. use "png" instead of ".png")
string iOSFilePicker.ConvertExtensionToUTI( string extension );