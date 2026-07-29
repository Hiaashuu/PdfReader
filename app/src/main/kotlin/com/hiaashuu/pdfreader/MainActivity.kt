package com.hiaashuu.pdfreader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hiaashuu.pdfreader.PDFView
import com.hiaashuu.pdfreader.scroll.DefaultScrollHandle
import com.hiaashuu.pdfreader.util.FitPolicy
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let {

                        contentResolver.takePersistableUriPermission(
                            it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        selectedPdfUri = it
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "PDF Reader Demo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 25.sp
                                )
                            },
                            actions = {
                                IconButton(onClick = {
                                    launcher.launch(arrayOf("application/pdf"))
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Pick PDF")
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (selectedPdfUri != null) {
                            PdfViewerComposable(uri = selectedPdfUri!!)
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No PDF Selected", color = Color.Gray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { launcher.launch(arrayOf("application/pdf")) }) {
                                    Text("Pick a PDF")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfViewerComposable(uri: Uri) {
    AndroidView(
        factory = { context ->
            PDFView(context, null).apply {
                fromUri(uri)
                    .defaultPage(0)
                    .enableAnnotationRendering(true)
                    .scrollHandle(DefaultScrollHandle(context))
                    .spacing(10)
                    .pageFitPolicy(FitPolicy.BOTH)
                    .load()
            }
        },
        update = { view ->

            view.recycle()
            view.fromUri(uri)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .scrollHandle(DefaultScrollHandle(view.context))
                .spacing(10)
                .pageFitPolicy(FitPolicy.BOTH)
                .load()
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
    )
}