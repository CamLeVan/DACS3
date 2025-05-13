package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamDao
import com.example.taskapplication.data.database.dao.TeamMemberDao
import com.example.taskapplication.data.database.dao.UserDao
import com.example.taskapplication.data.database.entities.TeamEntity
import com.example.taskapplication.data.database.entities.TeamMemberEntity
import com.example.taskapplication.data.local.dao.TeamRoleHistoryDao
import com.example.taskapplication.data.local.entity.TeamRoleHistoryEntity
import com.example.taskapplication.data.mapper.TeamRoleHistoryMapper.toTeamRoleHistory
import com.example.taskapplication.data.mapper.TeamRoleHistoryMapper.toTeamRoleHistoryEntity
import com.example.taskapplication.data.mapper.TeamRoleHistoryMapper.toTeamRoleHistoryList
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Team
import com.example.taskapplication.domain.model.TeamPermission
import com.example.taskapplication.domain.model.TeamRole
import com.example.taskapplication.domain.model.TeamRoleHistory
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.TeamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepositoryImpl @Inject constructor(
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
    private val userDao: UserDao,
    private val teamRoleHistoryDao: TeamRoleHistoryDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : TeamRepository {

    private val TAG = "TeamRepository"

    override fun getAllTeams(): Flow<List<Team>> {
        return teamDao.getAllTeams()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getTeamsForUser(userId: String): Flow<List<Team>> {
        return teamDao.getTeamsForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getTeamById(teamId: String): Flow<Team?> {
        return teamDao.getTeamById(teamId)
            .map { it?.toDomainModel() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun createTeam(team: Team): Result<Team> {
        try {
            // Lấy thông tin người dùng hiện tại
            val currentUserId = dataStoreManager.getCurrentUserId()
                ?: return Result.failure(IOException("User not logged in"))

            // Tạo ID mới nếu chưa có
            val teamWithId = if (team.id.isBlank()) {
                team.copy(
                    id = UUID.randomUUID().toString(),
                    createdBy = currentUserId
                )
            } else {
                team.copy(createdBy = currentUserId)
            }

            // Lưu vào local database trước
            val teamEntity = teamWithId.toEntity().copy(
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis()
            )
            teamDao.insertTeam(teamEntity)

            // Thêm người tạo team vào danh sách thành viên với vai trò admin
            val teamMemberEntity = TeamMemberEntity(
                id = UUID.randomUUID().toString(),
                teamId = teamWithId.id,
                userId = currentUserId,
                role = "admin", // Người tạo team mặc định là admin
                joinedAt = System.currentTimeMillis(),
                invitedBy = currentUserId,
                serverId = null,
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            teamMemberDao.insertTeamMember(teamMemberEntity)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Triển khai đồng bộ với server ở đây
                    // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi đồng bộ nhóm lên server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(teamEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error creating team", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateTeam(team: Team): Result<Team> {
        try {
            // Lưu vào local database trước
            val teamEntity = team.toEntity().copy(
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )
            teamDao.updateTeam(teamEntity)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Triển khai đồng bộ với server ở đây
                    // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing team update to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(teamEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating team", e)
            return Result.failure(e)
        }
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> {
        try {
            val team = teamDao.getTeamByIdSync(teamId)
            if (team != null) {
                if (team.serverId != null) {
                    // Nếu team đã được đồng bộ với server, đánh dấu để xóa sau
                    val updatedTeam = team.copy(
                        syncStatus = "pending_delete",
                        lastModified = System.currentTimeMillis()
                    )
                    teamDao.updateTeam(updatedTeam)
                } else {
                    // Nếu team chưa được đồng bộ với server, xóa luôn
                    teamDao.deleteTeam(team.id)
                }

                // Nếu có kết nối mạng, đồng bộ lên server
                if (connectionChecker.isNetworkAvailable() && team.serverId != null) {
                    try {
                        // Triển khai xóa trên server ở đây
                        // Hiện tại chỉ trả về thành công vì chúng ta đã xử lý trong local database
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing team deletion to server", e)
                        // Không trả về lỗi vì đã xử lý thành công trong local database
                    }
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting team", e)
            return Result.failure(e)
        }
    }

    override fun getTeamMembers(teamId: String): Flow<List<com.example.taskapplication.domain.model.TeamMember>> {
        return teamMemberDao.getTeamMembers(teamId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun inviteUserToTeam(teamId: String, userEmail: String): Result<com.example.taskapplication.domain.model.TeamMember> {
        try {
            // Kiểm tra xem team có tồn tại không
            val team = teamDao.getTeamByIdSync(teamId)

            if (team == null) {
                return Result.failure(IOException("Team not found"))
            }

            // Lấy user từ email (trong thực tế, có thể cần gọi API để tìm user)
            val user = userDao.getUserByEmail(userEmail)

            if (user == null) {
                return Result.failure(IOException("User not found"))
            }

            // Lấy thông tin người dùng hiện tại
            val currentUserId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))

            // Thêm thành viên vào team
            val teamMemberEntity = TeamMemberEntity(
                id = UUID.randomUUID().toString(),
                teamId = teamId,
                userId = user.id,
                role = "member", // Mặc định là member
                joinedAt = System.currentTimeMillis(),
                invitedBy = currentUserId,
                serverId = null,
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            teamMemberDao.insertTeamMember(teamMemberEntity)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Triển khai đồng bộ với server ở đây
                    // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing team member invitation to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(teamMemberEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error inviting user to team", e)
            return Result.failure(e)
        }
    }

    override suspend fun removeUserFromTeam(teamId: String, userId: String): Result<Unit> {
        try {
            val teamMember = teamMemberDao.getTeamMemberSync(teamId, userId)

            if (teamMember != null) {
                // Nếu team member đã được đồng bộ với server, đánh dấu để xóa sau
                if (teamMember.serverId != null) {
                    val updatedTeamMember = teamMember.copy(
                        syncStatus = "pending_delete",
                        lastModified = System.currentTimeMillis()
                    )
                    teamMemberDao.updateTeamMember(updatedTeamMember)
                } else {
                    // Nếu team member chưa được đồng bộ với server, xóa luôn
                    teamMemberDao.deleteTeamMember(teamMember)
                }

                // Nếu có kết nối mạng, đồng bộ lên server
                if (connectionChecker.isNetworkAvailable() && teamMember.serverId != null) {
                    try {
                        // Triển khai xóa trên server ở đây
                        // Hiện tại chỉ trả về thành công vì chúng ta đã xử lý trong local database
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing team member removal to server", e)
                        // Không trả về lỗi vì đã xử lý thành công trong local database
                    }
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing team member", e)
            return Result.failure(e)
        }
    }

    override suspend fun changeUserRole(teamId: String, userId: String, newRole: String): Result<com.example.taskapplication.domain.model.TeamMember> {
        try {
            val teamMember = teamMemberDao.getTeamMemberSync(teamId, userId)

            if (teamMember == null) {
                return Result.failure(IOException("Team member not found"))
            }

            val oldRole = teamMember.role

            // Kiểm tra xem có phải là admin cuối cùng không
            if (oldRole == "admin" && newRole != "admin") {
                val adminCount = teamMemberDao.countAdminsInTeam(teamId)
                if (adminCount <= 1) {
                    return Result.failure(IOException("Cannot demote the last admin of the team"))
                }
            }

            // Cập nhật vai trò
            val updatedTeamMember = teamMember.copy(
                role = newRole,
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )
            teamMemberDao.updateTeamMember(updatedTeamMember)

            // Lưu lịch sử thay đổi vai trò
            val currentUserId = dataStoreManager.getCurrentUserId()
            if (currentUserId != null) {
                val roleHistory = TeamRoleHistoryEntity(
                    teamId = teamId,
                    userId = userId,
                    oldRole = oldRole,
                    newRole = newRole,
                    changedByUserId = currentUserId,
                    timestamp = System.currentTimeMillis(),
                    syncStatus = "pending"
                )
                teamRoleHistoryDao.insertRoleHistory(roleHistory)
            }

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Triển khai đồng bộ với server ở đây
                    // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing role change to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(updatedTeamMember.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error changing user role", e)
            return Result.failure(e)
        }
    }

    override fun isUserAdminOfTeam(teamId: String, userId: String): Flow<Boolean> {
        return teamMemberDao.isUserAdminOfTeamFlow(teamId, userId)
    }

    override fun isUserMemberOfTeam(teamId: String, userId: String): Flow<Boolean> {
        return teamMemberDao.isUserMemberOfTeamFlow(teamId, userId)
    }

    override suspend fun syncTeams(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            // Triển khai đồng bộ với server ở đây
            // 1. Đẩy các thay đổi local lên server
            // 2. Lấy các thay đổi từ server về

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing teams", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncTeamMembers(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            // Triển khai đồng bộ với server ở đây
            // 1. Đẩy các thay đổi local lên server
            // 2. Lấy các thay đổi từ server về

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing team members", e)
            return Result.failure(e)
        }
    }

    // Role history methods
    override fun getRoleHistoryForTeam(teamId: String): Flow<List<TeamRoleHistory>> {
        return teamRoleHistoryDao.getRoleHistoryForTeam(teamId)
            .map { it.toTeamRoleHistoryList() }
            .flowOn(Dispatchers.IO)
    }

    override fun getRoleHistoryForUser(teamId: String, userId: String): Flow<List<TeamRoleHistory>> {
        return teamRoleHistoryDao.getRoleHistoryForUser(teamId, userId)
            .map { it.toTeamRoleHistoryList() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun addRoleHistory(roleHistory: TeamRoleHistory): Result<TeamRoleHistory> {
        try {
            val entity = roleHistory.toTeamRoleHistoryEntity()
            val id = teamRoleHistoryDao.insertRoleHistory(entity)
            return Result.success(entity.copy(id = id).toTeamRoleHistory())
        } catch (e: Exception) {
            Log.e(TAG, "Error adding role history", e)
            return Result.failure(e)
        }
    }

    // Role permissions methods
    override fun hasPermission(teamId: String, userId: String, permission: String): Flow<Boolean> {
        return getUserRole(teamId, userId).map { role ->
            val teamPermission = try {
                TeamPermission.valueOf(permission)
            } catch (e: IllegalArgumentException) {
                return@map false
            }

            role.permissions.contains(teamPermission)
        }
    }

    override fun getUserRole(teamId: String, userId: String): Flow<TeamRole> {
        return teamMemberDao.getUserRoleInTeam(teamId, userId)
            .map { roleName ->
                roleName?.let { TeamRole.fromString(it) } ?: TeamRole.GUEST
            }
            .flowOn(Dispatchers.IO)
    }
}
