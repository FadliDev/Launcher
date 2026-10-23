package com.sally.freshlauncher.core

import org.json.JSONArray

/** Evaluasi "rules" dari metadata versi Mojang. Hanya OS "linux" yang dianggap cocok. */
object Rules {
    fun allowed(rules: JSONArray?): Boolean {
        if (rules == null || rules.length() == 0) return true
        var result = false
        for (i in 0 until rules.length()) {
            val rule = rules.getJSONObject(i)
            if (rule.has("features")) continue
            val os = rule.optJSONObject("os")
            val applies = os == null || os.optString("name") == "linux"
            if (applies) result = rule.getString("action") == "allow"
        }
        return result
    }
}
