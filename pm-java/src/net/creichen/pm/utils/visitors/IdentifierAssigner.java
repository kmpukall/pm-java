package net.creichen.pm.utils.visitors;

import java.util.HashMap;
import java.util.Map;

import net.creichen.pm.models.NameModel;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

public class IdentifierAssigner extends ASTVisitor {
    // We should visit more than simle names here
    // We also care about field accesses, right?
    // method invocations, etc.??
    final Map<IBinding, String> identifiersForBindings = new HashMap<IBinding, String>();

    private final Map<Name, String> identifiers = new HashMap<Name, String>();

    public final Map<Name, String> getIdentifiers() {
        return this.identifiers;
    }

    @Override
    public boolean visit(final SimpleName simpleName) {
        IBinding binding = simpleName.resolveBinding();

        if (binding instanceof IVariableBinding) {
            binding = ((IVariableBinding) binding).getVariableDeclaration();
        }

        if (binding instanceof ITypeBinding) {
            binding = ((ITypeBinding) binding).getTypeDeclaration();
        }

        if (binding instanceof IMethodBinding) {
            binding = ((IMethodBinding) binding).getMethodDeclaration();
        }

        String identifier = this.identifiersForBindings.get(binding);

        if (identifier == null) {
            identifier = NameModel.generateNewIdentifier();
            this.identifiersForBindings.put(binding, identifier);
        }
        this.identifiers.put(simpleName, identifier);
        return true;
    }
}