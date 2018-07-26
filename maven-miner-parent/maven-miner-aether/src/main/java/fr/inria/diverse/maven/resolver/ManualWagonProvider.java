package fr.inria.diverse.maven.resolver;

import org.apache.maven.wagon.Wagon;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.maven.wagon.AhcWagon;

public class ManualWagonProvider implements WagonProvider{

	@Override
	public Wagon lookup(String str) throws Exception {
		 return "http".equals(str) ? new AhcWagon() : null;
	}

	@Override
	public void release(Wagon arg0) {
		
	}

}
