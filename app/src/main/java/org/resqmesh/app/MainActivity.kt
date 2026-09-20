package org.resqmesh.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.view.WindowManager
import org.resqmesh.app.ui.ResqMeshApp
class MainActivity : ComponentActivity() {
    private val vm: MeshViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { ResqMeshApp(vm) }
    }
    // Foreground-only MVP: never imply networking continues after the app is hidden.
    override fun onStop() { vm.stop(); super.onStop() }
}
