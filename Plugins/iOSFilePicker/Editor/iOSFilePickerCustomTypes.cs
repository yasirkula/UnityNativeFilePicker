using UnityEditor;
using UnityEngine;

namespace iOSFilePickerNamespace
{
	[HelpURL( "https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_declare/understand_utis_declare.html" )]
	public class iOSFilePickerCustomTypes : ScriptableObject
	{
		[System.Serializable]
		public class TypeHolder
		{
			[Tooltip( "A unique identifier for the type (UTI) (e.g. com.mycompany.mydata)" )]
			public string identifier;
			[Tooltip( "Description of the type (e.g. Save data for My App)" )]
			public string description;
			[Tooltip( "If this type is owned by your app (i.e. exclusive to your app), set this value to true. If another app generates files of this type and your app is merely importing/modifying them, set this value to false." )]
			public bool isExported;
			[Tooltip( "Which official UTI(s) is this type part of (e.g. public.data for generic types or public.image for image types)\nFull list available at: https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html" )]
			public string[] conformsTo;
			[Tooltip( "Extension(s) associated with this type (don't include the period; i.e. use myextension instead of .myextension)" )]
			public string[] extensions;
		}

#pragma warning disable 0649
		[SerializeField]
		private TypeHolder[] customTypes;
#pragma warning restore 0649

		public static TypeHolder[] GetCustomTypes()
		{
			string[] instances = AssetDatabase.FindAssets( "t:iOSFilePickerCustomTypes" );
			if( instances != null && instances.Length > 0 )
			{
				iOSFilePickerCustomTypes instance = AssetDatabase.LoadAssetAtPath<iOSFilePickerCustomTypes>( AssetDatabase.GUIDToAssetPath( instances[0] ) );
				if( instance )
					return instance.customTypes;
			}

			return null;
		}
	}
}