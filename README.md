# Wextly Player (нативный Android: Kotlin + Jetpack Compose + Media3)

Этап 1: основа плеера. Музыка играет в фоне, плеер показывается в шторке уведомлений и на экране
блокировки (с обложкой и кнопками), работают кнопки наушников и Bluetooth.

## Что уже есть (этап 1)
- Библиотека: добавление музыки с телефона, теги и обложки из файлов, удаление из списка.
- Плеер: перемотка, пауза, следующий/предыдущий, shuffle, повтор (выкл / все / один), блок «Далее».
- Служба воспроизведения (MediaSession): шторка, экран блокировки, наушники, пауза при звонке.

## План дальше
- Этап 2: плейлисты, очередь с перестановкой, избранное, поиск по библиотеке, сортировка.
- Этап 3: поиск и импорт с SoundCloud (через NewPipeExtractor), темы и палитра, эквалайзер.
- Этап 4: проверка обновлений прямо в приложении, виджет, Android Auto.

## Как собрать APK через GitHub
1. Создай НОВЫЙ репозиторий (например, `wextly-player-android`), приватный, пустой.
2. Add file → Upload files. Перетащи папку `app` и файлы из корня:
   `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `.gitignore`, `README.md`.
   Commit changes.
3. Add file → Create new file. Имя: `.github/workflows/build.yml` (слэши создают папки).
   Вставь содержимое файла `build.yml` из архива. Commit changes.
4. Вкладка Actions → «Сборка APK» → Run workflow. Сборка занимает 10-15 минут.
5. Скачай `wextly-player-apk` в разделе Artifacts внизу страницы запуска, распакуй, установи APK.

## Свой ключ подписи (рекомендуется)
Без него каждая сборка может подписываться новым временным ключом, и обновление не встанет
поверх старой версии. С одним своим ключом обновления ставятся поверх.
1. Установи Java (в PowerShell): `winget install EclipseAdoptium.Temurin.17.JDK`, затем открой новое окно.
2. Создай ключ: `keytool -genkeypair -v -keystore wextly.jks -alias wextly -keyalg RSA -keysize 2048 -validity 10000`
   (придумай пароль и запомни его).
3. Переведи файл в текст: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("wextly.jks")) | Set-Clipboard`
4. В репозитории: Settings → Secrets and variables → Actions → New repository secret. Добавь четыре секрета:
   `KEYSTORE_B64` (вставь из буфера), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (wextly), `KEY_PASSWORD`.
5. Файл `wextly.jks` и пароли храни у себя и никому не отправляй. Если потерять ключ, обновления
   придётся ставить заново.
