package com.skamz.shadercam.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.skamz.shadercam.R
import com.skamz.shadercam.databinding.ActivityShaderSelectBinding
import com.skamz.shadercam.shaders.util.ShaderAttributes
import com.skamz.shadercam.shaders.util.Shaders
import com.skamz.shadercam.util.TaskRunner
import java.util.concurrent.Callable


class ShaderSelectActivity: AppCompatActivity() {

    companion object {
        lateinit var cameraActivityIntent: Intent
        val shaders = mutableMapOf(
            Shaders.noopShader.name to Shaders.noopShader,
            Shaders.brightShader.name to Shaders.brightShader,
        )
    }

    lateinit var mListView: ListView;

    internal class LoadShadersAsync : Callable<Boolean> {
        internal class Callback(_callback: ((Boolean) -> Unit)?) : TaskRunner.Callback<Boolean> {
            var callback: (Boolean) -> Unit = {}
            init {
                if (_callback != null) {
                    callback = _callback
                }
            }
            override fun onComplete(result: Boolean) {
                callback(result)
            }
        }
        override fun call(): Boolean {
            val userShaders = CameraActivity.shaderDao.getAll()
            userShaders.forEach {
                shaders[it.name] = ShaderAttributes(
                    it.name,
                    it.shaderMainText,
                    mutableListOf()
                )
            }
            return true
        }
    }

    private fun loadShaders() {
        shaders.clear()
        TaskRunner().executeAsync(LoadShadersAsync(), LoadShadersAsync.Callback {
            updateShadersList()
        })
    }

    private fun updateShadersList() {
        val arrayAdapter: ArrayAdapter<*>
        arrayAdapter = ArrayAdapter(this,
            R.layout.shader_list_item, shaders.keys.toTypedArray())
        mListView.adapter = arrayAdapter
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        loadShaders()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = ActivityShaderSelectBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val cameraActivityIntent = Intent(this, CameraActivity::class.java)
        cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        mListView = findViewById(R.id.list_view)

        mListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val name = mListView.getItemAtPosition(position) as String
                val filter = shaders[name]!!
                CameraActivity.shaderAttributes = filter
                startActivity(cameraActivityIntent)
            }

        loadShaders()

        val cameraLink = findViewById<Button>(R.id.camera_link);
        cameraLink.setOnClickListener {
            startActivity(cameraActivityIntent)
        }
    }
}