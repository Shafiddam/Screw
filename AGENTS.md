# AGENTS.md — Шуруп (Screw)

Файл для агентов/разработчиков: ключевые факты, карта проекта, контракт движка,
правила стиля и команды сборки. Читать перед любыми правками.

## Что это
Клон «шурупного» сортировочного пазла (Screw Sort / Screw Box) по 15
скриншотам. Механика (выверена по реальным правилам): тап по доступному
шурупу → он летит в ОТКРЫТЫЙ ЯЩИК СВОЕГО ЦВЕТА (вверху 2 ящика-«заказа»,
в каждом ≤3 гнёзд). Ящик наполнился — уезжает, из очереди приезжает следующий,
и подходящие винты автоматически перелетают со СКЛАДА (5 кружков под ящиками).
Винт без своего ящика кладётся на склад; склад (ПУЛ) забит ПОД ЗАВЯЗКУ и на доске
ещё есть винты — поражение сразу («5 винтов в пуле = конец игры», даже если какой-то
доступный винт формально подошёл бы ящику). «Открыть ящик» (2 плитки) — бонус за 300
монет/рекламу: открывает следующий ящик очереди дополнительным активным.
Когда у детали не остаётся шурупов — она падает. Победа: доска пуста.

Осознанный скоуп (выбор пользователя):
- **Реклама — РЕАЛЬНАЯ VK Ads (myTarget)** (слоты зарегистрированы в кабинете ВК,
  см. раздел «Реклама»): баннер 320x50, rewarded-видео на «бесплатных» кнопках,
  interstitial после каждых 5 завершённых уровней. IAP — по-прежнему ЗАГЛУШКА
  (toast «iap_stub»), реальных покупок нет.
- **Уровни:** 15 ручных (обучающих) + детерминированный генератор на ≥240.
- **Графика:** весь арт рисуется кодом (Compose Canvas), КРОМЕ картинок бонусов
  пользователя (`pic_shurupovert`/`pic_gorelka`/`pic_molotok`/`pic_yashik`/`pic_koshelek` —
  `res/drawable-nodpi`, ПРОЗРАЧНЫЕ PNG: белый фон вырезан flood-fill'ом от краёв,
  внутренние блики сохранены; идут через `DrillIcon/FlaskIcon/HammerIcon/BoxIcon/WalletIcon`) и
  стандартных Material-иконок: шестерёнка настроек `Icons.Default.Settings` и кошелёк
  `Icons.Default.AccountBalanceWallet` (последняя есть только в `material-icons-extended`,
  он добавлен как зависимость — в debug APK это ≈+7 МБ, в release R8 вырезает неиспользуемые).
  Тест проходимости и движок от этого не зависят.
- **Звук:** реальные ассеты в `res/raw` (не ToneGenerator): `fun_back_3` — фоновая музыка
  (зациклена, `MediaPlayer`), `tap2` — клик кнопок, `sound_ok` — короткое «правильно»,
  `right` — средний «правильно»/приезд ящика, `wrong` — ошибка/поражение, `sound_win` —
  победа, `coin1` — звон монет (ЛЮБОЙ монетный обмен: получение и трата). Всё через `SoundPool` в `Feedback.kt`, уважают тумблер «звук»; музыка — тумблер
  «музыка» + пауза на `ON_PAUSE`. Клик по ЛЮБОЙ кнопке/тапабельному элементу — через
  `LocalClickSound` (в `CartoonButton`/`StatPill`/`GameDialog`-крестик/`SwitchRow`/плитки
  игры/вкладки BottomNav/аватар и боковые кнопки карты/узлы уровней/«На главную» в
  победе/аватары в `Profile`), звук винта-тапа — `sound_ok`/`wrong` в `GameScreen`.
  НОВЫЙ тапабельный виджет обязан читать `LocalClickSound.current` и звать его в клике.

## Ключевые факты
- applicationId / namespace: `com.fidapp.screw`
- versionName `1.1`, versionCode `1`, minSdk `24`, compile/targetSdk `37`
- UI: Jetpack Compose (BOM), Material3; Kotlin; AGP 9.x; Compose UI 1.11
- Язык интерфейса — ВСЕГДА русский (переключателя нет; в `Labels.kt` англ. значения
  оставлены про запас, `L()` берёт только первую пару)
- Кнопка «Назад»: в игре — на карту; с вкладок магазин/трофеи — сначала на карту
  (иначе один «Назад» после возврата из внешних ссылок вел к алерту выхода);
  на карте — алерт «Вы хотите выйти из игры? Да/Нет»
  (BackHandler в `ScrewGame.kt`, «Да» — `activity.finish()`).
  **Дополнительно — глушение «призрачного бэка»**: когда внешнее приложение
  (браузер/RuStore с «Каталогом») закрывают системным «Назад», событие иногда
  доходит уже до нашего возобновлённого окна и игрок из каталога «выходит из игры».
  Решение: `LifecycleEventObserver` пишет `resumedAt` на `ON_RESUME`, а `BackHandler`
  первые 900 мс после резюма ничего не делает. Мерить именно от RESUME (не от STOP):
  игрок мог сидеть в каталоге минуты, а «призрак» приходит сразу после возврата.
- Иконки бонусов (сверло/плашка/молоток/ящик) — ЗАМЕНЕНЫ на картинки пользователя
  (`pic_shurupovert/pic_gorelka/pic_molotok/pic_yashik` в `res/drawable-nodpi` —
  ПРОЗРАЧНЫЕ PNG, белый фон вырезан офлайн, исходники-жегипи остались в `res/raw`):
  все вызовы идут через `DrillIcon/FlaskIcon/
  HammerIcon/BoxIcon` — менять тело там, а не в местах вызова. `BoxIcon(dots>0)`
  (ревайв) остаётся нарисованным — нужен цветной ящик с красными точками.
  Шестерёнка настроек и иконка «Кошелёк» — НАМЕРЕННО стандартные Material-иконки
  (`Icons.Default.Settings` / `Icons.Default.AccountBalanceWallet`), их рисовать кодом не нужно
- Ориентация: портрет; `configChanges` включает `uiMode|density|fontScale|locale` —
  активность не пересоздаётся при повороте/смене темы; тема приложения Light (не DayNight)
- Стартовый фон окна (splash до отрисовки Compose): белый в `values/themes.xml`,
  чёрный в `values-night/themes.xml` (`android:windowBackground`) — по теме устройства.
  Первый кадр Compose в `MainActivity` подстраивается туда же
  (`isSystemInDarkTheme() ? Black : 0xFFE9EDF3`); сама игра всегда светлая.
- Релизный APK подписывает и публикует пользователь сам; агент готовит сборку/описание.

## Карта проекта
`app/src/main/java/com/fidapp/screw/`
- `game/` — чистая логика, без Android-зависимостей (реюзится unit-тестом):
  - `Models.kt` — `Board` (W=1000f,H=1400f — логическая система координат),
    `ScrewColor`, `Screw`, `Piece` (contains/bounds — block-body!), `Level`.
  - `GameEngine.kt` — движок + `Event`. Контракт ниже.
  - `LevelIO.kt` — `Levels.forNumber(n)`, `LevelGenerator` (xorshift-рандом, `shuffled`);
    генератор ПЕРЕБИРАЕТ сиды, пока уровень не пройдёт `greedySolve()` (детерминировано).
  - `HandcraftedLevels.kt` — 15 уровней + хелперы форм; `level()` фиксирует
    `holdSlots = 5` (ящики движок выводит из цветов шурупов сам).
  - `PlayerStore.kt` — `Economy` (цены/константы) + стигейт игрока на SharedPreferences.
- `ui/game/` — Compose-слой:
  - `ScrewGame.kt` — корень: навигация MAP/SHOP/TROPHY + SETTINGS (по
    тапу на правую вкладку нижней панели открывается `SettingsDialog` как
    оверлей, а не как отдельный экран — состояние `showSettings`); тикер жизней,
    `gameLevel`-оверлей; нижняя панель — внизу экрана с `navigationBarsPadding`.
    Вкладки `BottomNav` (4 шт.) теперь весят одинаково: `Row(spacedBy(2.dp))`
    + `Modifier.weight(1f)` на каждой + `padding(horizontal=2.dp)` внутри,
    иконки 26dp, текст 10sp maxLines=1 — иначе «Настройки» обрезались до «Нас»
    при фикс. 112dp. **ВСЕ иконки нижней панели ЖЁЛТЫЕ** (`tabYellow =
    Color(0xFFF6B500)`, тон как у `TrophyIcon` по умолчанию) — пользователь:
    «закрась шестерёнку и вообще все иконки нижней панели жёлтым, как у трофеев»:
    `ShopIcon(color=)`, `HomeIcon(color=)`, `TrophyIcon(color=)` и Material-
    шестерёнка `Icon(tint = tabYellow)`. У `ShopIcon` добавлен параметр `color:`
    (красит «навес»; корпус белый, витрины синие). `BackHandler` обрабатывает `showSettings` (закрытие
    диалога) раньше, чем `gameLevel`. `GameScreen` обёрнут в `key(gameNonce)`,
    а `onNext` крутит `gameNonce`: «Заново» на ТОМ ЖЕ уровне иначе не
    сбрасывал движок (gameLevel не менялся → remember не пересоздавался).
  - `GameScreen.kt` — геймплей; анимация винта ДВУХФАЗНАЯ (`FlyingScrew.progress`):
    0..1 — ВЫКРУЧИВАНИЕ на месте (шлиц крутится ПРОТИВ часовой ~1 оборот за
    `UNSCREW_MS`=520 мс, рисуется на доске списком `unscrewing`), 1..2 — перелёт
    дугой в overlay в конкретную ячейку (`FLY_MS`=400 мс). ОБЕ фазы считаются от
    одного `startAt` (момента тапа) по абсолютному времени — НЕ сбрасывать `startAt`
    на границе 1f (иначе ветви перекрывались и винт залипал, не летел). `holes` —
    БЕЛЫЙ кружок-отверстие (радиус 31 лог. ед. = половина винта, толщина как кайма
    фигур) за каждым ВЫКРУЧЕННЫМ винтом: берётся из движка (`isCollected && (деталь
    жива ИЛИ деталь уже в списке `falling`)`), живёт до падения фигуры и исчезает
    вместе с ней. **Порядок: `FallingPiece.beginAt = now + FALL_DELAY_MS`**.
    По просьбе пользователя `FALL_DELAY_MS = UNSCREW_MS` (520) — деталь
    срывается ОДНОВРЕМЕННО с улётом последнего винта (как только тот закончил
    выкручиваться и полетел), БЕЗ прежнего запаса `+FLY_MS+30` (из-за него
    фигура «залипала» до полного приземления винта). До `beginAt` тикер не
    наращивает `progress`, а фигура с её отверстиями рисуется в слое падения
    неподвижно. Повторный
    тап по винту, уже летящему, игнорируется (`flying.any { it.screwId == … }`);
    тап по плитке «Открыть ящик» и по плашкам бонусов → `pickBonus(type)`: сначала
    списывается КУПЛЕННЫЙ В МАГАЗИНЕ экземпляр (`player.spendBonus` → применение сразу
    и без видео), и только при пустом инвентаре открывается `BonusDialog` (цена / rewarded);
    для `BOX` сперва проверка `engine.canOpenBox()` (иначе тост «ящиков нет» и бонус
    НЕ тратится). На плитках бонусов зелёный бейдж «× N» — число экземпляров в инвентаре.
    `TrayRow` рисует каждый ящик через `BoxSwapSlot(index)`: при смене цвета в ячейке
    (полный уехал → приехал новый) старый показывается «призраком» и уезжает ВВЕРХ
    с растворением, новый въезжает сверху в свою ячейку (`Animatable` 0→1, `BOX_SWAP_MS`;
    `onGloballyPositioned` снят с неподвижного контейнера-обёртки, иначе цель полёта
    винта съехала бы). `BOX_SWAP_MS` = 1100 мс (подняли с 700 — пользователь
    «ящики улетают слишком быстро, я не вижу анимации»). **Ящики НЕ растягиваются
    на всю ширину ряда** (пользователь: «когда остаётся последний ящик — у него
    меняется размер, не надо так»): `Modifier.weight(1f, fill = false).widthIn(max =
    96.dp)` на каждом боксе И на плитках «Открыть ящик» — ряд не «дышит» при смене
    числа ящиков. Баннер — РЕАЛЬНЫЙ `MyTargetBanner()` в потоке Column, не overlay;
    падение деталей — лист `falling`, на доску передаются только живые детали
    (`livePieces`). Кнопка паузы и плитки «Открыть ящик»/бонусов — с тёмной нижней
    кромкой (объём, `darkenForEdge`).
  - `MapScreen.kt` — карта: СПИРАЛЬНАЯ лента уровней (синусоида по X:
    `xDp = sin(i * 0.95) * 90.dp`, между соседями — `CurveConnector` (белый
    «берег» + оранжевая кубическая Безье-«река»)). Кнопка уровня (`LevelNode`) —
    скруглённая плашка 70×64dp, номер по центру (18sp); У ТЕКУЩЕГО ширина
    удвоена (138dp) и крупный жёлтый `ScrewIcon` (26dp) слева И справа от числа.
    БЕЛАЯ кайма `border(3.dp, Color.White)` у всех, у текущего — ЖЁЛТАЯ
    (`Palette.yellow`). Пройденный — `Palette.mapDone` (зелёный), текущий —
    `Palette.mapCurrent` (синий), заблокированный — `Palette.mapLocked` + 🔒.
    **АВТО-ЦЕНТРИРОВАНИЕ текущего узла — ДЕТЕРМИНИРОВОАННО** (не через
    `onGloballyPositioned`/`boundsInRoot` — те измерение ПОСЛЕ `verticalScroll`
    даёт CONTENT center, а не VIEWPORT center, delta~0 и лента не ехала).
    Обёрнуто в `BoxWithConstraints(Modifier.weight(1f))`: `viewportPx =
    maxHeight.toPx()`, `iCur = endLevel - bestLevel`, `nodeCenterPx =
    (6.dp + (70.dp+26.dp) * iCur + 70.dp/2).toPx()`, а `LaunchedEffect(bestLevel,
    viewportPx) { delay(80); stripScroll.scrollTo((nodeCenterPx - viewportPx/2)
    .roundToInt().coerceIn(0, stripScroll.maxValue)) }`. `Dp * Int` не
    компилируется — только `Dp * Float` (`iCur.toFloat()`).
    Верх: `StatPill` (140dp, зелёный «+») → `LivesPill` (120dp) → **аватар СПРАВА**
    (пользователь: «расположи аватарку справа, сейчас слева»; между жизнями и
    аватаром — `Spacer(weight(1f))`).
    **ШАЕСТЕРЁНКИ СПРАВА ВВЕРХУ БОЛЬШЕ НЕТ** — настройки переехали в нижнее
    меню. Под верхней панелью `Spacer(14.dp)` и по центру `WalletButton`
    (жёлтая 260×56dp, ОРАНЖЕВАЯ рамка, Material-кошелёк + бейдж `piggy/goal`) —
    ОТДЕЛЁН от счётчиков. Внизу: «Играть» КРУПНЕЕ — `fillMaxWidth().padding(
    horizontal=16.dp)`, 80dp высота, 34sp текст, жёлтые `ScrewIcon` 44dp
    по бокам; ПОД НЕЙ «Задание дня» (красная, 170×50dp, 12sp, СИНИЕ винты
    22dp с отступом 6dp).
  - `ShopTrophy.kt` — магазин и трофеи, ОБА на той же `GameFrame` (на всю ширину):
    шапка-табличка, винтики, красный крестик справа вверху (= закрыть → вкладка
    ГЛАВНАЯ, `onClose` в `ScrewGame`), тело с вертикальным скроллом. Магазин:
    1) бонусы — строки `BonusRow` ТАПАБЕЛЬНЫЕ (подсказка «Нажми на бонус, чтобы
    купить»): тап → `BonusDialog(type, owned, note)`; «за монеты» → `spendCoins(priceOf)
    + addBonus`, «Бесплатно» → rewarded-видео → `addBonus`; диалог НЕ закрывается, а
    показывает «В инвентаре: N» и зелёный «× N» в строке — купленное применяется в
    игре по тапу без видео; 2) `ShopCoinsCard` — +10 монет раз в 6 ч: крупная ЗОЛОТАЯ
    МОНЕТА `CoinIcon(46dp)` (не винт — пользователь просил монету именно здесь),
    «+10», «Получить» → после взятия оранжевый таймер «05:59:59» (`shopCountdown`),
    выдача — `player.claimShopCoins()` + `feedback.coin` + `CoinRain` (дождь золотых
    монет со свечением поверх магазина, триггер `rain`); 3) блок кнопок-ссылок
    (`LinkRow(icon: @Composable, label, onClick)` — подпись 16sp + «›», vertical-отступ
    8dp): «Оценить в RuStore» ⭐, «Условия использования» 📄 (`LegalDialog` поверх
    магазина), «Каталог» — `CatalogIcon` (сетка 2×2, тап = `Links.openCatalog`),
    «Наш ТГ-канал» — `TelegramIcon` (самолётик), «Поддержать разработчика» ❤️.
    Иконки каталога/ТГ-port'ы из Tetris (`SettingsScreen.kt`), эмодзи остаются только
    там, где их нет в Canvas. Плотность строк магазина ЗАДАНА ЯВНО (просьба
    пользователя «уменьшить расстояния»): строка бонуса `padding(vertical = 2)`,
    блоки `padding(vertical = 6..8)`, `LinkRow — vertical = 8`, зазор между блоками 8dp.
    Трофеи — кубок на лучах + ЗВАНИЕ по числу побед (`rank_novice/pro/super/legend`,
    10/40/100) и строка «До звания «X» ещё N побед» (`rank_next`/`rank_max`),
    5 звёзд (по 10 побед = звезда), затем белая карточка `StatCard` (иконка + подпись
    + значение, разделительная линия): лучший уровень, всего побед, макс. серия,
    жизни, монеты сейчас, всего собрано в копилке.
  - `Dialogs.kt` — Settings/Bonus/Win/Lose/Revive/Lives/Piggy/FreeCoins попапы
    (`FreeCoinsDialog` — +10 монет раз в 12 ч, открывается «плюсиком» на плашке монет
    и на карте, и в игре). Рамки окон тематические (`GameDialog(theme = ...)`):
    `PiggyDialog` — `FrameTheme.Gold` (золотая рамка/шапка в цвет кнопки-кошелька),
    `LivesDialog` — `FrameTheme.Red` (красная, «Восполнить жизни»),
    `FreeCoinsDialog` — `FrameTheme.Green` (зелёная, «Получить монеты») — по просьбе
    пользователя «фон рамки окна восполнить жизни замени на красный, получить
    монеты — на зелёный». `FreeCoinsDialog`: заголовок `L("free_coins_get")`
    = «Получить монеты» (было «Монеты»), верхний мини-блок (мелкая монета + «+10»)
    УБРАН — осталась только крупная картинка на лучах.
    `WinDialog` — с АНИМАЦИЕЙ: под шапкой `Starburst` (вращающиеся лучи),
    поверх содержимого `Confetti` (приватный хелпер: 46 цветных падающих
    «конфетти» со вращением, `rememberInfiniteTransition` + `Canvas`,
    `Modifier.matchParentSize()` — табы проходят сквозь; параметры считаются
    от индекса, без Random в кадре → нет телепорта). Между числом монет и
    кнопкой «Удвоить награду» — `Spacer(40.dp)` (было 12, «слипались»).
    В `LivesDialog` обе кнопки с ПОДПИСЯМИ-ПОЯСНЕНИЯМИ сверху (игрок не
    понимал, что значит «300» и «видео»): над левой — «за монеты» (11sp,
    серый), над правой — «за рекламу» (11sp, серый); сами кнопки — «+2 ♥ ·
    300» (синяя, `CoinIcon` 22dp) и «+1 ♥ · видео» (жёлтая, `VideoIcon` 20dp),
    обе в `Column(weight(1f))` рядом. Каждая строка — `spacedBy(10.dp)`.
    Монетные кнопки (покупка жизней, получение монет,
    выплата копилки) играют `feedback.coin` (coin1). Rewarded-места: «Бесплатно» в
    BonusDialog (сверло/плавка/заделка/ящик) и ReviveDialog, «♥ 1» в LivesDialog,
    «Удвоить награду» в WinDialog (+Economy.WIN_COINS, скрыть после удвоения),
    «Продолжить» в LoseDialog (возврат жизни + перезавод уровня). Interstitial —
    `notifyLevelFinished()` на итогах: Win, Lose без возможности воскрешения,
    отказ от воскрешения (onDecline).
  - `Profile.kt` — экран «Мое» (`MyDialog`: аватар + ник с переименованием + медаль
    уровня + сетка статистики) и выбор аватара (`AvatarDialog`, список `AVATARS`
    эмодзи с разблокировкой по номеру уровня). Открывается тапом по аватару в левом
    верхнем углу карты; тап по аватару внутри «Мое» → «Аватары». Ник/аватар/серия
    побед в `PlayerStore` (`nickname`/`avatar`/`maxWinStreak`, методы `rename()`
    /`chooseAvatar()` — НЕ `setX`: делегат `by mutableStateOf` уже генерирует сеттер).
    `StatBanner` («Статистика») стилизован под шапку `GameFrame`: pill-скругление
    (`RoundedCornerShape(percent = 50)`) + БЕЛАЯ кайма (`Color.White.copy(0.6f)`,
    была жёлтая) + два `ScrewDot` по краям (тот же винтик, что украшает заголовок
    «Мое»; `ScrewDot` в `Widgets.kt` стал `internal` ради переиспользования).
  - `Icons.kt` — векторные иконки: `CoinIcon` — НЕ вектор, а картинка пользователя
    `drawable-nodpi/coin2_1024.jpg` (реальная золотая монета на белом фоне):
    `Image(ContentScale.Crop)` + `clip(CircleShape)` + `graphicsLayer { scale 1.10 }`
    (зум срезает белую кромку) + ободок `border(1dp, 0x80C98600)`. Параметр `drawPlus`
    УДАЛЁН — все 13 мест монет (счётчики карты/игры, цены и +10 в магазине, трофеи,
    BonusDialog/ReviveDialog/LivesDialog/WinDialog/FreeCoinsDialog, `CoinRain`,
    `PromoBundle`) рисуют одну и ту же картинку. Фото ТЁМНОЕ (средняя яркость ~130),
    поэтому к нему применён `COIN_FILTER` = `ColorFilter.colorMatrix(ColorMatrix(...))`:
    +18 % насыщенности и +34/+26/+8 по каналам — без фильтра монета в 20–24 dp
    выглядела тёмно-коричневым пятном (замеры и подбор — `tools/coin_probe.ps1`,
    `tools/coin_preview.ps1`). Остальные: `HeartIcon`, `DrillIcon`, `FlaskIcon`,
    `HammerIcon`, `BoxIcon`, `PigIcon`, `WalletIcon` (pic_koshelek в диалоге кошелька), `VideoIcon`, `PauseIcon`, `CloseIcon`, `HomeIcon`,
    `TrophyIcon`, `ShopIcon`, `StarIcon`, `ScrewIcon` (шляпка винта через `drawScrewAt`,
    есть параметр `color:` — им красят СИНИЕ винты «Задания дня»), `TelegramIcon`
    (бумажный самолётик) и `CatalogIcon` (сетка 2×2 плашек, правая верхняя α 0.55) —
    port'ы `IconSend`/`IconCatalog` из Tetris `SettingsScreen.kt`, координаты те же.
    Четыре бонус-иконки (Drill/Flask/Hammer/Box без точек) — уже НЕ рисованные:
    приватный `BonusPicture(res)` = `Image(painterResource(...))` картинок
    пользователя из `drawable-nodpi`. `BoxIcon(dots>0)` (ревайв) остаётся рисованным.
    Удалены `GearIcon`/`GiftIcon` (шестерёнка — стандартная Material, кнопка-подарок убрана).
  - `BoardCanvas.kt` — отрисовка доски + `ToolboxWidget` (ящик-заказ: рамка цвета,
    ручка, гнёзда со шляпками) и `HoldRow` (кружки склада, ряд фикс. высоты 44dp;
    радиус = `size.width / maxOf(n, MAX_HOLD_SLOTS=10) * 0.42` — место под
    расширение склада «сверлом» зарезервировано заранее); `drawScrewAt`/`darkenSelf`
    — `internal`, чтобы `ScrewIcon` из `Icons.kt` рисовал ту же шляпку.
    **Физика «висящей» детали = маятник (классы `PieceTilt`/`PieceTilts` здесь же,**
    экземпляр живёт в `GameScreen` и передаётся обязательным параметром `tilts`):
    опора — ЦЕНТРОИД ОСТАВШИХСЯ (невывернутых) винтов детали; целевой угол
    `atan2(comX − pivotX, comY − pivotY)` — такой, при котором центр масс корпуса
    встал ровно ПОД опору (экранные координаты, y вниз ⇒ угол по часовой). Ограничен
    по числу оставшихся винтов: 1 → ±100°, 2 → ±12°, ≥3 → ±5°. Угол накатывается
    демпфированной пружиной `acc = (target−angle)*STIFFNESS − vel*DAMPING`
    (62 / 5,4 — мягко, один заметный перелёт) с реальным `dt` между кадрами
    (`System.nanoTime()` в `clock`). Плюс проседание `sag = sev*8f` и покачивание
    `sway*swayWeight`. Порядок преобразования при рисовании:
    `translate(top = sag*scale) { rotate(angle, pivot, body) }`, поэтому `apply()`
    (куда уехала точка — для старта полёта винта) = поворот + sag, а `invert()`
    (попадание тапа по уехавшему винту) = поворот на −angle от (x, y − sag).
    Деталь срывается на падение СО СВОЕГО ПОСЛЕДНЕГО наклона (иначе «щёлкает»
    обратно). Отверстия винтов рисуются ОБЩИМ хелпером `drawHoles(...)` — и в теле
    живой детали, и в слое `falling` (иначе дырка исчезает раньше, чем деталь упала).
    **Кадр-окно маятника:** тикер `GameScreen` крутит `frameTime` только пока живёт
    `flying`/`falling`, поэтому после прилёта винта доска ЗАМИРАЛА и пружина не
    доигрывала. Решение: при выкручивании винта ставить `swingUntil = now +
    SWING_MS` (1800), тикер продлевать на это окно, а амплитуду покачки гасить
    множителем `swayAmp = swingLeft` (оставшаяся доля окна). Без окна «фигуры не
    вращаются» — при 0 кадров `step()` не вызывается ни разу.
  - `Widgets.kt` — `CartoonButton`, `StatPill`, `GameDialog` (тонкая обёртка над
    `GameFrame(..., width 320dp)`), `GameFrame` — ОБЪЁМНАЯ рамка всех диалогов и
    магазина: тень 12dp + градиентный кант (panelBlue→panelBlueDark→panelEdge) +
    белый блик-обводка + шапка с табличкой/винтиками/крестиком. У `GameFrame`/`GameDialog`
    есть параметр `theme: FrameTheme = FrameTheme.Blue` — enum `FrameTheme { Blue,
    Gold, Red, Green }` красит кант/шапку/винтики под содержимое окна (Gold —
    «Кошелёк», Red — «Восполнить жизни», Green — «Получить монеты»). Тоны собраны
    в приватный `Septet` (7 цветов: кант top/mid/bot, шапка top/bot, фон и знак
    «×»), винтики рисуются через `FrameDot(bg, cross)` вместо прежних `ScrewDot`/
    `GoldDot` (`ScrewDot` остался только в `Profile.kt`). `StatPill` зелёный «+» — ИДЕАЛЬНЫЙ
    КРУГ (`size(32.dp).padding(end=6)` до clip, раньше `size(30).padding(end=4)`
    давал овал 26×30). `Starburst`,
    `ReviveBurst`, `CoinRain` (дождь монет по изменению `trigger`). `FakeAdBanner` удалён — реклама реальная.
  - `AdBanner.kt` — `MyTargetBanner()`: баннер 320x50 через `AndroidView` (MyTargetView),
    высота 0dp пока объявление не загружено (`onLoad`), `destroy()` в `onDispose`.
  - `Links.kt` — внешние ссылки (те же, что в Tetris): `CATALOG` (страница разработчика
    rustore.ru/catalog/developer/tivtpi), `TELEGRAM` (t.me/fidappstudio), `DONATE`
    (Ozon Bank СБП), `EMAIL`. `openAppPage()` целится в пакет RuStore (голый market://
    перехватывает Google Play), при неудаче — веб-карточка. Ничего не роняет при
    отсутствии браузера/магазина.
    **ВАЖНО: каталог открывается ТОЛЬКО через `openCatalog()` → браузер** (перебор
    пакетов `BROWSERS`, `setPackage` + `CATEGORY_BROWSABLE`, при неудаче — обычный
    `open()`). Голый `ACTION_VIEW` на `https://www.rustore.ru/...` перехватывает
    приложение RuStore и «Назад» уводит на рабочий стол, минуя игру (см. «Грабли»).
  - `Legal.kt` — `TERMS_OF_USE_TEXT` (адаптация текста Tetris под монеты/жизни/копилку),
    `splitLegal()` (короткая строка с точкой = заголовок), `LegalDialog()` — скролливаемый
    показ юр. текста с синей шапкой и нижним «ЗАКРЫТЬ».
  - `Palette.kt` (цвета), `Labels.kt` (`L(key)` + `LocalPlayerStore`), `Feedback.kt`
    (звук на ассетах res/raw + вибо + музыка + `LocalClickSound`).
- `ads/Ads.kt` — `AdSlots` + `RewardAdManager` + `InterstitialManager` (см. «Реклама»).
- `notify/` — напоминания «поиграй сегодня» (см. «Напоминания»): `Reminders.kt`
  (канал/разрешение/план) + `ReminderWorker.kt` (ежедневное локальное уведомление).
- `MainActivity.kt` — создаёт `PlayerStore`, показывает `ScrewGame`; в `onCreate`
  предзагрузка рекламы: `RewardAdManager.init(this)`, `InterstitialManager.init(this)`;
  сверка тумблера напоминаний с джобой (вкл+разрешение → `schedule`, иначе `cancel`).
- Тест проходимости: `app/src/test/java/com/fidapp/screw/LevelSolvabilityTest.kt`.

## Контракт движка (не менять без нужды)
1. Тап по доступному шурупу → в открытый ящик СВОЕГО ЦВЕТА (`boxFor`),
   иначе на свободный кружок склада (`hold`); ни того ни другого — Rejected.
   Цель полёта — `Destination.BoxSlot(i)` / `Destination.HoldSlot(j)`,
   UI рисует overlay-дугу в экранную координату этой ячейки.
2. Ёмкость ящика = фактическое число винтов этого цвета на уровне, но ≤3
   (`buildBoxQueue`: 7 зелёных → ящики 3+2+2); порядок очереди детерминирован
   номером уровня, первые два ящика — разных цветов.
3. Ящик наполнился (3 шт.) → ещё виден полным; уезжает он только когда UI
   вызовет `settle()`. Порядок тайминга в `GameScreen`: тап → `tapScrew` (движок
   сразу коммитит винт в `box.count`/`hold`, но TRAY НЕ обновляем — винт ещё летит);
   прилёт → `refreshTray()` (винт появляется в ящике/ПУЛе, полный показан на 3)
   и `settleSeq++`; отдельный `LaunchedEffect(settleSeq)` через ~320 мс зовёт
   `settle()` (полный ящик уезжает, приезжает следующий, определяется итог).
   `tapScrew` НЕ делает settle и НЕ определяет итог — итог возвращает `settle()`.
   «милосердие»: первым приезжает цвет, которого больше всего на складе, и его
   винты автоматически перелетают в новый ящик.
4. Доступность: верхняя живая деталь перекрывает шляпку винта **ХОТЬ ЧАСТИЧНО** —
   винт недоступен (пробы: центр + 4 точки на радиусе `SCREW_HEAD_R*0.55≈34`
   логич. единиц). ТО ЖЕ для винтов одной детали: шляпка перекрыта шляпкой винта
   с БОЛЬШИМ id (нарисован позже = «выше слоем») — нижний не выкрутить, пока не
   выкручен верхний (`overlapsScrew`, окружности радиуса `SCREW_HEAD_R`).
   БЛОКИРУЮТ и серые «стёкла» (`locked`) — сквозь них выкручивать
   нельзя; стекло убирается только бонусом `melt()`. Поэтому `evaluateEnd`
   НЕ объявляет поражение, пока на доске живо хоть одно стекло (`hasLiveLocked`)
   И ПУЛ не забит.
   `greedySolve` при тупике тоже плавит стекло (шаг-заглушка = `melt()`).
5. У детали не осталось шурупов → `Event.PieceFell` → UI анимирует падение
   (FallingPiece: вниз + поворот + fade), НО НЕ сразу: у неё стоит `beginAt =
   now + FALL_DELAY_MS`, то есть деталь висит, пока последний винт выкручивается
   и улетает в ящик (порядок просил пользователь). Выкручена часть винтов → деталь
   «висит на оставшихся»: наклон + покачивание + проседание ВОКРУГ ЦЕНТРА МАССЫ
   ОСТАВШИХСЯ винтов (BoardCanvas), а не вокруг центра фигуры.
6. Победа — доска пуста (`Event.Win`); поражение — шурупы ещё на доске И
   (ЛИБО ПУЛ забит под завязку `hold.size >= holdCapacity` — конец СРАЗУ, независимо
   от стекла и подходящих ящиков (правило пользователя «5 винтов в пуле = конец
   игры»), ЛИБО легальных ходов нет и сплавить нечего (`accessibleScrews().none
   { canTap } && !hasLiveLocked()`)). Итог определяет только `settle()`.
   Если игрок тапнул невозможный винт — UI зовёт `checkStuck()` (публичная обёртка
   `evaluateEnd`) и показывает поражение сразу, не дожидаясь прилёта другого винта.
7. Движок ничего не знает про деньги/Android — только логика + события.
- Бонусы: `drill()` +1 отверстие в ПУЛ/склад (до +5, итого 10 — потолок
  `MAX_HOLD_SLOTS` в `BoardCanvas`, ряд заранее держит место под все десять:
  радиус считается от `size.width / maxOf(n, MAX_HOLD_SLOTS)`, иначе на
  стартовых 5 кружки выглядят крупно и новые «упираются» в край); `melt()` убрать верхнюю locked/
  пустую деталь; `seal()` освободить ВЕСЬ склад; `openBoxTile()` (+нужен
  `canOpenBox()`) — открыть плитку «Открыть ящик» (цена/реклама — в UI).
- Экономика (`Economy`/`PlayerStore`): регенерация жизни — 1 шт. каждые
  `LIFE_REGEN_MS` = 5 минут (не 30); бесплатные монеты `FREE_COINS`=10 раз в
  `FREE_COINS_COOLDOWN_MS`=12 ч (`freeCoinsReady`/`freeCoinsWaitMs`/`claimFreeCoins`).
  Плашка монет «+» ведёт в `FreeCoinsDialog`, а НЕ в бонус «Открыть ящик».
  +10 в магазине: `SHOP_COINS`=10 раз в `SHOP_COINS_COOLDOWN_MS`=6 ч
  (`shopCoinsReady`/`shopCoinsWaitMs`/`claimShopCoins`, ключ prefs `lastShopCoinAt`).
  Цена бонуса — ТОЛЬКО через `Economy.priceOf(type)` (магазин, игра, диалог).
  **Инвентарь бонусов:** `bonusCount/addBonus/spendBonus(type)`, ключ prefs
  `bonus_<ordinal>`; купленное в магазине применяется в игре без денег и видео.
  **Счётчики для трофеев:** `totalWins` (ключ prefs `wins`, растёт в `onLevelWon`),
  `maxWinStreak`.
- `greedySolve()` — само-проход с просмотром на 1 ход (симуляция на клоне),
  используется тестом и генератором. Каждый шаг = `tapScrew()` затем `settle()`
  (иначе очередь ящиков не едет и уровень ложно нерешаем).

## Инвариант проходимости (важно!)
Генератор: максимум 4 цвета (иначе «чужих» винтов больше кружков склада —
deadlock), цвета по кругу (каждая фигура — микс цветов, иначе верхние слои
из цветов без активного ящика), плюс подбор сида через `greedySolve()`.
Критическая правка в генераторе: `randomPiece` обязан передавать `locked = locked`
во ВСЕ конструкции `Piece`, иначе «locked-заполнитель» становится обычной деталью
без шурупов и навсегда перекрывает другие (ловушка уровня 17).

## Реклама (VK Ads / myTarget) — реальная
- Зависимость `com.my.target:mytarget-sdk:5.52.5` (mavenCentral), `buildConfig = true`,
  permissions `INTERNET` + `ACCESS_NETWORK_STATE`. Глобальная инициализация по appId
  НЕ НУЖНА — кабинет ВК привязан к пакету; достаточно `MyTargetManager.setDebugMode(BuildConfig.DEBUG)`.
- Слоты (`AdSlots` в `ads/Ads.kt`, одни и те же в debug и релизе; в отладке запросы
  помечаются тестовыми): баннер `2101504`, rewarded `2101507`, interstitial `2101510`.
- `RewardAdManager`: экземпляр `RewardedAd` ОДНОРАЗОВЫЙ — после show/dismiss/error
  создаём новый и предзагружаем (перезагрузка использованного = ошибка 4001).
  Награда выдаётся СТРОГО в `onReward`. Видео не готово → тост, действие НЕ выполняется.
- `InterstitialManager`: показ после каждых 5 завершённых уровней (победа или финальное
  поражение), счётчик в SharedPreferences `ad_prefs/levels_since_ad`. В debug-сборке
  ПОЛНОСТЬЮ отключена (`ENABLED_IN_DEBUG = false`), чтобы не мешать тестам.
- Баннер в Compose — `MyTargetBanner()` (`ui/game/AdBanner.kt`) через `AndroidView`,
  фикс. формат `ADSIZE_320x50`; высота 0dp до `onLoad` (ряд не «прыгает»), `destroy()`
  при dispose. `setListener` у InterstitialAd помечен deprecated —WARNING нормален
  (в Finddifferences так же, работает).
- Референс интеграции — проект `C:\Users\user\AndroidStudioProjects\Finddifferences\`
  (`AdManager.kt`/`BannerAd.kt`/`RewardAdManager.kt`); там Activity+XML, здесь — Compose.

## Напоминания (локальные пуши, без сервера)
- 4-й тумблер в настройках (после музыки): «Напоминания» 🔔 (`L("reminders")`,
  `PlayerStore.reminders`, дефолт ВЫКЛ). Тот же `SettingsBody` и в паузе, и на карте.
- Включение: если `POST_NOTIFICATIONS` не выдано (API 33+) — `rememberLauncherForActivityResult
  (RequestPermission)`; отказ → тумблер честно остаётся выключен. В debug сразу после
  включения — `Reminders.debugTest()` (тестовое уведомление через ~3с).
- `ReminderWorker`: раз в сутки ~19:00; молчит, если флаг выключен, нет разрешения
  или уже пинговали сегодня (`reminders_state/last_notify_day`). Повод 1 — сегодня
  не играли (`PlayerStore.playedToday()`, день ставится в
  `LaunchedEffect(levelNumber)` игры); повод 2 — играл, но `bestLevel` в 2..50.
  Тексты ротируются по номеру дня. Tap → MainActivity.
- Зависимость `androidx.work:work-runtime-ktx:2.9.1`, permission `POST_NOTIFICATIONS`
  (в манифесте — для модератора RuStore: локальные уведомления, без сбора данных).

## Стиль / конвенции UI
- В настройках (`SettingsBody` — пауза и карта) — четыре тумблера: вибрация, звук,
  музыка, напоминания; кнопок поддержки/промо нет (ссылки — в магазине).
  **Правило для ВСЕХ игр пользователя:** выключенный (неактивный) тумблер — БЕЗ
  обводки/рамки, просто БЛЕДНЕЕ (`Modifier.alpha(0.5f)` + `uncheckedBorderColor =
  Color.Transparent` в `SwitchDefaults.colors`); фон выключенного — светло-зелёный
  (`0xFFC9E8C9`), не серый.
- Диалог «Кошелёк» (`PiggyDialog`) — ЖЁЛТЫЙ градиент (был сиреневый), текст тёмно-коричневый.
  С РАМКОЙ `GameDialog` как у всех окон (пользователь: «рамка как везде — ты забыл»);
  внутренний жёлтый «кубик» с собственным close убран — заголовок/крестик даёт шапка.
- Координаты доски — в логических `Board.W/H`, Canvas масштабирует под реальный размер.
- Мультяшный объём: `CartoonButton` = цветной верх + тёмный нижний торец (offset -3dp).
- Все подписи только через `L("key")`; новые ключи — в словарь `Labels.kt` (ru,en).
- `PlayerStore` читается/пишется через `LocalPlayerStore`; состояние — Compose-стейт.
- Лента уровней на карте (`MapScreen`) ПРИНУДИТЕЛЬНО центрирует текущий узел:
  меряем `boundsInRoot().center.y` вьюпорта и текущего `LevelNode` через
  `onGloballyPositioned`, ждём оба значения `snapshotFlow { a to b }.first { оба > 0 }`
  и крутим полоску `stripScroll.animateScrollTo((stripScroll.value + delta).roundToInt())`
  (просьба пользователя: без этого уровень прятался под кнопкой «Играть»). Не
  «угадывать» долю прокрутки — только реальные замеры layout.
- Верхняя панель карты: аватар → монеты (ширина 140dp, «плюсик» не должен
  упираться в край) → `Spacer(weight(1f))` → жизни (120dp) → шестерёнка. Пустое
  тянется МЕЖДУ монетами и жизнями, поэтому жизни прижимаются к шестерёнке
  (просьба пользователя).
- Одинаковая высота карточек статистики в «МОЕ» (`Profile.StatsGrid`): ряд
  `Row(Modifier.height(IntrinsicSize.Max))` + `Modifier.weight(1f).fillMaxHeight()`
  у карточки + `Spacer(Modifier.weight(1f).height(6.dp))` перед плашкой значения.
  Фиксировать высоту заголовка (`Modifier.height(32.dp)`) НЕЛЬЗЯ — на крупном
  системном шрифте вторая строка обрезается.

## Грабли (уже пройдены)
- `L()` — `@Composable`. Нельзя звать внутри не-composable локальных fun/колбэков.
  Решение: вычислить строку на верхнем уровне composable (`val notEnough = L(...)`)
  и захватить в замыкание. ТО ЖЕ для `LocalClickSound.current()` — вызывать его
  внутри лямбды `clickable {}` нельзя (Нарушение: «@Composable invocations can only
  happen…»): читать в начало функции `val click = LocalClickSound.current`.
  `val click` обязан лежать ДО Модификатора, который его использует (иначе unresolved).
- `@Composable`-лямбду нельзя положить в обычный `Triple/Quad` и вызвать позже —
  для отложенной иконки делать отдельный `@Composable fun`.
- Делегат-свойство `var x by mutableStateOf(...)` генерирует JVM-сеттер `setX`,
  что коллидится с одноимённым методом. Для `vibration/sound/music` — явный
  геттер/сеттер поверх приватного `*State`.
- Material3 1.4.0: у `Switch` НЕТ параметра `border:` — обводка убирается только
  через `SwitchDefaults.colors(checkedBorderColor = Color.Transparent,
  uncheckedBorderColor = Color.Transparent)` (попытка `border = null` = «No parameter
  with name 'border'»).
- Белый фон картинок пользователя вырезан офлайн PowerShell+`System.Drawing`
  (flood-fill от границ, порог каналов ≥210) в прозрачный PNG; `.jpg` и `.png` с ОДНИМ
  base-name в одной папке = конфликт ресурсов AAPT, поэтому jpg-копии удалены.
- `Piece.contains/bounds` — только block-body (expression-body + `return` = ошибка).
- Литералы float в Canvas-арифметике писать как `0.3f`, иначе Double/Float mismatch.
- `.graphicsLayer {}` требует явного `import androidx.compose.ui.graphics.graphicsLayer`
  (без него — unresolved); для простого статичного поворота удобнее `Modifier.rotate`.
  В `BoxSwapSlot` именно `graphicsLayer { translationY/alpha }`: смещение НЕ влияет на
  layout, поэтому `onGloballyPositioned` внешней обёртки продолжает давать верную
  цель полёта винта.
- Compose 1.11: `boundsInRoot()` — extension-функция, нужен явный
  `import androidx.compose.ui.layout.boundsInRoot`, иначе unresolved.
- Edge-to-edge (API 35+): нижний контент закрывают системные кнопки — везде,
  где нужен отступ снизу, использовать `Modifier.navigationBarsPadding()`.
- Цель полёта в ящик: в ряду `weight(1f)` делят И ящики, И плитки «Открыть
  ящик» — делить ширину ряда на число ящиков нельзя. Каждый `ToolboxWidget`
  сдаёт свой `boundsInRoot()` в `boxCells` через `onGloballyPositioned`.
- `material-icons-core` (лёгкое ядро) содержит ТОЛЬКО ~50 иконок: Settings, Create,
  Home, Build, Favorite, Lock, Star… — `AccountBalance`/`AccountBalanceWallet` там нет
  (это `material-icons-extended`). В этом проекте extended ПОДКЛЮЧЕН ради кошелька; в
  debug это ≈+7 МБ, release (R8 `optimization.enable=true`) подстригает неиспользуемое.
  `Modifier.tint`/`aspectRatio` недоступны — красить через параметр `tint =` у `Icon`.

- Ряд ящиков/склада (`TrayRow`) обязан читать снимок в `mutableStateOf`
  (`trayBoxes`/`trayHold`/`trayTiles`), который обновляет `refreshTray()` после
  каждой мутации движка (тап, прилёт→`settle()`, бонус, старт уровня). Чтение
  полей `engine` напрямую НЕ перерисовывается: `engine` — обычный класс без
  snapshot-наблюдения, и на телефоне лоток залипал пустым (винты «улетают и
  пропадают», полный ящик не уезжал).
- `MutableIntState` (из `mutableIntStateOf`) — `intValue` это СВОЙСТВО, а не функция:
  `state.intValue` / `state.intValue += 1`, но НЕ `state.intValue()` (иначе
  «Expression 'intValue' of type 'Int' cannot be invoked as a function»). В
  `PlayerStore.bonusCount` — именно как свойство.
- `tween(...)` ждёт `durationMillis: Int`, поэтому константу времени анимации надо
  объявлять `private const val BOX_SWAP_MS = 700` (Int), а не `320L`. Значение
  поднято с 320: игрок не успевал увидеть, как полный ящик уезжает вверх.
- `Column` внутри `Column`: `Modifier.weight(1f)` растягивает ТОЛЬКО по главной оси
  родителя (вертикали), поэтому внутренняя лента обязана дополнительно иметь
  `.fillMaxWidth()` + `horizontalAlignment = CenterHorizontally` — иначе её контент
  прижимается к левому краю (так было с лентой уровней).
- Идиома «одноразовой» анимации смены значения (`BoxSwapSlot`): `var previous by
  remember { mutableStateOf(v) }` + `val ghost = remember(v) { previous.takeIf { it != v } }`
  + `LaunchedEffect(v) { previous = v; anim.snapTo(0f); anim.animateTo(1f) }`.
  Состояние запоминается по ПОЗИЦИИ в ряду (без `key`) — при изменении числа
  элементов «призрак» может появиться ложно (чисто косметика).
- **Каталог RuStore «выбрасывает» из игры (3 ложных diagnostics-фикса, причина
  найдена adb-репро):** голый `ACTION_VIEW` на `https://www.rustore.ru/...`
  перехватывает приложение RuStore (`ru.vk.store/.app.MainActivity`), которое
  ДОБИВАЕТ запись, запущенную от игры (`Duplicate finish request`), и пересоздаёт
  своё окно с `flg=0x1000c000` = NEW_TASK | RESET_TASK_IF_NEEDED |
  **EXCLUDE_FROM_RECENTS** — цепочка «назад» рвётся, и системный «Назад» ведёт
  на launcher (рабочий стол), минуя игру. Игра при этом ЖИВА (в crash-buffer
  пусто), поэтому НИКАКОЙ BackHandler/тайминг-гард помочь не мог — это не
  «призрачный бэк». Лечится только обходом app-link: `Links.openCatalog()`
  жёстко целит ссылку в браузер (`setPackage` по перебору `BROWSERS` +
  `CATEGORY_BROWSABLE`; `startActivity` не ограничен package-visibility, `<queries>`
  в манифесте не нужен). Проверено на realme RMX3263 (Android 11): Chrome
  открывается, «Назад» возвращает в игру.
- Кадровый цикл, завязанный на «активные листы» (`flying`/`falling`), глохнет,
  как только последний полёт завершился, — и любая физика, интегрируемая в
  `draw`-блоке (маятник `PieceTilts.step`), замирает на полуслове. Для отыгрыша
  анимации после полёта нужно ЯВНОЕ окно кадров (`swingUntil`/`SWING_MS`), а не
  надежда на бесконечную transition: `rememberInfiniteTransition` сама по себе
  перерисовку доски не гарантирует, если Canvas завернул её значение в state,
  который не меняется.
- **Таймер-«заморозка» в Compose:** `Text(countdown(nextAt))`, где `countdown` внутри
  читает `System.currentTimeMillis()`, НИКОГДА не обновится — Compose не видит
  source-of-truth и считает аргументы неизменными (баг жалоба: «таймер не идет
  в диалоге „Восполнить жизни?“»). Лечится ЯВНЫМ state-параметром:
  `var now by remember { mutableLongStateOf(...) }` + `LaunchedEffect { while(true)
  { now = ...; delay(1000) } }` + `Text(countdown(nextAt, now))`. В `LivesPill` на
  карте — тот же приём (`countdownLabel(nextAt, now)`).

## Команды сборки (Windows PowerShell)
```powershell
cd C:\Users\user\AndroidStudioProjects\Screw
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest --console=plain    # тест проходимости
.\gradlew.bat :app:assembleDebug --console=plain        # debug APK
```
Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
JAVA_HOME обязан указывать на JBR Android Studio (toolchain 25, Gradle 9.x).
