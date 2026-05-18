package com.example.wellnesskeyboard.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

class WellnessInference(context: Context) : Closeable {
    private val interpreter: Interpreter = Interpreter(loadModelFile(context))

    fun infer(features: FloatArray): Float {
        require(features.size == 8) { "Expected 8 daily features in the trained order." }

        val input = arrayOf(features.copyOf())
        val output = Array(1) { FloatArray(1) }
        interpreter.run(input, output)

        val rawScore = output[0][0]
        return normalizeToUnitInterval(rawScore)
    }

    override fun close() {
        interpreter.close()
    }

    private fun normalizeToUnitInterval(value: Float): Float {
        if (!value.isFinite()) return 0f
        if (value in 0f..1f) return value
        return (1f / (1f + exp(-value))).toFloat().coerceIn(0f, 1f)
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("wellness_model.tflite")
        FileInputStream(assetFileDescriptor.fileDescriptor).channel.use { channel ->
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
        }
    }
}