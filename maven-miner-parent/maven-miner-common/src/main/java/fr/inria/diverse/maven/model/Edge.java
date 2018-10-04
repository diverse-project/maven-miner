package fr.inria.diverse.maven.model;

import java.util.UUID;

import javax.validation.constraints.NotNull;

public class Edge {
	
	private final UUID source;
	private final UUID target;
	private Scope scope;
	
	public Edge(final UUID source, final UUID target, Scope scope) {
		this.source = source;
		this.target = target;
		this.setScope(scope);
	}
	
	public Edge(final UUID source, final UUID target) {
		this.source = source;
		this.target = target;
		this.setScope(Scope.Compile);
	}
	

	public UUID getSource() {
		return source;
	}


	public UUID getTarget() {
		return target;
	}


	public Scope getScope() {
		return scope;
	}

	public void setScope(@NotNull final Scope scope) {
		this.scope = scope;
	}


	public enum Scope {
	    Compile,
	    Provided,
	    Runtime,
	    Test,
	    System,
	    Import;

	    public static Scope parseFromString(@NotNull final String scope) {
	        switch (scope) {
	            case "compile":
	                return Scope.Compile;
	            case "provided":
	                return Scope.Provided;
	            case "import":
	                return Scope.Import;
	            case "runtime":
	                return Scope.Runtime;
	            case "system":
	                return Scope.System;
	            case "test":
	                return Scope.Test;
	            default:
	                return Scope.Compile;
	        }
	    }
	}
}
