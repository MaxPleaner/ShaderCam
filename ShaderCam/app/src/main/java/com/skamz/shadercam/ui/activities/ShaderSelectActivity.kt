package com.skamz.shadercam.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.skamz.shadercam.R
import com.skamz.shadercam.databinding.ActivityShaderSelectBinding
import com.skamz.shadercam.logic.database.FirebaseShaderDao
import com.skamz.shadercam.logic.database.FirebaseUserDao
import com.skamz.shadercam.logic.database.UserInfo
import com.skamz.shadercam.logic.shaders.util.ShaderAttributes
import com.skamz.shadercam.logic.shaders.util.ShaderParam
import com.skamz.shadercam.logic.shaders.util.Shaders
import com.skamz.shadercam.logic.util.TaskRunner
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.Callable


class ShaderSelectActivity: AppCompatActivity() {

    companion object {
        var shaders = mutableMapOf(
            Shaders.noopShader.name to Shaders.noopShader,
            Shaders.brightShader.name to Shaders.brightShader,
        )
        var pageType: String = "Root"
    }

    private lateinit var mListView: ListView
    private lateinit var cameraActivityIntent: Intent
    private var selectedUserUid: String = ""
    private var selectedUserName: String = ""

    class LoadMineAndDefaultShadersAsync : Callable<MutableMap<String, ShaderAttributes>> {
        internal class Callback(_callback: ((MutableMap<String, ShaderAttributes>) -> Unit)?) : TaskRunner.Callback<MutableMap<String, ShaderAttributes>> {
            var callback: (MutableMap<String, ShaderAttributes>) -> Unit = {}
            init {  if (_callback != null) {  callback = _callback  } }
            override fun onComplete(result: MutableMap<String, ShaderAttributes>) { callback(result) }
        }

        override fun call(): MutableMap<String, ShaderAttributes> {
            val userShaders = CameraActivity.shaderDao.getUserShaders()
            val newShadersList = mutableMapOf<String, ShaderAttributes>()
            userShaders.forEach {
                val params = Json.decodeFromString<MutableList<ShaderParam>>(it.paramsJson)
                newShadersList[it.name] = ShaderAttributes(
                    it.name,
                    it.shaderMainText,
                    params
                )
            }
            Shaders.all.forEach {
                it.isTemplate = true
                newShadersList[it.name] = it
            }
            return newShadersList
        }
    }

    private fun loadMineAndDefaultShaders(callback: () -> Unit) {
        TaskRunner().executeAsync(LoadMineAndDefaultShadersAsync(), LoadMineAndDefaultShadersAsync.Callback {
            shaders = it
            callback()
        })
    }

    private fun loadOtherUserShaders(selectedUserUid: String, callback: () -> Unit) {
        FirebaseShaderDao.getUserShaders(selectedUserUid) {
            shaders = it
            callback()
        }
    }

    private fun setupCurrentPage() {
        val pageTitles = mapOf(
            "Root" to "Shader Categories",
            "Defaults" to "Default Shaders",
            "Mine" to "My Shaders",
            "Discover (By User)" to "Discover Shaders (By User)",
            "Favorites" to "Favorite Shaders",
            "UserShaders" to "${selectedUserName}'s Shaders"
        )
        findViewById<TextView>(R.id.page_title).text = pageTitles[pageType]

        when (pageType) {
            "Root" -> setupRootPage()
            "Defaults" -> setupDefaultsPage()
            "Mine" -> setupMinePage()
            "Discover (By User)" -> setupDiscoverPage()
            "Favorites" -> setupFavoritesPage()
            "UserShaders" -> setupOtherUserShadersPage()
        }
    }

    private fun setupRootPage() {
        val arrayAdapter: ArrayAdapter<*>
        arrayAdapter = ArrayAdapter(this,
            R.layout.shader_list_item, arrayListOf("Defaults", "Mine", "Discover (By User)", "Favorites")
        )
        mListView.adapter = arrayAdapter
        mListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val name = mListView.getItemAtPosition(position) as String
                pageType = name
                setupCurrentPage()
            }
    }

    private fun setupShaderList(shaders: Map<String, ShaderAttributes>, backPageName: String = "Root") {
        val shaderList = shaders.keys.toMutableSet()
        shaderList.add("<-- BACK")
        val arrayAdapter = ArrayAdapter(this,
            R.layout.shader_list_item, shaderList.toTypedArray().sortedBy {
                if (it == "<-- BACK") {
                    ""
                } else {
                    it
                }
            })
        mListView.adapter = arrayAdapter
        mListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val name = mListView.getItemAtPosition(position) as String
                if (name == "<-- BACK") {
                    pageType = backPageName
                    setupCurrentPage()
                } else {
                    val filter = Companion.shaders[name]!!
                    CameraActivity.shaderAttributes = filter
                    cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(cameraActivityIntent)
                }
            }
    }

    private fun setupUserList(users: List<UserInfo>) {
        val userList = users.map { it.name }.toMutableSet()
        userList.add("<-- BACK")
        val arrayAdapter = ArrayAdapter(this,
            R.layout.shader_list_item, userList.toTypedArray().sortedBy {
                if (it == "<-- BACK") {
                    ""
                } else {
                    it
                }
            })
        mListView.adapter = arrayAdapter
        mListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val name = mListView.getItemAtPosition(position) as String
                if (name == "<-- BACK") {
                    pageType = "Root"
                    setupCurrentPage()
                } else {
                    selectedUserUid = users.find { it.name == name }!!.uid
                    selectedUserName = name
                    pageType = "UserShaders"
                    setupCurrentPage()
                }
            }
    }

    private fun setupDefaultsPage() {
        loadMineAndDefaultShaders {
            setupShaderList(shaders.filter {
                it.value.isTemplate
            }, backPageName = "Root")
        }
    }

    private fun setupMinePage() {
        loadMineAndDefaultShaders {
            setupShaderList(shaders.filter {
                !it.value.isTemplate
            }, backPageName = "Root")
        }
    }

    private fun setupDiscoverPage() {
        FirebaseUserDao.getOtherUsers {
            setupUserList(it)
        }
    }

    private fun setupOtherUserShadersPage() {
        loadOtherUserShaders(selectedUserUid) {
            setupShaderList(shaders, backPageName = "Discover (By User)")
        }
    }

    private fun setupFavoritesPage() {

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setupCurrentPage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = ActivityShaderSelectBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        cameraActivityIntent = Intent(this, CameraActivity::class.java)

        mListView = findViewById(R.id.list_view)

        setupCurrentPage()

        val cameraLink = findViewById<Button>(R.id.camera_link)
        cameraLink.setOnClickListener {
            cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(cameraActivityIntent)
        }
    }
}