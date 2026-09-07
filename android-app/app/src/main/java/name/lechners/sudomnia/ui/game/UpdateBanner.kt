package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.ui.theme.AppSurfaceHigh
import name.lechners.sudomnia.ui.theme.LogoBlue
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.update.UpdateState

/**
 * One line above the board, and only when there is genuinely something to install.
 *
 * It renders nothing in every other state -- including "checking" and "up to date" --
 * because the board is sized from the leftover height: a permanently visible status
 * line would cost grid area every single game to report a non-event.
 */
@Composable
fun UpdateBanner(state: UpdateState, onInstall: () -> Unit, modifier: Modifier = Modifier) {
    val version = state.installableVersion ?: return
    val downloading = state is UpdateState.Downloading

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppSurfaceHigh)
            .clickable(enabled = !downloading) { onInstall() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(
                if (downloading) R.string.update_banner_loading else R.string.update_banner,
                version,
            ),
            color = TextPrimary,
            fontSize = 14.sp,
        )
        if (!downloading) {
            Text(
                text = stringResource(R.string.update_install),
                color = LogoBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
