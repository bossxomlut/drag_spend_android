---
description: "Use when creating or reviewing any @Composable function. Enforces 4-variant Previews covering light/dark mode × English/Vietnamese locale."
applyTo: "app/src/main/**/*.kt"
---

# Compose Preview Standards

Every `@Composable` function that is **publicly visible** (non-private, non-internal helper) **must** have a
corresponding Preview block with exactly **4 variants**: Light+EN, Dark+EN, Light+VI, Dark+VI.

---

## Preferred: Use the Project-Level `@PreviewAllVariants` Annotation

Define **once** in `ui/preview/PreviewAnnotations.kt`, then reuse everywhere.

```kotlin
package com.bossxomlut.dragspend.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light – EN", locale = "en")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark – EN", locale = "en")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light – VI", locale = "vi")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark – VI", locale = "vi")
annotation class PreviewAllVariants
```

Usage in any composable file:

```kotlin
import com.bossxomlut.dragspend.ui.preview.PreviewAllVariants
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme

@PreviewAllVariants
@Composable
private fun MyComponentPreview() {
    DragSpendTheme {
        MyComponent(
            // provide realistic sample data here
        )
    }
}
```

---

## Fallback: Explicit 4-Annotation Form

Use this when `@PreviewAllVariants` is not yet available in the file's module.

```kotlin
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light – EN", locale = "en")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark – EN", locale = "en")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light – VI", locale = "vi")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark – VI", locale = "vi")
@Composable
private fun MyComponentPreview() {
    DragSpendTheme {
        MyComponent(
            // provide realistic sample data here
        )
    }
}
```

---

## Rules

| #   | Rule                                                                                                                     |
| --- | ------------------------------------------------------------------------------------------------------------------------ |
| 1   | **4 variants required**: Light+EN, Dark+EN, Light+VI, Dark+VI — all four, every time.                                    |
| 2   | **`locale = "en"`** and **`locale = "vi"`** are mandatory — they match `values/strings.xml` and `values-vi/strings.xml`. |
| 3   | **`showBackground = true`** on all variants.                                                                             |
| 4   | Preview functions must be **`private`**.                                                                                 |
| 5   | Always wrap content in **`DragSpendTheme { ... }`**.                                                                     |
| 6   | Preview function name pattern: **`{ComposableName}Preview`** (e.g. `ExpenseCardPreview`).                                |
| 7   | Provide **realistic sample data** — avoid empty strings or zero values that would hide UI issues.                        |
| 8   | **No business logic** inside a preview function (no ViewModel, no repository calls).                                     |
| 9   | All required imports must be explicit — no wildcard imports.                                                             |

---

## Required Imports Checklist

```kotlin
import android.content.res.Configuration.UI_MODE_NIGHT_NO   // if not using @PreviewAllVariants
import android.content.res.Configuration.UI_MODE_NIGHT_YES   // if not using @PreviewAllVariants
import androidx.compose.ui.tooling.preview.Preview            // if not using @PreviewAllVariants
import androidx.compose.runtime.Composable
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
// OR
import com.bossxomlut.dragspend.ui.preview.PreviewAllVariants
```

---

## Full Example

```kotlin
// --- production composable ---
@Composable
fun ExpenseCard(
    title: String,
    amount: String,
    category: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = amount, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// --- previews (4 variants) ---
@PreviewAllVariants
@Composable
private fun ExpenseCardPreview() {
    DragSpendTheme {
        ExpenseCard(
            title = "Lunch",
            amount = "85,000 ₫",
            category = "Food",
        )
    }
}
```

---

## Pre-Completion Checklist

- [ ] 4 `@Preview` variants present (Light EN, Dark EN, Light VI, Dark VI)
- [ ] Preview function is `private`
- [ ] Content is wrapped in `DragSpendTheme`
- [ ] Preview function name ends with `Preview`
- [ ] Sample data is realistic and non-empty
- [ ] All imports are explicit (no wildcards)
