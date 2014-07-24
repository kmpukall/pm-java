package net.creichen.pm.analysis;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public final class DefUseUtil {

	private DefUseUtil() {
	}

	public static List<ASTNode> findDefiningNodesUnderNode(
			final ASTNode rootNode) {
		final List<ASTNode> result = new ArrayList<ASTNode>();

		final ASTVisitor visitor = new ASTVisitor() {

			boolean isAnalyzable(final ASTNode node) {
				return node instanceof SimpleName;
			}

			@Override
			public boolean visit(final Assignment assignment) {
				// plain-old x = y + 1

				if (isAnalyzable(assignment.getLeftHandSide())) {
					result.add(assignment);
				}
				return true;
			}

			@Override
			public boolean visit(final PostfixExpression postfixExpression) {
				// all postfix operators are definitions
				// x++
				if (isAnalyzable(postfixExpression.getOperand())) {
					result.add(postfixExpression);
				}
				return true;
			}

			@Override
			public boolean visit(final PrefixExpression prefixExpression) {
				// Can't have things like ! being definitions
				if ((prefixExpression.getOperator() == PrefixExpression.Operator.INCREMENT || prefixExpression
						.getOperator() == PrefixExpression.Operator.DECREMENT)
						&& isAnalyzable(prefixExpression
								.getOperand())) {
					result.add(prefixExpression);
				}

				return true;
			}

			@Override
			public boolean visit(
					final SingleVariableDeclaration singleVariableDeclaration) {
				// Used in parameter lists and catch clauses
				// There is an implicit definition here

				result.add(singleVariableDeclaration);

				return true;
			}

			@Override
			public boolean visit(
					final VariableDeclarationFragment variableDeclarationFragment) {
				// int x, y, z = 7; //etc

				result.add(variableDeclarationFragment);

				return true;
			}

		};

		rootNode.accept(visitor);

		return result;
	}

}
