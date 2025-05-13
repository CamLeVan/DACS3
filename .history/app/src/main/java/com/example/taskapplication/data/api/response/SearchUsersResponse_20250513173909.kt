package com.example.taskapplication.data.api.response

/**
 * Lớp đại diện cho phản hồi từ API khi tìm kiếm người dùng
 * API trả về một đối tượng JSON thay vì một mảng
 */
data class SearchUsersResponse(
    val users: List<UserResponse> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 10,
    val message: String? = null,
    val status: String? = null
)
