package com.example.taskapplication.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Định dạng timestamp thành chuỗi ngày tháng
 * @param timestamp Timestamp tính bằng milliseconds
 * @param pattern Mẫu định dạng (mặc định: dd/MM/yyyy)
 * @return Chuỗi ngày tháng đã định dạng
 */
fun formatDate(timestamp: Long, pattern: String = "dd/MM/yyyy"): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat(pattern, Locale("vi"))
    return formatter.format(date)
}

/**
 * Định dạng timestamp thành chuỗi ngày tháng có giờ
 * @param timestamp Timestamp tính bằng milliseconds
 * @return Chuỗi ngày tháng định dạng "dd/MM/yyyy HH:mm"
 */
fun formatDateWithTime(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi"))
    return formatter.format(date)
}

/**
 * Định dạng ngày tháng thành chuỗi với định dạng thân thiện
 * @param timestamp Thời gian tính bằng milliseconds
 * @return Chuỗi ngày tháng thân thiện (Hôm nay, Ngày mai, Hôm qua, hoặc dd/MM/yyyy)
 */
fun formatDateFriendly(timestamp: Long): String {
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val tomorrow = Calendar.getInstance()
    tomorrow.add(Calendar.DAY_OF_YEAR, 1)
    tomorrow.set(Calendar.HOUR_OF_DAY, 0)
    tomorrow.set(Calendar.MINUTE, 0)
    tomorrow.set(Calendar.SECOND, 0)
    tomorrow.set(Calendar.MILLISECOND, 0)

    val yesterday = Calendar.getInstance()
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    yesterday.set(Calendar.HOUR_OF_DAY, 0)
    yesterday.set(Calendar.MINUTE, 0)
    yesterday.set(Calendar.SECOND, 0)
    yesterday.set(Calendar.MILLISECOND, 0)

    val date = Calendar.getInstance()
    date.timeInMillis = timestamp
    date.set(Calendar.HOUR_OF_DAY, 0)
    date.set(Calendar.MINUTE, 0)
    date.set(Calendar.SECOND, 0)
    date.set(Calendar.MILLISECOND, 0)

    return when (date.timeInMillis) {
        today.timeInMillis -> "Hôm nay"
        tomorrow.timeInMillis -> "Ngày mai"
        yesterday.timeInMillis -> "Hôm qua"
        else -> formatDate(timestamp)
    }
}

/**
 * Định dạng ngày tháng thành chuỗi với định dạng thời gian tương đối
 * @param timestamp Thời gian tính bằng milliseconds
 * @return Chuỗi thời gian tương đối (vài giây trước, 5 phút trước, 2 giờ trước, v.v.)
 */
fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "vài giây trước"
        diff < TimeUnit.HOURS.toMillis(1) -> "${diff / TimeUnit.MINUTES.toMillis(1)} phút trước"
        diff < TimeUnit.DAYS.toMillis(1) -> "${diff / TimeUnit.HOURS.toMillis(1)} giờ trước"
        diff < TimeUnit.DAYS.toMillis(7) -> "${diff / TimeUnit.DAYS.toMillis(1)} ngày trước"
        diff < TimeUnit.DAYS.toMillis(30) -> "${diff / TimeUnit.DAYS.toMillis(7)} tuần trước"
        diff < TimeUnit.DAYS.toMillis(365) -> "${diff / TimeUnit.DAYS.toMillis(30)} tháng trước"
        else -> "${diff / TimeUnit.DAYS.toMillis(365)} năm trước"
    }
}

/**
 * Chuyển đổi chuỗi ngày tháng thành timestamp
 * @param dateString Chuỗi ngày tháng
 * @param pattern Mẫu định dạng (mặc định: dd/MM/yyyy)
 * @return Timestamp tính bằng milliseconds hoặc null nếu không thể chuyển đổi
 */
fun parseDate(dateString: String, pattern: String = "dd/MM/yyyy"): Long? {
    return try {
        val formatter = SimpleDateFormat(pattern, Locale("vi"))
        formatter.parse(dateString)?.time
    } catch (e: Exception) {
        null
    }
}

/**
 * Chuyển đổi timestamp thành chuỗi ISO 8601
 * @param timestamp Thời gian tính bằng milliseconds
 * @return Chuỗi ngày tháng theo định dạng ISO 8601
 */
fun toIsoString(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(timestamp))
}

/**
 * Chuyển đổi chuỗi ISO 8601 thành timestamp
 * @param isoString Chuỗi ngày tháng theo định dạng ISO 8601
 * @return Timestamp tính bằng milliseconds hoặc null nếu không thể chuyển đổi
 */
fun fromIsoString(isoString: String): Long? {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        formatter.parse(isoString)?.time
    } catch (e: Exception) {
        try {
            // Thử với định dạng không có phần mili giây
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            formatter.parse(isoString)?.time
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Kiểm tra xem một ngày có phải là quá hạn không
 * @param timestamp Ngày hạn tính bằng milliseconds
 * @return true nếu quá hạn, false nếu không
 */
fun isOverdue(timestamp: Long): Boolean {
    return timestamp < System.currentTimeMillis()
}

/**
 * Lấy ngày đầu tiên của tuần hiện tại
 * @return Timestamp của ngày đầu tiên trong tuần
 */
fun getStartOfWeek(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/**
 * Lấy ngày cuối cùng của tuần hiện tại
 * @return Timestamp của ngày cuối cùng trong tuần
 */
fun getEndOfWeek(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    calendar.add(Calendar.DAY_OF_WEEK, 6)
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}

/**
 * Lấy ngày đầu tiên của tháng hiện tại
 * @return Timestamp của ngày đầu tiên trong tháng
 */
fun getStartOfMonth(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/**
 * Lấy ngày cuối cùng của tháng hiện tại
 * @return Timestamp của ngày cuối cùng trong tháng
 */
fun getEndOfMonth(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}
