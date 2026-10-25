plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)

    alias(mihonx.plugins.spotless)

    alias(libs.plugins.valkyrie)
}

android {
    namespace = "mihon.icons.phosphor"
}

spotless {
    format("svg") {
        target("src/**/*.svg")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Phosphor Icons (MIT, https://github.com/phosphor-icons/core), see LICENSE in this folder.
// Bold for idle navigation and toolbar icons, Fill for the selected navigation destination.
valkyrie {
    packageName = "mihon.icons.phosphor"
    generateAtSync = true

    imageVector {
        suppressUnusedReceiverWarning = true
    }

    iconPack {
        name = "Phosphor"
        targetSourceSet = "main"

        nested {
            name = "Bold"
            sourceFolder = "bold"
            autoMirror = false
        }

        nested {
            name = "Fill"
            sourceFolder = "fill"
            autoMirror = false
        }

        // Direction-sensitive glyphs (back arrow), flipped in right-to-left layouts
        nested {
            name = "BoldAutoMirrored"
            sourceFolder = "boldAutoMirrored"
            autoMirror = true
        }
    }
}

dependencies {
    api(libs.androidx.compose.ui)
}
