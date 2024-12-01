data class ScreenState(
    val generalRegisters: List<Register> = emptyList(),
    val memory: List<MemoryCell> = emptyList(),         // Состояние памяти
    val stack: List<Register> = emptyList(),            // Состояние стека
    val commands: List<CommandState> = emptyList(),     // Список команд программы
    val output: List<String> = emptyList(),             // Вывод
    val isHalted: Boolean = false
)

data class MemoryCell(
    val address: String,     // Адрес ячейки памяти в шестнадцатеричном формате
    val hexValue: String     // Значение в ячейке памяти в шестнадцатеричном формате
)

data class Register(
    val name: String,
    val value: String,
)

data class CommandState (
    val index: Int,
    val name: String,
    val hexValue: String,
    val isCurrent: Boolean
)

