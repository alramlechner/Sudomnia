package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.ui.theme.PaperTrial
import name.lechners.sudomnia.ui.theme.TextSecondary

/**
 * The trial branch: one button to open it, a bar with two exits while it is open.
 *
 * It sits between the board and the keypad because that is where it is read -- right
 * after looking at the yellow cells it talks about. The bar carries the same yellow
 * as those cells; nothing else on the dark chrome is that colour, so "a branch is
 * open" is visible without reading a word.
 *
 * Both exits are always live, including at zero cells: opening a branch by mistake
 * has to be undoable, and with nothing entered yet keeping and discarding are the
 * same thing anyway.
 */
@Composable
fun BranchBar(
    inBranch: Boolean,
    cells: Int,
    enabled: Boolean,
    onBegin: () -> Unit,
    onCommit: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!inBranch) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = onBegin, enabled = enabled) {
                Text(stringResource(R.string.branch_start))
            }
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PaperTrial.copy(alpha = 0.14f))
            .border(1.dp, PaperTrial.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(start = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (cells == 0) stringResource(R.string.branch_empty)
            else pluralStringResource(R.plurals.branch_cells, cells, cells),
            color = if (cells == 0) TextSecondary else PaperTrial,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
        )
        TextButton(onClick = onDiscard) {
            Text(stringResource(R.string.branch_discard))
        }
        TextButton(onClick = onCommit) {
            Text(stringResource(R.string.branch_commit), fontWeight = FontWeight.SemiBold)
        }
    }
}
