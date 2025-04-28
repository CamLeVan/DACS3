package com.example.taskapplication.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Định dạng timestamp thành chuỗi ngày tháng
 * @param timestamp Timestamp tính bằng milliseconds
 * @return Chuỗi ngày tháng định dạng "dd/MM/yyyy"
 */
fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("vi"))
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
 * Kiểm tra xem ngày đã qua hạn chưa
 * @param timestamp Timestamp tính bằng milliseconds
 * @return true nếu ngày đã qua hạn, false nếu chưa
 */
fun isOverdue(timestamp: Long): Boolean {
    return timestamp < System.currentTimeMillis()
}
