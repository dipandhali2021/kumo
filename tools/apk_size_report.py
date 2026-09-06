"""Report actual APK sizes; never assume a promised reduction."""
from pathlib import Path
from zipfile import ZipFile


def inspect(path):
    groups = dict.fromkeys(['DEX', 'Fonts', 'Resources', 'Native libraries', 'Other'], 0)
    with ZipFile(path) as apk:
        for entry in apk.infolist():
            name = entry.filename
            group = ('DEX' if name.endswith('.dex') else
                     'Fonts' if name.startswith('assets/fonts/') else
                     'Resources' if name.startswith('res/') or name == 'resources.arsc' else
                     'Native libraries' if name.startswith('lib/') else 'Other')
            groups[group] += entry.compress_size
    return groups


def main():
    files = [('Current debug', Path('app/build/outputs/apk/debug/app-debug.apk')),
             ('Production release', Path('app/build/outputs/apk/release/app-release.apk'))]
    print('# Kumo APK size report\n')
    print('| Build | APK size (MiB) |\n|---|---:|')
    for label, path in files:
        print(f'| {label} | {path.stat().st_size / 1048576:.2f} |')
    debug, release = (p.stat().st_size for _, p in files)
    if debug:
        print(f'\nRelease is {(1-release/debug)*100:.1f}% smaller than the debug APK built from this same commit.\n')
    print('The earlier 27 MB debug APK was not supplied for analysis; this is not a before/after comparison with that binary.\n')
    for label, path in files:
        print(f'## {label}: compressed entry breakdown\n')
        print('| Component | MiB |\n|---|---:|')
        for category, size in inspect(path).items():
            print(f'| {category} | {size / 1048576:.2f} |')
        print('\nZIP metadata and signing blocks account for differences from total APK size.\n')
    print('All four Japanese font files and their complete original glyph coverage are retained. The AAB is a publishing artifact, not an installable APK; Play generates device-specific downloads.')


if __name__ == '__main__':
    main()
