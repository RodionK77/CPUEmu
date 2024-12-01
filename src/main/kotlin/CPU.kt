// безадресные команды
// архитектура фон-неймана
// поиск максимума среди элементов массива

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

enum class Instructions(val code: UInt) {
    PUSH(0b0001u),   // При выполнении команды PUSH, следующая команда кладется на вершину стека, т.е. она не команда, а число.
    READ(0b0010u),   // При выполнении команды в стек кладется число из памяти по адресу, лежащему на вершине стека. Т.е. если вы хотите прочитать число из 100 адреса, то вы должны выполнить следующие команды: PUSH 100 READ.
    WRITE(0b0011u),  // При выполнении команды, в адрес лежащий на вершине стека, кладется число, которое лежало на стеке под адресом. Оба значения со стека естественно исчезают.
    DUP(0b0100u),    // Продублировать число на вершине стека
    DROP(0b0101u),   // Удалить число с вершины стека
    JMP(0b0110u),    // Загрузить число со стека в регистр-счетчик
    OUT(0b0111u),    // Вывод из вершины стека
    CMP(0b1000u),    // Сравнивает два верхних числа на стеке
    INC(0b1001u),    // Инкремент числа на вершине стека
    DEC(0b1010u),    // декремент числа на вершине стека
    JE(0b1011u),     // прыжок с проверкой если равно
    JG(0b1100u),     // прыжок с проверкой если больше
    ADD(0b1101u),    // Складывает числа на вершине стека, результат кладется на вершину стека. Слагаемые исчезают со стека.
    JL(0b1110u),     // прыжок с проверкой если меньше
    HLT(0b1111u)     // Остановка выполнения программы
}

data class Command(
    val instructions: Instructions,
) {

    constructor(code: UInt) : this(
        instructions = Instructions.values().first { it.code == code.getBits(0, 3) },
    )
}

class Cpu {

    val output = mutableListOf<String>()
    var pc = 0u
    var sp = 0u

    //val stack = UIntArray(16)
    var memory = UIntArray(1024)
    val stackMemoryStart = 72u
    val stackMemoryEnd = 87u
    var flag = 0b001u

    fun loadProgram(program: UIntArray) {

        program.forEachIndexed { index, command ->
            memory[index] = command
        }
    }

    fun loadArray(array: ArrayItem) {
        array.elements.forEachIndexed { index, element ->
            memory[array.start + index.toUInt()] = element
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun executeCommand() {
        val command = Command(memory[pc])
        when (command.instructions) {
            Instructions.PUSH -> {
                pc++ // Переход к следующему числу в памяти
                memory[stackMemoryStart + sp++] = memory[pc] // Помещаем число на вершину стека
                pc++ // Увеличиваем счетчик команд для перехода к следующей команде
            }

            Instructions.READ -> {
                val address = memory[stackMemoryStart + --sp] // Получаем адрес из вершины стека
                memory[stackMemoryStart + sp++] = memory[address] // Читаем из памяти и кладем значение на вершину стека
                pc++
            }

            Instructions.WRITE -> {
                val address = memory[stackMemoryStart + --sp] // Получаем адрес для записи
                memory[address] = memory[stackMemoryStart + --sp] // Записываем значение на указанное место в памяти
                pc++
            }

            Instructions.DUP -> {
                memory[stackMemoryStart + sp] = memory[stackMemoryStart + sp - 1u] // Дублируем значение на вершине стека
                sp++ // Увеличиваем указатель стека
                pc++
            }

            Instructions.DROP -> {
                sp-- // Убираем вершину стека
                pc++
            }

            Instructions.JMP -> {
                val point = memory[stackMemoryStart + --sp]
                pc = point
            }

            Instructions.OUT -> {
                output.add(memory[stackMemoryStart + --sp].toString())
                pc++
            }

            Instructions.CMP -> {
                flag = 1u
                val b = memory[stackMemoryStart + --sp]
                val a = memory[stackMemoryStart + --sp]
                if (b > a) {
                    //println("больше первое")
                    flag = flag or 0b010u
                } else if (a > b) {
                    //println("больше второе")
                    flag = flag or 0b100u
                }
                pc++
            }

            Instructions.INC -> {
                memory[stackMemoryStart + sp - 1u]++ // Инкрементируем значение на вершине стека
                pc++
            }

            Instructions.DEC -> {
                memory[stackMemoryStart + sp - 1u]-- // Декрементируем значение на вершине стека
                pc++
            }

            Instructions.JE -> {
                val point = memory[stackMemoryStart + --sp]
                if (flag.getBits(2, 2) == 0u && flag.getBits(1, 1) == 0u) {
                    pc = point    // проверяем число сравнений
                } else pc++
                //flag = 1u
            }

            Instructions.JG -> {
                val point = memory[stackMemoryStart + --sp]
                if (flag.getBits(1, 1) == 1u) {
                    pc = point // проверяем число сравнений
                } else pc++
                //flag = 1u
            }

            Instructions.ADD -> {
                val b = memory[stackMemoryStart + --sp]
                val a = memory[stackMemoryStart + --sp]
                memory[stackMemoryStart + sp++] = a + b // Складываем два верхних значения на стеке
                pc++
            }

            Instructions.JL -> {
                val point = memory[stackMemoryStart + --sp]
                if (flag.getBits(2, 2) == 1u) {
                    pc = point
                } else pc++
                //flag = 1u
            }


            Instructions.HLT -> {
                flag = 0u
            }

        }
    }
}

data class ArrayItem(val name: String, val start: UInt, val elements: UIntArray)

@OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class, DelicateCoroutinesApi::class)
object CpuController {
    private lateinit var cpu: Cpu
    private var executionJob: Job? = null
    var executionDelay = 50L
        private set
    private val _screenState = MutableStateFlow(ScreenState())
    val screenState: StateFlow<ScreenState> = _screenState

    var program = uintArrayOf(
        0b0001u,
        89u,        // кладем в стек 89
        0b0010u,    // читаем значение из адреса, текущий макс элемент (первый)
        0b0001u,
        88u,        // кладем в стек 88
        0b0010u,    // читаем значение из адреса, размер массива
        0b0001u,
        89u,        // кладем в стек 89
        0b1101u,    // складываем, получаем адрес, в котором будет максимальный элемент (следующая ячейка после конца массива)
        0b0011u,    // записываем в ячейку после массива первый элемент

        0b0001u,
        88u,        // кладем в стек 88
        0b0010u,    // читаем значение из адреса

        0b1010u,    // уменьшаем вершину на 1 (13 команда-loop перед ней)
        0b0100u,    // дублируем вершину
        0b0001u,
        0u,         // кладем в стек 0
        0b1000u,    // сравниваем с нулем
        0b0001u,
        54u,        // кладем в стек 54 для переходу к концу
        0b1011u,    // если сравнения закончились, то переходим к выводу результата, иначе продолжаем
        0b0100u,    // дублируем вершину
        0b1001u,    // увеличиваем на 1
        0b0001u,
        88u,        // кладем в стек 88
        0b1101u,    // складываем вершины
        0b0010u,    // читаем значение из адреса
        0b0001u,
        88u,        // кладем в стек 88
        0b0010u,    // читаем значение из адреса, размер массива
        0b0001u,
        89u,        // кладем в стек 89
        0b1101u,    // складываем, получаем адрес, в котором будет максимальный элемент (следующая ячейка после конца массива)
        0b0010u,    // читаем значение из адреса
        0b1000u,    // сравниваем две вершины
        0b0001u,
        13u,        // кладем в стек 13 для гипотетического перехода
        0b1100u,    // если максимум больше, заново идем в цикл, иначе обновляем максимум
        0b0100u,    // дублируем вершину
        0b1001u,    // увеличиваем на 1
        0b0001u,
        88u,        // кладем в стек 88
        0b1101u,    // складываем вершины
        0b0010u,    // читаем значение из адреса
        0b0001u,
        88u,        // кладем в стек 88
        0b0010u,    // читаем значение из адреса, размер массива
        0b0001u,
        89u,        // кладем в стек 89
        0b1101u,    // складываем, получаем адрес, в котором будет максимальный элемент (следующая ячейка после конца массива)
        0b0011u,    // записываем текущее наибольшее значение
        0b0001u,
        13u,        // кладем в стек 13 для перехода
        0b0110u,    // заново идем в цикл

        0b0001u,    // команда номер 54 финальный вывод (END перед ней)
        88u,        // кладем в стек 88
        0b0010u,    // читаем значение из адреса, размер массива
        0b0001u,
        89u,        // кладем в стек 89
        0b1101u,    // складываем, получаем адрес, в котором будет максимальный элемент (следующая ячейка после конца массива)
        0b0010u,    // читаем значение из адреса
        0b0111u,    // выводим результат
        0b1111u
    )

    var array = listOf(ArrayItem("data", 88u, uintArrayOf(
        10u, 77u, 15u, 3u, 18u, 7u, 1u, 111u, 53u, 11u, 21u
    )))

    init {
        reset()
    }

    fun resetFile(filePath: String) {
        val lines = File(filePath).readLines().filter { it.isNotBlank() && !it.startsWith("//") }
        val program = mutableListOf<UInt>()
        val data = mutableListOf<ArrayItem>()
        val labels = mutableMapOf<String, Int>()

        // первый цикл для меток и массивов
        var address = 0
        for (line in lines) {
            val parts = line.split(" ")
            if (parts.size > 1 && !Instructions.values().map { it.name }.contains(parts[0])) {
                if(parts[1].split(",").size > 1 && !parts[1].startsWith(",") && !parts[1].endsWith(",")) {
                    val elements = parts[1].trim().split(",").map { it.trim().toUInt() }
                    data.add(
                        ArrayItem(
                            parts[0],
                            if(data.isEmpty()) cpu.stackMemoryEnd + 1u else data.last().start + data.last().elements.size.toUInt() + 3u,
                            elements.toUIntArray()
                        )
                    )
                }
            }  else {
                if (line.endsWith(":")) {
                    val label = line.removeSuffix(":")
                    labels[label] = address
                }
                val parts = line.split(" ")
                if(parts.size > 1){
                    if(parts[0] == "JL" || parts[0] == "JG" || parts[0] == "JE" || parts[0] == "JE") {
                        address++
                    }
                    address++
                }
                address++
            }
        }

        for (line in lines) {
            val parts = line.split(" ")
            if (Instructions.values().any { it.name == parts[0] }) {
                val instruction = Instructions.valueOf(parts[0])
                when (instruction) {
                    Instructions.PUSH -> {
                        if (parts.size > 1) {
                            program.add(instruction.code)
                            val parts2 = parts[1].split("+")
                            if(data.any{it.name == parts2[0]}){
                                if(parts2.size > 1){
                                    if(parts2[1].toIntOrNull() != null){
                                        program.add(data.find{it.name==parts2[0]}!!.start + parts2[1].toUInt())
                                    } else error("Undefined increase: ${parts[1]}")
                                } else program.add(cpu.stackMemoryEnd + 1u)
                            } else program.add(parts[1].toUInt())
                        }
                    }

                    Instructions.JL, Instructions.JG, Instructions.JE, Instructions.JMP -> {
                        if (parts.size > 1) {
                            if (labels[parts[1]]?.toUInt() != null) {
                                program.add(Instructions.PUSH.code)
                                program.add(labels[parts[1]]?.toUInt()!!)
                                program.add(instruction.code)
                            } else {
                                error("Undefined label: ${parts[1]}")
                            } }
                    }
                    else -> program.add(instruction.code)
                }
            }
        }

        reset(program.toUIntArray(), data)
    }


    fun reset(program: UIntArray = uintArrayOf(), array: List<ArrayItem> = listOf()) {
        cpu = Cpu()
        if (program.isEmpty() and array.isEmpty()) {
            for(i in this.array){
                cpu.loadArray(i)
            }
            cpu.loadProgram(this.program)
        } else {
            for(i in array){
                cpu.loadArray(i)
            }
            cpu.loadProgram(program)
        }
        updateState()
        pause()
    }

    private fun updateState() {

        val pcRegister = Register("PC", cpu.pc.toString(16).uppercase())
        val spRegister = Register("SP", cpu.sp.toString(16).uppercase())
        val flag = Register("FLAG", value = cpu.flag.toString(2).padStart(3, '0'))

        val stackList = cpu.memory.slice(cpu.stackMemoryStart.toInt()..cpu.stackMemoryEnd.toInt()).take(cpu.sp.toInt())
            .mapIndexed { index, value ->
                Register("S$index", value.toHexString())
            }

        val memoryList = cpu.memory.mapIndexed { index, value ->
            MemoryCell(index.toString(), value.toHexString())
        }

        val commands = cpu.memory.take(cpu.stackMemoryStart.toInt() - 1)
        val commandsList = commands.takeWhile { it != 0b1111u }
            .plus(commands.find { it == 0b1111u })
            .filterNotNull()
            .mapIndexed { index, value ->
                CommandState(
                    index = index,
                    name = if (index > 0 && commands[index - 1] == 0b0001u)
                        value.toInt().toString()
                    else
                        Instructions.values().find { it.code == value }?.name ?: value.toInt().toString(),
                    hexValue = value.toHexString(),
                    isCurrent = index == cpu.pc.toInt()
                )
            }

        _screenState.value = ScreenState(
            generalRegisters = listOf(pcRegister, spRegister, flag),
            memory = memoryList,
            stack = stackList,
            commands = commandsList,
            output = cpu.output,
        )
    }

    fun resume() {
        if (_screenState.value.isHalted && executionJob == null && cpu.flag.getBits(0, 0) == 1u) {
            _screenState.value = _screenState.value.copy(isHalted = false)
            executionJob = GlobalScope.launch {
                while (!_screenState.value.isHalted) {
                    delay(executionDelay)
                    cpu.executeCommand()
                    updateState()
                }
            }
        }
    }

    fun pause() {
        _screenState.value = _screenState.value.copy(isHalted = true)
        executionJob?.cancel()
        executionJob = null
    }

    fun next() {
        if (_screenState.value.isHalted && cpu.flag.getBits(0, 0) == 1u) {
            cpu.executeCommand()
            updateState()
            _screenState.value = _screenState.value.copy(isHalted = true)
        }
    }
}
