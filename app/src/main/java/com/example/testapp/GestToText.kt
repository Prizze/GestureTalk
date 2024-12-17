package com.example.testapp

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


class GestToText : ComponentActivity() {
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private lateinit var overlayView: OverlayView
    private lateinit var viewFinder: PreviewView
    private lateinit var textView: TextView
    private var lastSentTime: Long = 0 // Время последнего отправленного запроса
    var previousLetter: String? = null  // Для хранения предыдущей буквы
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gest_to_text)

        // Инициализация PreviewView и OverlayView
        viewFinder = findViewById(R.id.viewFinder)
        overlayView = findViewById(R.id.ovrView)
        textView = findViewById(R.id.recognizedLetters)

        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            handLandmarkerHelperListener = object : HandLandmarkerHelper.LandmarkerListener {
                override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
                    // Передача результатов в OverlayView для отображения
                    overlayView.setResults(
                        handLandmarkerResults = resultBundle.results.first(),
                        imageHeight = resultBundle.inputImageHeight,
                        imageWidth = resultBundle.inputImageWidth,
                        runningMode = RunningMode.LIVE_STREAM
                    )

                    val landmarks = resultBundle.results.first().landmarks()

                    if (landmarks.isNotEmpty()) {
                        val coordinates = landmarks
                            .flatten() // Преобразуем список списков в один список
                            .map { landmark ->
                                // Нормализованные координаты x, y и z
                                val x = landmark.x() // Нормализованное значение x
                                val y = landmark.y() // Нормализованное значение y
                                val z = landmark.z() // Нормализованное значение z
                                // Можно также передавать эти координаты в виде строки или массива
                                arrayOf(x, y, z)
                            }
                            val imageWidth = resultBundle.inputImageWidth
                            val imageHeight = resultBundle.inputImageHeight
                            // Проверяем, прошло ли 1 секунды с последнего запроса
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastSentTime > TimeUnit.MILLISECONDS.toMillis(1000)) {
                                lastSentTime = currentTime // Обновляем время последнего запроса
                                sendDataToServer(coordinates, imageWidth, imageHeight) // Отправляем запрос
                        }
                    } else{
                        previousLetter = null
                    }
                }
                override fun onError(error: String, errorCode: Int) {
                    // Обработка ошибок
                    Log.e("GestToText", "Error: $error")
                }
            }
        )
        startCamera()
    }
    private fun startCamera() {
        // Код для запуска камеры и передачи ImageProxy в HandLandmarkerHelper
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                    handLandmarkerHelper.detectLiveStream(
                        imageProxy = imageProxy,
                        isFrontCamera = true
                    )
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("GestToText", "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun sendDataToServer(coordinates: List<Array<Float>>, imageWidth: Int, imageHeight: Int) {

        val url = "http://172.20.10.7:5000/predict" // URL Flask-сервера

        // Создаем JSON объект с данными
        val json = JSONObject()

        // Добавляем координаты в JSON
        val coordinatesJsonArray = JSONArray()
        coordinates.forEach { coordinate ->
            val coordinateJson = JSONArray(coordinate.map { it.toString() })
            coordinatesJsonArray.put(coordinateJson)
        }

        // Добавляем размеры изображения в JSON
        json.put("input", coordinatesJsonArray)
        json.put("image_width", imageWidth)
        json.put("image_height", imageHeight)

        // Создаем RequestBody для POST запроса
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            json.toString()
        )

        // Создаем POST запрос
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        // Выполняем запрос
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GestToText", "Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.i("GestToText", "Response: $responseBody")

                    // Обработка ответа
                    val alphabet = listOf(
                        "А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "И", "Й", "К", "Л", "М", "Н", "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ", "Ъ", "Ы", "Ь","Э", "Ю", "Я", "_"
                    )
                    val prediction = JSONObject(responseBody ?: "").getJSONArray("prediction")
                    val recognizedNumber = prediction.getInt(0) // Предположим, это число от 1 до 34

                    if (recognizedNumber != -1) {
                        val recognizedLetter = alphabet.getOrNull(recognizedNumber) ?: ""
                        // Логируем значения
                        Log.d("LetterRecognition", "recognizedNumber: $recognizedNumber")
                        Log.d("LetterRecognition", "recognizedLetter: $recognizedLetter")
                        Log.d("LetterRecognition", "previousLetter: $previousLetter")
                        if (recognizedLetter != previousLetter) {
                            // Обновляем текст
                            runOnUiThread {
                                textView.append(recognizedLetter)  // Добавляем букву, если она новая
                            }
                            previousLetter = recognizedLetter  // Обновляем предыдущую букву
                        }
                    } else {
                        // Не выводим ничего, если результат -1
                        Log.d("GestToText", "No confident prediction, waiting for next landmarks...")
                    }
                } else {
                    Log.e("GestToText", "Server error: ${response.message}")
                }
            }
        })
    }
}
