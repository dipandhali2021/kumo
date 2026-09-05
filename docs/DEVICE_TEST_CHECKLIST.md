# Device acceptance checklist (not yet executed)

- Build debug, run unit tests, run lint and run connected renderer tests.
- Launch on Android API 24 and a current Android version; include a physical target phone.
- Inspect Studio, Words, Schedule and every dialog in light/dark mode, at 100% and 200% system font scaling. Check no clipping, overlap, inaccessible controls or illegible text.
- Test tall/small phones and a tablet/foldable; check scrolling, bottom/navigation insets, and rotation.
- Apply default gradient; verify lock screen changes and home screen stays unchanged.
- Pick portrait, landscape and EXIF-rotated/mirrored JPEGs; compare exported image and wallpaper.
- Cancel photo/export pickers. Try corrupt/unsupported/over-40MB images; verify a helpful message and preserved original.
- Check long katakana, mixed kanji/kana, max font size and top/bottom positions; avoid clock/fingerprint areas.
- Search Japanese and English, try an empty result and each category. Check selected word and Next wraparound.
- Export PNG; inspect saved dimensions and verify no preview clock is embedded.
- Enable daily rotation, restart/reboot and inspect WorkManager with Android Studio Background Task Inspector.
- Confirm delayed work advances a word, uses the latest original photo/settings, and reports failures.
- Disable rotation, then verify no subsequent work changes the wallpaper. Test battery restrictions/force-stop behavior.
- Try a restricted/work profile and verify graceful wallpaper-policy errors.
- Repeatedly import/export/apply images; inspect memory on a low-RAM device.
- Test screen reader labels, focus order, radio choices and touch targets.
- Review dependency licenses, vocabulary accuracy, target SDK policy and release signing before distribution.
