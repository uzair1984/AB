package com.ab.agentx.call

import android.telecom.Call

interface CallController {
    fun answer(call: Call)
    fun end(call: Call)
}
