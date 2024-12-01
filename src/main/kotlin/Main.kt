import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState


@Composable
@Preview
fun App() {



    val state by CpuController.screenState.collectAsState()

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {

            Row {
                Column {
                    state.generalRegisters.forEach { register ->
                        Row {
                            Text(register.name, modifier = Modifier.width(60.dp))
                            Text(if(register.name != "FLAG") register.value.toInt(radix = 16).toString() else register.value)
                        }
                    }
                }

                // Отображение команд
                val listState = rememberLazyListState() // Состояние списка

                val currentIndex = state.commands.indexOfFirst { it.isCurrent }

                // Автоматическая прокрутка к текущей команде
                LaunchedEffect(currentIndex) {
                    if (currentIndex >= 0) {
                        listState.animateScrollToItem(currentIndex)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .padding(start = 40.dp),
                    state = listState // Передаём состояние списка
                ) {
                    item {
                        Text("Instructions:")
                    }
                    items(state.commands) { command ->
                        val modifier = if (command.isCurrent) {
                            Modifier.background(color = Color(0x7700FF00))
                        } else {
                            Modifier
                        }
                        Row(modifier = modifier) {
                            Text(command.index.toString(), modifier = Modifier.width(40.dp), color = Color.Gray)
                            Text(command.hexValue)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(command.name)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .padding(start = 40.dp)
                ) {
                    item {
                        Text("Memory:")
                    }
                    items(state.memory) { memoryCell ->
                        Row {
                            Text(memoryCell.address, modifier = Modifier.width(80.dp), color = Color.Gray)
                            Text(memoryCell.hexValue)
                        }
                    }
                }

                // Отображение состояния стека
                Column(modifier = Modifier.padding(start = 40.dp)) {
                    Text("Stack:")
                    state.stack.forEach { stackItem ->
                        Text(stackItem.value) // Показываем значение на вершине стека
                    }
                }

                Column(modifier = Modifier.padding(start = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    Text("Результат:")
                    state.output.forEach { outputLine ->
                        Text(outputLine)
                    }

                    Spacer(modifier = Modifier.height(64.dp))

                    Button(
                        onClick = {
                            CpuController.reset()
                                  },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color(0xffffffff),
                            backgroundColor = Color(0xff000000)
                        )
                    ){ Text("Сбросить выполнение", textAlign = TextAlign.Center) }

                    Button(
                        onClick = {
                            CpuController.resetFile("/Users/rodionkasirin/Dev/JavaKotlin/CPU/src/main/resources/program.txt")
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color(0xffffffff),
                            backgroundColor = Color(0xff000000)
                        )
                    ){ Text("Сбросить и выполнить файл", textAlign = TextAlign.Center) }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {CpuController.next()},
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color(0xffffffff),
                            backgroundColor = Color(0xff000000)
                        )
                    ){ Text("Следующий шаг", textAlign = TextAlign.Center) }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {CpuController.resume()},
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color(0xffffffff),
                            backgroundColor = Color(0xff000000)
                        )
                    ){ Text("Продолжить выполнение", textAlign = TextAlign.Center) }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {CpuController.pause()},
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color(0xffffffff),
                            backgroundColor = Color(0xff000000)
                        )
                    ){ Text("Приостановить выполнение", textAlign = TextAlign.Center) }

                }
            }

        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1000.dp, 800.dp))
    Window(onCloseRequest = ::exitApplication, state = windowState) {
        App()
    }
}