# Drag Spend – GitHub Copilot Instructions

Đây là dự án **Android Kotlin + Jetpack Compose** (`com.bossxomlut.dragspend`).  
Mọi tính năng mới, sửa lỗi, hay refactor đều phải tuân thủ bốn quy tắc dưới đây.

---

## 1. Không được có lỗi syntax

- Chỉ dùng cú pháp Kotlin hợp lệ, tương thích với **Kotlin 2.0+** và **Compose BOM 2024.09+**.
- Luôn import đầy đủ — không để IDE tự suy luận import bị thiếu.
- Trước khi kết thúc bất kỳ thay đổi nào, tự kiểm tra lại:
  - Tất cả dấu ngoặc `{}`, `()`, `[]` đều được đóng đúng.
  - Không có tham chiếu đến biến / hàm / class chưa được khai báo.
  - Không có `TODO` hoặc code placeholder được để lại trong production code.
- Với file Gradle (`.kts`), đảm bảo cú pháp Kotlin DSL đúng, không trộn lẫn Groovy DSL.
- Với file XML (layout, manifest, strings, themes), đảm bảo well-formed XML, đúng namespace.

---

## 2. Format code sau khi code xong

Áp dụng **Kotlin official code style** (`kotlin.code.style=official` đã cấu hình trong `gradle.properties`).

### Quy tắc formatting bắt buộc

| Mục                 | Quy tắc                                                                                                                                                                                                                                                                                                            |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Indent              | 4 spaces (không dùng tab)                                                                                                                                                                                                                                                                                          |
| Dòng tối đa         | 120 ký tự                                                                                                                                                                                                                                                                                                          |
| Trailing whitespace | Xoá hết                                                                                                                                                                                                                                                                                                            |
| Blank lines         | Tối đa 1 dòng trống liên tiếp trong body; 2 dòng giữa các top-level declaration                                                                                                                                                                                                                                    |
| Import              | Không dùng wildcard import (`import foo.*`); xếp theo thứ tự: android → androidx → kotlinx → java/javax → third-party → internal                                                                                                                                                                                   |
| Modifier order      | Theo thứ tự chuẩn Kotlin: `public/private/protected/internal` → `expect/actual` → `final/open/abstract/sealed/const` → `external` → `override` → `lateinit` → `tailrec` → `vararg` → `suspend` → `inner` → `enum/annotation/fun` (nếu là interface) → `companion` → `inline/value` → `infix` → `operator` → `data` |
| Lambda              | Lambda 1 tham số dùng `it`; lambda nhiều tham số phải đặt tên rõ ràng                                                                                                                                                                                                                                              |
| Trailing comma      | Dùng trailing comma trong danh sách tham số / argument nhiều dòng                                                                                                                                                                                                                                                  |

### Jetpack Compose cụ thể

- Mỗi `@Composable` function được đặt trên một dòng riêng, có blank line phân tách.
- Modifier chain căn thẳng theo chiều dọc khi có ≥ 2 modifier:

  ```kotlin
  // Đúng
  Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .background(color)

  // Sai
  Modifier.fillMaxWidth().padding(16.dp).background(color)
  ```

- Dùng `@Preview` cho tất cả composable công khai, preview cả light và dark:
  ```kotlin
  @Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
  @Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
  @Composable
  fun MyComponentPreview() { ... }
  ```

---

## 3. Đầy đủ locale: Tiếng Anh và Tiếng Việt

### Cấu trúc file strings

```
app/src/main/res/
├── values/
│   └── strings.xml          ← tiếng Anh (ngôn ngữ mặc định / fallback)
└── values-vi/
    └── strings.xml          ← tiếng Việt
```

### Quy tắc bắt buộc

1. **Mọi chuỗi hiển thị cho người dùng** phải được khai báo trong `strings.xml` — không được hardcode string trong Kotlin hay XML layout.
2. Mỗi khi thêm một `<string>` vào `values/strings.xml`, **bắt buộc** thêm bản dịch tương ứng vào `values-vi/strings.xml` trong cùng commit / thay đổi.
3. Đặt tên key theo format `snake_case` mô tả ngữ nghĩa, không mô tả vị trí:

   ```xml
   <!-- Đúng -->
   <string name="login_button_label">Log In</string>
   <string name="error_invalid_email">Invalid email address</string>

   <!-- Sai -->
   <string name="button1">Log In</string>
   <string name="text_view_error">Invalid email address</string>
   ```

4. Trong Compose, luôn dùng `stringResource(R.string.key)` thay vì string literal.
5. Không để lại key trong một file mà thiếu ở file kia — cả hai file phải đồng bộ key.

### Ví dụ mẫu

`values/strings.xml`:

```xml
<resources>
    <string name="app_name">Drag Spend</string>
    <string name="home_title">Overview</string>
    <string name="add_expense_button">Add Expense</string>
    <string name="error_amount_required">Amount is required</string>
</resources>
```

`values-vi/strings.xml`:

```xml
<resources>
    <string name="app_name">Drag Spend</string>
    <string name="home_title">Tổng quan</string>
    <string name="add_expense_button">Thêm chi tiêu</string>
    <string name="error_amount_required">Vui lòng nhập số tiền</string>
</resources>
```

---

## 4. Hỗ trợ Dark Mode và Light Mode

### Theme setup (đã có sẵn trong project)

File `ui/theme/Theme.kt` đã có `DragSpendTheme` với `DarkColorScheme` / `LightColorScheme`.  
**Mọi screen / component mới đều phải được bọc trong `DragSpendTheme` hoặc là con của nó.**

### Quy tắc bắt buộc

1. **Không hardcode màu sắc** trong composable. Chỉ dùng màu từ `MaterialTheme.colorScheme.*`:

   ```kotlin
   // Đúng
   Text(color = MaterialTheme.colorScheme.onBackground)
   Surface(color = MaterialTheme.colorScheme.surface)

   // Sai
   Text(color = Color(0xFF000000))
   Surface(color = Color.White)
   ```

2. **Không hardcode màu trong XML** (themes, drawables). Dùng `?attr/colorPrimary` và các thuộc tính Material.

3. Khi cần màu custom, khai báo trong `Color.kt` với cặp light/dark rõ ràng và đăng ký vào cả `LightColorScheme` và `DarkColorScheme` trong `Theme.kt`.

4. **Icons và drawables** phải dùng tint từ theme thay vì màu cố định:

   ```kotlin
   Icon(
       painter = painterResource(R.drawable.ic_delete),
       tint = MaterialTheme.colorScheme.error,
       contentDescription = stringResource(R.string.delete_action)
   )
   ```

5. **Elevation và shadow**: Dùng `MaterialTheme.colorScheme.surfaceVariant` hoặc `Surface(tonalElevation = ...)` để có hiệu ứng elevation đúng trong cả hai mode.

6. **Preview bắt buộc cả hai mode**:

   ```kotlin
   @Preview(uiMode = UI_MODE_NIGHT_NO, name = "Light Mode")
   @Preview(uiMode = UI_MODE_NIGHT_YES, name = "Dark Mode")
   @Composable
   private fun MyScreenPreview() {
       DragSpendTheme {
           MyScreen()
       }
   }
   ```

7. **Dynamic Color** (Android 12+): Project đã bật `dynamicColor = true` trong `DragSpendTheme`. Khi dùng màu custom, hãy test trên cả thiết bị có và không có dynamic color.

---

## Checklist trước khi hoàn thành bất kỳ thay đổi nào

- [ ] Code không có lỗi syntax (kiểm tra cả export type, import, dấu ngoặc)
- [ ] Code đã được format theo Kotlin official style
- [ ] Tất cả string người dùng nhìn thấy đều nằm trong `strings.xml` (cả `values/` và `values-vi/`)
- [ ] Màu sắc và UI component dùng token từ `MaterialTheme.colorScheme`, không hardcode
- [ ] Có `@Preview` cho cả Light và Dark mode với mọi composable công khai mới
