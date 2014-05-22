/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.scoring;

import autofrob.transform.Transformation;
import static autofrob.scoring.ResultKind.*;

public class Result<T extends Transformation>
{
	private T transformation;

	// Why?  Because I felt like it.
	private static final int INDEX_SHIFT = 4;
	private static final int INDEX_MASK = 0x000f;

	private static final int RAN_TRANSFORM = 0x0001;
	private static final int ALLOWED_TRANSFORM = 0x0002;
	private static final int RAN_TEST = 0x0004;
	private static final int TEST_OK = 0x0008;

	private static final int TRANSFORM_DISALLOWED_RESULT = RAN_TRANSFORM;
	private static final int TRANSFORM_OK_RESULT = RAN_TRANSFORM | ALLOWED_TRANSFORM | RAN_TEST | TEST_OK;
	private static final int TRANSFORM_ERRONEOUS_RESULT = RAN_TRANSFORM | ALLOWED_TRANSFORM | RAN_TEST;

	private int status = 0;

	public Result(T t)
	{
		this.transformation = t;
		assert (t != null);
	}

	public T
	getTransformation()
	{
		return transformation;
	}

	/**
	 * Report whether the specified transformation was allowed (i.e., if the preconditions permitted the transformation)
	 */
	public void
	setTransformationAllowed(Transformation.Kind kind, boolean allowed)
	{
		int shift = INDEX_SHIFT * kind.getIndex();

		assert (0 == (status & (RAN_TRANSFORM << shift))); // don't report twice
		status |= (RAN_TRANSFORM | (allowed ? ALLOWED_TRANSFORM : 0)) << shift;
	}

	/**
	 * Report whether unit tests succeeded after a successful transformation.
	 */
	public void
	setUnitTestsSucceeded(Transformation.Kind kind, boolean succeeded)
	{
		int shift = INDEX_SHIFT * kind.getIndex();

		int transform_success_mask = (RAN_TRANSFORM | ALLOWED_TRANSFORM) << shift;

		assert ((status & transform_success_mask) != 0);
		assert ((status & (RAN_TEST << shift)) == 0);

		status |= (RAN_TEST | (succeeded ? TEST_OK : 0)) << shift;
	}

	public ResultKind
	getResult(Transformation.Kind kind)
	{
		int shift = INDEX_SHIFT * kind.getIndex();
		int result = (status >> shift) & INDEX_MASK;

		if (result == TRANSFORM_DISALLOWED_RESULT)
			return ResultKind.TRANSFORM_DISALLOWED;
		if (result == TRANSFORM_OK_RESULT)
			return ResultKind.TRANSFORM_OK;
		if (result == TRANSFORM_ERRONEOUS_RESULT)
			return ResultKind.TRANSFORM_ERRONEOUS;

		throw new RuntimeException("Transformation test not completed (status " + result + ")");
	}

	public String
	getResultString(Transformation.Kind kind)
	{
		try {
			switch (getResult(kind)) {
			case TRANSFORM_DISALLOWED: return "disallowed";
			case TRANSFORM_OK: return "ok";
			case TRANSFORM_ERRONEOUS: return "erroneous";
			}
		} catch (RuntimeException _) {
			return "(indeterminate)";
		}
		throw new RuntimeException("unpossible");
	}

	/**
	 * Ran and reported all relevant status
	 */
	boolean
	isCompleted()
	{
		for (Transformation.Kind kind : Transformation.Kind.values()) {
			int shift = INDEX_SHIFT * kind.getIndex();

			if ((status & (RAN_TRANSFORM << shift)) == 0)
				return false;

			if (((status & (ALLOWED_TRANSFORM << shift)) != 0)
			    && (0 == (status & (RAN_TEST << shift))))
				return false;
		}

		return true;
	}

	public String
	toString()
	{
		return transformation.getName() + "(" + getResult(Transformation.Kind.PM).getChar() + getResult(Transformation.Kind.ECLIPSE).getChar() + ")"
			+ " PM: " + getResultString(Transformation.Kind.PM) + ", Eclipse: " + getResultString(Transformation.Kind.ECLIPSE);
	}
}