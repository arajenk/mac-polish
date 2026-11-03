# Mac Polish

Client-side Fabric mod for Minecraft 26.3. It only does anything on macOS: on other systems its
mixins aren't applied at all. It only needs Fabric Loader.

Build with `./gradlew build`. The jar is `build/libs/macpolish-1.3.0.jar`; copy it to your
`mods` folder.

## Fixes

### 1. Pointer unlocked and cursor visible after Cmd-Tab
`PointerLockMixin` and `CocoaPointerLock`: after a focus change, SDL 3.4.14 can get stuck in its
"focus click pending" state. It then skips locking the pointer and hiding the cursor while still
reporting relative mode as on, so the pointer roams behind the game, shows at the screen edges, and
can open the menu bar or Dock. During play this clears that state through SDL's own window listener,
which makes SDL re-apply the lock, and ignores mouse movement for 3 frames so the camera doesn't jump.

It waits while a mouse button is held, and turns itself off if a future SDL renames these methods.

Test each of these in the full-screen mode you play in:
1. In a world, look all the way up and down. The cursor should stay hidden.
2. Cmd-Tab away and straight back, then repeat. The cursor should stay hidden with no camera jump.
3. Cmd-Tab away for a few seconds so the pause menu opens, come back, click Back to Game, and repeat.
4. Open your inventory, Cmd-Tab away and back. The cursor should stay visible and usable.

### 2. Shift+scroll goes the wrong way (optional, off by default)
`ShiftScrollMixin`: vanilla's Shift+scroll is right on a stock Mac. It goes the opposite way to normal
scrolling if something changes vertical scrolling but not horizontal, such as Scroll Reverser setting
your mouse to Classic while macOS stays on natural. Set `flipShiftScroll=true` if that's your setup.

To test, scroll over the hotbar with and without Shift. The selection should move the same way.

### 3. Held item cut off on Mac displays
| Before | After |
|---|---|
| ![Before](docs/images/framing-before.png) | ![After](docs/images/framing-after.png) |

`HeldItemFramingMixin` and `HeldItemFraming`: Mac screens are narrower than 16:9, which moves the
screen edge inward and cuts off more of the item in your hand. This slides the hand toward the center
by exactly that difference, using a shear rather than a 3D move, so it is the same image you would see
at 16:9, in the same place relative to the edge. Screens at 16:9 or wider are unchanged, two-handed maps
stay centered, and an empty hand stays where vanilla puts it.

To test, hold an axe or sword in full screen with any small-tools resource pack turned off. Then check
attack swings, eating, a shield or bow, an empty hand, the off hand, and a map in one and both hands.

`./gradlew runClientGameTest` renders the held item at 16:9 and at Mac aspect ratios to compare. The
screenshots go to `build/run/clientGameTest/screenshots`. This test is not part of the jar.

## Settings
With Mod Menu installed, open Mods, select Mac Polish and click the settings icon. Each fix has its
own on/off button that applies right away. Inside a world the background stays clear, so you can watch
the held item move behind the screen, which makes a quick before and after.

Without Mod Menu, edit `config/macpolish.properties`, created on first launch, and restart the game.

| Setting | Default | What it does |
|---|---|---|
| `pointerLockRepair` | `true` | Keeps the pointer locked after Cmd-Tab, see fix 1 |
| `flipShiftScroll` | `false` | Reverses Shift+scroll, see fix 2 |
| `heldItemFraming` | `true` | Frames held items as at 16:9, see fix 3 |

## Adding a fix
1. Add one mixin to `dev.macpolish.mixin`. Keep logic that can be unit tested in its own class.
2. List it under `client` in `macpolish.mixins.json`.
3. Add a setting to `MacPolishConfig`, and a toggle and text to `MacPolishConfigScreen` and `en_us.json`.

It is then limited to macOS automatically by `MacOnlyMixinPlugin`.

## License
MIT, see `LICENSE`.
