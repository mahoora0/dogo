package com.example.dogo.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class VectorUtils {

	public static byte[] toBytes(float[] vector) {
		ByteBuffer buf = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		for (float v : vector) buf.putFloat(v);
		return buf.array();
	}

	public static float[] fromBytes(byte[] bytes) {
		FloatBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
		float[] vector = new float[buf.remaining()];
		buf.get(vector);
		return vector;
	}

	/** 두 정규화된 벡터의 코사인 유사도 = 내적 */
	public static float cosineSimilarity(float[] a, float[] b) {
		float dot = 0f;
		for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
		return dot;
	}
}
