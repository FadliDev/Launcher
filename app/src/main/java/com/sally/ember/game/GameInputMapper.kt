package com.sally.ember.game

import android.view.InputDevice
import android.view.KeyEvent
import net.kdt.pojavlaunch.LwjglGlfwKeycode as K

/** Maps Android hardware keyboards/gamepads to the same GLFW input path as touch controls. */
internal object GameInputMapper {
    fun keyboardKey(code: Int): Int? = when {
        code in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> K.GLFW_KEY_A.toInt() + (code - KeyEvent.KEYCODE_A)
        code == KeyEvent.KEYCODE_0 -> K.GLFW_KEY_0.toInt()
        code in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9 -> K.GLFW_KEY_1.toInt() + (code - KeyEvent.KEYCODE_1)
        code in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> K.GLFW_KEY_F1.toInt() + (code - KeyEvent.KEYCODE_F1)
        else -> when (code) {
            KeyEvent.KEYCODE_SPACE -> K.GLFW_KEY_SPACE.toInt()
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> K.GLFW_KEY_ENTER.toInt()
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> K.GLFW_KEY_ESCAPE.toInt()
            KeyEvent.KEYCODE_TAB -> K.GLFW_KEY_TAB.toInt()
            KeyEvent.KEYCODE_DEL -> K.GLFW_KEY_BACKSPACE.toInt()
            KeyEvent.KEYCODE_FORWARD_DEL -> K.GLFW_KEY_DELETE.toInt()
            KeyEvent.KEYCODE_INSERT -> K.GLFW_KEY_INSERT.toInt()
            KeyEvent.KEYCODE_DPAD_RIGHT -> K.GLFW_KEY_RIGHT.toInt()
            KeyEvent.KEYCODE_DPAD_LEFT -> K.GLFW_KEY_LEFT.toInt()
            KeyEvent.KEYCODE_DPAD_DOWN -> K.GLFW_KEY_DOWN.toInt()
            KeyEvent.KEYCODE_DPAD_UP -> K.GLFW_KEY_UP.toInt()
            KeyEvent.KEYCODE_PAGE_UP -> K.GLFW_KEY_PAGE_UP.toInt()
            KeyEvent.KEYCODE_PAGE_DOWN -> K.GLFW_KEY_PAGE_DOWN.toInt()
            KeyEvent.KEYCODE_MOVE_HOME -> K.GLFW_KEY_HOME.toInt()
            KeyEvent.KEYCODE_MOVE_END -> K.GLFW_KEY_END.toInt()
            KeyEvent.KEYCODE_SHIFT_LEFT -> K.GLFW_KEY_LEFT_SHIFT.toInt()
            KeyEvent.KEYCODE_SHIFT_RIGHT -> K.GLFW_KEY_RIGHT_SHIFT.toInt()
            KeyEvent.KEYCODE_CTRL_LEFT -> K.GLFW_KEY_LEFT_CONTROL.toInt()
            KeyEvent.KEYCODE_CTRL_RIGHT -> K.GLFW_KEY_RIGHT_CONTROL.toInt()
            KeyEvent.KEYCODE_ALT_LEFT -> K.GLFW_KEY_LEFT_ALT.toInt()
            KeyEvent.KEYCODE_ALT_RIGHT -> K.GLFW_KEY_RIGHT_ALT.toInt()
            KeyEvent.KEYCODE_CAPS_LOCK -> K.GLFW_KEY_CAPS_LOCK.toInt()
            KeyEvent.KEYCODE_COMMA -> K.GLFW_KEY_COMMA.toInt()
            KeyEvent.KEYCODE_PERIOD -> K.GLFW_KEY_PERIOD.toInt()
            KeyEvent.KEYCODE_MINUS -> K.GLFW_KEY_MINUS.toInt()
            KeyEvent.KEYCODE_EQUALS -> K.GLFW_KEY_EQUAL.toInt()
            KeyEvent.KEYCODE_LEFT_BRACKET -> K.GLFW_KEY_LEFT_BRACKET.toInt()
            KeyEvent.KEYCODE_RIGHT_BRACKET -> K.GLFW_KEY_RIGHT_BRACKET.toInt()
            KeyEvent.KEYCODE_SLASH -> K.GLFW_KEY_SLASH.toInt()
            KeyEvent.KEYCODE_BACKSLASH -> K.GLFW_KEY_BACKSLASH.toInt()
            KeyEvent.KEYCODE_SEMICOLON -> K.GLFW_KEY_SEMICOLON.toInt()
            KeyEvent.KEYCODE_APOSTROPHE -> K.GLFW_KEY_APOSTROPHE.toInt()
            KeyEvent.KEYCODE_GRAVE -> K.GLFW_KEY_GRAVE_ACCENT.toInt()
            KeyEvent.KEYCODE_MENU -> K.GLFW_KEY_MENU.toInt()
            KeyEvent.KEYCODE_NUMPAD_0 -> K.GLFW_KEY_KP_0.toInt()
            KeyEvent.KEYCODE_NUMPAD_1 -> K.GLFW_KEY_KP_1.toInt()
            KeyEvent.KEYCODE_NUMPAD_2 -> K.GLFW_KEY_KP_2.toInt()
            KeyEvent.KEYCODE_NUMPAD_3 -> K.GLFW_KEY_KP_3.toInt()
            KeyEvent.KEYCODE_NUMPAD_4 -> K.GLFW_KEY_KP_4.toInt()
            KeyEvent.KEYCODE_NUMPAD_5 -> K.GLFW_KEY_KP_5.toInt()
            KeyEvent.KEYCODE_NUMPAD_6 -> K.GLFW_KEY_KP_6.toInt()
            KeyEvent.KEYCODE_NUMPAD_7 -> K.GLFW_KEY_KP_7.toInt()
            KeyEvent.KEYCODE_NUMPAD_8 -> K.GLFW_KEY_KP_8.toInt()
            KeyEvent.KEYCODE_NUMPAD_9 -> K.GLFW_KEY_KP_9.toInt()
            KeyEvent.KEYCODE_NUMPAD_DOT -> K.GLFW_KEY_KP_DECIMAL.toInt()
            KeyEvent.KEYCODE_NUMPAD_ADD -> K.GLFW_KEY_KP_ADD.toInt()
            KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> K.GLFW_KEY_KP_SUBTRACT.toInt()
            KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> K.GLFW_KEY_KP_MULTIPLY.toInt()
            KeyEvent.KEYCODE_NUMPAD_DIVIDE -> K.GLFW_KEY_KP_DIVIDE.toInt()
            KeyEvent.KEYCODE_NUMPAD_EQUALS -> K.GLFW_KEY_KP_EQUAL.toInt()
            KeyEvent.KEYCODE_NUM_LOCK -> K.GLFW_KEY_NUM_LOCK.toInt()
            KeyEvent.KEYCODE_SCROLL_LOCK -> K.GLFW_KEY_SCROLL_LOCK.toInt()
            KeyEvent.KEYCODE_PRINT_SCREEN -> K.GLFW_KEY_PRINT_SCREEN.toInt()
            KeyEvent.KEYCODE_PAUSE -> K.GLFW_KEY_PAUSE.toInt()
            else -> null
        }
    }

    fun gamepadKey(code: Int): HardwareAction? = when (code) {
        KeyEvent.KEYCODE_BUTTON_A -> HardwareAction.Key(K.GLFW_KEY_SPACE.toInt())
        KeyEvent.KEYCODE_BUTTON_B -> HardwareAction.Key(K.GLFW_KEY_LEFT_SHIFT.toInt())
        KeyEvent.KEYCODE_BUTTON_X -> HardwareAction.Mouse(K.GLFW_MOUSE_BUTTON_LEFT.toInt())
        KeyEvent.KEYCODE_BUTTON_Y -> HardwareAction.Mouse(K.GLFW_MOUSE_BUTTON_RIGHT.toInt())
        KeyEvent.KEYCODE_BUTTON_L1 -> HardwareAction.Key(K.GLFW_KEY_Q.toInt())
        KeyEvent.KEYCODE_BUTTON_R1 -> HardwareAction.Key(K.GLFW_KEY_E.toInt())
        KeyEvent.KEYCODE_BUTTON_L2 -> HardwareAction.Mouse(K.GLFW_MOUSE_BUTTON_RIGHT.toInt())
        KeyEvent.KEYCODE_BUTTON_R2 -> HardwareAction.Mouse(K.GLFW_MOUSE_BUTTON_LEFT.toInt())
        KeyEvent.KEYCODE_BUTTON_START -> HardwareAction.Key(K.GLFW_KEY_ESCAPE.toInt())
        KeyEvent.KEYCODE_BUTTON_SELECT -> HardwareAction.Key(K.GLFW_KEY_E.toInt())
        KeyEvent.KEYCODE_BUTTON_THUMBL -> HardwareAction.Key(K.GLFW_KEY_LEFT_CONTROL.toInt())
        KeyEvent.KEYCODE_DPAD_UP -> HardwareAction.Key(K.GLFW_KEY_UP.toInt())
        KeyEvent.KEYCODE_DPAD_DOWN -> HardwareAction.Key(K.GLFW_KEY_DOWN.toInt())
        KeyEvent.KEYCODE_DPAD_LEFT -> HardwareAction.Key(K.GLFW_KEY_LEFT.toInt())
        KeyEvent.KEYCODE_DPAD_RIGHT -> HardwareAction.Key(K.GLFW_KEY_RIGHT.toInt())
        else -> null
    }

    fun isGamepad(event: KeyEvent): Boolean {
        val source = event.source
        return (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }

    sealed class HardwareAction {
        data class Key(val code: Int) : HardwareAction()
        data class Mouse(val code: Int) : HardwareAction()
    }
}
