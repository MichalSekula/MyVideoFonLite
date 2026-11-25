package pl.sekula.myvideofonlite.data.tuya

import com.thingclips.smart.android.device.bean.SchemaBean
import com.thingclips.smart.sdk.bean.DeviceBean

/**
 * Wyszukiwanie punktów danych (DP) w schemacie urządzenia.
 *
 * Numery DP różnią się między produktami, dlatego szukamy ich po kodzie i nazwie,
 * a numer znany z VERUS One (148 – „Lock Switch”) jest tylko ostatnią deską ratunku.
 */
internal object DpMap {
    private val GATE_CODES = listOf("accessory_lock", "lock_switch", "door_unlock", "unlock", "wireless_unlock")
    private const val GATE_FALLBACK_ID = "148"
    private val RING_CODES = listOf("doorbell_active", "doorbell_ring_exist", "doorbell_pic")
    private val MOTION_CODES = listOf("movement_detect_pic", "alarm_message")

    private fun schema(bean: DeviceBean): Collection<SchemaBean> = bean.schemaMap?.values.orEmpty()

    private fun byCodes(bean: DeviceBean, codes: List<String>): String? {
        val all = schema(bean)
        for (code in codes) all.firstOrNull { it.code.equals(code, ignoreCase = true) }?.let { return it.id }
        return null
    }

    fun ringDpId(bean: DeviceBean): String? = byCodes(bean, RING_CODES)

    fun motionDpId(bean: DeviceBean): String? = byCodes(bean, MOTION_CODES)

    fun gateDpId(bean: DeviceBean): String? {
        byCodes(bean, GATE_CODES)?.let { return it }
        val writableBool = schema(bean).filter { it.schemaType == "bool" && it.mode?.contains("w") == true }
        writableBool.firstOrNull { it.name?.contains("lock", ignoreCase = true) == true }?.let { return it.id }
        return schema(bean).firstOrNull { it.id == GATE_FALLBACK_ID }?.id
    }
}
