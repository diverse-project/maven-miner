package fr.inria.diverse.maven.resolver.processor.amqtasks;

import java.util.Properties;

public interface Task {
	void init(Properties config);
	void run();
	void stop();
}
