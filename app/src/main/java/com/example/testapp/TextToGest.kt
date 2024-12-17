package com.example.testapp

import android.os.Bundle
import android.text.InputFilter
import androidx.activity.ComponentActivity
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TextToGest : ComponentActivity() {

    private lateinit var inputEditText: EditText
    private lateinit var displayTextView: TextView

    val filter = InputFilter { source, start, end, dest, dstart, dend ->
    val regex = "^[а-яА-Я ]*$" // Разрешаем только русские буквы
        val input = source.toString()

        if (input.matches(regex.toRegex())) {
            null // Разрешаем ввод
        } else {
            "" // Отказываем в вводе символов, не являющихся русскими буквами
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.text_to_gest)

        inputEditText = findViewById(R.id.inputEditText)
        displayTextView = findViewById(R.id.displayTextView)

        inputEditText.filters = arrayOf(filter)

        inputEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val inputText = s.toString()
                val spannable = SpannableString(inputText)

                // Для каждого символа, если это буква, добавляем картинку
                for (i in inputText.indices) {
                    val letter = inputText[i]
                    if (letter.isLetter()) {
                        val imageResource = getImageForLetter(letter)
                        val imageSpan = ImageSpan(this@TextToGest, imageResource)
                        spannable.setSpan(imageSpan, i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }

                // Устанавливаем SpannableString в TextView
                displayTextView.text = spannable
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun getImageForLetter(letter: Char): Int {

        // Определяем индекс буквы в алфавите (для русского алфавита)
        val letterIndex = letter.lowercaseChar() - 'а'

        // Проверяем, что индекс находится в допустимом диапазоне
        if (letterIndex in 0..32) {
            // Формируем имя ресурса: "l1.png", "l2.png", ..., "l33.png"
            val resourceName = "l${letterIndex + 2}" // Индексация начинается с 1
            // Получаем идентификатор ресурса по имени
            return resources.getIdentifier(resourceName, "drawable", packageName)
        }

        // Если буква не в диапазоне от "а" до "я", возвращаем 0 (или дефолтное изображение)
        return 0
    }
}