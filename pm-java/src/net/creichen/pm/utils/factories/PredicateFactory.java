package net.creichen.pm.utils.factories;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import com.google.common.base.Predicate;

public class PredicateFactory {

    private static final Predicate<TypeDeclaration> NOT_INTERFACE = new Predicate<TypeDeclaration>() {

        @Override
        public boolean apply(final TypeDeclaration type) {
            return !type.isInterface();
        }
    };

    public static Predicate<TypeDeclaration> hasClassName(final String className) {
        return new Predicate<TypeDeclaration>() {

            @Override
            public boolean apply(final TypeDeclaration type) {
                return type.getName().getIdentifier().equals(className);
            }
        };
    }

    public static Predicate<MethodDeclaration> hasMethodName(final String methodName) {
        return new Predicate<MethodDeclaration>() {

            @Override
            public boolean apply(MethodDeclaration method) {
                return method.getName().getIdentifier().equals(methodName);
            }
        };
    }

    public static Predicate<VariableDeclaration> hasVariableName(final String className) {
        return new Predicate<VariableDeclaration>() {

            @Override
            public boolean apply(final VariableDeclaration type) {
                return type.getName().getIdentifier().equals(className);
            }
        };
    }

    public static Predicate<TypeDeclaration> isNotInterface() {
        return NOT_INTERFACE;
    }

}
