package com.example.tomatodiseasedetector

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TomatoDiseaseScreen()
        }
    }
}

@Composable
fun TomatoDiseaseScreen() {

    val context = LocalContext.current

    // Create the classifier
    val classifier = remember {
        Classifier(context)
    }

    // Selected image
    var selectedImage by remember {
        mutableStateOf<Bitmap?>(null)
    }

    // Prediction result
    var result by remember {
        mutableStateOf("Prediction will appear here")
    }

    // Confidence
    var confidence by remember {
        mutableStateOf("Confidence: --")
    }

    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {

            try {

                val bitmap: Bitmap

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                    val source = ImageDecoder.createSource(
                        context.contentResolver,
                        uri
                    )

                    bitmap = ImageDecoder.decodeBitmap(source)

                } else {

                    @Suppress("DEPRECATION")
                    bitmap = MediaStore.Images.Media.getBitmap(
                        context.contentResolver,
                        uri
                    )
                }

                selectedImage = bitmap

                result = "Ready to predict"
                confidence = "Confidence: --"

            } catch (e: Exception) {

                result = "Image loading error"
                confidence = e.message ?: ""

            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Tomato Disease Detector",
            fontSize = 28.sp
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Show selected image
        if (selectedImage != null) {

            Image(
                bitmap = selectedImage!!.asImageBitmap(),
                contentDescription = "Selected tomato image",
                modifier = Modifier.size(300.dp)
            )

        } else {

            Text(
                text = "No image selected",
                fontSize = 18.sp
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // SELECT IMAGE BUTTON
        Button(
            onClick = {
                imagePicker.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Select Tomato Image")
        }

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        // PREDICT BUTTON
        Button(
            onClick = {

                if (selectedImage == null) {

                    result = "Please select an image first"
                    confidence = "Confidence: --"

                } else {

                    try {

                        // THIS IS THE IMPORTANT PART
                        val prediction =
                            classifier.classify(selectedImage!!)

                        result =
                            "Disease: ${prediction.first}"

                        confidence =
                            "Confidence: ${
                                String.format(
                                    "%.2f",
                                    prediction.second * 100
                                )
                            }%"

                    } catch (e: Exception) {

                        result = "Prediction error"

                        confidence =
                            e.message ?: "Unknown error"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Predict Disease")
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        // RESULT
        Text(
            text = result,
            fontSize = 20.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        // CONFIDENCE
        Text(
            text = confidence,
            fontSize = 18.sp
        )
    }
}