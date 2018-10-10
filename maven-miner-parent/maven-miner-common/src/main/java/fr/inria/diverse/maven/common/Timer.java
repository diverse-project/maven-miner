package fr.inria.diverse.maven.common;

public class Timer {
	  /**
	   * start time 	
	   */
	  private long startTime = 0;
	  /**
	   * end time 
	   */
	  private long endTime   = 0;
	  /**
	   * Starting the timer
	   */
	  boolean isPaused = false;
	  public void start(){
	    this.startTime = System.currentTimeMillis();
	  }
	  /**
	   * Pausing the timer
	   */
	  public void pause() {
		  this.endTime = System.currentTimeMillis();
		  isPaused = true;
	  }
	  /**
	   * restarting the timer
	   */
	  public void restart() {
		  startTime = 0;
		  endTime = 0;
		  isPaused = false;
	  }
	  /**
	   * resuming the timer
	   */
	  public void resume() {
		  this.startTime =  System.currentTimeMillis() - (endTime - startTime);
		  isPaused = false;
	  }
	  /**
	   * 
	   * @return the elapsed time as {@link Long}. 
	   * If the timer was paused, it returns the latest value before pausing
	   */
	  public long elapsed() {
		  if (isPaused) return endTime - startTime;
		  return System.currentTimeMillis() - startTime;
	  }
	  
	}
