# Open Sudoku 2

[Open Sudoku 2](https://github.com/buliasz/open-sudoku-2) is an open-source Sudoku game based on the
original Open Sudoku project. Open Sudoku 2 has been converted/rewritten to the Kotlin
programming language with many modifications and reimplementation to be up to date with the current
Android software development standards. New functionalities as well as improvements for the existing
ones are also in progress.

It's designed to be controlled both by finger and keyboard.
It's preloaded with 90 puzzles in 3 difficulty levels.
More puzzles can be downloaded from the web, and it also allows you to enter your own puzzles.

## Authors and Contributors

Open Sudoku 2 is authored by [Bart Uliasz](https://github.com/buliasz).

Open Sudoku was authored by [Óscar García Amor](https://ogarcia.me/).

The first version of Open Sudoku was developed by [Roman Mašek](https://github.com/romario333) and
was contributed to by Vit Hnilica, Martin Sobola, Martin Helff, and Diego Pierotto.

### Contributors

* [Sergey Pimanov](https://github.com/spimanov):
    * Added functionality to highlight cells with similar values

* [TacoTheDank](https://github.com/TacoTheDank):
    * Compile with and updated support up to Android Pie (except for the SDK 23+ permission check
      for external storage access)
    * Added Java 1.8 compatibility (lambdas, methods, etc)
    * Reformatted code
    * Slightly improved app performance within "res" files
    * Fixed the grammar of some strings
    * Migrated App to AppCompat
    * Migrated to AndroidX

* [steve3424](https://github.com/steve3424):
    * Fixed some bugs
    * Added permission checks for reading/writing external storage
    * Added puzzle solver

* [Justin Nordin](https://github.com/jlnordin):
    * Added awesome theme preview
    * Added new title screen with resume feature
    * Added spectacular full App UI themes
    * New action buttons for undo and settings in game screen
    * New option for highlight notes that match a particular number when a cell is selected
    * New menu option to undo to before mistake
    * New config option to remove notes on number entry
    * New config option to skip titlescreen
    * Fix folder view to show sudoku previews with custom theme colors
    * Fix the crash when no cell was selected and you used the _hint_ command
    * Fix crash when using the new feature to automatically remove cell notes

* [Chris Lane](https://github.com/ChrisLane):
    * Keep same values highlighted after empty cell selection

* [Jesus Fuentes](https://github.com/fuentesj11):
    * New menu option to sort puzzles by last played and play time

* [Daniel](https://github.com/demield):
    * Added copy and paste ability in sudoku editor
    * Added ASC/DESC sorting and hex input for custom theme colours

* [Роман](https://github.com/D0ct0rZl0):
    * Added _reset all_ option in sudoku list menu
