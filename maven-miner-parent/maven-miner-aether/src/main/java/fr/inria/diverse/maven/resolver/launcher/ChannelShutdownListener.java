package fr.inria.diverse.maven.resolver.launcher;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Method;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

public class ChannelShutdownListener implements ShutdownListener{
	/**
	 * A rabbitMQ connection
	 */
	private Connection connection;
	/**
	 * A Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerResolverApp.class);
	/**
	 * Public constructor
	 * @param connection
	 */
	public ChannelShutdownListener(Connection connection) {
		this.connection = connection;
	}
	/**
	 * @see ShutdownListener#shutdownCompleted(ShutdownSignalException)
	 */
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
	    	 
	    	try {
				connection.close();
			} catch (IOException e) {
				LOGGER.error("There has been some issues closing the RabbitMQ connection");
			}
	    	 
	 }
}