package com.findmine.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.findmine.data.CarryContext
import com.findmine.data.MemoryRecord
import com.findmine.data.OnDeviceSpeechInput
import com.findmine.data.PhotoMemoryLink
import com.findmine.data.SmartSuggestion
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class VoiceTarget {
    Search,
    Draft,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderNotifier.ensureChannel(this)

        setContent {
            FindMineTheme {
                FindMineApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindMineApp(
    viewModel: FindMineViewModel = viewModel(),
) {
    // This screen owns Android-only launchers while the ViewModel owns all searchable memory state.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val section by viewModel.section.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val watchList by viewModel.watchList.collectAsStateWithLifecycle()
    val lastSeenRecords by viewModel.lastSeenRecords.collectAsStateWithLifecycle()
    val carryContext by viewModel.carryContext.collectAsStateWithLifecycle()
    val smartSuggestions by viewModel.smartSuggestions.collectAsStateWithLifecycle()
    val smartBrief by viewModel.smartBrief.collectAsStateWithLifecycle()
    val answer by viewModel.answer.collectAsStateWithLifecycle()
    val ocrState by viewModel.ocrState.collectAsStateWithLifecycle()
    val lastSavedItem by viewModel.lastSavedItem.collectAsStateWithLifecycle()
    val reminderItems = smartSuggestions.map { it.record }.ifEmpty { watchList }

    var voiceTarget by remember { mutableStateOf(VoiceTarget.Search) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    // Result launchers feed speech, camera, gallery, and notification outcomes back into one ViewModel.
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val transcript = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()

        if (transcript.isBlank()) return@rememberLauncherForActivityResult

        when (voiceTarget) {
            VoiceTarget.Search -> viewModel.applyVoiceToSearch(transcript)
            VoiceTarget.Draft -> viewModel.applyVoiceToDraft(transcript)
        }
    }

    fun launchVoice(target: VoiceTarget) {
        voiceTarget = target
        val prompt = if (target == VoiceTarget.Search) {
            "찾을 물건을 말해주세요"
        } else {
            "물건과 위치를 말해주세요"
        }
        val intent = OnDeviceSpeechInput.recognizerIntent(prompt)

        try {
            voiceLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "이 기기에서 음성 입력을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingImageUri
        if (success && uri != null) {
            viewModel.clearOcrState()
            viewModel.updateDraft { it.copy(imageUri = uri.toString()) }
            viewModel.analyzeDraftImage()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createPhotoUri(context)
            pendingImageUri = uri
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = createPhotoUri(context)
            pendingImageUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.clearOcrState()
            viewModel.updateDraft { it.copy(imageUri = uri.toString()) }
            viewModel.analyzeDraftImage()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            ReminderNotifier.show(context, reminderItems, smartBrief)
        }
    }

    fun showReminder() {
        if (reminderItems.isEmpty()) {
            Toast.makeText(context, "알림으로 보낼 물건이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ReminderNotifier.show(context, reminderItems, smartBrief)
        }
    }

    LaunchedEffect(lastSavedItem) {
        val saved = lastSavedItem ?: return@LaunchedEffect
        scope.launch {
            snackbarHostState.showSnackbar("$saved 위치를 저장했습니다.")
            viewModel.clearLastSavedItem()
        }
    }

    Scaffold(
        // The scaffold keeps the three workflows connected through shared navigation and reminders.
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = sectionTitle(section),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = sectionSubtitle(section),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            if (section != AppSection.Add) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.setSection(AppSection.Add) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("기록") },
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = section == AppSection.Search,
                    onClick = { viewModel.setSection(AppSection.Search) },
                    icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = { Text("검색") },
                )
                NavigationBarItem(
                    selected = section == AppSection.Add,
                    onClick = { viewModel.setSection(AppSection.Add) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    label = { Text("기록") },
                )
                NavigationBarItem(
                    selected = section == AppSection.Alerts,
                    onClick = { viewModel.setSection(AppSection.Alerts) },
                    icon = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                    label = { Text("알림") },
                )
            }
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AnimatedContent(
                targetState = section,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (
                        fadeIn() + slideInHorizontally { width -> direction * width / 6 }
                        ).togetherWith(
                        fadeOut() + slideOutHorizontally { width -> -direction * width / 6 },
                    ).using(SizeTransform(clip = false))
                },
                label = "section_transition",
            ) { targetSection ->
                when (targetSection) {
                    AppSection.Search -> SearchScreen(
                        query = query,
                        answer = answer,
                        results = results,
                        records = records,
                        onQueryChange = viewModel::setQuery,
                        onSubmitSearch = viewModel::submitSearch,
                        onVoiceSearch = { launchVoice(VoiceTarget.Search) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onDelete = viewModel::delete,
                        historyFor = viewModel::historyFor,
                    )
                    AppSection.Add -> AddScreen(
                        draft = draft,
                        ocrState = ocrState,
                        onDraftChange = viewModel::updateDraft,
                        onVoiceDraft = { launchVoice(VoiceTarget.Draft) },
                        onCamera = ::launchCamera,
                        onGallery = { imagePickerLauncher.launch(arrayOf("image/*")) },
                        onAnalyzeImage = viewModel::analyzeDraftImage,
                        onClearOcr = viewModel::clearOcrState,
                        onUsePhotoLink = viewModel::applyPhotoLink,
                        onSave = viewModel::saveDraft,
                    )
                    AppSection.Alerts -> AlertsScreen(
                        carryContext = carryContext,
                        smartBrief = smartBrief,
                        smartSuggestions = smartSuggestions,
                        lastSeenRecords = lastSeenRecords,
                        watchList = watchList,
                        records = records,
                        onContextChange = viewModel::updateCarryContext,
                        onShowReminder = ::showReminder,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onDelete = viewModel::delete,
                        historyFor = viewModel::historyFor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    query: String,
    answer: String,
    results: List<ScoredMemory>,
    records: List<MemoryRecord>,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onVoiceSearch: () -> Unit,
    onToggleFavorite: (MemoryRecord) -> Unit,
    onDelete: (MemoryRecord) -> Unit,
    historyFor: (MemoryRecord) -> List<MemoryRecord>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MemoryOverview(records = records)
        }

        item {
            SearchInput(
                query = query,
                onQueryChange = onQueryChange,
                onSubmitSearch = onSubmitSearch,
                onVoiceSearch = onVoiceSearch,
            )
        }

        if (answer.isNotBlank()) {
            item {
                AnimatedVisibility(visible = answer.isNotBlank()) {
                    AnswerBand(answer = answer)
                }
            }
        }

        if (records.isEmpty()) {
            item { EmptyState(title = "아직 기록이 없습니다.") }
        } else if (results.isEmpty()) {
            item { EmptyState(title = "검색 결과가 없습니다.") }
        } else {
            item {
                SectionTitle(
                    title = if (query.isBlank()) "최근 기록" else "검색 결과",
                    count = results.size,
                )
            }
            items(results, key = { it.record.id }) { scored ->
                MemoryRecordCard(
                    record = scored.record,
                    history = historyFor(scored.record),
                    score = scored.score.takeIf { query.isNotBlank() },
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDelete,
                )
            }
        }
    }
}

private fun sectionTitle(section: AppSection): String =
    when (section) {
        AppSection.Search -> "찾기"
        AppSection.Add -> "기록"
        AppSection.Alerts -> "챙기기"
    }

private fun sectionSubtitle(section: AppSection): String =
    when (section) {
        AppSection.Search -> "내 물건"
        AppSection.Add -> "새 위치"
        AppSection.Alerts -> "외출 전"
    }

@Composable
private fun MemoryOverview(records: List<MemoryRecord>) {
    val photoCount = records.count { it.imageUri != null }
    val favoriteCount = records.count { it.favorite }
    val todayCount = records.count { System.currentTimeMillis() - it.createdAt < 86_400_000L }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricTile(
            label = "기록",
            value = records.size.toString(),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = "사진",
            value = photoCount.toString(),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = "오늘",
            value = todayCount.toString(),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = "중요",
            value = favoriteCount.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 72.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onVoiceSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "검색어 지우기")
                    }
                }
            },
            label = { Text("내 물건 어디 있지?") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
        )
        FilledIconButton(onClick = onVoiceSearch) {
            Icon(Icons.Rounded.Mic, contentDescription = "음성 검색")
        }
    }
}

@Composable
private fun AnswerBand(answer: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AddScreen(
    draft: MemoryDraft,
    ocrState: OcrUiState,
    onDraftChange: ((MemoryDraft) -> MemoryDraft) -> Unit,
    onVoiceDraft: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onAnalyzeImage: () -> Unit,
    onClearOcr: () -> Unit,
    onUsePhotoLink: (MemoryRecord) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "새 위치 기록",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onVoiceDraft) {
                Icon(Icons.Rounded.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("음성")
            }
            OutlinedButton(onClick = onCamera) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("촬영")
            }
            OutlinedButton(onClick = onGallery) {
                Icon(Icons.Rounded.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("사진")
            }
        }

        draft.imageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "첨부 사진",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 260.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ElevatedButton(
                    onClick = onAnalyzeImage,
                    enabled = !ocrState.running,
                ) {
                    if (ocrState.running) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("OCR 분석")
                }
                if (ocrState.text.isNotBlank() || ocrState.error.isNotBlank()) {
                    OutlinedButton(onClick = onClearOcr) {
                        Icon(Icons.Rounded.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("초기화")
                    }
                }
            }
        }

        AiAssistPanel(
            ocrState = ocrState,
            onUsePhotoLink = onUsePhotoLink,
        )

        if (draft.confidence < 1f) {
            Text(
                text = "자동 추출 신뢰도 ${(draft.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = draft.itemName,
            onValueChange = { value -> onDraftChange { it.copy(itemName = value) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("물건 이름") },
        )
        OutlinedTextField(
            value = draft.location,
            onValueChange = { value -> onDraftChange { it.copy(location = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("위치") },
        )
        OutlinedTextField(
            value = draft.note,
            onValueChange = { value -> onDraftChange { it.copy(note = value) } },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
            label = { Text("메모") },
        )
        OutlinedTextField(
            value = draft.tags,
            onValueChange = { value -> onDraftChange { it.copy(tags = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("태그") },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (draft.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text("자주 잃어버림")
            }
            Switch(
                checked = draft.favorite,
                onCheckedChange = { checked -> onDraftChange { it.copy(favorite = checked) } },
            )
        }

        Button(
            onClick = onSave,
            enabled = draft.canSave,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
        ) {
            Icon(Icons.Rounded.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("저장")
        }
    }
}

@Composable
private fun AiAssistPanel(
    ocrState: OcrUiState,
    onUsePhotoLink: (MemoryRecord) -> Unit,
) {
    when {
        ocrState.running || ocrState.message.isNotBlank() -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Text(
                            text = if (ocrState.running) "사진 분석 중" else ocrState.message,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (ocrState.text.isNotBlank()) {
                        Text(
                            text = ocrState.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (ocrState.labels.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ocrState.labels.take(6).forEach { label ->
                                StaticPill(
                                    text = "${label.text} ${(label.confidence * 100).toInt()}%",
                                    color = MaterialTheme.colorScheme.surface,
                                )
                            }
                        }
                    }
                    if (ocrState.links.isNotEmpty()) {
                        Text(
                            text = "과거 기록 연결",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        ocrState.links.take(3).forEach { link ->
                            PhotoLinkRow(
                                link = link,
                                onUsePhotoLink = onUsePhotoLink,
                            )
                        }
                    }
                }
            }
        }
        ocrState.error.isNotBlank() -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Text(
                    text = ocrState.error,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PhotoLinkRow(
    link: PhotoMemoryLink,
    onUsePhotoLink: (MemoryRecord) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.record.itemName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${link.record.location} · ${link.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(onClick = { onUsePhotoLink(link.record) }) {
                Text("적용")
            }
        }
    }
}

@Composable
private fun AlertsScreen(
    carryContext: CarryContext,
    smartBrief: String,
    smartSuggestions: List<SmartSuggestion>,
    lastSeenRecords: List<MemoryRecord>,
    watchList: List<MemoryRecord>,
    records: List<MemoryRecord>,
    onContextChange: ((CarryContext) -> CarryContext) -> Unit,
    onShowReminder: () -> Unit,
    onToggleFavorite: (MemoryRecord) -> Unit,
    onDelete: (MemoryRecord) -> Unit,
    historyFor: (MemoryRecord) -> List<MemoryRecord>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "외출 전 추천",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                ElevatedButton(onClick = onShowReminder) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("알림")
                }
            }
        }

        item {
            SmartContextPanel(
                context = carryContext,
                smartBrief = smartBrief,
                onContextChange = onContextChange,
            )
        }

        if (smartSuggestions.isEmpty()) {
            item {
                EmptyState(title = "추천할 기록이 아직 충분하지 않습니다.")
            }
        } else {
            items(smartSuggestions, key = { "smart-${it.record.id}" }) { suggestion ->
                SmartSuggestionCard(suggestion = suggestion)
            }
        }

        if (lastSeenRecords.isNotEmpty()) {
            item {
                SectionTitle(title = "마지막으로 본 장소", count = lastSeenRecords.size)
            }
            items(lastSeenRecords.take(5), key = { "last-${it.id}" }) { record ->
                CompactHistoryRow(record = record)
            }
        }

        if (watchList.isNotEmpty()) {
            item {
                SectionTitle(title = "습관 기반 물건", count = watchList.size)
            }
            items(watchList, key = { "habit-${it.id}" }) { record ->
                MemoryRecordCard(
                    record = record,
                    history = historyFor(record),
                    score = null,
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDelete,
                )
            }
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            SectionTitle(title = "전체 기록", count = records.size)
        }

        items(records.take(8), key = { "recent-${it.id}" }) { record ->
            CompactHistoryRow(record = record)
        }
    }
}

@Composable
private fun SmartContextPanel(
    context: CarryContext,
    smartBrief: String,
    onContextChange: ((CarryContext) -> CarryContext) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Text(
                    text = smartBrief,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ContextSwitch(
                    label = "외출",
                    checked = context.goingOut,
                    onCheckedChange = { checked ->
                        onContextChange { it.copy(goingOut = checked) }
                    },
                    modifier = Modifier.weight(1f),
                )
                ContextSwitch(
                    label = "비",
                    checked = context.raining,
                    onCheckedChange = { checked ->
                        onContextChange { it.copy(raining = checked) }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = context.scheduleText,
                onValueChange = { value ->
                    onContextChange { it.copy(scheduleText = value) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("오늘 일정") },
                placeholder = { Text("예: 학교, 출근, 여행") },
            )
        }
    }
}

@Composable
private fun ContextSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SmartSuggestionCard(suggestion: SmartSuggestion) {
    val progress by animateFloatAsState(
        targetValue = (suggestion.priority / 120f).coerceIn(0.08f, 1f),
        label = "suggestion_priority",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PhotoThumb(uri = suggestion.record.imageUri)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = suggestion.record.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${suggestion.priority}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                LocationLine(location = suggestion.record.location)
                Text(
                    text = suggestion.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MemoryRecordCard(
    record: MemoryRecord,
    history: List<MemoryRecord>,
    score: Int?,
    onToggleFavorite: (MemoryRecord) -> Unit,
    onDelete: (MemoryRecord) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PhotoThumb(uri = record.imageUri)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = record.itemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        score?.let {
                            Text(
                                text = "$it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    LocationLine(location = record.location)
                    Text(
                        text = formatTime(record.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onToggleFavorite(record) }) {
                        Icon(
                            imageVector = if (record.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "즐겨찾기",
                            tint = if (record.favorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDelete(record) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "삭제")
                    }
                }
            }

            if (record.note.isNotBlank()) {
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            TagRow(tags = record.tags)

            if (history.size > 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "위치 히스토리",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    history.take(3).forEach { item ->
                        Text(
                            text = "${formatTime(item.createdAt)}  ${item.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoThumb(uri: String?) {
    if (uri == null) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    } else {
        AsyncImage(
            model = uri,
            contentDescription = "기록 사진",
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun LocationLine(location: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Rounded.Place,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = location,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TagRow(tags: String) {
    val parsed = tags
        .split(",", " ")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)

    if (parsed.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        parsed.forEach { tag ->
            StaticPill(
                text = tag,
                dotColor = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun StaticPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    dotColor: Color? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            dotColor?.let {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(it),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompactHistoryRow(record: MemoryRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.itemName,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = record.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatTime(record.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FindMineTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val fallbackLight = lightColorScheme(
        primary = Color(0xFF2F6B4F),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD8F0E2),
        onPrimaryContainer = Color(0xFF102116),
        secondary = Color(0xFF5667A6),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0E5FF),
        onSecondaryContainer = Color(0xFF121A36),
        tertiary = Color(0xFFB86F20),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFDDB8),
        onTertiaryContainer = Color(0xFF2D1600),
        background = Color(0xFFF8F8F6),
        onBackground = Color(0xFF1B1C1A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1B1C1A),
        surfaceVariant = Color(0xFFE2E5DD),
        onSurfaceVariant = Color(0xFF454840),
        outline = Color(0xFF74796F),
        outlineVariant = Color(0xFFC5C9BE),
    )
    val fallbackDark = darkColorScheme(
        primary = Color(0xFF9ED3B3),
        onPrimary = Color(0xFF06391F),
        primaryContainer = Color(0xFF1D5238),
        onPrimaryContainer = Color(0xFFD8F0E2),
        secondary = Color(0xFFBEC6F5),
        onSecondary = Color(0xFF27345F),
        secondaryContainer = Color(0xFF3D4C77),
        onSecondaryContainer = Color(0xFFE0E5FF),
        tertiary = Color(0xFFFFBE79),
        onTertiary = Color(0xFF4B2800),
        tertiaryContainer = Color(0xFF6D3A00),
        onTertiaryContainer = Color(0xFFFFDDB8),
        background = Color(0xFF121412),
        onBackground = Color(0xFFE3E3DD),
        surface = Color(0xFF1B1D1A),
        onSurface = Color(0xFFE3E3DD),
        surfaceVariant = Color(0xFF454840),
        onSurfaceVariant = Color(0xFFC5C9BE),
        outline = Color(0xFF8F9388),
        outlineVariant = Color(0xFF454840),
    )
    val colorScheme = if (darkTheme) fallbackDark else fallbackLight
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp),
        extraLarge = RoundedCornerShape(16.dp),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}

private fun createPhotoUri(context: Context): Uri {
    val imageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "memory")
    imageDir.mkdirs()
    val imageFile = File(imageDir, "findmine_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
    )
}

private fun formatTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREAN)
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private object ReminderNotifier {
    private const val CHANNEL_ID = "frequent_items"
    private const val CHANNEL_NAME = "Find Mine reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        items: List<MemoryRecord>,
        brief: String = "",
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val names = items.take(5).joinToString(", ") { it.itemName }
        val message = if (names.isBlank()) {
            "챙길 물건 기록을 확인하세요."
        } else if (brief.isNotBlank()) {
            brief
        } else {
            "$names 위치를 확인하세요."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("자주 잃어버리는 물건")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(3100, notification)
    }
}
