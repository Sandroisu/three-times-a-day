package io.github.sandroisu.threetimesaday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.sandroisu.threetimesaday.app.App
import io.github.sandroisu.threetimesaday.core.storage.AndroidKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(keyValueStorage = AndroidKeyValueStorage(applicationContext))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(keyValueStorage = InMemoryKeyValueStorage())
}