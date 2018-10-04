package fr.inria.diverse.maven.resolver.model;

/**
 * 
 * @author Amine BENELALLAM
 *
 */
public class JarCounter {
	/**
	 * entries count
	 */
	private final int [] entries = new int [JarEntryType.values().length + 1];

	/**
	 * 
	 */
	public int getValueForType(JarEntryType type) {
		return entries[type.getIndex()];	
	}

	/**
	 * @return the annotations
	 */
	public int getAnnotations() {
		return entries[JarEntryType.ANNOTATION.getIndex()];	
	}

	/**
	 * @return the methods
	 */
	public int getMethods() {
		return entries[JarEntryType.METHOD.getIndex()];	
	}
	
	/**
	 * @return the classes
	 */
	public int getClasses() {
		return entries[JarEntryType.CLASS.getIndex()];	

	}
	
	/**
	 * @return the interfaces
	 */
	public int getInterfaces() {
		return entries[JarEntryType.INTERFACE.getIndex()];	

	}

	/**
	 * @return the enums
	 */
	public int getEnums() {
		return entries[JarEntryType.ENUM.getIndex()];	
	}
	/**
	 * Constructor
	 * @param methods
	 * @param classes
	 * @param interfaces
	 * @param enums
	 * @param annotations
	 */
	public JarCounter(int methods, int classes, int interfaces, int enums, int annotations) {
		super();
		entries[JarEntryType.METHOD.getIndex()] = methods;
		entries[JarEntryType.CLASS.getIndex()] = classes;
		entries[JarEntryType.INTERFACE.getIndex()] = interfaces;
		entries[JarEntryType.ENUM.getIndex()] = enums;
		entries[JarEntryType.ANNOTATION.getIndex()] = annotations;
	}
	/**
	 * 
	 * @author Amine BENELALLAM
	 *
	 */
	public enum JarEntryType {
		
		CLASS("class_count",0),
		INTERFACE("interface_count",1),
		ENUM("enum_count",2),
		ANNOTATION("annotation_count",3),
		METHOD("method_count",4);
		
		private JarEntryType(String name, int index) {
			this.name = name;
			this.index = index;
		}
	
		/*
		 * The name of the jar entry
		 */
		private String name;
		/**
		 * The index of the jar entry
		 */
		private int index;
		
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @return the index
		 */
		public int getIndex() {
			return index;
		}
	
	}
	
}
