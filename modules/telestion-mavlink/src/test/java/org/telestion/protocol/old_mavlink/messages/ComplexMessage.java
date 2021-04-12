package org.telestion.protocol.old_mavlink.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.telestion.protocol.old_mavlink.annotation.MavArray;
import org.telestion.protocol.old_mavlink.annotation.MavField;
import org.telestion.protocol.old_mavlink.annotation.MavInfo;
import org.telestion.protocol.old_mavlink.annotation.NativeType;
import org.telestion.protocol.old_mavlink.message.MavlinkMessage;

@SuppressWarnings("preview")
@MavInfo(id = 1, crc = 0)
public record ComplexMessage(
		@MavArray(length = 2) @MavField(extension = false, nativeType = NativeType.INT_64)
		@JsonProperty long[] bigArray,

		@MavArray(length = 2) @MavField(extension = false, nativeType = NativeType.INT_32)
		@JsonProperty int[] mediumArray,

		@MavArray(length = 2) @MavField(extension = false, nativeType = NativeType.INT_16)
		@JsonProperty short[] smallArray,

		@MavArray(length = 2) @MavField(extension = false, nativeType = NativeType.INT_8)
		@JsonProperty byte[] tinyArray,

		@MavArray(length = 2) @MavField(extension = false, nativeType = NativeType.DOUBLE)
		@JsonProperty double[] bigFloatingArray,

		@MavArray(length = 2) @MavField(extension = false, nativeType = NativeType.FLOAT)
		@JsonProperty float[] floatingArray,

		@MavArray(length = 2) @MavField(extension = false, nativeType = NativeType.CHAR)
		@JsonProperty char[] charArray,

		@MavField(extension = false, nativeType = NativeType.INT_64) @JsonProperty long big,

		@MavField(extension = false, nativeType = NativeType.INT_32) @JsonProperty int medium,

		@MavField(extension = false, nativeType = NativeType.INT_16) @JsonProperty short small,

		@MavField(extension = false, nativeType = NativeType.INT_8) @JsonProperty byte tiny,

		@MavField(extension = false, nativeType = NativeType.DOUBLE) @JsonProperty double bigFloating,

		@MavField(extension = false, nativeType = NativeType.FLOAT) @JsonProperty float floating,

		@MavField(extension = false, nativeType = NativeType.CHAR) @JsonProperty char character,

		@MavArray(length = 10) @MavField(extension = false, nativeType = NativeType.CHAR)
		@JsonProperty char[] extension)
		implements MavlinkMessage {

	@SuppressWarnings("unused")
	private ComplexMessage() {
		this(null, null, null, null, null, null, null, 0, 0, (short) 0, (byte) 0, 0.0, 0.0f, (char) 0x0, null);
	}
}
