package com.example.taskapplication.data.api.response

import com.google.gson.annotations.SerializedName

data class SearchUsersResponse(
    @SerializedName("data")
    val data: List<UserResponse>,

    @SerializedName("meta")
    val meta: PaginationMeta
)
