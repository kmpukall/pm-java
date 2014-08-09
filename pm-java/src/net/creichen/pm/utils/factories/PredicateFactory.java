package net.creichen.pm.utils.factories;

import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.base.Predicate;

public class PredicateFactory {

    private static final Predicate<TypeDeclaration> NOT_INTERFACE = new Predicate<TypeDeclaration>() {

        @Override
        public boolean apply(final TypeDeclaration type) {
            return !type.isInterface();
        }
    };

    public static Predicate<TypeDeclaration> isNotInterface() {
        return NOT_INTERFACE;
    }

    public static Predicate<TypeDeclaration> hasClassName(final String className) {
        return new Predicate<TypeDeclaration>() {

            @Override
            public boolean apply(final TypeDeclaration type) {
                return type.getName().getIdentifier().equals(className);
            }
        };
    }

    public static Predicate<VariableDeclarationFragment> hasVariableName(final String className) {
        return new Predicate<VariableDeclarationFragment>() {

            @Override
            public boolean apply(final VariableDeclarationFragment type) {
                return type.getName().getIdentifier().equals(className);
            }
        };
    }

}
