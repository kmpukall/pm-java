package net.creichen.pm.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

public final class RangeUtil {

    private RangeUtil() {

    }

    public static List<Range<Integer>> rangesBetween(final List<ASTNode> statements) {
        final List<Range<Integer>> ranges = new ArrayList<Range<Integer>>();
        if (statements.isEmpty()) {
            // in this case, any cursor position within the parent node is valid
            final Range<Integer> unboundedRange = Range.all();
            ranges.add(unboundedRange);
        } else {
            ranges.add(Range.atMost(statements.get(0).getStartPosition()));
            for (int i = 0; i < statements.size() - 1; i++) {
                final ASTNode current = statements.get(i);
                final ASTNode next = statements.get(i + 1);
                ranges.add(Range.closed(current.getStartPosition() + current.getLength(), next.getStartPosition()));
            }
            final ASTNode lastStatement = Iterables.getLast(statements);
            ranges.add(Range.atLeast(lastStatement.getStartPosition() + lastStatement.getLength()));
        }
        return ranges;
    }

}
