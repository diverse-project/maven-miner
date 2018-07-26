package fr.inria.diverse.maven.resolver;


import java.util.HashSet;
import java.util.Set;

import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;

import fr.inria.diverse.maven.resolver.tasks.DependencyVisitorTask;

public class MultiTaskDependencyVisitor implements DependencyVisitor {

    private Set<DependencyVisitorTask> taskList;
    
    public boolean addTask(DependencyVisitorTask task) {
    	return taskList.add(task);
    }
    public void taskInit() {
    	taskList.forEach(task -> task.init());
    }
    public MultiTaskDependencyVisitor() {
    	super();
    	taskList = new HashSet<DependencyVisitorTask>();
    }
    
    public boolean visitEnter(DependencyNode node) {
    	taskList.forEach(task -> task.enter(node));
        return true;
    }
    
    public boolean visitLeave(DependencyNode node) {
        taskList.forEach(task -> task.leave(node));
        return true;
    }
    
    public Set<DependencyVisitorTask> getTasksList() {
    	return taskList;
    }
}
