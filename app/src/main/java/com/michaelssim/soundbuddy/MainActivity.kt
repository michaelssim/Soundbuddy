package com.michaelssim.soundbuddy

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michaelssim.soundbuddy.ui.theme.SoundbuddyTheme
import java.util.*
import kotlin.concurrent.timerTask

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            SoundbuddyTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    // val mediaPlayer = MediaPlayer.create(this, R.raw.vintage)
                    // Sound sampled from Franz Electric Metronome Model: LM-FB-4
                    // Originally used MediaPlayer but it would slowly become inaccurate
                    // Now using ToneGenerator instead
                    // Might use it later when more features get added (e.g., custom tones)
                    MetronomeScreen()
                }
            }
        }
    }
}

@Composable
fun MetronomeScreen(
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableStateOf(92F) }
    var buttonState by remember { mutableStateOf("▶") }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Copyright()
        Text(
            text = sliderPosition.toInt().toString(),
            fontSize = 172.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 33.dp)
                .offset(y = 33.dp)
        )
        Text(
            text = stringResource(R.string.metronome_bpm_info),
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp)
        )
        Text(
            text = updateTempoRangeText(sliderPosition),
            fontSize = 48.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = stringResource(R.string.tempo_label),
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp)
        )
        Spacer(Modifier.height(68.dp))
        MetronomeSlider(
            sliderPosition = sliderPosition,
            onValueChange = { sliderPosition = it },
            buttonState
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = {
                    if (sliderPosition >= 41)
                        sliderPosition--
                    toggleMetronomeControl(buttonState, sliderPosition)
                }
            ) {
                Text(
                    text = "-",
                    fontSize = 20.sp
                )
            }
            Button(
                onClick = {
                    buttonState = if (buttonState == "▶") {
                        "■"
                    } else {
                        "▶"
                    }
                    toggleMetronomeControl(buttonState, sliderPosition)
                },
            ) {
                Text(
                    text = buttonState,
                    fontSize = 20.sp
                )
            }
            OutlinedButton(
                onClick = {
                    if (sliderPosition < 208)
                        sliderPosition++
                    toggleMetronomeControl(buttonState, sliderPosition)
                }
            ) {
                Text(
                    text = "+",
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun Copyright() {
    val context = LocalContext.current
    val intent =
        remember { Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/michaelssim")) }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.copyright_text),
            fontSize = 24.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 30.dp)
                .clickable { context.startActivity(intent) }
        )
    }
}

@Composable
fun MetronomeSlider(
    sliderPosition: Float,
    onValueChange: (Float) -> Unit,
    buttonState: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .wrapContentSize(Alignment.Center)
    ) {
        Slider(
            value = sliderPosition,
            onValueChange = onValueChange,
            valueRange = 40f..208f,
            onValueChangeFinished = {
                toggleMetronomeControl(buttonState, sliderPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.secondary,
                activeTrackColor = MaterialTheme.colors.secondary
            ),
            modifier = Modifier
                .padding(38.dp, 0.dp)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Preview
@Composable
fun PreviewMetronomeScreen() {
    MetronomeScreen()
}

private fun toggleMetronomeControl(buttonState: String, sliderPosition: Float) {
    if (buttonState == "■") {
        Metronome.stop()
        Metronome.start(sliderPosition.toLong())
    } else {
        Metronome.stop()
    }
}

private fun updateTempoRangeText(sliderPosition: Float): String {
    val tempoText = when (sliderPosition.toInt()) {
        in 40..60 -> "LARGO"
        in 61..66 -> "LARGHETTO"
        in 67..76 -> "ADAGIO"
        in 77..108 -> "ANDANTE"
        in 109..120 -> "MODERATO"
        in 121..168 -> "ALLEGRO"
        in 169..200 -> "PRESTO"
        else -> "PRESTISSIMO"
    }
    return tempoText
}

object Metronome {
    // MediaPlayer causes lag, so the quick solution is to use ToneGenerator
    private const val METRONOME_TONE = ToneGenerator.TONE_PROP_BEEP

    private var metronomeState: Boolean
    private var metronomeTimer: Timer

    init {
        metronomeState = false
        metronomeTimer = Timer()
    }

    private fun calculatePeriod(bpm: Long): Long {
        // if BPM is 60, period should be 1000 milliseconds
        // if BPM is 120, period duration should be 500 milliseconds
        return (1000 * (60 / bpm.toDouble())).toLong()
    }

    fun start(bpm: Long): Boolean {
        metronomeState = true
        metronomeTimer.schedule(
            timerTask {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGenerator.startTone(METRONOME_TONE)
                toneGenerator.release()
            },
            0L,
            // period:
            calculatePeriod(bpm)
        )
        return true
    }

    fun stop(): Boolean {
        metronomeState = false
        metronomeTimer.cancel()

        // Without creating a new timer, it crashes: "Timer already cancelled"
        metronomeTimer = Timer()
        return true
    }
}
