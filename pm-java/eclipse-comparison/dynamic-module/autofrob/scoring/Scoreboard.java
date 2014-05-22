/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.scoring;

import autofrob.transform.*;
import java.util.*;
import java.io.*;
import static autofrob.scoring.ResultKind.*;

public class Scoreboard<T extends Transformation>
{
	private LinkedList<Result<T>> results = new LinkedList<Result<T>>();
	private TransformationGenerator<T> transform_gen;
	private int runs;
	private Transformer transformer = null;

	private PrintStream real_out = System.out;
	private PrintStream real_err = System.err;
	private PrintStream logger = null;

	public
	Scoreboard(TransformationGenerator<T> transform_gen, int runs)
	{
		this.transform_gen = transform_gen;
		this.runs = runs;
	}

	public void
	setTransformer(Transformer t)
	{
		this.transformer = t;
	}

	final static int MAX_REPORT_COUNT_INDEX = 128;

	int report_count_index = 0;
	String report_label = "";

	private void
	resetCountIndex()
	{
		if (report_count_index != 0)
			real_out.println("");
		report_count_index = 0;
	}

	public void
	reportChar(char c)
	{
		if (report_count_index == 0)
			real_out.print("[" + report_label + "] ");

		real_out.print(c);

		if (++report_count_index == MAX_REPORT_COUNT_INDEX)
			resetCountIndex();
	}

	public void
	reportStart(char c)
	{
		reportChar(c);
	}

	public void
	reportStop(char c)
	{
		reportChar('.');
	}

	public void 
	reportTransformGeneratorChange(String label)
	{
		report_label = label;
		resetCountIndex();
	}

	public void
	flushOutput()
	{
		resetCountIndex();
	}

	public void
	doStart(Transformation.Kind kind)
	{
		if (logger != null)
			logger.println("[" + kind + "] Start.");
		this.reportStart((kind.getIndex() + "").charAt(0));
	}


	public void
	doStop(Transformation.Kind kind)
	{
		this.reportStop((kind.getIndex() + "").charAt(0));
		if (logger != null)
			logger.println("[" + kind + "] Completed.");
	}

	public void
	run()
	{
		int count = 0;
		assert (runs > 0);
		assert (transformer != null);

		try {
			logger = new PrintStream("log/" + this.transform_gen.getName() + ".log");
			System.setErr(this.logger);
			System.setOut(this.logger);
		} catch (Exception _) {}

		while (runs-- > 0) {
			final int index = count++;
			final String bdir = "log/" + this.transform_gen.getName() + "." + index;
			if (logger != null)
				logger.println("--- " + bdir);
			final Result<T> result = transformer.doTransform(this.transform_gen, this, bdir);
			results.addLast(result);
			if (logger != null)
				logger.println("=== Result:" + bdir);
			logResult(result);
		}

		if (logger != null) {
			logger.close();
			logger = null;
		}
		System.setErr(real_err);
		System.setOut(real_out);
	}

	int result_runs = 0;
	int result_more_flex[] = new int[2];
	int result_more_correct[] = new int[2]; // more conservative 
	int result_more_powerful[] = new int[2];
	int result_same[] = new int[3];

	static final int RESULT_DISALLOWED_INDEX = 0;
	static final int RESULT_OK_INDEX = 1;
	static final int RESULT_ERRONEOUS_INDEX = 2;

	public void
	logResult(Result<T> result)
	{
		if (logger != null)
			logger.println("\t" + result);

		final Transformation.Kind PM = Transformation.Kind.PM;
		final Transformation.Kind ECLIPSE = Transformation.Kind.ECLIPSE;

		final ResultKind pm_result = result.getResult(PM);
		final ResultKind eclipse_result = result.getResult(ECLIPSE);

		++result_runs;

		if (pm_result != eclipse_result) {
			switch (pm_result) {
			case TRANSFORM_DISALLOWED:
				if (eclipse_result == TRANSFORM_OK)
					result_more_flex[ECLIPSE.getIndex()]++;
				else if (eclipse_result == TRANSFORM_ERRONEOUS)
					result_more_correct[PM.getIndex()]++;
				break;

			case TRANSFORM_OK:
				if (eclipse_result == TRANSFORM_DISALLOWED)
					result_more_flex[PM.getIndex()]++;
				else if (eclipse_result == TRANSFORM_ERRONEOUS)
					result_more_powerful[PM.getIndex()]++;
				break;

			case TRANSFORM_ERRONEOUS:
				if (eclipse_result == TRANSFORM_OK)
					result_more_powerful[ECLIPSE.getIndex()]++;
				else if (eclipse_result == TRANSFORM_DISALLOWED)
					result_more_correct[ECLIPSE.getIndex()]++;
				break;

			}
		} else switch (pm_result) {
			case TRANSFORM_DISALLOWED:
				result_same[RESULT_DISALLOWED_INDEX]++;
				break;
			case TRANSFORM_OK:
				result_same[RESULT_OK_INDEX]++;
				break;
			case TRANSFORM_ERRONEOUS:
				result_same[RESULT_ERRONEOUS_INDEX]++;
				break;
			}
	}

	private static final int NAME_CELL_WIDTH = 64;
	private static final int CELL_WIDTH = 16;

	private static String
	pad(String s, int len)
	{
		while (s.length() < len)
			s = s + " ";
		return s;
	}

	public static void
	printScoreboardHeader(PrintStream out)
	{
		int i;
		String header = pad("Transformation", NAME_CELL_WIDTH)
			+ pad("Runs", CELL_WIDTH)
			+ pad("Same (-|+|!)", CELL_WIDTH)
			+ pad("Safer (PM|E)", CELL_WIDTH)
			+ pad("More flexible", CELL_WIDTH)
			+ pad("More powerful", CELL_WIDTH)
			+ pad("Skipped", CELL_WIDTH)
			;
		
		out.println(header);
		for (i = 0; i < header.length(); i++)
			out.print("-");
		out.println("");
	}

	int result_skipped_transformation[] = new int[2];

	/**
	 * Claims to have transformed, but didn't?
	 */
	public void
	countSkippedTransformation(Transformation.Kind kind)
	{
		result_skipped_transformation[kind.getIndex()]++;
	}

	public void
	printScoreboard(PrintStream out)
	{
		final Transformation.Kind PM = Transformation.Kind.PM;
		final Transformation.Kind ECLIPSE = Transformation.Kind.ECLIPSE;

		final int pm_i = PM.getIndex();
		final int eclipse_i = ECLIPSE.getIndex();

		out.print(pad(transform_gen.getName(), NAME_CELL_WIDTH));
		out.print(pad("" + result_runs, CELL_WIDTH));
		out.print(pad(""
			      + result_same[RESULT_DISALLOWED_INDEX]
			      + " | "
			      + result_same[RESULT_OK_INDEX]
			      + " | "
			      + result_same[RESULT_ERRONEOUS_INDEX],
			      CELL_WIDTH));
		out.print(pad("" + result_more_correct[pm_i] + " | " + result_more_correct[eclipse_i], CELL_WIDTH));
		out.print(pad("" + result_more_flex[pm_i] + " | " + result_more_flex[eclipse_i], CELL_WIDTH));
		out.print(pad("" + result_more_powerful[pm_i] + " | " + result_more_powerful[eclipse_i], CELL_WIDTH));
		out.print(pad("" + result_skipped_transformation[pm_i] + " | " + result_skipped_transformation[eclipse_i], CELL_WIDTH));
		out.println("");
	}
}
