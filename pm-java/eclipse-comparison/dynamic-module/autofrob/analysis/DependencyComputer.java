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

public abstract class DependencyComputer extends ASTVisitor
{
	protected Set<IMethod> initial_methods;
	protected Set<IMethod> new_methods;
	protected Set<IField> initial_fields;
	protected Set<IField> new_fields;
	protected ICompilationUnit compilation_unit;

	public void
	computeDependencies(Set<IMethod> initial_methods_arg, Set<IField> initial_fields_arg, ICompilationUnit icu, ASTNode dest)
	{
		this.initial_methods = initial_methods_arg;
		this.initial_fields = initial_fields_arg;

		if (this.initial_methods == null)
			this.initial_methods = new HashSet<IMethod>();
		if (this.initial_fields == null)
			this.initial_fields = new HashSet<IField>();

		this.compilation_unit = icu;

		this.new_methods = new HashSet<IMethod>();
		this.new_fields = new HashSet<IField>();

		dest.accept(this);
	}

	public void
	computeDependencies(Set<IMember> initials, ICompilationUnit icu, ASTNode dest)
	{
		Set<IMethod> methods = new HashSet<IMethod>();
		Set<IField> fields = new HashSet<IField>();

		for (IMember member : initials)
			if (member instanceof IMethod)
				methods.add((IMethod) member);
			else if (member instanceof IField)
				fields.add((IField) member);

		computeDependencies(methods, fields, icu, dest);
	}

	public void
	add(IMethod m)
	{
		if (m != null
		    && m.getCompilationUnit() != null
		    && m.getCompilationUnit().equals(this.compilation_unit)) {
			if (!this.initial_methods.contains(m))
				this.new_methods.add(m);
		}
	}

	public void
	add(IField m)
	{
		if (m != null
		    && m.getCompilationUnit() != null
		    && m.getCompilationUnit().equals(this.compilation_unit)) {
			if (!this.initial_fields.contains(m))
				this.new_fields.add(m);
		}
	}

	public void
	add(IMember m)
	{
		if (m instanceof IField)
			this.add((IField) m);
		else if (m instanceof IMethod)
			this.add((IMethod) m);
	}

	public void
	add(IJavaElement elt)
	{
		if (elt instanceof IMember)
			this.add((IMember) elt);
	}

	protected boolean
	have(IMethod m)
	{
		return this.initial_methods.contains(m);
	}

	protected boolean
	have(IField f)
	{
		return this.initial_fields.contains(f);
	}

	protected boolean
	have(IMember m)
	{
		if (m instanceof IField)
			return this.have((IField) m);
		else if (m instanceof IMethod)
			return this.have((IMethod) m);
		else
			return false;
	}

	protected boolean
	have(IJavaElement elt)
	{
		if (elt instanceof IMember)
			return this.have((IMember) elt);
		else
			return false;
	}

	public Set<IMethod>
	getMethods()
	{
		return this.new_methods;
	}

	public Set<IField>
	getFields()
	{
		return this.new_fields;
	}
}