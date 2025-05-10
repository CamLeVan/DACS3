package com.example.taskapplication.ui.team.document

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

/**
 * Dialog for creating a new document version
 */
@Composable
fun CreateVersionDialog(
    documentId: String,
    onDismiss: () -> Unit,
    onCreateVersion: (file: File, changeNotes: String) -> Unit
) {
    val context = LocalContext.current
    var changeNotes by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex("_display_name")
                    if (displayNameIndex != -1) {
                        selectedFileName = cursor.getString(displayNameIndex)
                    }
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm phiên bản mới") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = changeNotes,
                    onValueChange = { changeNotes = it },
                    label = { Text("Ghi chú thay đổi") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Chọn file")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chọn file")
                }
                
                if (selectedFileName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File đã chọn: $selectedFileName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedFileUri?.let { uri ->
                        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.${getFileExtension(selectedFileName)}")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        onCreateVersion(tempFile, changeNotes)
                    }
                },
                enabled = selectedFileUri != null
            ) {
                Text("Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

/**
 * Get file extension from file name
 */
private fun getFileExtension(fileName: String): String {
    return fileName.substringAfterLast('.', "")
}
