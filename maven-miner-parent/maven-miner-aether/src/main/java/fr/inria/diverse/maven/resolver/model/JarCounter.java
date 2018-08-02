package fr.inria.diverse.maven.resolver.model;

public class JarCounter {

	private int methods;
	private int classes;
	private int interfaces;
	private int enums;
	private int annotations;
	/**
	 * @return the annotations
	 */
	public int getAnnotations() {
		return annotations;
	}

	/**
	 * @return the methods
	 */
	public int getMethods() {
		return methods;
	}
	
	/**
	 * @return the classes
	 */
	public int getClasses() {
		return classes;
	}
	
	/**
	 * @return the interfaces
	 */
	public int getInterfaces() {
		return interfaces;
	}

	/**
	 * @return the enums
	 */
	public int getEnums() {
		return enums;
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
		this.methods = methods;
		this.classes = classes;
		this.interfaces = interfaces;
		this.enums = enums;
		this.annotations = annotations;
	}

	
}
