package com.sally.ember.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import net.kdt.pojavlaunch.LwjglGlfwKeycode as K
import kotlin.math.roundToInt

/** Portable, versioned control layout. Coordinates are normalized against the game view. */
internal data class ControlSpec(
    var id: String,
    var label: String,
    var action: String,
    var x: Float,
    var y: Float,
    var widthDp: Int = 54,
    var heightDp: Int = 54,
    var opacity: Float = 0.68f,
    var background: Int = 0x66000000,
    var outline: Int = 0xB3FFFFFF.toInt(),
    var keyCodes: MutableList<Int> = mutableListOf(),
    var toggle: Boolean = false,
    var displayInGame: Boolean = false,
    var displayInMenu: Boolean = false,
    var group: String = "",
    var swipeable: Boolean = false,
    var passThrough: Boolean = false,
    var cornerRadius: Float = 48f,
    var strokeWidthDp: Float = 1f,
    var joystickAbsolute: Boolean = false,
    var joystickForwardLock: Boolean = false,
    var drawerOrientation: String = "LEFT"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("label", label); put("action", action); put("x", x.toDouble()); put("y", y.toDouble())
        put("widthDp", widthDp); put("heightDp", heightDp); put("opacity", opacity.toDouble())
        put("background", background); put("outline", outline); put("toggle", toggle)
        put("displayInGame", displayInGame); put("displayInMenu", displayInMenu); put("group", group)
        put("swipeable", swipeable); put("passThrough", passThrough)
        put("cornerRadius", cornerRadius.toDouble()); put("strokeWidthDp", strokeWidthDp.toDouble())
        put("joystickAbsolute", joystickAbsolute); put("joystickForwardLock", joystickForwardLock)
        put("drawerOrientation", drawerOrientation)
        put("keyCodes", JSONArray().apply { keyCodes.forEach { put(it) } })
    }

    companion object {
        fun fromJson(o: JSONObject): ControlSpec {
            val keys = o.optJSONArray("keyCodes") ?: JSONArray()
            return ControlSpec(
                id = o.optString("id", "control-${System.nanoTime()}"),
                label = o.optString("label", "BTN"), action = o.optString("action", "key"),
                x = o.optDouble("x", 0.5).toFloat().coerceIn(0f, 1f),
                y = o.optDouble("y", 0.5).toFloat().coerceIn(0f, 1f),
                widthDp = o.optInt("widthDp", 54).coerceIn(28, 260),
                heightDp = o.optInt("heightDp", 54).coerceIn(28, 200),
                opacity = o.optDouble("opacity", 0.68).toFloat().coerceIn(0.15f, 1f),
                background = o.optInt("background", 0x66000000), outline = o.optInt("outline", 0xB3FFFFFF.toInt()),
                keyCodes = MutableList(keys.length()) { i -> keys.optInt(i) },
                toggle = o.optBoolean("toggle", false), displayInGame = o.optBoolean("displayInGame", false),
                displayInMenu = o.optBoolean("displayInMenu", false), group = o.optString("group", ""),
                swipeable = o.optBoolean("swipeable", false), passThrough = o.optBoolean("passThrough", false),
                cornerRadius = o.optDouble("cornerRadius", 48.0).toFloat().coerceIn(0f, 100f),
                strokeWidthDp = o.optDouble("strokeWidthDp", 1.0).toFloat().coerceIn(0f, 8f),
                joystickAbsolute = o.optBoolean("joystickAbsolute", false),
                joystickForwardLock = o.optBoolean("joystickForwardLock", false),
                drawerOrientation = o.optString("drawerOrientation", "LEFT").uppercase().takeIf { it in setOf("DOWN", "LEFT", "UP", "RIGHT", "FREE") } ?: "LEFT"
            )
        }
    }
}

internal data class ControlProfile(
    var mouseSpeed: Float = 1.55f,
    var cameraSensitivity: Float = 1.10f,
    var scrollSensitivity: Float = 1.0f,
    var longPressMs: Int = 360,
    var directTouch: Boolean = false,
    /** Zalith's virtual-mouse/touchpad overlay; disabling it returns menu input to direct touch. */
    var touchpadEnabled: Boolean = true,
    var gesturesEnabled: Boolean = true,
    var doubleTapEnabled: Boolean = true,
    var hotbarEnabled: Boolean = true,
    var gyroEnabled: Boolean = false,
    var gyroSensitivity: Float = 1.0f,
    var gyroInvertX: Boolean = false,
    var gyroInvertY: Boolean = false,
    var joystickDeadZone: Float = 0.35f,
    var joystickSprint: Boolean = true,
    var buttonsAllCaps: Boolean = true,
    var pointerScale: Float = 1.0f,
    var pointerVisible: Boolean = true,
    var controlsVisible: Boolean = true,
    var controls: MutableList<ControlSpec> = defaultControls()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("schemaVersion", 1); put("mouseSpeed", mouseSpeed.toDouble()); put("cameraSensitivity", cameraSensitivity.toDouble())
        put("scrollSensitivity", scrollSensitivity.toDouble()); put("longPressMs", longPressMs)
        put("directTouch", directTouch); put("touchpadEnabled", touchpadEnabled)
        put("gesturesEnabled", gesturesEnabled); put("doubleTapEnabled", doubleTapEnabled)
        put("hotbarEnabled", hotbarEnabled); put("gyroEnabled", gyroEnabled); put("gyroSensitivity", gyroSensitivity.toDouble())
        put("gyroInvertX", gyroInvertX); put("gyroInvertY", gyroInvertY); put("joystickDeadZone", joystickDeadZone.toDouble())
        put("joystickSprint", joystickSprint); put("buttonsAllCaps", buttonsAllCaps); put("pointerScale", pointerScale.toDouble()); put("pointerVisible", pointerVisible)
        put("controlsVisible", controlsVisible)
        put("controls", JSONArray().apply { controls.forEach { put(it.toJson()) } })
    }

    fun save(context: Context) = runCatching {
        context.getSharedPreferences("sally_control_profile", Context.MODE_PRIVATE).edit()
            .putString("layout_json", toJson().toString()).apply()
    }

    companion object {
        fun load(context: Context): ControlProfile = runCatching {
            val raw = context.getSharedPreferences("sally_control_profile", Context.MODE_PRIVATE).getString("layout_json", null)
            if (raw.isNullOrBlank()) default() else fromJson(JSONObject(raw))
        }.getOrElse { default() }

        fun fromJson(o: JSONObject, screenWidth: Int = 1920, screenHeight: Int = 1080, screenDensity: Float = 1f): ControlProfile {
            // Also read ZalithLauncher/Pojav custom-control JSON so existing layouts can be imported.
            if (!o.has("schemaVersion") && (o.has("mControlDataList") || o.has("mDrawerDataList") || o.has("mJoystickDataList"))) {
                return fromZalithJson(o, screenWidth.coerceAtLeast(1), screenHeight.coerceAtLeast(1), screenDensity.coerceAtLeast(0.1f))
            }
            val array = o.optJSONArray("controls") ?: JSONArray()
            val controls = MutableList(array.length()) { i -> ControlSpec.fromJson(array.getJSONObject(i)) }
            return ControlProfile(
                mouseSpeed = o.optDouble("mouseSpeed", 1.55).toFloat().coerceIn(0.2f, 5f),
                cameraSensitivity = o.optDouble("cameraSensitivity", 1.1).toFloat().coerceIn(0.1f, 5f),
                scrollSensitivity = o.optDouble("scrollSensitivity", 1.0).toFloat().coerceIn(0.2f, 4f),
                longPressMs = o.optInt("longPressMs", 360).coerceIn(180, 1000),
                directTouch = o.optBoolean("directTouch", false), touchpadEnabled = o.optBoolean("touchpadEnabled", true),
                gesturesEnabled = o.optBoolean("gesturesEnabled", true),
                doubleTapEnabled = o.optBoolean("doubleTapEnabled", true), hotbarEnabled = o.optBoolean("hotbarEnabled", true),
                gyroEnabled = o.optBoolean("gyroEnabled", false), gyroSensitivity = o.optDouble("gyroSensitivity", 1.0).toFloat().coerceIn(0.1f, 4f),
                gyroInvertX = o.optBoolean("gyroInvertX", false), gyroInvertY = o.optBoolean("gyroInvertY", false),
                joystickDeadZone = o.optDouble("joystickDeadZone", 0.35).toFloat().coerceIn(0.1f, 0.8f),
                joystickSprint = o.optBoolean("joystickSprint", true), buttonsAllCaps = o.optBoolean("buttonsAllCaps", true),
                pointerScale = o.optDouble("pointerScale", 1.0).toFloat().coerceIn(0.5f, 3f),
                pointerVisible = o.optBoolean("pointerVisible", true), controlsVisible = o.optBoolean("controlsVisible", true),
                controls = if (controls.isEmpty()) defaultControls() else controls
            )
        }

        fun default(): ControlProfile = ControlProfile()

        private fun fromZalithJson(o: JSONObject, screenWidth: Int, screenHeight: Int, density: Float): ControlProfile {
            val result = ControlProfile(controls = mutableListOf())
            var serial = 0

            fun ratio(
                value: String?, axisWidth: Boolean, absolute: Double? = null,
                buttonWidthDp: Int = 52, buttonHeightDp: Int = 52
            ): Float {
                val extent = if (axisWidth) screenWidth else screenHeight
                val available = (extent - ((if (axisWidth) buttonWidthDp else buttonHeightDp) * density).roundToInt()).coerceAtLeast(1)
                if (absolute != null) {
                    // Convert legacy top-left pixels to normalized position in the available travel range.
                    return (absolute / available).toFloat().coerceIn(0f, 1f)
                }
                val expr = value.orEmpty().trim().lowercase()
                if (expr.isBlank()) return 0.5f

                val widthPx = buttonWidthDp * density
                val heightPx = buttonHeightDp * density
                val symbols = mapOf(
                    "top" to 0.0,
                    "left" to 0.0,
                    "right" to (screenWidth - widthPx).toDouble(),
                    "bottom" to (screenHeight - heightPx).toDouble(),
                    "width" to widthPx.toDouble(),
                    "height" to heightPx.toDouble(),
                    "screen_width" to screenWidth.toDouble(),
                    "screen_height" to screenHeight.toDouble(),
                    "margin" to (2f * density).toDouble(),
                    "preferred_scale" to 1.0
                )
                val expanded = Regex("\\$\\{([a-zA-Z_]+)}").replace(expr) { match ->
                    symbols[match.groupValues[1]]?.toString() ?: match.value
                }
                val absolutePx = runCatching { ArithmeticParser(expanded, density.toDouble()).parse() }.getOrNull()
                if (absolutePx != null && absolutePx.isFinite()) {
                    return (absolutePx / available).toFloat().coerceIn(0f, 1f)
                }
                // Some hand-written layouts use textual anchors rather than arithmetic.
                if (expr.contains("center")) return 0.45f
                if (expr.contains("right")) return 0.9f
                if (expr.contains("bottom")) return 0.85f
                if (expr.contains("margin")) return 0.035f
                return 0.5f
            }

            fun actionFor(rawKeys: List<Int>): Pair<String, MutableList<Int>> {
                // Zalith pads keycode arrays with GLFW_KEY_UNKNOWN (0); do not turn those into live key presses.
                val positive = rawKeys.filter { it > K.GLFW_KEY_UNKNOWN.toInt() && it <= K.GLFW_KEY_LAST.toInt() }.distinct().toMutableList()
                val special = rawKeys.firstOrNull { it < 0 }
                if (special != null) {
                    val action = when (special) {
                        -1 -> "keyboard"; -2 -> "toggle-controls"; -3 -> "mouse-left"; -4 -> "mouse-right"
                        -5 -> "toggle-mouse"; -6 -> "mouse-middle"; -7 -> "scroll-up"; -8 -> "scroll-down"
                        -9 -> "menu"; else -> "key"
                    }
                    return if (action == "key") action to positive else action to mutableListOf()
                }
                return "key" to positive
            }

            fun addButton(item: JSONObject, prefix: String = "zalith", group: String = "") {
                val rawKeys = item.optJSONArray("keycodes") ?: item.optJSONArray("keyCodes") ?: JSONArray()
                val keyList = MutableList(rawKeys.length()) { i -> rawKeys.optInt(i) }
                val (action, codes) = actionFor(keyList)
                val label = item.optString("name", item.optString("label", "BTN")).ifBlank { "BTN" }
                val width = item.optInt("width", item.optInt("widthDp", 52)).coerceIn(28, 260)
                val height = item.optInt("height", item.optInt("heightDp", if (action == "hotbar") 38 else width)).coerceIn(28, 200)
                val hasXY = item.has("x") && item.has("y")
                val x = ratio(item.optString("dynamicX", null), true, if (hasXY) item.optDouble("x") else null, width, height)
                val y = ratio(item.optString("dynamicY", null), false, if (hasXY) item.optDouble("y") else null, width, height)
                val scaleOpacity = if (item.has("opacity")) item.optDouble("opacity", 0.68).toFloat() else if (item.has("transparency")) (100 - item.optInt("transparency")).coerceIn(15, 100) / 100f else 0.68f
                val game = item.optBoolean("displayInGame", true)
                val menu = item.optBoolean("displayInMenu", false)
                result.controls.add(ControlSpec(
                    id = "$prefix-${serial++}", label = label.take(12), action = action, x = x, y = y,
                    widthDp = width, heightDp = height, opacity = scaleOpacity.coerceIn(0.15f, 1f),
                    background = item.optInt("bgColor", 0x4D000000), outline = item.optInt("strokeColor", 0xB3FFFFFF.toInt()),
                    keyCodes = codes, toggle = item.optBoolean("isToggle", false), displayInGame = game,
                    displayInMenu = menu, group = group, swipeable = item.optBoolean("isSwipeable", false),
                    passThrough = item.optBoolean("passThruEnabled", false),
                    cornerRadius = item.optDouble("cornerRadius", 48.0).toFloat().coerceIn(0f, 100f),
                    strokeWidthDp = item.optDouble("strokeWidth", 1.0).toFloat().coerceIn(0f, 8f)
                ))
            }

            val buttons = o.optJSONArray("mControlDataList") ?: JSONArray()
            for (i in 0 until buttons.length()) runCatching { addButton(buttons.getJSONObject(i)) }

            val joystickArray = o.optJSONArray("mJoystickDataList") ?: JSONArray()
            for (i in 0 until joystickArray.length()) runCatching {
                val item = joystickArray.getJSONObject(i)
                val stickWidth = item.optInt("width", 144).coerceIn(72, 220)
                val stickHeight = item.optInt("height", 144).coerceIn(72, 220)
                result.controls.add(ControlSpec(
                    id = "zalith-stick-${serial++}", label = "", action = "joystick",
                    x = ratio(item.optString("dynamicX", null), true, if (item.has("x")) item.optDouble("x") else null, stickWidth, stickHeight),
                    y = ratio(item.optString("dynamicY", null), false, if (item.has("y")) item.optDouble("y") else null, stickWidth, stickHeight),
                    widthDp = stickWidth, heightDp = stickHeight,
                    opacity = item.optDouble("opacity", 0.68).toFloat().coerceIn(0.15f, 1f),
                    background = item.optInt("bgColor", 0x4D000000), outline = item.optInt("strokeColor", 0xB3FFFFFF.toInt()),
                    displayInGame = item.optBoolean("displayInGame", true), displayInMenu = item.optBoolean("displayInMenu", false),
                    cornerRadius = item.optDouble("cornerRadius", 48.0).toFloat().coerceIn(0f, 100f),
                    strokeWidthDp = item.optDouble("strokeWidth", 1.0).toFloat().coerceIn(0f, 8f),
                    joystickAbsolute = item.optBoolean("absolute", false),
                    joystickForwardLock = item.optBoolean("forwardLock", false)
                ))
            }

            val drawers = o.optJSONArray("mDrawerDataList") ?: JSONArray()
            for (i in 0 until drawers.length()) runCatching {
                val drawer = drawers.getJSONObject(i)
                val properties = drawer.optJSONObject("properties") ?: drawer
                val group = "zalith-drawer-$i"
                val drawerOrientation = when (drawer.optString("orientation", "LEFT").uppercase()) {
                    "0", "DOWN" -> "DOWN"
                    "1", "LEFT" -> "LEFT"
                    "2", "UP" -> "UP"
                    "3", "RIGHT" -> "RIGHT"
                    "4", "FREE" -> "FREE"
                    else -> "LEFT"
                }
                val parent = ControlSpec(
                    id = "zalith-drawer-button-${serial++}", label = properties.optString("name", "+"), action = "drawer:$group",
                    x = ratio(properties.optString("dynamicX", null), true, if (properties.has("x")) properties.optDouble("x") else null,
                        properties.optInt("width", 52).coerceIn(32, 100), properties.optInt("height", 52).coerceIn(32, 100)),
                    y = ratio(properties.optString("dynamicY", null), false, if (properties.has("y")) properties.optDouble("y") else null,
                        properties.optInt("width", 52).coerceIn(32, 100), properties.optInt("height", 52).coerceIn(32, 100)),
                    widthDp = properties.optInt("width", 52).coerceIn(32, 100), heightDp = properties.optInt("height", 52).coerceIn(32, 100),
                    opacity = properties.optDouble("opacity", 0.68).toFloat().coerceIn(0.15f, 1f),
                    background = properties.optInt("bgColor", 0x4D000000), outline = properties.optInt("strokeColor", 0xB3FFFFFF.toInt()),
                    displayInGame = properties.optBoolean("displayInGame", true), displayInMenu = properties.optBoolean("displayInMenu", false),
                    cornerRadius = properties.optDouble("cornerRadius", 48.0).toFloat().coerceIn(0f, 100f),
                    strokeWidthDp = properties.optDouble("strokeWidth", 1.0).toFloat().coerceIn(0f, 8f),
                    drawerOrientation = drawerOrientation
                )
                result.controls.add(parent)
                val sub = drawer.optJSONArray("buttonProperties") ?: JSONArray()
                for (j in 0 until sub.length()) runCatching {
                    val before = result.controls.size
                    addButton(sub.getJSONObject(j), "zalith-drawer-$i", group)
                    if (result.controls.size > before) result.controls.last().let {
                        it.widthDp = parent.widthDp; it.heightDp = parent.heightDp
                    }
                }
            }

            if (result.controls.isEmpty()) result.controls = defaultControls()
            return result
        }

        fun preset(name: String): ControlProfile {
            val profile = default()
            when (name) {
                "Minimal" -> {
                    profile.controls.removeAll { it.id in setOf("drop", "f5", "chat", "sneak", "use") }
                    profile.controls.firstOrNull { it.id == "hit" }?.let { it.widthDp = 72; it.heightDp = 72 }
                    profile.controls.firstOrNull { it.id == "joystick" }?.let { it.widthDp = 132; it.heightDp = 132 }
                }
                "Mouse & Keyboard" -> {
                    profile.controls.removeAll { it.action == "joystick" || it.action == "hotbar" }
                    profile.directTouch = false
                }
                "Compact Touch" -> {
                    profile.controls.forEach { spec ->
                        if (spec.action !in setOf("settings", "hotbar")) {
                            spec.widthDp = (spec.widthDp * 0.82f).toInt().coerceAtLeast(32)
                            spec.heightDp = (spec.heightDp * 0.82f).toInt().coerceAtLeast(32)
                        }
                    }
                }
            }
            return profile
        }

        /** Small arithmetic evaluator for Zalith dynamicX/dynamicY expressions (no code execution). */
        private class ArithmeticParser(private val source: String, private val density: Double) {
            private var index = 0

            fun parse(): Double {
                val value = expression()
                skipSpaces()
                require(index == source.length) { "Unexpected character in layout expression" }
                return value
            }

            private fun expression(): Double {
                var value = term()
                while (true) {
                    skipSpaces()
                    value = when (peek()) {
                        '+' -> { index++; value + term() }
                        '-' -> { index++; value - term() }
                        else -> return value
                    }
                }
            }

            private fun term(): Double {
                var value = unary()
                while (true) {
                    skipSpaces()
                    value = when (peek()) {
                        '*' -> { index++; value * unary() }
                        '/' -> { index++; value / unary() }
                        else -> return value
                    }
                }
            }

            private fun unary(): Double {
                skipSpaces()
                return when (peek()) {
                    '+' -> { index++; unary() }
                    '-' -> { index++; -unary() }
                    else -> primary()
                }
            }

            private fun primary(): Double {
                skipSpaces()
                require(index < source.length) { "Missing number in layout expression" }
                if (peek() == '(') {
                    index++
                    val value = expression()
                    skipSpaces()
                    require(peek() == ')') { "Missing closing parenthesis" }
                    index++
                    return value
                }
                if (peek().isLetter()) {
                    val start = index
                    while (peek().isLetter()) index++
                    val fn = source.substring(start, index)
                    skipSpaces()
                    require(peek() == '(') { "Unsupported symbol: $fn" }
                    index++
                    val argument = expression()
                    skipSpaces()
                    require(peek() == ')') { "Missing function parenthesis" }
                    index++
                    return when (fn) {
                        "dp" -> argument / density
                        "px" -> argument * density
                        else -> error("Unsupported function: $fn")
                    }
                }
                val start = index
                var sawDot = false
                while (index < source.length) {
                    val ch = source[index]
                    if (ch.isDigit()) index++
                    else if (ch == '.' && !sawDot) { sawDot = true; index++ }
                    else break
                }
                require(index > start) { "Expected a number in layout expression" }
                return source.substring(start, index).toDouble()
            }

            private fun peek(): Char = source.getOrElse(index) { '\u0000' }
            private fun skipSpaces() { while (index < source.length && source[index].isWhitespace()) index++ }
        }

        private fun defaultControls(): MutableList<ControlSpec> {
            fun c(id: String, label: String, action: String, x: Float, y: Float, size: Int = 52,
                  game: Boolean = true, menu: Boolean = false, toggle: Boolean = false, key: Int? = null,
                  group: String = "", w: Int = size, h: Int = size) = ControlSpec(
                id, label, action, x, y, w, h, 0.68f, 0x66000000, 0xB3FFFFFF.toInt(),
                mutableListOf<Int>().apply { if (key != null) add(key) }, toggle, game, menu, group
            )
            return mutableListOf(
                c("settings", "☰", "settings", 0.018f, 0.035f, 44, true, true),
                c("joystick", "", "joystick", 0.045f, 0.655f, 146, true, false),
                c("jump", "JUMP", "key", 0.895f, 0.715f, 64, true, false, key=K.GLFW_KEY_SPACE.toInt()),
                c("hit", "HIT", "mouse-left", 0.895f, 0.525f, 64, true, false),
                c("use", "USE", "mouse-right", 0.785f, 0.655f, 56, true, false),
                c("sneak", "SNEAK", "key", 0.785f, 0.815f, 50, true, false, true, K.GLFW_KEY_LEFT_SHIFT.toInt()),
                c("escape", "ESC", "key", 0.715f, 0.035f, 38, true, true, key=K.GLFW_KEY_ESCAPE.toInt()),
                c("inventory", "E", "key", 0.765f, 0.035f, 38, true, true, key=K.GLFW_KEY_E.toInt()),
                c("chat", "T", "key", 0.815f, 0.035f, 38, true, true, key=K.GLFW_KEY_T.toInt()),
                c("keyboard", "KB", "keyboard", 0.865f, 0.035f, 38, true, true),
                c("mouse", "MOUSE", "toggle-mouse", 0.915f, 0.035f, 38, false, true),
                c("drop", "Q", "key", 0.715f, 0.125f, 38, true, false, key=K.GLFW_KEY_Q.toInt()),
                c("f5", "F5", "key", 0.765f, 0.125f, 38, true, false, key=K.GLFW_KEY_F5.toInt()),
                c("drawer", "+", "drawer:extras", 0.815f, 0.125f, 38, true, true),
                c("f3", "F3", "key", 0.715f, 0.215f, 38, true, false, key=K.GLFW_KEY_F3.toInt(), group="extras"),
                c("f11", "F11", "key", 0.765f, 0.215f, 38, true, false, key=K.GLFW_KEY_F11.toInt(), group="extras"),
                c("f1", "F1", "key", 0.815f, 0.215f, 38, true, false, key=K.GLFW_KEY_F1.toInt(), group="extras"),
                c("hotbar", "", "hotbar", 0.335f, 0.91f, 36, true, false, w=330, h=38)
            )
        }
    }
}
