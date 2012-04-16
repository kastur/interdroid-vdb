package interdroid.vdb.content.avro;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;

/**
 * A utility class which can verify projection
 * from writer's data to reader schema. This is of use for verifying
 * that a revised schema is a valid evolution that will not cause runtime
 * errors when reading existing data.
 * <br/>
 * Note that this class separates errors from warnings, since there are
 * some schema evolution operations which may or may not cause runtime errors
 * depending on the data written. For example, removal of an enumeration field
 * will not throw an error if the written data has never written that
 * enumeration value. Such warnings are treated as errors with respect
 * to validateProjection by default but this can be turned off setFailOnWarning
 * to turn this off.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public final class SchemaEvolutionValidator {

	/**
	 * A class which represents the hierarchy of names for a given
	 * field.
	 *
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 *
	 */
	public static final class FieldName {
		/** The names for this field. */
		private final String[] mNames;

		/**
		 * Construct a FieldName from a list of names.
		 * @param names the list of names
		 */
		private FieldName(final List<String> names) {
			mNames = names.toArray(new String[names.size()]);
		}

		/**
		 * Test for equality.
		 * @param otherObject the other object to test
		 * @return true if the names are equal.
		 */
		public boolean equals(final Object otherObject) {
			boolean ret = true;
			if (otherObject instanceof FieldName) {
				FieldName other = (FieldName) otherObject;
				if (mNames.length != other.mNames.length) {
					ret = false;
				} else {
					for (int i = 0; i < mNames.length; i++) {
						if (!mNames[i].equals(other.mNames[i])) {
							ret = false;
							break;
						}
					}
				}
			} else {
				ret = false;
			}

			return ret;
		}

		/**
		 * @return hash code for this object
		 */
		public int hashCode() {
			int ret = mNames.hashCode();
			for (String name : mNames) {
				ret += name.hashCode();
			}
			return ret;
		}

		/**
		 * @return this FieldName as a string
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			for (String name : mNames) {
				if (buffer.length() > 0) {
					buffer.append(".");
				}
				buffer.append(name);
			}
			return buffer.toString();
		}
	}

	/**
	 * A map of fields and their warnings.
	 */
	private Map<FieldName, String> mWarnings = new HashMap<FieldName, String>();

	/**
	 * A map of fields and their errors.
	 */
	private Map<FieldName, String> mErrors = new HashMap<FieldName, String>();

	/**
	 * Should we return false on a warning.
	 */
	private boolean mFailOnWarning = true;

	/**
	 * Construct an evolution validator.
	 */
	public SchemaEvolutionValidator() { }

	/**
	 * Toggles if validateProjection should return true of false on a
	 * warning.
	 *
	 * @param failOnWarning the new value for the failOnWarning switch
	 */
	public void setFailOnWarning(final boolean failOnWarning) {
		mFailOnWarning = failOnWarning;
	}

	/**
	 * Validates that the readerSchema can read the data written with the
	 * writerSchema. Note that this clears all errors and warnings before
	 * running so that getErrors and getWarnings always only return
	 * warnings and errors for the last call to this method.
	 *
	 * @param readerSchema the schema the reader will use
	 * @param writerSchema the schema the writer used
	 * @return true if the reader can read the writer's data.
	 */
	public boolean validateProjection(final Schema readerSchema,
			final Schema writerSchema) {

		// Clear any existing warnings from last run.
		mWarnings.clear();
		mErrors.clear();

		// Put the schema name on the stack to start with
		Stack<String> names = new Stack<String>();
		names.push(readerSchema.getFullName());

		// Run the validation
		validateProjectionImpl(readerSchema, writerSchema, names);

		// Now check the return condition.
		return hasErrorsOrWarnings();
	}

	/**
	 * @return true if there are warnings.
	 */
	public boolean hasWarnings() {
		return !mWarnings.isEmpty();
	}

	/**
	 * @return true if there are errors.
	 */
	public boolean hasErrors() {
		return !mErrors.isEmpty();
	}

	/**
	 * @return true if there are errors or there are warnings and fail
	 *         on warning is true.
	 */
	private boolean hasErrorsOrWarnings() {
		boolean ret = true;
		if (mFailOnWarning && hasWarnings()) {
			ret = false;
		} else if (hasErrors()) {
			ret = false;
		}
		return ret;
	}

	/**
	 * Validates that the readerSchema can read the data written with the
	 * writerSchema.
	 * @param readerSchema the schema the reader will use
	 * @param writerSchema the schema the writer used
	 * @param names the stack of field names.
	 */
	private void validateProjectionImpl(final Schema readerSchema,
			final Schema writerSchema, final Stack<String> names) {

		switch (readerSchema.getType()) {
		case ARRAY:
			validateArrayProjection(readerSchema, writerSchema, names);
			break;
		case MAP:
			validateMapProjection(readerSchema, writerSchema, names);
			break;
		case ENUM:
			validateEnumProjection(readerSchema, writerSchema, names);
			break;
		case FIXED:
			validateFixedProjection(readerSchema, writerSchema, names);
			break;
		case RECORD:
			validateRecordProjection(readerSchema, writerSchema, names);
			break;
		case UNION:
			validateUnionProjection(readerSchema, writerSchema, names);
			break;
		default:
			// Primitive Type
			validatePrimitiveProjection(readerSchema.getType(),
					writerSchema.getType(), names);
		}

	}

	/**
	 * Does not assume readerSchema is a union.
	 * @param readerSchema the schema of the reader
	 * @param writerSchema the schema of the writer
	 * @param names the stack of field names
	 */
	private void validateUnionProjection(final Schema readerSchema,
			final Schema writerSchema, final Stack<String> names) {
		// Recursive validator.
		SchemaEvolutionValidator validator = new SchemaEvolutionValidator();

		if (readerSchema.getType().equals(Type.UNION)) {
			if (writerSchema.getType().equals(Type.UNION)) {
				// Loop through the writer's types and verify they will
				// resolve in the reader.
				List<Schema> writerTypes = readerSchema.getTypes();
				for (Schema type : writerTypes) {
					names.push(type.getType().toString());
					if (isNamedType(type.getType())) {
						names.push(type.getFullName());
					}
					// Do a separate sub validation.
					if (!validator.validateProjection(readerSchema, type)) {
						mWarnings.put(new FieldName(names),
								"Writer's union branch does not project"
										+ " to branch in reader's union.");
					}
					names.pop();
				}
			} else {
				// Loop through the readers types and make sure
				// the writer will resolve into one of those types
				List<Schema> readerTypes = readerSchema.getTypes();
				boolean foundBranch = false;
				for (Schema type : readerTypes) {
					if (validator.validateProjection(type, writerSchema)) {
						foundBranch = true;
						break;
					}
				}
				if (!foundBranch) {
					mErrors.put(new FieldName(names),
							"Writer's type is not a branch in reader's union.");
				}
			}
		} else if (writerSchema.getType().equals(Type.UNION)) {
			// This is a warning condition but the reader may be
			// able to read it if only the right branch type is ever used.
			// Check for warning vs error by making sure one of the writer's
			// types can be read as reader's.

			// Loop through the writer's types and verify one of them
			// resolves to the readers.
			List<Schema> writerTypes = writerSchema.getTypes();
			boolean foundBranch = false;
			for (Schema type : writerTypes) {
				if (validator.validateProjection(readerSchema, type)) {
					foundBranch = true;
					break;
				}
			}
			if (!foundBranch) {
				mErrors.put(new FieldName(names),
						"Reader's type cannot read any branches in writer's"
								+ " union.");
			} else {
				mWarnings.put(new FieldName(names),
						"Reader's type can only read one branch in"
								+ " writer's union.");
			}
		}
	}

	/**
	 * @param type the type to be checked
	 * @return true if this type is named
	 */
	private boolean isNamedType(final Type type) {
		switch (type) {
		case FIXED:
		case ENUM:
		case RECORD:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Assumes readerSchema is already known to be record.
	 * @param readerSchema the schema of the reader
	 * @param writerSchema the schema of the writer
	 * @param names the stack of field names
	 */
	private void validateRecordProjection(final Schema readerSchema,
			final Schema writerSchema, final Stack<String> names) {

		// Is the writer a record?
		if (writerSchema.getType().equals(Type.RECORD)) {

			// Examine all reader fields and make sure we have data
			for (Field readerField: readerSchema.getFields()) {
				names.push(readerField.name());

				Field writerField = writerSchema.getField(readerField.name());

				// Writer has this field?
				if (writerField == null) {

					// No, reader must have a default.
					if (readerField.defaultValue() == null) {
						mErrors.put(new FieldName(names),
								"New field without a default.");
					}

				} else {
					// Run recursive validation
					validateProjectionImpl(readerField.schema(),
							writerField.schema(), names);
				}
				names.pop();
			}
		} else if (writerSchema.getType().equals(Type.UNION)) {
			validateUnionProjection(readerSchema, writerSchema, names);
		} else {
			mErrors.put(new FieldName(names), "Reader expected a record.");
		}

	}

	/**
	 * Assumes readerSchema is known to be FIXED.
	 * @param readerSchema the schema of the reader
	 * @param writerSchema the schema of the writer
	 * @param names the name of the field being projected
	 */
	private void validateFixedProjection(final Schema readerSchema,
			final Schema writerSchema, final Stack<String> names) {

		if (writerSchema.getType().equals(Type.FIXED)) {

			// Validate that names match
			if (!readerSchema.getFullName().equals(
					writerSchema.getFullName())) {
				mErrors.put(new FieldName(names), "Fixed names don't match.");
			}

			// Validate that the sizes are the same
			if (readerSchema.getFixedSize() != writerSchema.getFixedSize()) {
				mErrors.put(new FieldName(names), "Fixed sizes don't match.");
			}
		} else if (writerSchema.getType().equals(Type.UNION)) {
			validateUnionProjection(readerSchema, writerSchema, names);
		} else {
			mErrors.put(new FieldName(names), "Reader expected fixed.");
		}

	}

	/**
	 * Assumes readerSchema is known to be ENUM.
	 * @param readerSchema the schema of the reader
	 * @param writerSchema the schema of the writer
	 * @param names the name of the field being projected
	 */
	private void validateEnumProjection(final Schema readerSchema,
			final Schema writerSchema, final Stack<String> names) {

		if (writerSchema.getType().equals(Type.ENUM)) {

			// Verify that the names match
			if (!readerSchema.getFullName().equals(
					writerSchema.getFullName())) {
				mErrors.put(new FieldName(names),
						"Enumeration names don't match.");
			}

			// Validate the enumeration symbols
			List<String> readerSymbols = readerSchema.getEnumSymbols();
			for (String symbol : writerSchema.getEnumSymbols()) {
				names.push(symbol);
				if (!readerSymbols.contains(symbol)) {
					mWarnings.put(new FieldName(names),
							"Writer has an enumeration symbol"
									+ " which the reader lacks.");
				}
				names.pop();
			}
		} else if (writerSchema.getType().equals(Type.UNION)) {
			validateUnionProjection(readerSchema, writerSchema, names);
		} else {
			mErrors.put(new FieldName(names), "Reader expected an enumertion.");
		}

	}

	/**
	 * Assumes readerSchema is known to be MAP.
	 * @param readerSchema the schema of the reader
	 * @param writerSchema the schema of the writer
	 * @param names
	 * @param names the name of the field being projected
	 */
	private void validateMapProjection(final Schema readerSchema,
			final Schema writerSchema, final Stack<String> names) {

		if (writerSchema.getType().equals(Type.MAP)) {

			// Validate that the value types match recursively
			validateProjectionImpl(readerSchema.getValueType(),
					writerSchema.getValueType(), names);
		} else if (writerSchema.getType().equals(Type.UNION)) {
			validateUnionProjection(readerSchema, writerSchema, names);
		} else {
			mErrors.put(new FieldName(names), "Reader expected a map.");
		}
	}

	/**
	 * Assumes readerSchema is known to be ARRAY.
	 * @param readerSchema the schema of the reader
	 * @param writerSchema the schema of the writer
	 * @param names the name of the field being projected
	 */
	private void validateArrayProjection(final Schema readerSchema,
			final Schema writerSchema, final Stack<String> names) {

		if (writerSchema.getType().equals(Type.ARRAY)) {

			// Validate that the element types match recursively
			validateProjectionImpl(readerSchema.getElementType(),
					writerSchema.getElementType(), names);
		} else if (writerSchema.getType().equals(Type.UNION)) {
			validateUnionProjection(readerSchema, writerSchema, names);
		} else {
			mErrors.put(new FieldName(names), "Reader expected an array.");
		}

	}

	/**
	 * Assumes readerType is known to be primitive.
	 * @param readerType the reader's type
	 * @param writerType the writer's type
	 * @param names the name of the field being projected
	 */
	private void validatePrimitiveProjection(final Type readerType,
			final Type writerType, final Stack<String> names) {
		boolean isPromotable = false;

		if (readerType.equals(writerType)) {
			isPromotable = true;
		} else {
			switch (writerType) {
			case INT:
				// int is promotable to long, float, or double
				if (readerType.equals(Type.LONG)
						|| readerType.equals(Type.FLOAT)
						|| readerType.equals(Type.DOUBLE)) {
					isPromotable = true;
				}
				break;
			case LONG:
				// long is promotable to float or double
				if (readerType.equals(Type.FLOAT)
						|| readerType.equals(Type.DOUBLE)) {
					isPromotable = true;
				}
				break;
			case FLOAT:
				// float is promotable to double
				if (readerType.equals(Type.DOUBLE)) {
					isPromotable = true;
				}
				break;
			default:
				// isPromotobale is already false
			}
		}
		if (!isPromotable) {
			mErrors.put(new FieldName(names),
					"Primitive types do not match and cannot be promoted.");
		}

	}

	public Map<FieldName, String> getErrors() {
		return mErrors;
	}

	public Map<FieldName, String> getWarnings() {
		return mWarnings;
	}
}
