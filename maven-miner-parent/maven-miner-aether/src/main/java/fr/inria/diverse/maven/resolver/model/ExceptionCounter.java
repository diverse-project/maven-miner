package fr.inria.diverse.maven.resolver.model;

import org.sonatype.aether.resolution.ArtifactResolutionException;

/**
 * 
 * @author Amine Benelallam
 * 
 */
public class ExceptionCounter {
	/**
	 * exceptions count
	 */
	private final int [] exceptions = new int[ExceptionType.values().length + 1];
	/**
	 * Constructor
	 * @param classCastCount
	 * @param classNoDefCount
	 * @param classNotFound
	 * @param securityCount
	 * @param nullPointerCount
	 * @param otherCount
	 */
	public ExceptionCounter(int classNoDefCount, 
							int illegalAccess, 
							int securityCount,
							int nullPointerCount, 
							int otherCount) {
		
		setIllegalAccessCount(illegalAccess);
		setNoClassDefCount(classNoDefCount);
		setNullPointerCount(nullPointerCount);
		setSecurityCount(securityCount);
		setOther(otherCount);
		
	}
	public int getValueForType(ExceptionType type) {
		return exceptions[type.getIndex()];	
	}
	/**
	 * 
	 * @param value int
	 */
	private void setOther(int value) {
		exceptions[ExceptionType.NOCLASSDEF.getIndex()] = value;	
	
	}
	/**
	 * @return the classNotFoundCount
	 */
	private void setIllegalAccessCount(int value) {	
		exceptions[ExceptionType.ILLEGALACCESS.getIndex()] = value;	
	}

	/**
	 * @return the nullPointerCount
	 */
	private void setNullPointerCount(int value) {	
		exceptions[ExceptionType.NULLPOINTER.getIndex()] = value;	
	}

	/**
	 * @return the noClassDefCount
	 */
	private void setNoClassDefCount(int value) {	
		exceptions[ExceptionType.NOCLASSDEF.getIndex()] = value;	
	}

	/**
	 * @return the securityCount
	 */
	private void setSecurityCount(int value) {
		exceptions[ExceptionType.SECURITY.getIndex()] = value;		
	}


	/**
	 * Supported Exceptions
	 * @author Amine BENELALLAM
	 *
	 */
	public enum ExceptionType {

		ILLEGALACCESS (IllegalAccessError.class.getName(),0),
		NULLPOINTER(NullPointerException.class.getName(),1),
		NOCLASSDEF(NoClassDefFoundError.class.getName(),2),
		SECURITY(SecurityException.class.getName(),3),
		OTHER("OtherExceptions",4), 
		RESOLUTION(ArtifactResolutionException.class.getName(),5);
		
		/**
		 * The displayed name, referring to the original class
		 */
		private String name;
		/**
		 * The index on the table
		 */
		private int index; 
		/**
		 * Constructor
		 * @param name {@link String}
		 * @param index {@link Integer}
		 */
		private ExceptionType (String name, int index) {
			this.name=name;
			this.index=index;
		}		
		/**
		 * returns the name 
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * returns the index
		 * @return
		 */
		public int getIndex() {
			return index;
		}
	}
	
}
