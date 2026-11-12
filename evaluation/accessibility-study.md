# Accessibility study

This extends the usability study in the ID document (Evaluation Plan, Usability Study) with
three accessibility checks. They test NFR5 in the SE document: colour is never the only
signal, and every control works from the keyboard.

Do not record participant names, student numbers or details of anyone's eyesight in this
repository. Record only the session code, the condition and the observations below.

## 1. Keyboard-only navigation

**Who:** any peer participant. Unplug or move the mouse away before starting, and turn on
**Settings → Keyboard → Strong focus outline** (it is on by default).

**Tasks:** read each one aloud and do not demonstrate the keys.

| # | Task | Done when |
|---|---|---|
| K1 | Open the Diagnostic Gate. | The Diagnostic Gate screen is showing. |
| K2 | Answer the first diagnostic question and go to the next one. | Question 2 is showing. |
| K3 | Finish the diagnostic. | The result card is showing. |
| K4 | Open lesson L1 and reach the first Try question. | A Try page is showing. |
| K5 | Use the hint on a Try question, then answer it. | Feedback is showing under the answer. |
| K6 | Open the Tree Activity, insert 30, and step through the trace. | The trace reaches its last step. |
| K7 | In Settings, move the text size slider to 120% and press Apply. | All text is larger. |

**Record for each task:** completed (yes / with help / no), time, the number of times the
participant lost track of where the focus was, and any control they could not reach.

**Keys the app supports:**
- Tab and Shift+Tab move between controls.
- Space or Enter activates the focused control.
- A–D pick an answer.
- Enter submits the answer and then moves on.
- Ctrl+1 to Ctrl+5 jump between the sections.
- ← → and Esc step through a trace and reset it.

Note whether the participant found these without being told. The list is on the Settings page.

## 2. Low-vision participants

**Who:** participants who use magnification or large text day to day, or who have low vision.
Recruit through the usual channels and do not ask anyone to disclose a diagnosis. Running the
condition with any participant also gives some evidence, but say so in the report.

**Set-up:** let the participant choose a text size (Large or Extra large) and a palette (try
High contrast first) in Settings before the tasks. Note which they chose. If they use their own
magnifier, record that too.

**Tasks:** K1 to K5 above (mouse allowed), then:

| # | Task | Done when |
|---|---|---|
| V1 | Say which node in the tree is the current node. | Correct node named. |
| V2 | Say whether your last answer was right, without reading the feedback text aloud. | Correct, from the card's mark or colour. |
| V3 | Turn on "Describe trees in text" and read the description of the tree. | The participant finds and reads the description. |

**Record:**
- Completion and time.
- Any text that was clipped, overlapped or ran off screen at their chosen size.
- Whether the window needed scrolling sideways.
- Comments on the palette and on the focus outline.

## 3. Screen-reader check

The app has screen-reader labels, but they have **not yet been tested with a screen reader**.
Run this check before claiming screen-reader support. Use Narrator (Windows, Ctrl+Win+Enter)
and, if available, NVDA.

| Check | Expected |
|---|---|
| Tab through the sidebar | Each section is read by name, for example "Lessons, button". |
| Focus an answer card | "Option A: ..., button". After choosing it: "..., Selected". |
| Submit an answer | The card reads "Your answer: correct" or "not correct". The feedback panel reads the heading and explanation. |
| Focus a tree drawing | The description is read, for example "Tree with 7 nodes. The root is 50. ...". |
| Activate a card with the screen reader's own "activate" command | The answer is chosen. |
| Settings text size slider and colour filters | The slider reads "Text size, Large, 120%" as it moves. Each colour filter is read by name, with its description as help. |

Record anything that is read wrongly, read twice or not read at all, and add it to the issue
tracker.

## 4. Colour-vision check

The palettes are checked automatically by `PaletteTest`. It simulates protanopia, deuteranopia,
tritanopia and greyscale vision using the Machado et al. (2009) model, and it checks WCAG AA
text contrast. In a session, a participant with colour-vision deficiency can also do V1 and V2
in their preferred palette.
