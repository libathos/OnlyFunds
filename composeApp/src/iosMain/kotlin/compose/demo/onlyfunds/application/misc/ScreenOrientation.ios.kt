package compose.demo.onlyfunds.application.misc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskLandscape
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindowSceneGeometryPreferencesIOS
import platform.UIKit.setNeedsUpdateOfSupportedInterfaceOrientations

object OrientationTracker {
    var currentOrientationMask: UIInterfaceOrientationMask = UIInterfaceOrientationMaskPortrait
}

@Composable
actual fun LockScreenOrientation(orientation: ScreenOrientation) {
    DisposableEffect(orientation) {
        val previousMask = OrientationTracker.currentOrientationMask
        applyOrientationMask(orientation.toMask())
        onDispose {
            applyOrientationMask(previousMask)
        }
    }
}

private fun ScreenOrientation.toMask(): UIInterfaceOrientationMask = when (this) {
    ScreenOrientation.LANDSCAPE -> UIInterfaceOrientationMaskLandscape
    ScreenOrientation.PORTRAIT -> UIInterfaceOrientationMaskPortrait
}

private fun applyOrientationMask(mask: UIInterfaceOrientationMask) {
    OrientationTracker.currentOrientationMask = mask

    val scenes = UIApplication.sharedApplication.connectedScenes.mapNotNull { it as? UIWindowScene }
    val windowScene = scenes.firstOrNull {
        it.activationState == UISceneActivationStateForegroundActive
    } ?: scenes.firstOrNull() ?: return

    windowScene.topViewController()?.setNeedsUpdateOfSupportedInterfaceOrientations()

    val preferences = UIWindowSceneGeometryPreferencesIOS(interfaceOrientations = mask)
    windowScene.requestGeometryUpdateWithPreferences(preferences, errorHandler = null)
}

private fun UIWindowScene.topViewController(): UIViewController? {
    val rootViewController = keyWindow?.rootViewController
        ?: windows.mapNotNull { (it as? UIWindow)?.rootViewController }.firstOrNull()
    var controller = rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
