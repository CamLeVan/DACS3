package com.example.taskapplication.data.api.response

import com.google.gson.annotations.SerializedName

/**
 * Lớp đại diện cho phản hồi từ API khi tìm kiếm người dùng
 * API trả về một đối tượng JSON thay vì một mảng
 */
data class SearchUsersResponse(
    // Dựa trên log, API có thể trả về dữ liệu trong các trường khác nhau
    // Thêm các annotation SerializedName để hỗ trợ nhiều tên trường khác nhau
    @SerializedName(value = "users", alternate = ["data", "results", "items"])
    val users: List<UserResponse> = emptyList(),

    @SerializedName(value = "total", alternate = ["total_count", "count"])
    val total: Int = 0,

    @SerializedName(value = "page", alternate = ["current_page"])
    val page: Int = 1,

    @SerializedName(value = "limit", alternate = ["per_page"])
    val limit: Int = 10,

    val message: String? = null,

    @SerializedName(value = "status", alternate = ["status_code"])
    val status: String? = null,

    // Thêm trường user để hỗ trợ trường hợp API trả về một người dùng duy nhất
    // trong trường "user" thay vì một mảng "users"
    val user: UserResponse? = null
)
