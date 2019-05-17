package fr.inria.diverse.maven.resolver.processor.testfinder;

import org.apache.maven.model.Model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenHelper {

	// Pattern corresponding to maven properties ${propertyName}
	private static Pattern mavenProperty = Pattern.compile("\\$\\{.*\\}");

	/*public static String replaceVarIfExist(Model model, String url) {
		if()

		return str.contains(str);
	}*/

	/**
	 * Extract the variable from a string
	 */
	public static String extractVariable(Model model, String value) {
		String val = value;
		if (value != null && value.contains("$")) {
			Matcher matcher = mavenProperty.matcher(value);
			while (matcher.find()) {
				String var = matcher.group();
				val = val.replace(var, getProperty(model, var.substring(2, var.length() - 1)));
			}
		}
		return val;
	}

	/**
	 * Get the value of a property
	 * @param key the key of the property
	 * @return the property value if key exists or null
	 */
	public static String getProperty(Model model, String key) {
		if ("project.version".equals(key)  || "pom.version".equals(key)) {
			if (model.getVersion() != null) {
				return model.getVersion();
			} else if (model.getParent() != null) {
				return model.getParent().getVersion();
			}
		} else if ("project.groupId".equals(key) || "pom.groupId".equals(key)) {
			if (model.getGroupId() != null) {
				return model.getGroupId();
			} else if (model.getParent() != null) {
				return model.getParent().getGroupId();
			}
		} else if ("project.artifactId".equals(key)  || "pom.artifactId".equals(key)) {
			if (model.getArtifactId() != null) {
				return model.getArtifactId();
			} else if (model.getParent() != null) {
				return model.getParent().getArtifactId();
			}
		}
		String value = extractVariable(model, model.getProperties().getProperty(key));
		/*if (value == null) {
			if (parent == null) {
				return null;
			}
			return parent.getProperty(key);
		}*/
		return value;
	}
}
