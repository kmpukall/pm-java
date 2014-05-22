/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.analysis;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class TransitiveClosureInAST
{
	protected Set<IMethod> methods;
	protected Set<IField> fields;
	protected ICompilationUnit compilation_unit;
	protected ASTNode root;
	protected DependencyComputer dep_comp;

	public TransitiveClosureInAST(DependencyComputer dep_comp,
				      Set<IMethod> initial_methods, Set<IField> initial_fields,
				      ICompilationUnit icu, ASTNode dest)
	{
		this.dep_comp = dep_comp;
		this.methods = initial_methods;
		this.fields = initial_fields;
		this.compilation_unit = icu;
		this.root = dest;

		if (this.methods == null)
			this.methods = new HashSet<IMethod>();
		if (this.fields == null)
			this.fields = new HashSet<IField>();

		doCompute();
	}

	public TransitiveClosureInAST(DependencyComputer dep_comp,
				      Set<IMember> initials,
				      ICompilationUnit icu, ASTNode dest)
	{
		this.dep_comp = dep_comp;
		this.methods = new HashSet<IMethod>();
		this.fields = new HashSet<IField>();
		this.compilation_unit = icu;
		this.root = dest;

		for (IMember m : initials)
			if (m instanceof IMethod)
				this.methods.add((IMethod) m);
			else if (m instanceof IField)
				this.fields.add((IField) m);

		if (this.methods == null)
			this.methods = new HashSet<IMethod>();
		if (this.fields == null)
			this.fields = new HashSet<IField>();

		doCompute();
	}

	public Set<IMethod>
	getMethods()
	{
		return this.methods;
	}

	public Set<IField>
	getFields()
	{
		return this.fields;
	}

	public void
	doCompute()
	{
		Set<IMethod> new_methods;
		Set<IField> new_fields;

		do {
			dep_comp.computeDependencies(this.methods, this.fields, this.compilation_unit, this.root);

			new_methods = dep_comp.getMethods();
			new_fields = dep_comp.getFields();

			this.methods.addAll(new_methods);
			this.fields.addAll(new_fields);
		} while (new_methods.size() > 0
			 || new_fields.size() > 0);
	}
}