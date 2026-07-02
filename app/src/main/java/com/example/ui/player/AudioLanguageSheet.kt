package com.example.ui.player

import androidx.compose.runtime.Composable
import com.example.data.model.DubModel

/**
 * Audio-language (dub) selector for the player. Lists every dub from the (untouched) detail API
 * and highlights the one currently playing (subjectId == [currentSubjectId]) with the shared
 * orange background + check mark. Selecting a different language delegates to [onSelect], which
 * triggers a seamless stream reload for that dub's subjectId. Uses the same premium AMOLED
 * ModalBottomSheet scaffold (24dp top corners) as the other player selectors.
 */
@Composable
fun AudioLanguageSheet(
    dubs: List<DubModel>,
    currentSubjectId: String,
    onSelect: (DubModel) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerBottomSheet(title = "Select Audio Language", onDismiss = onDismiss) {
        if (dubs.isEmpty()) {
            PlayerOptionRow(
                label = "No languages available",
                selected = false,
                onClick = {},
            )
        } else {
            dubs.forEach { dub ->
                PlayerOptionRow(
                    label = dub.lanName ?: "Unknown",
                    selected = dub.subjectId == currentSubjectId,
                    onClick = { onSelect(dub) },
                )
            }
        }
    }
}
