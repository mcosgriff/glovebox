---
name: design-push
description: Send boards edited in `design/source/` back to the Glovebox design canvas, so the canvas draws them and the next pull keeps them. Use when the user asks to push, send or sync design changes to the canvas, or after editing a `.dc.html` here that the canvas should show.
---

# Push boards to the design canvas

The canvas is the source of truth for screens (see `design-pull`); this is the way back, for a board
edited here. Canvas: https://claude.ai/artifact/Git6fFmT44PutXCxW4gxo9 (the `code/artifact/7f52…` link in
CLAUDE.md is the same one; publish to this short form, the long one is refused). Pushing changes the
user's live canvas: do it only when asked, and name the boards first.

## Steps

1. **Pick the boards.** The ones the user named, else every `design/source/*.dc.html` changed since its
   last commit that came from a pull (`git diff` / `git status`). `canvas.json` goes only when boards are
   added, removed, moved or resized.
2. **Read the canvas's copy.** Artifact `read` on the canvas, no `path`, first (a publish is refused until
   the artifact itself was read this session), then `paths` with each `project/<Name>.dc.html`, in one
   call. Note the version id.
3. **Check nobody edited it there.** For each board, `git hash-object` the saved canvas copy and compare it
   with the file's history (`git log --format=%H -- design/source/<Name>.dc.html`, `git rev-parse <commit>:<path>`):
   - same as the local file: nothing to push, drop it;
   - same as a committed version: the canvas holds what was last pulled, safe to push;
   - neither: **someone saved a change on the canvas since the pull.** Stop, say which boards and the
     version id, and offer to merge it into the local file first. Never publish over it.
4. **Stage.** Copy each board to `<scratchpad>/canvas/project/<Name>.dc.html` (and `project/canvas.json`
   when it goes). The canvas serves a file at its path under `root`, so the `project/` folder is required.
5. **Publish once.** Artifact publish with `url` (the short form), `root` = `<scratchpad>/canvas`,
   `file_path` = the absolute path of one board, `files` = the rest as `{"project/<Name>.dc.html":
   "project/<Name>.dc.html"}`, and a short `label`. Refused because the canvas changed meanwhile: go back
   to step 2 for the boards it names. A third refusal: tell the user and stop.
6. **Report.** The new version number and id and the boards sent. The PNGs in `design/screens/` come only
   from Claude Design's export: list each pushed board's PNG as **render stale** until the user exports
   it and runs `design-pull`. Commit only when asked (`docs(design): …`, on a non-main branch).

## Rules for the files

- Keep each board a whole `.dc.html`: the `<script src="./support.js"></script>` head line exactly, the
  `<x-dc>` markup, and the `<script type="text/x-dc" data-dc-script>` block. `{{hole}}` is a lookup into
  `renderVals()`, never an expression.
- Sample data stays made up, as in `design-pull`: before a push, check for real VINs, names or addresses.
- Don't render, screenshot or re-read the canvas to check a push. Open the canvas link if the user wants
  to see it.
