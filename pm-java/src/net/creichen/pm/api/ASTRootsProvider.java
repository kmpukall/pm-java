package net.creichen.pm.api;

import java.util.Collection;

import org.eclipse.jdt.core.dom.ASTNode;

public interface ASTRootsProvider {

    Collection<ASTNode> getASTRoots();

}
