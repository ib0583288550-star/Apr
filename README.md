# מחליף הטפטים

אפליקציית Android ב-Kotlin + Jetpack Compose.

## מה יש כרגע
- בחירת תמונות מהטלפון
- החלפה ידנית של הטפט
- החלפה אוטומטית עם WorkManager
- ממשק עברית
- בניית APK דרך GitHub Actions

## בנייה
כל push ל-main מפעיל בנייה אוטומטית. בסיום אפשר להוריד את `app-debug` מתוך Artifacts של GitHub Actions.

> ב-Android WorkManager מרווח מחזורי מינימלי אמין הוא 15 דקות, ולכן ערכים נמוכים מזה לא משמשים להחלפה מחזורית בפועל.
