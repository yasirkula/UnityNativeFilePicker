package com.yasirkula.unity;

public interface NativeFilePickerResultReceiver
{
	void OnFilePicked( String path );
	void OnMultipleFilesPicked( String paths );
	void OnFilesExported( boolean result );
}