package com.example.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import android.widget.TextView
import android.content.Intent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity)

        val toTextSender = findViewById<TextView>(R.id.sendToGestToText)
        val toGestSender = findViewById<TextView>(R.id.sendToTextToGest)

        toTextSender.setOnClickListener {
            // Переход на GestToText
            val intent = Intent(this, GestToText::class.java)
            startActivity(intent)
        }
        toGestSender.setOnClickListener {
            // Переход на TextToGest
            val intent = Intent(this, TextToGest::class.java)
            startActivity(intent)
        }
    }
}
