package fr.inria.diverse.maven.resolver.tasks;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.sonatype.aether.graph.DependencyNode;


public class DependencyGraphPrettyPrinterTask implements DependencyVisitorTask {
	
	private PrintStream stream;
	
	private String currentIndent = "";
	    
	private int currentIndentation = 0;
	
	public DependencyGraphPrettyPrinterTask() {
		this.stream = System.out;
	}
	public DependencyGraphPrettyPrinterTask(PrintStream stream) {
    	this.stream = stream;
	}
	public DependencyGraphPrettyPrinterTask (String file) {
		try {
			this.stream = new PrintStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public PrintStream getStream() {
		return stream;
	}

	public void setStream(PrintStream stream) {
		this.stream = stream;
	}

	public String getCurrentIndent() {
		return currentIndent;
	}

	public void setCurrentIndent(String currentIndent) {
		this.currentIndent = currentIndent;
	}

	public int getCurrentIndentation() {
		return currentIndentation;
	}

	public void setCurrentIndentation(int currentIndentation) {
		this.currentIndentation = currentIndentation;
	}
	@Override
	public void enter(DependencyNode node) {
        currentIndentation += 1;
        stream.println(currentIndent + node + "(" + currentIndentation + ")");
        if (currentIndent.length() <= 0) {
            currentIndent = "+- ";
        } else {
            currentIndent = "|  " + currentIndent;
        }
	}
	@Override
	public void leave(DependencyNode node) {
		 currentIndent = currentIndent.substring(3, currentIndent.length());
	     currentIndentation -= 1;
	}
	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}
	
	
}
