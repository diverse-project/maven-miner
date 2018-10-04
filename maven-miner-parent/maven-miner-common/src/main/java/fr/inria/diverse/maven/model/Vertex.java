package fr.inria.diverse.maven.model;

import java.util.Set;
import java.util.UUID;

public class Vertex {
	
		private final UUID id; 
		private final String groupId;
		private final String artifactId;
		private String version;
		private String classifier;
		private Packaging packaging;
		
		public Vertex(UUID id, Set<UUID> dependencies, String groupId, String artifactId, String version,
				String classifier, Packaging packaging) {
			super();
			this.id = id;
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
			this.classifier = classifier;
			this.packaging = packaging;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		public String getClassifier() {
			return classifier;
		}
		public void setClassifier(String classifier) {
			this.classifier = classifier;
		}
		public Packaging getPackaging() {
			return packaging;
		}
		public void setPackaging(Packaging packaging) {
			this.packaging = packaging;
		}
		public UUID getId() {
			return id;
		}
		public String getGroupId() {
			return groupId;
		}
		public String getArtifactId() {
			return artifactId;
		}
		public Vertex( String groupId, String artifactId, String version,String classifier) {
			super();
			this.id = UUID.randomUUID();
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
			this.classifier = classifier;
			this.packaging = Packaging.Jar;
		}
		public Vertex(String groupId, String artifactId,  String version, String classifier, Packaging packaging) {
			super();
			this.id = UUID.randomUUID();
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
			this.classifier = classifier;
		}
		public enum Packaging {
		    Jar, War, Ear, Pom;

		    public static Packaging parseFromString(String packaging) {
		        switch (packaging) {
		            case "jar":
		                return Packaging.Jar;
		            case "war":
		                return Packaging.War;
		            case "ear":
		                return Packaging.Ear;
		            case "pom":
		                return Packaging.Pom;
		            default: // Use default packaging "jar"
		                return Packaging.Jar;
		        }
		    }
		}
	}
