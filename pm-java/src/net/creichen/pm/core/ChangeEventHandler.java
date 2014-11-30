package net.creichen.pm.core;

import net.creichen.pm.consistency.ConsistencyValidator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class ChangeEventHandler implements IResourceChangeListener {

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        System.out.println("Resource has changed - visiting deltas...");
        try {
            event.getDelta().accept(new IResourceDeltaVisitor() {
                @Override
                public boolean visit(IResourceDelta delta) throws CoreException {
                    IResource resource = delta.getResource();
                    System.out.println("checking resource " + resource.getName());
                    if (resource.getType() == IResource.PROJECT) {
                        IProject iProject = (IProject) resource;
                        if (iProject.hasNature(JavaCore.NATURE_ID)) {
                            IJavaProject targetProject = JavaCore.create(iProject);
                            Project project = Workspace.getInstance().getProject(targetProject);
                            ConsistencyValidator.getInstance().rescanForInconsistencies(project);
                        }
                        return false;
                    }
                    return true;
                }
            });
        } catch (CoreException e) {
            System.err.println("An error occurred when visiting changed resource delta: " + e.getMessage());
        }
    }

}
