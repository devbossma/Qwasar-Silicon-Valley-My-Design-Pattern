# Coverage screenshots — capture guide

Drop your screenshots into this folder using **exactly these filenames** (PNG or JPG —
if you use `.jpg`, update the extension in the main `README.md` image links to match).
Each one corresponds to a numbered step in the main `README.md`'s
**"Measuring Test Coverage"** section.

## IntelliJ
| Filename | What to capture |
|---|---|
| `intellij-1-run-with-coverage.png` | Right-click a test class/package (e.g. `ChatServiceTest`) → the context menu showing **"Run '...' with Coverage"** |
| `intellij-2-coverage-tool-window.png` | The **Coverage** tool window that opens afterward, showing per-package/class percentages |
| `intellij-3-gutter-highlighting.png` | An open source file with green/red gutter highlighting next to the line numbers |

## Eclipse
| Filename | What to capture |
|---|---|
| `eclipse-1-install-eclemma.png` | **Help → Eclipse Marketplace**, searching for "EclEmma" |
| `eclipse-2-coverage-as-junit-test.png` | Right-click a test class/package → **Coverage As → JUnit Test** |
| `eclipse-3-coverage-view-results.png` | The **Coverage** view showing results after the run |

## VS Code
| Filename | What to capture |
|---|---|
| `vscode-1-install-coverage-gutters.png` | The Extensions marketplace with "Coverage Gutters" found/installed |
| `vscode-2-display-coverage-command.png` | The Command Palette with **"Coverage Gutters: Display Coverage"** selected (after running `mvn test`) |
| `vscode-3-gutter-highlighting-result.png` | The resulting green/red gutter highlighting in an open source file |

Once these 9 files are in place, the main README will render them automatically — no
further edits needed on your end.
