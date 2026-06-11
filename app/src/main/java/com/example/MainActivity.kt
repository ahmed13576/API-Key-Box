package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.api.GeminiClient
import com.example.data.ApiKeyEntry
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VaultDashboardScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(viewModel: VaultViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var showInfoDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<ApiKeyEntry?>(null) }

    // Handle toast alert notifications
    LaunchedEffect(uiState.alertMessage) {
        uiState.alertMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.setAlertMessage(null)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Vault Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "API Key Vault",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Hardware Encrypted • On-Device Only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "AES-256",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { showInfoDialog = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Security Info",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                )

                // Quick Security Stat Indicator Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Shield Protected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Android KeyStore AES-256-GCM hardware protection is ACTIVE.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .testTag("create_key_fab")
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New API Key"
                    )
                    Text(text = "New Key", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Metrics Overview Section
            MetricsBanner(
                totalKeysCount = uiState.keysList.size,
                llmCount = uiState.keysList.count { 
                    !it.providerName.contains("mcp", ignoreCase = true) && 
                    !it.modelIdentifier.contains("mcp", ignoreCase = true) &&
                    !it.baseUrl.contains("mcp", ignoreCase = true)
                },
                mcpCount = uiState.keysList.count { 
                    it.providerName.contains("mcp", ignoreCase = true) || 
                    it.modelIdentifier.contains("mcp", ignoreCase = true) ||
                    it.baseUrl.contains("mcp", ignoreCase = true)
                }
            )

            // Search and Filtering Section
            SearchAndFiltersSection(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                selectedType = uiState.searchProviderType,
                onTypeSelect = { viewModel.updateProviderFilter(it) }
            )

            // Keys List State
            if (uiState.filteredKeysList.isEmpty()) {
                EmptyStateView(
                    isFilterEmpty = uiState.keysList.isNotEmpty(),
                    onCreateClick = { viewModel.setShowAddDialog(true) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredKeysList, key = { it.id }) { entry ->
                        ApiKeyCard(
                            entry = entry,
                            isVisible = uiState.decryptKeyId == entry.id,
                            onToggleVisibility = { viewModel.toggleKeyVisibility(entry.id) },
                            onCopy = {
                                val decrypted = viewModel.getDecryptedKey(entry.encryptedApiKey)
                                val clip = ClipData.newPlainText("API Key", decrypted)
                                clipboardManager.setPrimaryClip(clip)
                                viewModel.setAlertMessage("Decrypted key copied to clipboard!")
                            },
                            onDelete = { entryToDelete = entry },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // Add New Key Modal / Dialog
    if (uiState.showAddDialog) {
        AddKeyDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowAddDialog(false) }
        )
    }

    // Informational Dialog explaining hardware-backed encryption
    if (showInfoDialog) {
        SecurityInfoDialog(onDismiss = { showInfoDialog = false })
    }

    // Deletion confirmation
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Credential?") },
            text = { Text("Are you sure you want to remove the API key for ${entry.providerName} (${entry.modelIdentifier})? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteKey(entry.id)
                        entryToDelete = null
                    },
                    modifier = Modifier.testTag("delete_confirm_button")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MetricsBanner(totalKeysCount: Int, llmCount: Int, mcpCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricItemCard(
            title = "Total Keys",
            count = totalKeysCount.toString(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        MetricItemCard(
            title = "LLMs",
            count = llmCount.toString(),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        MetricItemCard(
            title = "MCP Servers",
            count = mcpCount.toString(),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MetricItemCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFiltersSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedType: String,
    onTypeSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input"),
            placeholder = { Text("Search by provider, model or notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        // Providers category filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("ALL" to "All Vault", "LLM" to "LLMs Only", "MCP" to "MCPs Only")
            filters.forEach { (typeKey, label) ->
                val isSelected = selectedType == typeKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelect(typeKey) },
                    label = { Text(text = label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(14.dp)) }
                    } else null,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ApiKeyCard(
    entry: ApiKeyEntry,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    viewModel: VaultViewModel
) {
    var copyAlertActive by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Header: Provider & Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = entry.providerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Badge denoting LLM vs MCP
                        val isMcp = entry.providerName.contains("mcp", ignoreCase = true) ||
                                entry.modelIdentifier.contains("mcp", ignoreCase = true) ||
                                entry.baseUrl.contains("mcp", ignoreCase = true)
                        
                        val tagLabel = if (isMcp) "MCP" else "LLM"
                        val tagColor = if (isMcp) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(tagColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tagLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tagColor
                            )
                        }
                    }
                    if (entry.modelIdentifier.isNotEmpty()) {
                        Text(
                            text = entry.modelIdentifier,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_key_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Key",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            // Connection Details (Base URL & endpoint)
            if (entry.baseUrl.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "URL:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = entry.baseUrl,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (entry.endpointStructure.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Route:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = entry.endpointStructure,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Masked Password/API Key Display Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Key Display (either dots or plain text)
                val displayText = if (isVisible) {
                    viewModel.getDecryptedKey(entry.encryptedApiKey)
                } else {
                    "••••••••••••••••••••••••••••"
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    // Reveal/Eye Button
                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reveal_key_button")
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.Close else Icons.Default.Lock,
                            contentDescription = if (isVisible) "Hide Key" else "Reveal Key",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }

                    // Copy Key Button
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("copy_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh, // Standard share or fetch copy style
                            contentDescription = "Copy key",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Custom description/metadata notes
            if (entry.notes.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Notes info",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(isFilterEmpty: Boolean, onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Secure Lock Indicator",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = if (isFilterEmpty) "No matching keys found" else "Vault is Empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = if (isFilterEmpty) {
                "Try adjusting your filters or search query to match stored configurations."
            } else {
                "Encrypt and store API keys for LLMs/MCPs locally on this device. Key suggestions will dynamically query service docs."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (!isFilterEmpty) {
            Button(
                onClick = onCreateClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Plus icon")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Your First Key")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKeyDialog(viewModel: VaultViewModel, onDismiss: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    var providerName by remember { mutableStateOf("") }
    var modelIdentifier by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var endpointStructure by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var apiVisible by remember { mutableStateOf(false) }

    // If suggestion results are successfully returned, automatically populate the settings fields!
    LaunchedEffect(uiState.suggestedDetails) {
        uiState.suggestedDetails?.let { details ->
            baseUrl = details.baseUrl
            endpointStructure = details.endpointStructure
            notes = if (details.documentationNotes.isNotEmpty()) {
                "Recommended Headers: ${details.headerStructure}\nNote: ${details.documentationNotes}"
            } else ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Register Secure Credential", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close overlay")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Heuristic templates section
                item {
                    Text(
                        text = "Quick Provider Presets",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val templates = listOf(
                            Triple("OpenAI", "gpt-4o", "LLM"),
                            Triple("Anthropic", "claude-3-5-sonnet", "LLM"),
                            Triple("Gemini", "gemini-3.5-flash", "LLM"),
                            Triple("DeepSeek", "deepseek-chat", "LLM"),
                            Triple("Groq", "llama-3.3-70b-versatile", "LLM"),
                            Triple("Local MCP", "mcp-server", "MCP")
                        )
                        // Make scrollable or split row
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    templates.take(3).forEach { (p, m, t) ->
                                        SuggestionChip(
                                            onClick = {
                                                providerName = p
                                                modelIdentifier = m
                                                // Trigger fast suggestion filling
                                                val localDetails = GeminiClient.getFallbackDocs(p, m)
                                                baseUrl = localDetails.baseUrl
                                                endpointStructure = localDetails.endpointStructure
                                                notes = "Recommended Headers: ${localDetails.headerStructure}"
                                            },
                                            label = { Text("$p ($m)") }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    templates.drop(3).forEach { (p, m, t) ->
                                        SuggestionChip(
                                            onClick = {
                                                providerName = p
                                                modelIdentifier = m
                                                val localDetails = GeminiClient.getFallbackDocs(p, m)
                                                baseUrl = localDetails.baseUrl
                                                endpointStructure = localDetails.endpointStructure
                                                notes = "Recommended Headers: ${localDetails.headerStructure}"
                                            },
                                            label = { Text("$p ($m)") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Divider
                item { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }

                // Provider Name Input
                item {
                    OutlinedTextField(
                        value = providerName,
                        onValueChange = { providerName = it },
                        label = { Text("Service Provider Name *") },
                        placeholder = { Text("e.g. Cohere, Groq, Anthropic, Custom MCP...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // LLM model Identifier input
                item {
                    OutlinedTextField(
                        value = modelIdentifier,
                        onValueChange = { modelIdentifier = it },
                        label = { Text("Model Identifier (or MCP channel)") },
                        placeholder = { Text("e.g. command-r-plus, claude-3, sse") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Dynamic documentation fetch suggestion button
                item {
                    Button(
                        onClick = {
                            viewModel.fetchDocsSuggestions(providerName, modelIdentifier)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("fetch_docs_button"),
                        enabled = providerName.isNotEmpty() && !uiState.isFetchingSuggestions,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isFetchingSuggestions) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Querying Service Docs...")
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Gemini Suggestion")
                                Text("Ask Gemini to Auto-Fill Base URL & Route", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Suggested Configuration Card (When suggestions are returned)
                if (uiState.suggestedDetails != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2B2930) // Matches #2B2930 from design
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                            ) {
                                // Left colored borders/bar
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "⚡",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "AUTO-CONFIGURED VIA TECH DOCS",
                                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Column {
                                        Text(
                                            text = "BASE URL & DETECTED HOST",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = uiState.suggestedDetails!!.baseUrl,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Column {
                                        Text(
                                            text = "ENDPOINT SCHEMATIC ROUTE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = uiState.suggestedDetails!!.endpointStructure,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Suggested / Editable Base URL Input
                item {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL *") },
                        placeholder = { Text("e.g. https://api.anthropic.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Suggested / Editable Endpoint Route Input
                item {
                    OutlinedTextField(
                        value = endpointStructure,
                        onValueChange = { endpointStructure = it },
                        label = { Text("Endpoint Route / Path") },
                        placeholder = { Text("e.g. /v1/messages or /chat/completions") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Private API Key Input
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("Private API Key *") },
                            placeholder = { Text("Will paste key. Stored encrypted on local device") },
                            singleLine = true,
                            visualTransformation = if (apiVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { apiVisible = !apiVisible }) {
                                    Icon(
                                        imageVector = if (apiVisible) Icons.Default.Close else Icons.Default.Lock,
                                        contentDescription = "Toggle Key Visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4ADE80), // emerald-400 equivalent green
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Encrypted before writing to storage",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Notes / Custom headers metadata
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Suggested Headers & Notes") },
                        placeholder = { Text("Authentication tags or instructions placeholder...") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Submit/Cancelling layout
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("cancel_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val success = viewModel.saveKey(
                                    providerName = providerName,
                                    modelIdentifier = modelIdentifier,
                                    baseUrl = baseUrl,
                                    endpointStructure = endpointStructure,
                                    rawApiKey = apiKey,
                                    notes = notes
                                )
                                if (success) {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                                .testTag("save_key_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Encryption Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Security Safeguards")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "API Key Vault protects your master keys using the highest standards of local security architecture:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Cryptographic Hardware isolation: An AES-256-GCM symmetric key is generated inside Android Keystore environment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Off-Device prevention: No key, configuration, or text metadata is cached or sent outside. Everything is stored locally on this physical device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• AI Helper: The documentation auto-complete relies on Gemini API. To query, the server-side assistant reads your provider's name, but does NOT see or access your private saved keys under any circumstances.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}
