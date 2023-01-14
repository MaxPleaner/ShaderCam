package com.skamz.shadercam.shaders.util

abstract class AbstractShader : BaseFilterPatch() {
    abstract var dataValues: MutableMap<String, Float>
    abstract var params: MutableList<ShaderParam>

    abstract val name: String

    override fun copy(): AbstractShader {
        val copy = onCopy()
        if (size != null) {
            copy.setSize(size.width, size.height)
        }
        copy.dataValues = this.dataValues
        return copy
    }

    override fun onCopy(): AbstractShader {
        return try {
            javaClass.newInstance()
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Filters should have a public no-arguments constructor.", e)
        } catch (e: InstantiationException) {
            throw RuntimeException("Filters should have a public no-arguments constructor.", e)
        }
    }
}