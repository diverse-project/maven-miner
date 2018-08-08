package fr.inria.diverse.maven.resolver.processor;


import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;

import fr.inria.diverse.maven.resolver.tasks.DependencyVisitorTask;
/**
 * 
 * @author Amine BENELALLAM
 *
 */
public class MultiTaskDependencyVisitor implements DependencyVisitor {
	/**
	 * a set of tasklist the visitor loops over to 
	 */
    private Set<DependencyVisitorTask> taskList;
    /**
     * Adding a task to the task set
     * @param task {@link DependencyVisitorTask}
     * @return {@link Boolean}
     */
    public boolean addTask(@NotNull DependencyVisitorTask task) {
    	return taskList.add(task);
    }
    /**
     * Initializing the different visiting task
     */
    public void taskInit() {
    	taskList.forEach(task -> task.init());
    }
    /**
     * Default constructor
     */
    public MultiTaskDependencyVisitor() {
    	super();
    	taskList = new HashSet<DependencyVisitorTask>();
    }
    /**
     * @see DependencyVisitor#visitEnter(DependencyNode)
     * {@inheritDoc}
     */
    public boolean visitEnter( DependencyNode node) {
    	taskList.forEach(task -> task.enter(node));
        return true;
    }
    /**
     * @see DependencyVisitor#visitLeave(DependencyNode)
     * {@inheritDoc}
     */
    public boolean visitLeave(DependencyNode node) {
        taskList.forEach(task -> task.leave(node));
        return true;
    }
    /**
     * Returns the task set
     * @return
     */
    @NotNull
    public Set<DependencyVisitorTask> getTaskSet() {
    	return taskList;
    }
}
