package app.template.patches.viber.glass

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_VIBER
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private const val HOOK_CLASS = "Lapp/template/extension/viber/LiquidGlassHook;"

object BottomNavAttachFingerprint : Fingerprint(
    definingClass = "Lcom/viber/voip/ui/bottomnavigation/BottomNavigationView;",
    name = "onAttachedToWindow",
    returnType = "V"
)

@Suppress("unused")
val liquidGlassNavBarPatch = bytecodePatch(
    name = "Liquid Glass Navigation Bar",
    description = "Floating draggable App Store / Telegram Liquid Glass capsule with adaptive light/dark tinting.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIBER)
    extendWith("extensions/extension.mpe")

    dependsOn(resourcePatch {
        execute {
            listOf("res/layout/activity_home.xml", "res/layout-land/activity_home.xml").forEach { path ->
                val file = get(path)
                if (file.exists()) {
                    file.writeText(file.readText().replace(
                        """app:layout_constraintBottom_toTopOf="@id/bottom_nav_bar"""",
                        """app:layout_constraintBottom_toBottomOf="parent""""
                    ))
                }
            }
        }
    })

    execute {
        BottomNavAttachFingerprint.method.addInstruction(
            0,
            "invoke-static {p0}, $HOOK_CLASS->applyLiquidGlass(Landroid/view/View;)V"
        )
    }
}

@Suppress("unused")
val cleanHeaderIconsPatch = resourcePatch(
    name = "Clean Modern Header Icons",
    description = "Replaces Viber's back arrow, voice call, and video call icons with sleek modern vector icons.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIBER)

    execute {
        listOf(
            "figma_ic_chevron_left_arrow.xml",
            "figma_ic_line_call_phone.xml",
            "figma_ic_line_video.xml",
            "figma_ic_line_call_phone_video_combo.xml"
        ).forEach { name ->
            val stream = this::class.java.classLoader.getResourceAsStream("viber/drawable/$name")
            if (stream != null) {
                val target = get("res/drawable/$name")
                target.parentFile?.mkdirs()
                Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}

@Suppress("unused")
val tabletAdaptiveLayoutPatch = resourcePatch(
    name = "Tablet Adaptive Layout",
    description = "Centers Viber's chat container and floating glass bar on tablets (sw600dp+).",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIBER)

    execute {
        val phoneLayout = get("res/layout/activity_home.xml")
        if (phoneLayout.exists()) {
            val tabletDir = get("res/layout-sw600dp")
            tabletDir.mkdirs()
            val xml = phoneLayout.readText()
                .replace("""android:id="@id/activity_home_root" android:layout_width="0.0dp" """, """android:id="@id/activity_home_root" android:layout_width="0.0dp" app:layout_constraintWidth_max="680dp" """)
                .replace("""android:id="@id/bottom_nav_bar" android:layout_width="0.0dp" """, """android:id="@id/bottom_nav_bar" android:layout_width="0.0dp" app:layout_constraintWidth_max="460dp" """)
            File(tabletDir, "activity_home.xml").writeText(xml)
        }
    }
}
