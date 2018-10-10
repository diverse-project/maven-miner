package fr.inria.diverse.maven.resolver.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Method;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

public class ConnectionShutdownListener implements ShutdownListener{
	
	//private Connection connection;
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerResolverApp.class);
	
	@Override
	public void shutdownCompleted(ShutdownSignalException cause) {
    	   LOGGER.info("Connection shutdown! ");
	       if (cause.isHardError()) {
	    	    //Connection conn = (Connection)cause.getReference();
	    	    if (!cause.isInitiatedByApplication())  {
	    	      Method reason = cause.getReason();
	    	      LOGGER.error("The shutdown was caused by the server! ");
	    	      LOGGER.error(reason.protocolMethodName());
		    	} else {
			    	    Method reason = cause.getReason();
		    	    	LOGGER.info("The shutdown was caused by the application! ");
			    	    LOGGER.info("invoking {}",reason.protocolMethodName());
		    	}
	    	} else {
	    	    //Channel ch = (Channel)cause.getReference();
	    	    LOGGER.error("The shutdown was caused by the application! ");
	    	    LOGGER.error(cause.getMessage());		    					      
	    	}
	    	 
	    	 
	 }
}