package com.sally.modforge.script

import kotlinx.serialization.Serializable

enum class PortType { EXEC, BOOL, INT, FLOAT, STRING, ITEM, BLOCK, ENTITY, PLAYER, POS }
enum class Category(val label: String) {
    EVENT("Event"), CONDITION("Kondisi"), ACTION("Aksi"), VARIABLE("Variabel"), MATH("Math/Logic")
}

data class Port(val name: String, val type: PortType)
/** params = field input literal pada node (mis. effect id, teks pesan). */
data class NodeSpec(
    val id: String, val title: String, val category: Category,
    val inputs: List<Port> = emptyList(), val outputs: List<Port> = emptyList(), val params: List<Port> = emptyList(),
)

// Format simpan graph: scripts/*.vs.json -> IR -> generator Java per loader x versi.
@Serializable data class GraphNode(val id: String, val type: String, val x: Float, val y: Float, val params: Map<String, String> = emptyMap())
@Serializable data class Edge(val fromNode: String, val fromPort: String, val toNode: String, val toPort: String)
@Serializable data class Graph(val nodes: List<GraphNode> = emptyList(), val edges: List<Edge> = emptyList())

object NodeCatalog {
    private fun p(n: String, t: PortType) = Port(n, t)
    private val exec = p("exec", PortType.EXEC)

    val all: List<NodeSpec> = listOf(
        NodeSpec("event.block_break", "Saat block dipecah", Category.EVENT, outputs = listOf(exec, p("player", PortType.PLAYER), p("pos", PortType.POS), p("block", PortType.BLOCK))),
        NodeSpec("event.item_use", "Saat item diklik kanan", Category.EVENT, outputs = listOf(exec, p("player", PortType.PLAYER), p("item", PortType.ITEM)), params = listOf(p("itemId", PortType.STRING))),
        NodeSpec("event.entity_death", "Saat entity mati", Category.EVENT, outputs = listOf(exec, p("entity", PortType.ENTITY), p("killer", PortType.ENTITY))),
        NodeSpec("event.player_join", "Saat player join", Category.EVENT, outputs = listOf(exec, p("player", PortType.PLAYER))),
        NodeSpec("event.tick", "Setiap tick", Category.EVENT, outputs = listOf(exec)),

        NodeSpec("cond.branch", "Jika (Branch)", Category.CONDITION, inputs = listOf(exec, p("cond", PortType.BOOL)), outputs = listOf(p("true", PortType.EXEC), p("false", PortType.EXEC))),
        NodeSpec("cond.holding_item", "Player memegang item", Category.CONDITION, inputs = listOf(p("player", PortType.PLAYER)), outputs = listOf(p("result", PortType.BOOL)), params = listOf(p("itemId", PortType.STRING))),
        NodeSpec("cond.chance", "Peluang acak", Category.CONDITION, outputs = listOf(p("result", PortType.BOOL)), params = listOf(p("percent", PortType.FLOAT))),

        NodeSpec("act.spawn_entity", "Spawn entity", Category.ACTION, inputs = listOf(exec, p("pos", PortType.POS)), outputs = listOf(exec, p("entity", PortType.ENTITY)), params = listOf(p("entityId", PortType.STRING))),
        NodeSpec("act.lightning", "Spawn petir", Category.ACTION, inputs = listOf(exec, p("pos", PortType.POS)), outputs = listOf(exec)),
        NodeSpec("act.give_effect", "Beri efek", Category.ACTION, inputs = listOf(exec, p("entity", PortType.ENTITY)), outputs = listOf(exec), params = listOf(p("effectId", PortType.STRING), p("seconds", PortType.INT), p("amplifier", PortType.INT))),
        NodeSpec("act.drop_item", "Drop item", Category.ACTION, inputs = listOf(exec, p("pos", PortType.POS)), outputs = listOf(exec), params = listOf(p("itemId", PortType.STRING), p("count", PortType.INT))),
        NodeSpec("act.send_message", "Kirim pesan", Category.ACTION, inputs = listOf(exec, p("player", PortType.PLAYER)), outputs = listOf(exec), params = listOf(p("text", PortType.STRING))),
        NodeSpec("act.teleport", "Teleport", Category.ACTION, inputs = listOf(exec, p("entity", PortType.ENTITY), p("pos", PortType.POS)), outputs = listOf(exec)),
        NodeSpec("act.set_block", "Ubah block", Category.ACTION, inputs = listOf(exec, p("pos", PortType.POS)), outputs = listOf(exec), params = listOf(p("blockId", PortType.STRING))),

        NodeSpec("var.get", "Ambil variabel", Category.VARIABLE, outputs = listOf(p("value", PortType.FLOAT)), params = listOf(p("name", PortType.STRING))),
        NodeSpec("var.set", "Set variabel", Category.VARIABLE, inputs = listOf(exec, p("value", PortType.FLOAT)), outputs = listOf(exec), params = listOf(p("name", PortType.STRING))),

        NodeSpec("math.add", "Tambah", Category.MATH, inputs = listOf(p("a", PortType.FLOAT), p("b", PortType.FLOAT)), outputs = listOf(p("result", PortType.FLOAT))),
        NodeSpec("math.compare", "Bandingkan", Category.MATH, inputs = listOf(p("a", PortType.FLOAT), p("b", PortType.FLOAT)), outputs = listOf(p("result", PortType.BOOL)), params = listOf(p("op", PortType.STRING))),
        NodeSpec("logic.and", "AND", Category.MATH, inputs = listOf(p("a", PortType.BOOL), p("b", PortType.BOOL)), outputs = listOf(p("result", PortType.BOOL))),
        NodeSpec("logic.not", "NOT", Category.MATH, inputs = listOf(p("a", PortType.BOOL)), outputs = listOf(p("result", PortType.BOOL))),
    )
}
