# README images

- `screens/*.png`: screenshots of the debug build on the `Glovebox_E2E` emulator (Pixel 10 Pro skin),
  resized to 540 px wide.
- `demo/*.webp`: the three clips in "See it work", animated WebP at 360 px, played 1.5× faster.
- `badge_obtainium.png`: Obtainium's own badge (GPL-3.0), trimmed of its transparent padding.

**Only made-up data:** a fresh install on a throwaway emulator, VIN `JTEBU5JR8K5000000` (a valid 2019
4Runner pattern with serial 000000), odometer 48,210. Never capture the emulator or phone with your
own records on it.

How they were made, to redo them after the screens change:

- The app sets `FLAG_SECURE` ("Hide in recent apps", on by default), so `adb shell screencap` and
  `screenrecord` come out black. The emulator's own capture isn't blocked:
  `adb emu screenrecord screenshot <file.png>` and `adb emu screenrecord start --time-limit 180 <file.webm>`
  / `adb emu screenrecord stop`. Use demo mode for a clean status bar
  (`am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941`, etc.).
- Clips: `ffmpeg -i in.webm -vf "setpts=PTS/1.5,fps=10,scale=360:-1:flags=lanczos" fr/%04d.png`, then
  `img2webp -loop 0 -lossy -q 60 -m 6 -d 100 -mixed fr/*.png -o out.webp`. Animated WebP is about a
  quarter the size of a GIF of the same clip, and GitHub shows it inline; a video file in the repo it
  doesn't.
- Stills: `magick in.png -resize 540x -strip out.png`.
