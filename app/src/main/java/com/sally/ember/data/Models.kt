package com.sally.ember.data

data class Account(val name: String, val uuid: String)

data class McVersion(val id: String, val type: String, val url: String)

sealed interface DlState {
    data object Idle : DlState
    data class Running(val label: String, val done: Int, val total: Int) : DlState
    data class Failed(val msg: String) : DlState
}
