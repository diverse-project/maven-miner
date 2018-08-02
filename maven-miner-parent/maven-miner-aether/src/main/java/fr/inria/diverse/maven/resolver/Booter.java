
package fr.inria.diverse.maven.resolver;

import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

/**
 * A helper to boot the repository system and a repository system session.
 */
public class Booter {

    public static RepositorySystem newRepositorySystem() {
        return ManualRepositorySystemFactory.newRepositorySystem();
    }

    public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
    	MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));


        return session;
    }

    public static RemoteRepository newCentralRepository() {
        return new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
//        return new RemoteRepository("central", "default", "http://jtechbd-cldsrvc.cloudapp.net:8090/nexus/content/repositories/maven");
//        return new RemoteRepository("central", "default", "http://jtechbd-nexus:8090/nexus/content/repositories/maven");

    }
}
