# KamsyView

[![](https://jitpack.io/v/mbobiosio/KamsyView.svg)](https://jitpack.io/#mbobiosio/KamsyView)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.0-blue.svg?logo=kotlin)](http://kotlinlang.org)

A powerful, highly customizable Android avatar view library with advanced features including dynamic shapes, borders, status indicators, animations, and BlurHash support. 
Built entirely in Kotlin. This uses ShapeableImageView which belongs to the Android Material Design Library.

## 🌟 Features

### Core Features
- **Multiple Shapes**: Circle, Rounded Square, Squircle, Hexagon, Diamond, Star, and Custom shapes
- **Smart Placeholder System**: Automatic initials generation with customizable colors
- **Advanced Border System**: Solid colors, gradients, and animated arches
- **Status Indicators**: Online/offline/away/busy status with customizable positions
- **Badge Support**: Notification badges and custom badges
- **BlurHash Integration**: Progressive image loading with BlurHash placeholders
- **Font Support**: System fonts and custom font resources
- **Volumetric Effects**: 3D lighting effects and shadows

### Image Loading
- **Coil 3.x Integration**: Modern async image loading with GIF and SVG support
- **Glide Support**: Alternative image loading with transformations
- **Automatic Fallbacks**: Graceful degradation to initials when images fail
- **Multiple Formats**: JPEG, PNG, WebP, GIF, SVG, and BlurHash support
- **Multiple Sources**: URL, resource, drawable, BlurHash support

### Customization
- **Fluent DSL**: Kotlin-first configuration API
- **XML Attributes**: Comprehensive attribute support
- **Style Presets**: Material3, Gaming, Professional, Minimal themes
- **Runtime Changes**: Dynamic shape and style updates
- **Animation Support**: Border rotations, pulses, and volumetric effects

## 📦 Installation

### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("com.github.mbobiosio:KamsyView:<latest-version>")
    
    // Image loading (choose one)
    implementation("io.coil-kt:coil:3.2.0") // Recommended
    implementation("io.coil-kt:coil-gif:3.2.0") // For GIF support
    implementation("io.coil-kt:coil-svg:3.2.0") // For SVG support
    // OR
    implementation("com.github.bumptech.glide:glide:4.15.1")
    
    // BlurHash support (optional)
    implementation("com.github.woltapp:blurhash:1.1.0")
    
    // Dependency injection (optional)
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
}
```

### Gradle (Groovy)
```groovy
dependencies {
    implementation 'com.github.mbobiosio:KamsyView:1.0.0'
    implementation 'io.coil-kt:coil:3.2.0'
    implementation 'io.coil-kt:coil-gif:3.2.0'
    implementation 'io.coil-kt:coil-svg:3.2.0'
    implementation 'com.github.woltapp:blurhash:1.1.0'
}
```

## 🚀 Quick Start

### Basic Usage

#### XML Layout
```xml
<com.github.kamsyview.core.KamsyView
    android:id="@+id/avatar"
    android:layout_width="80dp"
    android:layout_height="80dp"
    app:kamsyShape="circle"
    app:borderWidth="2dp"
    app:borderColor="?colorPrimary"
    app:placeholderText="JD"
    app:statusIndicator="online" />
```

#### Kotlin Code
```kotlin
// Load avatar image
binding.avatar.loadAvatar(
    avatarUrl = "https://example.com/avatar.jpg",
    name = "John Doe"
)

// Or with configuration
binding.avatar.configure {
    border {
        width(4)
        color(Color.BLUE)
    }
    appearance {
        shape(KamsyShape.SQUIRCLE)
    }
    overlay {
        status {
            online()
        }
    }
}
```

## 🎨 Shapes

KamsyView supports multiple built-in shapes that can be set via XML or programmatically:

### Available Shapes
- **Circle**: Perfect circular avatar (default)
- **Rounded Square**: Square with rounded corners
- **Squircle**: iOS-style rounded square
- **Hexagon**: Six-sided polygon
- **Diamond**: Rotated square
- **Star**: Five-pointed star effect
- **Custom**: Your own ShapeAppearanceModel

### XML Configuration
```xml
<com.github.kamsyview.core.KamsyView
    android:layout_width="100dp"
    android:layout_height="100dp"
    app:kamsyShape="squircle"
    app:cornerRadius="16dp" />
```

### Programmatic Configuration
```kotlin
// Set shape directly
avatarView.setShape(KamsyShape.HEXAGON)

// Or via DSL
avatarView.configure {
    appearance {
        shape(KamsyShape.DIAMOND)
    }
}

// Custom shape
avatarView.configure {
    appearance {
        customShape(
            ShapeAppearanceModel.builder()
                .setAllCorners(RoundedCornerTreatment())
                .setAllCornerSizes(24f)
                .build()
        )
    }
}
```

## 🎯 Borders

### Basic Borders
```xml
<com.github.kamsyview.core.KamsyView
    android:layout_width="80dp"
    android:layout_height="80dp"
    app:borderWidth="3dp"
    app:borderColor="#FF5722" />
```

### Gradient Borders
```xml
<com.github.kamsyview.core.KamsyView
    android:layout_width="80dp"
    android:layout_height="80dp"
    app:borderWidth="4dp"
    app:borderColor="#FF5722"
    app:borderColorSecondary="#2196F3"
    app:borderGradientAngle="45" />
```

### Arched Borders (Segmented)
```xml
<com.github.kamsyview.core.KamsyView
    android:layout_width="80dp"
    android:layout_height="80dp"
    app:borderWidth="3dp"
    app:borderColor="#FF5722"
    app:archesCount="8"
    app:archesDegreeArea="270"
    app:archesType="single" />
```

### Programmatic Border Configuration
```kotlin
avatarView.configure {
    border {
        width(4)
        gradient(Color.RED, Color.BLUE, 45)
        arches {
            count(6)
            degreeArea(300)
            mirror()
        }
    }
}
```

### Border Animations
```kotlin
// Rotating arches
avatarView.configure {
    animations {
        borderRotation(duration = 2000L)
    }
}

// Pulse animation
avatarView.borderDrawable?.pulseAnimation(
    pulseCount = 3,
    pulseDuration = 500L
)
```

## 📍 Status Indicators

Status indicators show user presence or state with small dots positioned around the avatar.

### XML Configuration
```xml
<com.github.kamsyview.core.KamsyView
    android:layout_width="80dp"
    android:layout_height="80dp"
    app:statusIndicator="online"
    app:statusPosition="bottomRight"
    app:statusSize="12dp"
    app:statusColor="?colorPrimary" />
```

### Available Status Types
- `online` - Green indicator
- `offline` - Gray indicator
- `away` - Yellow indicator
- `busy` - Red indicator
- `custom` - Uses `statusColor` attribute

### Status Positions
- `topLeft`
- `topRight`
- `bottomLeft`
- `bottomRight` (default)
- `center`

### Programmatic Status
```kotlin
// Predefined statuses
avatarView.configure {
    overlay {
        status {
            online()    // Green dot
            offline()   // Gray dot
            away()      // Yellow dot
            busy()      // Red dot
        }
    }
}

// Custom status
avatarView.configure {
    overlay {
        status {
            custom(
                color = Color.MAGENTA,
                position = StatusPosition.TOP_RIGHT
            )
        }
    }
}
```

## 🏷️ Badges

Badges display text or numbers, commonly used for notifications.

### Notification Badges
```kotlin
avatarView.configure {
    overlay {
        badge {
            notification(count = 5)  // Shows "5"
            notification(count = 150) // Shows "99+"
        }
    }
}
```

### Custom Badges
```kotlin
avatarView.configure {
    overlay {
        badge {
            custom(
                text = "VIP",
                backgroundColor = Color.GOLD,
                textColor = Color.BLACK,
                position = StatusPosition.TOP_RIGHT
            )
            
            vip()      // Predefined VIP badge
            premium()  // Predefined premium badge
        }
    }
}
```

## 🎭 Supported Image Formats

KamsyView supports a wide range of image formats through Coil integration:

### Static Images
- **JPEG** - Standard photo format
- **PNG** - Images with transparency
- **WebP** - Modern efficient format
- **BMP** - Bitmap images

### Animated Images
- **GIF** - Animated avatars and reactions
- **WebP (Animated)** - Modern animated format

### Vector Graphics
- **SVG** - Scalable vector graphics, perfect for icons and logos

### Progressive Loading
- **BlurHash** - Ultra-fast placeholder generation

### Setup for Advanced Formats
```kotlin
// Add to your dependencies for full format support
dependencies {
    implementation("io.coil-kt:coil:3.2.0")
    implementation("io.coil-kt:coil-gif:3.2.0") // For GIF support
    implementation("io.coil-kt:coil-svg:3.2.0") // For SVG support
}
```

### Usage Examples
```kotlin
// Animated GIF avatar
avatarView.loadAvatar(
    avatarUrl = "https://example.com/animated-profile.gif",
    name = "Animated User"
)

// SVG logo or icon
avatarView.loadAvatar(
    avatarUrl = "https://example.com/company-logo.svg", 
    name = "Company"
)

// Works seamlessly with all KamsyView features
avatarView.configure {
    border {
        width(3)
        color(Color.BLUE)
    }
    appearance {
        shape(KamsyShape.SQUIRCLE)
    }
}
```

### Coil Integration (Recommended)
```kotlin
// Basic loading
avatarView.loadAvatar(
    avatarUrl = "https://example.com/avatar.jpg",
    name = "John Doe"
)

// Loading GIF avatars
avatarView.loadAvatar(
    avatarUrl = "https://example.com/animated-avatar.gif",
    name = "John Doe"
)

// Loading SVG avatars
avatarView.loadAvatar(
    avatarUrl = "https://example.com/vector-avatar.svg",
    name = "John Doe"
)

// With BlurHash placeholder
avatarView.loadAvatar(
    avatarUrl = "https://example.com/avatar.jpg",
    name = "John Doe",
    blurHash = "LKO2:N%2Tw=w]~RBVZRi};RPxuwH"
)

// With custom ImageLoader
avatarView.loadAvatar(
    avatarUrl = "https://example.com/avatar.jpg",
    name = "John Doe",
    imageLoader = customImageLoader
) {
    // Custom Coil configuration
    transformations(CircleCropTransformation())
    placeholder(R.drawable.loading)
    error(R.drawable.error)
}
```

### Glide Integration
```kotlin
// Basic Glide loading
avatarView.loadAvatarGlide(
    avatarUrl = "https://example.com/avatar.jpg",
    name = "John Doe"
)

// With transformations
avatarView.loadAvatarGlideCircular(
    avatarUrl = "https://example.com/avatar.jpg",
    name = "John Doe"
)

// Custom transformations
avatarView.loadAvatarGlideTransformed(
    avatarUrl = "https://example.com/avatar.jpg",
    name = "John Doe",
    transformation = RoundedCorners(16)
)
```

### Reactive Loading
```kotlin
// With Flow
avatarView.loadAvatarFlow(
    avatarUrl = userFlow.map { it.avatarUrl },
    name = userFlow.map { it.name }
)

// With LiveData
avatarView.loadAvatarLiveData(
    avatarUrl = userLiveData.map { it.avatarUrl },
    name = userLiveData.map { it.name }
)
```

## 🎨 Styling and Themes

### Predefined Styles
```xml
<com.github.kamsyview.core.KamsyView
    android:layout_width="80dp"
    android:layout_height="80dp"
    app:kamsyStyle="material3" />
```

Available styles:
- `default` - Basic styling with border and volumetric effects
- `minimal` - Clean, borderless design
- `material3` - Material Design 3 styling
- `gaming` - Bold borders with animations and effects
- `professional` - Corporate-friendly styling

### Programmatic Styling
```kotlin
// Apply predefined style
avatarView.setStyle(KamsyStyle.Material3)

// Or via DSL
avatarView.configure {
    style(KamsyStyle.Gaming)
}
```

### Custom Styling
```kotlin
avatarView.configure {
    border {
        width(2)
        color("#6750A4".toColorInt())
    }
    
    volumetric { 
        all() 
    }
    
    appearance { 
        material3() 
    }
    
    placeholder {
        backgroundColor("#E3F2FD".toColorInt())
        textColor("#1976D2".toColorInt())
        textSizePercentage(1.2f)
    }
}
```

## 🔤 Text and Fonts

### Placeholder Configuration
```xml
<com.github.kamsyview.core.KamsyView
    android:layout_width="80dp"
    android:layout_height="80dp"
    app:placeholderText="AB"
    app:textColor="?colorOnPrimary"
    app:textSizePercentage="1.2"
    android:fontFamily="@font/custom_font" />
```

### Programmatic Text Configuration
```kotlin
avatarView.configure {
    placeholder {
        text("JS")
        textColor(Color.WHITE)
        textSize(1.5f) // Percentage of default size
        typeface(Typeface.MONOSPACE)
        backgroundColor(Color.BLUE)
        
        // Auto-generate from name
        initials("John Smith") // Becomes "JS"
        autoColor("John Smith") // Generates color from name hash
    }
}
```

### Font Support
```xml
<!-- System fonts -->
android:fontFamily="serif"
android:fontFamily="sans-serif"
android:fontFamily="monospace"
android:fontFamily="casual"

<!-- Custom fonts -->
android:fontFamily="@font/roboto_bold"
```

## ⚡ Animations and Effects

### Volumetric Effects
Volumetric effects add 3D depth and lighting to avatars.

```xml
<com.github.kamsyview.core.KamsyView
    android:layout_width="80dp"
    android:layout_height="80dp"
    app:kamsyVolumetricType="all"
    app:volumetricIntensity="0.5"
    app:volumetricLightAngle="45" />
```

```kotlin
avatarView.configure {
    volumetric {
        all()        // Apply to entire avatar
        drawable()   // Apply only to image
        placeholder() // Apply only to placeholder
        none()       // Disable effects
        
        intensity(
            highlight = 0.3f,
            shadow = 0.2f,
            ambient = 0.1f
        )
    }
}
```

### Border Animations
```kotlin
// Continuous rotation
avatarView.configure {
    animations {
        borderRotation(duration = 3000L)
    }
}

// Pulse effect
avatarView.configure {
    animations {
        borderPulse(
            pulseCount = 5,
            pulseDuration = 300L
        )
    }
}

// Stop all animations
avatarView.configure {
    animations {
        stopAll()
    }
}
```

### Volumetric Animations
```kotlin
avatarView.configure {
    animations {
        volumetricPulse(
            minIntensity = 0.1f,
            maxIntensity = 0.5f,
            duration = 1000L,
            cycles = 3
        )
        
        volumetricBreathe(
            baseIntensity = 0.2f,
            peakIntensity = 0.4f,
            duration = 2000L
        )
    }
}
```

## 🔧 Advanced Configuration

### Avatar Margins
Create space between the image and border:

```xml
<com.github.kamsyview.core.KamsyView
    android:layout_width="80dp"
    android:layout_height="80dp"
    app:avatarMargin="8dp"
    app:borderWidth="2dp"
    app:borderColor="?colorPrimary" />
```

Note: Avatar margin only works with circular shapes. For other shapes, use padding or adjust the border positioning.

### Overlay Configuration
```kotlin
avatarView.configure {
    overlay {
        tint(Color.BLUE, alpha = 0.3f)
        scrim(Color.BLACK, alpha = 0.2f)
        blendMode(BlendMode.MULTIPLY)
        
        enabled(true) // Enable/disable all overlays
        clearAll()    // Remove all overlays
    }
}
```

### Batch Operations
```kotlin
// Load multiple avatars
val avatarViews = listOf(avatar1, avatar2, avatar3)
val urls = listOf("url1.jpg", "url2.jpg", "url3.jpg")
val names = listOf("John", "Jane", "Bob")

avatarViews.loadAvatars(
    urls = urls,
    names = names,
    onProgress = { completed, total ->
        println("Loaded $completed of $total")
    },
    onComplete = {
        println("All avatars loaded!")
    }
)
```

### Preloading
```kotlin
// Preload avatar into cache
avatarView.preloadAvatar(
    avatarUrl = "https://example.com/large-avatar.jpg"
) { success ->
    if (success) {
        // Now load the cached image
        avatarView.loadAvatar(avatarUrl, name)
    }
}
```

## 📋 Complete XML Attributes Reference

### Shape and Appearance
```xml
app:kamsyShape="circle|roundedSquare|hexagon|squircle|diamond|star|custom"
app:cornerRadius="16dp"
app:kamsyStyle="default|minimal|material3|gaming|professional"
app:avatarMargin="8dp"
```

### Border Configuration
```xml
app:borderColor="?colorPrimary"
app:borderColorSecondary="?colorSecondary" 
app:borderWidth="2dp"
app:borderGradientAngle="45"
```

### Arch Borders
```xml
app:archesCount="8"
app:archesDegreeArea="270"
app:archesAngle="0"
app:archesType="single|mirror"
```

### Text and Placeholder
```xml
app:placeholderText="AB"
app:textColor="?colorOnPrimary"
app:textSizePercentage="1.2"
app:backgroundColor="?colorPrimary"
android:fontFamily="@font/custom_font"
```

### Status Indicators
```xml
app:statusIndicator="none|online|offline|away|busy|custom"
app:statusColor="?colorPrimary"
app:statusPosition="topLeft|topRight|bottomLeft|bottomRight|center"
app:statusSize="12dp"
```

### Badges
```xml
app:badgeText="VIP"
app:badgeBackgroundColor="?colorError"
app:badgeTextColor="?colorOnError"
app:badgePosition="topLeft|topRight|bottomLeft|bottomRight|center"
app:badgeSize="16dp"
```

### Effects
```xml
app:kamsyVolumetricType="none|all|drawable|placeholder"
app:volumetricIntensity="0.5"
app:volumetricLightAngle="45"
app:overlayTint="?colorPrimary"
app:overlayTintAlpha="0.3"
app:overlayScrim="?colorSurface"
app:overlayScrimAlpha="0.2"
```

### BlurHash
```xml
app:blurHash="LKO2:N%2Tw=w]~RBVZRi};RPxuwH"
app:blurHashPunch="1.0"
```

### Animations
```xml
app:animationEnabled="true"
app:borderAnimationDuration="2000"
app:volumetricAnimationDuration="1000"
app:autoStartAnimation="true"
```

## 🏗️ Architecture

### Dependency Injection
KamsyView supports Hilt for dependency injection but gracefully falls back to manual injection:

```kotlin
// With Hilt (automatic)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    // KamsyView will automatically inject dependencies
}

// Manual injection
val kamsyView = KamsyView(context)
kamsyView.injectDependencies(
    blurHashProcessor = customBlurHashProcessor,
    drawableFactory = customDrawableFactory,
    configuration = customConfiguration,
    logger = customLogger,
    metrics = customMetrics
)
```

### Custom Drawable Factory
```kotlin
class CustomKamsyDrawableFactory : IKamsyDrawableFactory {
    override fun createPlaceholderDrawable(/* params */): KamsyPlaceholderDrawable {
        // Custom placeholder creation
    }
    
    override fun createBorderDrawable(kamsyView: KamsyView): KamsyBorderDrawable {
        // Custom border creation
    }
    
    override fun createOverlayDrawable(kamsyView: KamsyView): KamsyOverlayDrawable {
        // Custom overlay creation
    }
}
```

### State Management
KamsyView uses StateFlow for reactive state management:

```kotlin
// Observe UI state
lifecycleScope.launch {
    kamsyView.uiState.collect { state ->
        when (state) {
            is KamsyUiState.Loading -> showLoading()
            is KamsyUiState.Success -> showSuccess(state.drawable)
            is KamsyUiState.Error -> showError(state.error)
            is KamsyUiState.BlurHashLoaded -> showBlurHash(state.drawable)
            is KamsyUiState.Placeholder -> showPlaceholder(state.drawable)
        }
    }
}
```

## 🎯 Best Practices

### Performance
1. **Use appropriate image sizes** - Don't load 4K images for 80dp avatars
2. **Preload important avatars** - Cache frequently used images
3. **Limit animated effects** - Use animations sparingly in lists
4. **Choose efficient shapes** - Circle is most performant

### Accessibility
```xml
<com.github.kamsyview.core.KamsyView
    android:layout_width="80dp"
    android:layout_height="80dp"
    android:contentDescription="User avatar for John Doe"
    app:contentDescription="Profile picture"
    app:accessibilityEnabled="true" />
```

### Memory Management
```kotlin
// Clean up when done
override fun onDestroy() {
    super.onDestroy()
    avatarView.clearAvatarCache()
}
```

### Error Handling
```kotlin
avatarView.loadAvatar(url, name) {
    listener(
        onError = { _, error ->
            Log.e("Avatar", "Failed to load avatar", error.throwable)
            // Handle error gracefully
        }
    )
}
```

## 🐛 Troubleshooting

### Common Issues

**Images not loading:**
- Ensure internet permission: `<uses-permission name="android.permission.INTERNET" />`
- Check image URLs are accessible
- Verify Coil/Glide is properly configured

**Shapes not appearing:**
- Ensure `app:kamsyShape` attribute is set
- Check if style is overriding shape settings
- Verify view has proper dimensions

**Borders not visible:**
- Set `app:borderWidth` > 0
- Ensure border color is visible against background
- Check if overlay is covering the border

**Status indicators not showing:**
- Verify `app:statusIndicator` is set
- Ensure status position is within view bounds
- Check if overlay drawable is enabled

**Fonts not working:**
- Verify font files exist in `res/font/`
- Check font file format (TTF/OTF)
- Ensure proper font family reference

### Performance Issues

**Slow rendering in lists:**
```kotlin
// Use simpler shapes in RecyclerView
avatarView.configure {
    appearance { shape(KamsyShape.CIRCLE) }
    volumetric { none() }
    animations { stopAll() }
}
```

**Memory leaks:**
```kotlin
// Properly clean up
override fun onViewRecycled(holder: ViewHolder) {
    super.onViewRecycled(holder)
    holder.avatarView.clearAvatarCache()
}
```

## 📄 License

```
MIT License

Copyright (c) 2024 Mbuodile Obiosio

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

### Development Setup
1. Clone the repository
2. Open in Android Studio
3. Run the sample app
4. Make your changes
5. Submit a pull request

---

**Made with ❤️ by Mbuodile Obiosio for the Android community**
