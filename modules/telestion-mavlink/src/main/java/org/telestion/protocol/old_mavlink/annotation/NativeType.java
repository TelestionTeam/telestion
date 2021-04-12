package org.telestion.protocol.old_mavlink.annotation;

/**
 * Defines all native Types of MAVLink which are supported by this Adapter.
 *
 * @author Cedric Boes
 * @version 1.0
 */
public enum NativeType {
	/**
	 * Representation of the int8_t type for MAVLink.
	 */
	INT_8(1, false),
	/**
	 * Representation of the uint8_t type for MAVLink.
	 */
	UINT_8(1, true),
	/**
	 * Representation of the int16_t type for MAVLink.
	 */
	INT_16(2, false),
	/**
	 * Representation of the uint16_t type for MAVLink.
	 */
	UINT_16(2, true),
	/**
	 * Representation of the int32_t type for MAVLink.
	 */
	INT_32(4, false),
	/**
	 * Representation of the uint32_t type for MAVLink.
	 */
	UINT_32(4, true),
	/**
	 * Representation of the int64_t type for MAVLink.
	 */
	INT_64(8, false),
	/**
	 * Representation of the uint64_t type for MAVLink.
	 */
	UINT_64(8, true),
	/**
	 * Representation of the float type for MAVLink.
	 */
	FLOAT(4, false),
	/**
	 * Representation of the double type for MAVLink.
	 */
	DOUBLE(8, false),
	/**
	 * Representation of the char type for MAVLink.<br>
	 * Basically {@link #UINT_8} but a parser can use this type to create Strings more conveniently.
	 */
	CHAR(1, true);

	/**
	 * Size in memory of a {@link NativeType}.
	 */
	public final int size;
	/**
	 * Indicates whether a {@link NativeType} is unsigned or not.
	 */
	public final boolean unsigned;

	/**
	 * Creating a new {@link NativeType}.<br>
	 * This can only be created by the enum-fields.
	 *
	 * @param size     in memory of the new {@link NativeType}
	 * @param unsigned indicates whether a {@link NativeType} is unsigned or not
	 */
	private NativeType(int size, boolean unsigned) {
		this.size = size;
		this.unsigned = unsigned;
	}
}
