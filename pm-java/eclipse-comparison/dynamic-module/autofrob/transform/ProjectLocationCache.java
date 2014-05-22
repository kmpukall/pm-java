/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.transform;

import org.eclipse.jdt.apt.core.build.JdtApt;
import org.eclipse.core.resources.*;
import org.eclipse.debug.core.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.apt.core.internal.AptPlugin;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.junit.model.*;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.debug.core.*;
import org.eclipse.debug.internal.core.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;

import autofrob.model.*;

import java.util.*;
import java.io.File;

public final class ProjectLocationCache
{
	private int offset_counter = 0; // only used in deterministic mode
	private static final boolean DETERMINISTIC_MODE = false; // `random nodes' are really just picked in sequence (for testing)

	private static final HashMap<TProject, ProjectLocationCache> plcache_map = new HashMap<TProject, ProjectLocationCache>();

	private HashMap<LocationNGenerator, ProjectLocationInfo> location_count_map = new HashMap<LocationNGenerator, ProjectLocationInfo>();
	private TProject project;
	Random random = new Random();

	public static ProjectLocationCache
	find(TProject project)
	{
		if (DETERMINISTIC_MODE) {
			System.err.println("   ----------------------------------------------------------   ");
			System.err.println("  --========================================================--  ");
			System.err.println(" -- PROJECT LOCATION CACHE IN DETERMINISTIC MODE : DEBUGGING -- ");
			System.err.println("  --========================================================--  ");
			System.err.println("   ----------------------------------------------------------   ");
		}
		if (plcache_map.containsKey(project))
			return plcache_map.get(project);
		else {
			ProjectLocationCache pl_cache = new ProjectLocationCache(project);
			plcache_map.put(project, pl_cache);
			return pl_cache;
		}
	}

	private
	ProjectLocationCache(TProject project)
	{
		this.project = project;
	}

	private void
	init_types_info(ProjectLocationInfo pli)
	{
		final LinkedList<IType> types = this.project.getTypes();
		int pairs_nr = 0;

		for (IType t : types)
			pairs_nr += this.project.getSupertypes(t).size();

		pli.total_types_nr = types.size();
		pli.total_super_subtype_pairs_nr = pairs_nr;
	}

	private ProjectLocationInfo
	getLocationInfo(LocationNGenerator gen)
	{
		if (location_count_map.containsKey(gen))
			return location_count_map.get(gen);

		ProjectLocationInfo pli = new ProjectLocationInfo();

		int total_count = 0;

		for (ICompilationUnit cu : this.project.getCompilationUnits()) {
			ASTNode ast = project.parseCompilationUnit(cu);
			if (ast == null)
				throw new RuntimeException("PMProject reported a null AST for compilation unit " + cu);
			int count = gen.allLocations(ast).size();
			CUData cd = new CUData(total_count, count);
			pli.locations_map.put(cu.getHandleIdentifier(), cd);
			total_count += count;
		}
		pli.total_locations = total_count;

		location_count_map.put(gen, pli);

		init_types_info(pli);

		return pli;
	}

	public int
	getRandom(int modulo)
	{
		if (DETERMINISTIC_MODE)
			return ++offset_counter % modulo;
		else
			return random.nextInt(modulo);
	}

	public LinkedList<ASuperSubtypePair>
	genSuperSubtypePairs()
	{
		LinkedList<ASuperSubtypePair> results = new LinkedList<ASuperSubtypePair>();

		for (IType subtype : this.project.getTypes())
			for (IType supertype : this.project.getSupertypes(subtype))
				results.add(new ASuperSubtypePair(this.project, supertype, subtype));

		return results;
	}

	public Location
	genRandomLocation(LocationNGenerator gen)
	{
		ProjectLocationInfo pli = getLocationInfo(gen);

		if (pli.total_locations == 0)
			throw new RuntimeException("No matching location for " + gen.getClass() + " in entire project");

		int index = getRandom(pli.total_locations);

		return new Location(gen, index);
	}

	public Location
	genRandomLocationIn(LocationNGenerator gen, ICompilationUnit cu)
	{
		ProjectLocationInfo pli = getLocationInfo(gen);
		if (cu == null)
			throw new RuntimeException("No compilation unit?");
		if (pli == null)
			throw new RuntimeException("No PLI?");
		CUData d = pli.locations_map.get(cu.getHandleIdentifier());

		if (d == null)
			throw new RuntimeException("Compilation unit data missing: " + cu);

		if (d.locations_nr == 0)
			throw new RuntimeException("Compilation unit devoid of named locations: " + cu);

		return new Location(gen,
				    d.locations_offset + getRandom(d.locations_nr));
	}

	/**
	 * Returns Pair<ASTNode, ICompilationUnit>
	 */
	public Pair<ASTNode, ICompilationUnit>
	getNode(LocationNGenerator gen, Location l)
	{
		ProjectLocationInfo pli = getLocationInfo(gen);

		int index = l.getIndex();

		for (ICompilationUnit cu : this.project.getCompilationUnits()) {
			final int local_index = index;
			index -= pli.locations_map.get(cu.getHandleIdentifier()).locations_nr;

			if (index < 0) {
				ASTNode ast = project.parseCompilationUnit(cu);

				if (ast == null)
					throw new RuntimeException("Could not parse compilation unit " + cu.getPath());

				return new Pair<ASTNode, ICompilationUnit>(gen.allLocations(ast).get(local_index),
									   cu);
			}
		}

		throw new RuntimeException("Out of AST nodes?");
	}

	private static class CUData
	{
		int locations_nr;
		int locations_offset;

		public
		CUData(int locations_offset, int locations_nr)
		{
			this.locations_offset = locations_offset;
			this.locations_nr = locations_nr;
		}
	}

	private static class ProjectLocationInfo
	{
		public int total_locations;
		public HashMap<String, CUData> locations_map = new HashMap<String, CUData>();

		public int total_types_nr;
		public int total_super_subtype_pairs_nr;
	}

}
