package com.example.task.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.StartViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
fun StartScreen(viewModel: StartViewModel) {
    val uiState = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // REMOVED: .safeContentPadding(), Scaffold padding handles this
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 200.dp, start = 24.dp, end = 24.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Lets Start with a ")
                    withStyle(style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )) {
                        append("Sample Task")
                    }
                    append(" for practice!")
                },
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
                color = Color(0xFF0F1E51)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Pehele hum ek sample task karte hain.",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }

        // --- Button (Interaction delegates to ViewModel) ---
        Button(
            onClick = viewModel::onStartTaskClick,
            enabled = uiState.isButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Start Sample Task", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Preview
@Composable
fun StartScreenPreview() {
    MaterialTheme {
        StartScreen(viewModel = StartViewModel(MainViewModel()))
    }
}